package de.montagezeit.app.work

import android.content.Context
import androidx.work.*
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import de.montagezeit.app.data.preferences.ReminderSettingsManager
import de.montagezeit.app.data.preferences.ReminderSettings
import de.montagezeit.app.diagnostics.AppDiagnosticsRuntime
import de.montagezeit.app.diagnostics.DiagnosticCategory
import de.montagezeit.app.diagnostics.DiagnosticTrace
import de.montagezeit.app.diagnostics.DiagnosticTraceRequest
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * Scheduler für Reminder-Worker
 *
 * Nutzt PeriodicWorkRequest für Window-Based Workers (morning, evening):
 * - Morning Worker: Startet am Anfang des Morning Windows, dann im konfigurierten Intervall
 * - Evening Worker: Startet am Anfang des Evening Windows, dann im konfigurierten Intervall
 * - Fallback Worker: Startet zur Fallback-Zeit (z.B. 22:30), periodic
 * - Daily Worker: Startet zur konfigurierten Zeit, periodic
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val reminderSettingsManager: ReminderSettingsManager
) {

    private val workManager by lazy { WorkManager.getInstance(context) }

    companion object {
        private const val MORNING_WORK_NAME = "morning_reminder_work"
        private const val EVENING_WORK_NAME = "evening_reminder_work"
        private const val FALLBACK_WORK_NAME = "fallback_reminder_work"
        private const val DAILY_WORK_NAME = "daily_reminder_work"
        private const val WINDOW_FLEX_MINUTES = 5L
    }

    /**
     * Plant alle Reminder-Worker
     */
    suspend fun scheduleAll() {
        val settings = reminderSettingsManager.settings.first()
        val trace = AppDiagnosticsRuntime.startTrace(
            DiagnosticTraceRequest(
                category = DiagnosticCategory.REMINDER_SCHEDULE,
                name = "schedule_all_reminders",
                sourceClass = "ReminderScheduler",
                screenOrWorker = "WorkManager",
                payload = settings.toDiagnosticPayload()
            )
        )
        try {
            scheduleMorningWorker(settings, trace)
            scheduleEveningWorker(settings, trace)
            scheduleFallbackWorker(settings, trace)
            scheduleDailyWorker(settings, trace)
            trace.finish(payload = mapOf("status" to "scheduled"))
        } catch (e: Exception) {
            trace.error("schedule_all_failed", throwable = e)
            trace.finish(status = de.montagezeit.app.diagnostics.DiagnosticStatus.ERROR)
            throw e
        }
    }

    /**
     * Cancel alle Reminder-Worker
     */
    fun cancelAll() {
        workManager.cancelUniqueWork(MORNING_WORK_NAME)
        workManager.cancelUniqueWork(EVENING_WORK_NAME)
        workManager.cancelUniqueWork(FALLBACK_WORK_NAME)
        workManager.cancelUniqueWork(DAILY_WORK_NAME)
    }

    /**
     * Plant den Morning-Worker
     *
     * Strategie:
     * - Startet am Anfang des Morning Windows (z.B. 06:00)
     * - Periodic (im konfigurierten Intervall, mindestens 15 Minuten)
     * - Keine Battery/Storage Constraints (Reminders sind kritisch)
     */
    private suspend fun scheduleMorningWorker(settings: ReminderSettings, trace: DiagnosticTrace? = null) {
        if (!settings.morningReminderEnabled) {
            workManager.cancelUniqueWork(MORNING_WORK_NAME)
            trace?.event(
                name = "schedule_morning_cancelled",
                payload = mapOf("reason" to "disabled")
            )
            return
        }

        // Berechne Verzögerung bis Start des Morning-Windows
        val now = LocalTime.now()
        val morningWindowStart = settings.morningWindowStart
        val morningWindowEnd = settings.morningWindowEnd
        val initialDelay = ReminderScheduleCalculator.delayToWindowStart(now, morningWindowStart, morningWindowEnd)
        val repeatIntervalMinutes = ReminderScheduleCalculator.periodicIntervalMinutes(
            settings.morningCheckIntervalMinutes
        )

        // Keine Constraints - Reminders müssen zuverlässig funktionieren
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<WindowCheckWorker>(
            repeatInterval = repeatIntervalMinutes,
            repeatIntervalTimeUnit = TimeUnit.MINUTES,
            flexTimeInterval = WINDOW_FLEX_MINUTES,
            flexTimeIntervalUnit = TimeUnit.MINUTES
        )
            .setInitialDelay(initialDelay.toMillis(), TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .addTag("morning_reminder")
            .setInputData(workDataOf(WindowCheckWorker.KEY_REMINDER_TYPE to ReminderType.MORNING.name))
            .build()

        workManager.enqueueUniquePeriodicWork(
            MORNING_WORK_NAME,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            workRequest
        )
        trace?.event(
            name = "schedule_morning_enqueued",
            payload = mapOf(
                "initialDelayMs" to initialDelay.toMillis(),
                "repeatIntervalMinutes" to repeatIntervalMinutes,
                "windowStart" to morningWindowStart.toString(),
                "windowEnd" to morningWindowEnd.toString()
            )
        )
    }

    /**
     * Plant den Evening-Worker
     *
     * Strategie:
     * - Startet am Anfang des Evening Windows (z.B. 18:00)
     * - Periodic (im konfigurierten Intervall, mindestens 15 Minuten)
     * - Keine Battery/Storage Constraints (Reminders sind kritisch)
     */
    private suspend fun scheduleEveningWorker(settings: ReminderSettings, trace: DiagnosticTrace? = null) {
        if (!settings.eveningReminderEnabled) {
            workManager.cancelUniqueWork(EVENING_WORK_NAME)
            trace?.event(
                name = "schedule_evening_cancelled",
                payload = mapOf("reason" to "disabled")
            )
            return
        }

        // Berechne Verzögerung bis Start des Evening-Windows
        val now = LocalTime.now()
        val eveningWindowStart = settings.eveningWindowStart
        val eveningWindowEnd = settings.eveningWindowEnd
        val initialDelay = ReminderScheduleCalculator.delayToWindowStart(now, eveningWindowStart, eveningWindowEnd)
        val repeatIntervalMinutes = ReminderScheduleCalculator.periodicIntervalMinutes(
            settings.eveningCheckIntervalMinutes
        )

        // Keine Constraints - Reminders müssen zuverlässig funktionieren
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<WindowCheckWorker>(
            repeatInterval = repeatIntervalMinutes,
            repeatIntervalTimeUnit = TimeUnit.MINUTES,
            flexTimeInterval = WINDOW_FLEX_MINUTES,
            flexTimeIntervalUnit = TimeUnit.MINUTES
        )
            .setInitialDelay(initialDelay.toMillis(), TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .addTag("evening_reminder")
            .setInputData(workDataOf(WindowCheckWorker.KEY_REMINDER_TYPE to ReminderType.EVENING.name))
            .build()

        workManager.enqueueUniquePeriodicWork(
            EVENING_WORK_NAME,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            workRequest
        )
        trace?.event(
            name = "schedule_evening_enqueued",
            payload = mapOf(
                "initialDelayMs" to initialDelay.toMillis(),
                "repeatIntervalMinutes" to repeatIntervalMinutes,
                "windowStart" to eveningWindowStart.toString(),
                "windowEnd" to eveningWindowEnd.toString()
            )
        )
    }

    /**
     * Plant den Fallback-Worker
     *
     * Strategie:
     * - Startet zur Fallback-Zeit (z.B. 22:30)
     * - Periodic (einmal pro Tag)
     * - Keine Battery/Storage Constraints (Reminders sind kritisch)
     */
    private suspend fun scheduleFallbackWorker(settings: ReminderSettings, trace: DiagnosticTrace? = null) {
        if (!settings.fallbackEnabled) {
            workManager.cancelUniqueWork(FALLBACK_WORK_NAME)
            trace?.event(
                name = "schedule_fallback_cancelled",
                payload = mapOf("reason" to "disabled")
            )
            return
        }

        // Berechne Verzögerung bis Fallback-Zeit
        val now = LocalTime.now()
        val fallbackTime = settings.fallbackTime
        val initialDelay = ReminderScheduleCalculator.delayToTime(now, fallbackTime)

        // Keine Constraints - Reminders müssen zuverlässig funktionieren
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<WindowCheckWorker>(
            repeatInterval = 1, TimeUnit.DAYS
        )
            .setInitialDelay(initialDelay.toMillis(), TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .addTag("fallback_reminder")
            .setInputData(workDataOf(WindowCheckWorker.KEY_REMINDER_TYPE to ReminderType.FALLBACK.name))
            .build()

        workManager.enqueueUniquePeriodicWork(
            FALLBACK_WORK_NAME,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            workRequest
        )
        trace?.event(
            name = "schedule_fallback_enqueued",
            payload = mapOf(
                "initialDelayMs" to initialDelay.toMillis(),
                "fallbackTime" to fallbackTime.toString()
            )
        )
    }

    /**
     * Plant den Daily-Worker
     *
     * Strategie:
     * - Startet zur konfigurierten Zeit
     * - Periodic (einmal pro Tag)
     * - Keine Battery/Storage Constraints (Reminders sind kritisch)
     */
    private suspend fun scheduleDailyWorker(settings: ReminderSettings, trace: DiagnosticTrace? = null) {
        if (!settings.dailyReminderEnabled) {
            workManager.cancelUniqueWork(DAILY_WORK_NAME)
            trace?.event(
                name = "schedule_daily_cancelled",
                payload = mapOf("reason" to "disabled")
            )
            return
        }

        val now = LocalTime.now()
        val dailyTime = settings.dailyReminderTime
        val initialDelay = ReminderScheduleCalculator.delayToTime(now, dailyTime)

        // Keine Constraints - Reminders müssen zuverlässig funktionieren
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<WindowCheckWorker>(
            repeatInterval = 1, TimeUnit.DAYS
        )
            .setInitialDelay(initialDelay.toMillis(), TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .addTag("daily_reminder")
            .setInputData(workDataOf(WindowCheckWorker.KEY_REMINDER_TYPE to ReminderType.DAILY.name))
            .build()

        workManager.enqueueUniquePeriodicWork(
            DAILY_WORK_NAME,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            workRequest
        )
        trace?.event(
            name = "schedule_daily_enqueued",
            payload = mapOf(
                "initialDelayMs" to initialDelay.toMillis(),
                "dailyTime" to dailyTime.toString()
            )
        )
    }

}

private fun ReminderSettings.toDiagnosticPayload(): Map<String, Any?> = mapOf(
    "morningReminderEnabled" to morningReminderEnabled,
    "morningWindowStart" to morningWindowStart.toString(),
    "morningWindowEnd" to morningWindowEnd.toString(),
    "morningCheckIntervalMinutes" to morningCheckIntervalMinutes,
    "eveningReminderEnabled" to eveningReminderEnabled,
    "eveningWindowStart" to eveningWindowStart.toString(),
    "eveningWindowEnd" to eveningWindowEnd.toString(),
    "eveningCheckIntervalMinutes" to eveningCheckIntervalMinutes,
    "fallbackEnabled" to fallbackEnabled,
    "fallbackTime" to fallbackTime.toString(),
    "dailyReminderEnabled" to dailyReminderEnabled,
    "dailyReminderTime" to dailyReminderTime.toString(),
    "autoOffWeekends" to autoOffWeekends,
    "autoOffHolidays" to autoOffHolidays,
    "holidayCount" to holidayDates.size
)
