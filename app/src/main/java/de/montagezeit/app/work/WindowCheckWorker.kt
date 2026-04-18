package de.montagezeit.app.work

import android.database.sqlite.SQLiteException
import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.preferences.ReminderFlagsStore
import de.montagezeit.app.data.preferences.ReminderSettings
import de.montagezeit.app.data.preferences.ReminderSettingsManager
import de.montagezeit.app.diagnostics.AppDiagnosticsRuntime
import de.montagezeit.app.diagnostics.DiagnosticCategory
import de.montagezeit.app.diagnostics.DiagnosticTrace
import de.montagezeit.app.diagnostics.DiagnosticTraceRequest
import de.montagezeit.app.diagnostics.DiagnosticWarningCodes
import de.montagezeit.app.diagnostics.toSanitizedDiagnosticPayload
import de.montagezeit.app.domain.usecase.EntryStatusResolver
import de.montagezeit.app.notification.ReminderNotificationManager
import java.io.IOException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.LocalDate
import java.time.ZonedDateTime

/**
 * WorkManager Worker für Window-Check
 *
 * Prüft ob Reminder notwendig sind:
 * - Morning Window (06:00-13:00): erinnert solange bis morning snapshot gesetzt oder dayType != WORK
 * - Evening Window (16:00-22:30): analog
 * - Fallback 22:30: wenn Tag unvollständig, 1x Reminder
 * - Daily: tägliche Erinnerung
 */
