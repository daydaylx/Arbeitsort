package de.montagezeit.app.work

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
import de.montagezeit.app.notification.ReminderNotificationManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.LocalTime

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
    private val reminderFlagsStore: ReminderFlagsStore
) : CoroutineWorker(context, workerParams) {

    // Mutex für atomare Operations (SharedPreferences + DB + Notification)
    private val operationMutex = Mutex()

    override suspend fun doWork(): Result {
        val today = LocalDate.now()
        val currentTime = LocalTime.now()
        val settings = reminderSettingsManager.settings.first()
        val reminderType = reminderTypeOrNull()

        try {
            when (reminderType) {
                ReminderType.MORNING -> {
                    if (!settings.morningReminderEnabled) {
                        return Result.success()
                    }
                    val inWindow = ReminderWindowEvaluator.isInMorningWindow(currentTime, settings)
                    if (!inWindow) {
                        return Result.success()
                    }
                    checkAndShowMorningReminder(today, settings)
                }
                ReminderType.EVENING -> {
                    if (!settings.eveningReminderEnabled) {
                        return Result.success()
                    }
                    val inWindow = ReminderWindowEvaluator.isInEveningWindow(currentTime, settings)
                    if (!inWindow) {
                        return Result.success()
                    }
                    checkAndShowEveningReminder(today, settings)
                }
                ReminderType.FALLBACK -> {
                    if (!settings.fallbackEnabled) {
                        return Result.success()
                    }
                    if (!ReminderWindowEvaluator.isAfterFallbackTime(currentTime, settings)) {
                        return Result.success()
                    }
                    checkAndShowFallbackReminder(today, settings)
                    // Fallback ist periodic, kein self-reschedule
                }
                ReminderType.DAILY -> {
                    if (!settings.dailyReminderEnabled) {
                        return Result.success()
                    }
                    checkAndShowDailyReminder(today)
                    // Daily ist periodic, kein self-reschedule
                }
                null -> {
                    // Legacy worker ohne reminder_type: no-op, um Doppeltrigger mit
                    // den dedizierten Morning/Evening/Fallback/Daily Workern zu vermeiden.
                    return Result.success()
                }
            }

            return Result.success()
        } catch (e: Exception) {
            return Result.retry()
        }
    }

    /**
     * Prüft ob Morning Reminder nötig ist und zeigt ihn an
     *
     * ATOMIC: Mutex schützt gesamte Operation (read + DB query + notification + write)
     */
    private suspend fun checkAndShowMorningReminder(
        date: LocalDate,
        settings: ReminderSettings
    ) {
        // Atomic Operation: Read flag + DB query + Show notification + Write flag
        operationMutex.withLock {
            if (reminderFlagsStore.isMorningReminded(date)) return
            val entry = workEntryDao.getByDate(date)
            if (entry == null && ReminderWindowEvaluator.isNonWorkingDay(date, settings, workEntryDao)) return
            if (shouldShowMorningReminder(entry)) {
                notificationManager.showMorningReminder(date)
                reminderFlagsStore.setMorningReminded(date)
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
        settings: ReminderSettings
    ) {
        operationMutex.withLock {
            if (reminderFlagsStore.isEveningReminded(date)) return
            val entry = workEntryDao.getByDate(date)
            if (entry == null && ReminderWindowEvaluator.isNonWorkingDay(date, settings, workEntryDao)) return
            if (shouldShowEveningReminder(entry)) {
                notificationManager.showEveningReminder(date)
                reminderFlagsStore.setEveningReminded(date)
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
        settings: ReminderSettings
    ) {
        operationMutex.withLock {
            if (reminderFlagsStore.isFallbackReminded(date)) return
            val entry = workEntryDao.getByDate(date)
            if (entry == null && ReminderWindowEvaluator.isNonWorkingDay(date, settings, workEntryDao)) return
            if (shouldShowFallbackReminder(entry)) {
                notificationManager.showFallbackReminder(date)
                reminderFlagsStore.setFallbackReminded(date)
            }
        }
    }

    /**
     * Prüft ob Daily Reminder nötig ist und zeigt ihn an
     *
     * ATOMIC: Mutex schützt gesamte Operation (read + DB query + notification + write)
     */
    private suspend fun checkAndShowDailyReminder(date: LocalDate) {
        operationMutex.withLock {
            if (reminderFlagsStore.isDailyReminded(date)) return
            val entry = workEntryDao.getByDate(date)
            reminderFlagsStore.setDailyReminded(date)
            if (shouldShowDailyReminder(entry)) {
                notificationManager.showDailyConfirmationNotification(date)
            }
        }
    }

    private fun reminderTypeOrNull(): ReminderType? {
        val rawType = inputData.getString(KEY_REMINDER_TYPE) ?: return null
        return runCatching { ReminderType.valueOf(rawType) }.getOrNull()
    }

    companion object {
        const val KEY_REMINDER_TYPE = "reminder_type"

        internal fun shouldShowMorningReminder(entry: WorkEntry?): Boolean {
            return entry == null || (entry.dayType == DayType.WORK && entry.morningCapturedAt == null)
        }

        internal fun shouldShowEveningReminder(entry: WorkEntry?): Boolean {
            return entry == null || (entry.dayType == DayType.WORK && entry.eveningCapturedAt == null)
        }

        internal fun shouldShowFallbackReminder(entry: WorkEntry?): Boolean {
            return entry == null || (
                entry.dayType == DayType.WORK &&
                    (entry.morningCapturedAt == null || entry.eveningCapturedAt == null)
                )
        }

        internal fun shouldShowDailyReminder(entry: WorkEntry?): Boolean {
            return entry?.confirmedWorkDay != true
        }
    }
}
