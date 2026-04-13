package de.montagezeit.app.work

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
    private val reminderSettingsManager: ReminderSettingsManager,
    private val reminderWorkEnqueuer: ReminderWorkEnqueuer
) {

    companion object {
        internal const val MORNING_WORK_NAME = "morning_reminder_work"
        internal const val EVENING_WORK_NAME = "evening_reminder_work"
        internal const val FALLBACK_WORK_NAME = "fallback_reminder_work"
        internal const val DAILY_WORK_NAME = "daily_reminder_work"
        internal const val WINDOW_FLEX_MINUTES = 5L
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
        reminderWorkEnqueuer.cancel(MORNING_WORK_NAME)
        reminderWorkEnqueuer.cancel(EVENING_WORK_NAME)
        reminderWorkEnqueuer.cancel(FALLBACK_WORK_NAME)
        reminderWorkEnqueuer.cancel(DAILY_WORK_NAME)
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
            reminderWorkEnqueuer.cancel(MORNING_WORK_NAME)
            trace?.event(
                name = "schedule_morning_cancelled",
                payload = mapOf("reason" to "disabled")
            )
            return
        }

        val workSpec = buildMorningReminderWorkSpec(settings)
        reminderWorkEnqueuer.enqueue(workSpec)
        trace?.event(
            name = "schedule_morning_enqueued",
            payload = mapOf(
                "initialDelayMs" to workSpec.initialDelayMillis,
                "repeatIntervalMinutes" to workSpec.repeatInterval,
                "windowStart" to settings.morningWindowStart.toString(),
                "windowEnd" to settings.morningWindowEnd.toString()
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
            reminderWorkEnqueuer.cancel(EVENING_WORK_NAME)
            trace?.event(
                name = "schedule_evening_cancelled",
                payload = mapOf("reason" to "disabled")
            )
            return
        }

        val workSpec = buildEveningReminderWorkSpec(settings)
        reminderWorkEnqueuer.enqueue(workSpec)
        trace?.event(
            name = "schedule_evening_enqueued",
            payload = mapOf(
                "initialDelayMs" to workSpec.initialDelayMillis,
                "repeatIntervalMinutes" to workSpec.repeatInterval,
                "windowStart" to settings.eveningWindowStart.toString(),
                "windowEnd" to settings.eveningWindowEnd.toString()
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
            reminderWorkEnqueuer.cancel(FALLBACK_WORK_NAME)
            trace?.event(
                name = "schedule_fallback_cancelled",
                payload = mapOf("reason" to "disabled")
            )
            return
        }

        val workSpec = buildFallbackReminderWorkSpec(settings)
        reminderWorkEnqueuer.enqueue(workSpec)
        trace?.event(
            name = "schedule_fallback_enqueued",
            payload = mapOf(
                "initialDelayMs" to workSpec.initialDelayMillis,
                "fallbackTime" to settings.fallbackTime.toString()
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
            reminderWorkEnqueuer.cancel(DAILY_WORK_NAME)
            trace?.event(
                name = "schedule_daily_cancelled",
                payload = mapOf("reason" to "disabled")
            )
            return
        }

        val workSpec = buildDailyReminderWorkSpec(settings)
        reminderWorkEnqueuer.enqueue(workSpec)
        trace?.event(
            name = "schedule_daily_enqueued",
            payload = mapOf(
                "initialDelayMs" to workSpec.initialDelayMillis,
                "dailyTime" to settings.dailyReminderTime.toString()
            )
        )
    }

}

internal fun buildMorningReminderWorkSpec(
    settings: ReminderSettings,
    now: LocalTime = LocalTime.now()
): ReminderPeriodicWorkSpec {
    return ReminderPeriodicWorkSpec(
        uniqueWorkName = ReminderScheduler.MORNING_WORK_NAME,
        tag = "morning_reminder",
        reminderType = ReminderType.MORNING,
        initialDelayMillis = ReminderScheduleCalculator
            .delayToWindowStart(now, settings.morningWindowStart, settings.morningWindowEnd)
            .toMillis(),
        repeatInterval = ReminderScheduleCalculator.periodicIntervalMinutes(settings.morningCheckIntervalMinutes),
        repeatIntervalTimeUnit = TimeUnit.MINUTES,
        flexInterval = ReminderScheduler.WINDOW_FLEX_MINUTES,
        flexIntervalTimeUnit = TimeUnit.MINUTES
    )
}

internal fun buildEveningReminderWorkSpec(
    settings: ReminderSettings,
    now: LocalTime = LocalTime.now()
): ReminderPeriodicWorkSpec {
    return ReminderPeriodicWorkSpec(
        uniqueWorkName = ReminderScheduler.EVENING_WORK_NAME,
        tag = "evening_reminder",
        reminderType = ReminderType.EVENING,
        initialDelayMillis = ReminderScheduleCalculator
            .delayToWindowStart(now, settings.eveningWindowStart, settings.eveningWindowEnd)
            .toMillis(),
        repeatInterval = ReminderScheduleCalculator.periodicIntervalMinutes(settings.eveningCheckIntervalMinutes),
        repeatIntervalTimeUnit = TimeUnit.MINUTES,
        flexInterval = ReminderScheduler.WINDOW_FLEX_MINUTES,
        flexIntervalTimeUnit = TimeUnit.MINUTES
    )
}

internal fun buildFallbackReminderWorkSpec(
    settings: ReminderSettings,
    now: LocalTime = LocalTime.now()
): ReminderPeriodicWorkSpec {
    return ReminderPeriodicWorkSpec(
        uniqueWorkName = ReminderScheduler.FALLBACK_WORK_NAME,
        tag = "fallback_reminder",
        reminderType = ReminderType.FALLBACK,
        initialDelayMillis = ReminderScheduleCalculator.delayToTime(now, settings.fallbackTime).toMillis(),
        repeatInterval = 1,
        repeatIntervalTimeUnit = TimeUnit.DAYS
    )
}

internal fun buildDailyReminderWorkSpec(
    settings: ReminderSettings,
    now: LocalTime = LocalTime.now()
): ReminderPeriodicWorkSpec {
    return ReminderPeriodicWorkSpec(
        uniqueWorkName = ReminderScheduler.DAILY_WORK_NAME,
        tag = "daily_reminder",
        reminderType = ReminderType.DAILY,
        initialDelayMillis = ReminderScheduleCalculator.delayToTime(now, settings.dailyReminderTime).toMillis(),
        repeatInterval = 1,
        repeatIntervalTimeUnit = TimeUnit.DAYS
    )
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