@HiltWorker
class WindowCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val workEntryDao: WorkEntryDao,
    private val reminderSettingsManager: ReminderSettingsManager,
    private val notificationManager: ReminderNotificationManager,
    private val reminderFlagsStore: ReminderFlagsStore,
    private val clock: Clock
) : CoroutineWorker(context, workerParams) {

    // Mutex für atomare Operations (SharedPreferences + DB + Notification)
    private val operationMutex = Mutex()

    override suspend fun doWork(): Result {
        val now = ZonedDateTime.now(clock)
        val today = now.toLocalDate()
        val currentTime = now.toLocalTime()
        val settings = reminderSettingsManager.settings.first()
        val reminderType = reminderTypeOrNull()
        val trace = AppDiagnosticsRuntime.startTrace(
            DiagnosticTraceRequest(
                category = DiagnosticCategory.REMINDER_DECISION,
                name = "window_check_worker",
                sourceClass = "WindowCheckWorker",
                screenOrWorker = "WorkManager",
                entityDate = today,
                payload = mapOf(
                    "reminderType" to reminderType?.name,
                    "currentTime" to currentTime.toString(),
                    "settings" to settings.toDiagnosticPayload()
                )
            )
        )

        try {
            val result = when (reminderType) {
                ReminderType.MORNING -> {
                    if (!settings.morningReminderEnabled) {
                        trace.event("morning_worker_skipped", payload = mapOf("reason" to "disabled"))
                        Result.success()
                    } else {
                        val inWindow = ReminderWindowEvaluator.isInMorningWindow(currentTime, settings)
                        if (!inWindow) {
                            trace.event(
                                "morning_worker_skipped",
                                payload = mapOf("reason" to "outside_window")
                            )
                            Result.success()
                        } else {
                            checkAndShowMorningReminder(today, settings, trace)
                            Result.success()
                        }
                    }
                }
                ReminderType.EVENING -> {
                    if (!settings.eveningReminderEnabled) {
                        trace.event("evening_worker_skipped", payload = mapOf("reason" to "disabled"))
                        Result.success()
                    } else {
                        val inWindow = ReminderWindowEvaluator.isInEveningWindow(currentTime, settings)
                        if (!inWindow) {
                            trace.event(
                                "evening_worker_skipped",
                                payload = mapOf("reason" to "outside_window")
                            )
                            Result.success()
                        } else {
                            checkAndShowEveningReminder(today, settings, trace)
                            Result.success()
                        }
                    }
                }
                ReminderType.FALLBACK -> {
                    if (!settings.fallbackEnabled) {
                        trace.event("fallback_worker_skipped", payload = mapOf("reason" to "disabled"))
                        Result.success()
                    } else if (!ReminderWindowEvaluator.isAfterFallbackTime(currentTime, settings)) {
                        trace.event("fallback_worker_skipped", payload = mapOf("reason" to "before_time"))
                        Result.success()
                    } else {
                        checkAndShowFallbackReminder(today, settings, trace)
                        Result.success()
                    }
                }
                ReminderType.DAILY -> {
                    if (!settings.dailyReminderEnabled) {
                        trace.event("daily_worker_skipped", payload = mapOf("reason" to "disabled"))
                        Result.success()
                    } else if (!ReminderWindowEvaluator.isAfterDailyReminderTime(currentTime, settings)) {
                        trace.event("daily_worker_skipped", payload = mapOf("reason" to "before_time"))
                        Result.success()
                    } else {
                        checkAndShowDailyReminder(today, settings, trace)
                        Result.success()
                    }
                }
                null -> {
                    // Legacy worker ohne reminder_type: no-op, um Doppeltrigger mit
                    // den dedizierten Morning/Evening/Fallback/Daily Workern zu vermeiden.
                    trace.event("legacy_worker_skipped", payload = mapOf("reason" to "missing_type"))
                    Result.success()
                }
            }

            trace.finish(payload = mapOf("result" to "success"))
            return result
        } catch (e: CancellationException) {
            trace.finish(status = de.montagezeit.app.diagnostics.DiagnosticStatus.CANCELLED)
            throw e
        } catch (e: Exception) {
            trace.error("window_check_failed", throwable = e)
            trace.finish(status = de.montagezeit.app.diagnostics.DiagnosticStatus.ERROR)
            android.util.Log.w("WindowCheckWorker", "doWork failed", e)
            return if (shouldRetryOn(e)) Result.retry() else Result.failure()
        }
    }

    /**
     * Prüft ob Morning Reminder nötig ist und zeigt ihn an
     *
     * ATOMIC: Mutex schützt gesamte Operation (read + DB query + notification + write)
     */
    private suspend fun checkAndShowMorningReminder(
        date: LocalDate,
        settings: ReminderSettings,
        trace: DiagnosticTrace? = null
    ) {
        // Atomic Operation: Read flag + DB query + Show notification + Write flag
        operationMutex.withLock {
            if (reminderFlagsStore.isMorningReminded(date)) {
                trace?.warning(
                    DiagnosticWarningCodes.REMINDER_SUPPRESSED_ALREADY_FLAGGED,
                    payload = mapOf("type" to ReminderType.MORNING.name)
                )
                return
            }
            val entry = workEntryDao.getByDate(date)
            if (entry == null && ReminderWindowEvaluator.isNonWorkingDay(date, settings, workEntryDao)) {
                trace?.warning(
                    DiagnosticWarningCodes.REMINDER_SUPPRESSED_AUTO_OFF,
                    payload = mapOf("type" to ReminderType.MORNING.name)
                )
                return
            }
            if (shouldShowMorningReminder(entry)) {
                notificationManager.showMorningReminder(date)
                reminderFlagsStore.setMorningReminded(date)
                trace?.event(
                    name = "morning_reminder_shown",
                    payload = mapOf("entry" to entry?.toSanitizedDiagnosticPayload())
                )
            } else {
                trace?.event(
                    name = "morning_reminder_suppressed",
                    payload = mapOf("entry" to entry?.toSanitizedDiagnosticPayload())
                )
            }
        }
    }

    /**
     * Prüft ob Evening Reminder nötig ist und zeigt ihn an
     *
     * ATOMIC: Mutex schützt gesamte Operation (read + DB query + notification + write)
     */
    private suspend fun checkAndShowEveningReminder(
        date: LocalDate,
        settings: ReminderSettings,
        trace: DiagnosticTrace? = null
    ) {
        operationMutex.withLock {
            if (reminderFlagsStore.isEveningReminded(date)) {
                trace?.warning(
                    DiagnosticWarningCodes.REMINDER_SUPPRESSED_ALREADY_FLAGGED,
                    payload = mapOf("type" to ReminderType.EVENING.name)
                )
                return
            }
            val entry = workEntryDao.getByDate(date)
            if (entry == null && ReminderWindowEvaluator.isNonWorkingDay(date, settings, workEntryDao)) {
                trace?.warning(
                    DiagnosticWarningCodes.REMINDER_SUPPRESSED_AUTO_OFF,
                    payload = mapOf("type" to ReminderType.EVENING.name)
                )
                return
            }
            if (shouldShowEveningReminder(entry)) {
                notificationManager.showEveningReminder(date)
                reminderFlagsStore.setEveningReminded(date)
                trace?.event(
                    name = "evening_reminder_shown",
                    payload = mapOf("entry" to entry?.toSanitizedDiagnosticPayload())
                )
            } else {
                trace?.event(
                    name = "evening_reminder_suppressed",
                    payload = mapOf("entry" to entry?.toSanitizedDiagnosticPayload())
                )
            }
        }
    }

    /**
     * Prüft ob Fallback Reminder nötig ist und zeigt ihn an
     *
     * ATOMIC: Mutex schützt gesamte Operation (read + DB query + notification + write)
     */
    private suspend fun checkAndShowFallbackReminder(
        date: LocalDate,
        settings: ReminderSettings,
        trace: DiagnosticTrace? = null
    ) {
        operationMutex.withLock {
            if (reminderFlagsStore.isFallbackReminded(date)) {
                trace?.warning(
                    DiagnosticWarningCodes.REMINDER_SUPPRESSED_ALREADY_FLAGGED,
                    payload = mapOf("type" to ReminderType.FALLBACK.name)
                )
                return
            }
            val entry = workEntryDao.getByDate(date)
            if (entry == null && ReminderWindowEvaluator.isNonWorkingDay(date, settings, workEntryDao)) {
                trace?.warning(
                    DiagnosticWarningCodes.REMINDER_SUPPRESSED_AUTO_OFF,
                    payload = mapOf("type" to ReminderType.FALLBACK.name)
                )
                return
            }
            if (shouldShowFallbackReminder(entry)) {
                notificationManager.showFallbackReminder(date)
                reminderFlagsStore.setFallbackReminded(date)
                trace?.event(
                    name = "fallback_reminder_shown",
                    payload = mapOf("entry" to entry?.toSanitizedDiagnosticPayload())
                )
            } else {
                trace?.event(
                    name = "fallback_reminder_suppressed",
                    payload = mapOf("entry" to entry?.toSanitizedDiagnosticPayload())
                )
            }
        }
    }

    /**
     * Prüft ob Daily Reminder nötig ist und zeigt ihn an
     *
     * ATOMIC: Mutex schützt gesamte Operation (read + DB query + notification + write)
     */
    private suspend fun checkAndShowDailyReminder(
        date: LocalDate,
        settings: ReminderSettings,
        trace: DiagnosticTrace? = null
    ) {
        operationMutex.withLock {
            if (reminderFlagsStore.isDailyReminded(date)) {
                trace?.warning(
                    DiagnosticWarningCodes.REMINDER_SUPPRESSED_ALREADY_FLAGGED,
                    payload = mapOf("type" to ReminderType.DAILY.name)
                )
                return
            }
            val entry = workEntryDao.getByDateWithTravel(date)
            val isAutoNonWorkingDay = entry == null && ReminderWindowEvaluator.isNonWorkingDay(date, settings, workEntryDao)
            if (isAutoNonWorkingDay) {
                reminderFlagsStore.setDailyReminded(date)
                trace?.warning(
                    DiagnosticWarningCodes.REMINDER_SUPPRESSED_AUTO_OFF,
                    payload = mapOf("type" to ReminderType.DAILY.name)
                )
                return
            }
            if (shouldShowDailyReminderWithTravel(entry)) {
                notificationManager.showDailyConfirmationNotification(date)
                reminderFlagsStore.setDailyReminded(date)
                trace?.event(
                    name = "daily_reminder_shown",
                    payload = mapOf("entry" to entry?.toSanitizedDiagnosticPayload())
                )
                return
            }
            if (isDailyReminderTerminalWithTravel(entry)) {
                reminderFlagsStore.setDailyReminded(date)
                trace?.event(
                    name = "daily_reminder_marked_terminal",
                    payload = mapOf("entry" to entry?.toSanitizedDiagnosticPayload())
                )
                return
            }
            trace?.event(
                name = "daily_reminder_suppressed",
                payload = mapOf("entry" to entry?.toSanitizedDiagnosticPayload())
            )
        }
    }

    private fun reminderTypeOrNull(): ReminderType? {
        val rawType = inputData.getString(KEY_REMINDER_TYPE) ?: return null
        return runCatching { ReminderType.valueOf(rawType) }.getOrNull()
    }

    companion object {
        const val KEY_REMINDER_TYPE = "reminder_type"

        internal fun shouldShowMorningReminder(entry: WorkEntry?): Boolean {
            if (entry?.confirmedWorkDay == true) return false
            return entry == null || (entry.dayType == DayType.WORK && entry.morningCapturedAt == null)
        }

        internal fun shouldShowEveningReminder(entry: WorkEntry?): Boolean {
            if (entry?.confirmedWorkDay == true) return false
            return entry == null || (entry.dayType == DayType.WORK && entry.eveningCapturedAt == null)
        }

        internal fun shouldShowFallbackReminder(entry: WorkEntry?): Boolean {
            if (entry?.confirmedWorkDay == true) return false
            return entry == null || (
                entry.dayType == DayType.WORK &&
                    (entry.morningCapturedAt == null || entry.eveningCapturedAt == null)
                )
        }

        internal fun shouldShowDailyReminder(entry: WorkEntry?): Boolean {
            if (entry == null) return true
            return shouldShowDailyReminderWithTravel(
                de.montagezeit.app.data.local.entity.WorkEntryWithTravelLegs(
                    workEntry = entry,
                    travelLegs = emptyList()
                )
            )
        }

        internal fun shouldShowDailyReminderWithTravel(
            entry: de.montagezeit.app.data.local.entity.WorkEntryWithTravelLegs?
        ): Boolean {
            if (entry == null) return true
            return !EntryStatusResolver.resolve(entry).isReminderTerminal
        }

        internal fun isDailyReminderTerminal(entry: WorkEntry?): Boolean {
            if (entry == null) return false
            return isDailyReminderTerminalWithTravel(
                de.montagezeit.app.data.local.entity.WorkEntryWithTravelLegs(
                    workEntry = entry,
                    travelLegs = emptyList()
                )
            )
        }

        internal fun isDailyReminderTerminalWithTravel(
            entry: de.montagezeit.app.data.local.entity.WorkEntryWithTravelLegs?
        ): Boolean {
            if (entry == null) return false
            return EntryStatusResolver.resolve(entry).isReminderTerminal
        }

        internal fun shouldRetryOn(throwable: Throwable): Boolean {
            return throwable is IOException || throwable is SQLiteException
        }
    }
}

private fun ReminderSettings.toDiagnosticPayload(): Map<String, Any?> = mapOf(
    "morningReminderEnabled" to morningReminderEnabled,
    "eveningReminderEnabled" to eveningReminderEnabled,
    "fallbackEnabled" to fallbackEnabled,
    "dailyReminderEnabled" to dailyReminderEnabled,
    "morningWindowStart" to morningWindowStart.toString(),
    "morningWindowEnd" to morningWindowEnd.toString(),
    "eveningWindowStart" to eveningWindowStart.toString(),
    "eveningWindowEnd" to eveningWindowEnd.toString(),
    "fallbackTime" to fallbackTime.toString(),
    "dailyReminderTime" to dailyReminderTime.toString(),
    "autoOffWeekends" to autoOffWeekends,
    "autoOffHolidays" to autoOffHolidays,
    "holidayCount" to holidayDates.size
)
