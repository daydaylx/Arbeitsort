package de.montagezeit.app.work

import android.content.Context
import androidx.work.*
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import de.montagezeit.app.data.preferences.ReminderSettingsManager
import de.montagezeit.app.data.preferences.ReminderSettings

/**
 * Scheduler für Reminder-Worker
 * 
 * Plant WindowCheckWorker mit UniqueWork für Reboot-Resilienz:
 * - Morning Worker: Läuft im Morning Window (06:00-13:00) alle 2 Stunden
 * - Evening Worker: Läuft im Evening Window (16:00-22:30) alle 3 Stunden
 * - Fallback Worker: Läuft nach 22:30 einmal
 * 
 * Verwendet PeriodicWorkRequest für tägliche Wiederholung
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
    }
    
    /**
     * Plant alle Reminder-Worker
     */
    suspend fun scheduleAll() {
        val settings = reminderSettingsManager.settings.first()
        scheduleMorningWorker(settings)
        scheduleEveningWorker(settings)
        scheduleFallbackWorker(settings)
        scheduleDailyWorker(settings)
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
     * - Startet um 06:00
     * - Wiederholt sich alle 2 Stunden (06:00, 08:00, 10:00, 12:00)
     * - Im Morning Window aktiv (06:00-13:00)
     * - Tägliche Wiederholung
     */
    private suspend fun scheduleMorningWorker(settings: ReminderSettings) {
        if (!settings.morningReminderEnabled) {
            workManager.cancelUniqueWork(MORNING_WORK_NAME)
            return
        }

        // Berechne Verzögerung bis Start des Morning-Windows
        val now = LocalTime.now()
        val morningWindowStart = settings.morningWindowStart
        val morningWindowEnd = settings.morningWindowEnd
        val initialDelay = delayToWindowStart(now, morningWindowStart, morningWindowEnd)
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(false)
            .build()

        val repeatMinutes = settings.morningCheckIntervalMinutes.coerceAtLeast(15)
        val workRequest = PeriodicWorkRequestBuilder<WindowCheckWorker>(
            repeatInterval = repeatMinutes.toLong(), TimeUnit.MINUTES
        )
            .setInitialDelay(initialDelay.toMinutes(), TimeUnit.MINUTES)
            .setConstraints(constraints)
            .addTag("morning_reminder")
            .setInputData(workDataOf(WindowCheckWorker.KEY_REMINDER_TYPE to ReminderType.MORNING.name))
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            MORNING_WORK_NAME,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            workRequest
        )
    }
    
    /**
     * Plant den Evening-Worker
     * 
     * Strategie:
     * - Startet um 16:00
     * - Wiederholt sich alle 3 Stunden (16:00, 19:00)
     * - Im Evening Window aktiv (16:00-22:30)
     * - Tägliche Wiederholung
     */
    private suspend fun scheduleEveningWorker(settings: ReminderSettings) {
        if (!settings.eveningReminderEnabled) {
            workManager.cancelUniqueWork(EVENING_WORK_NAME)
            return
        }

        // Berechne Verzögerung bis Start des Evening-Windows
        val now = LocalTime.now()
        val eveningWindowStart = settings.eveningWindowStart
        val eveningWindowEnd = settings.eveningWindowEnd
        val initialDelay = delayToWindowStart(now, eveningWindowStart, eveningWindowEnd)
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(false)
            .build()

        val repeatMinutes = settings.eveningCheckIntervalMinutes.coerceAtLeast(15)
        val workRequest = PeriodicWorkRequestBuilder<WindowCheckWorker>(
            repeatInterval = repeatMinutes.toLong(), TimeUnit.MINUTES
        )
            .setInitialDelay(initialDelay.toMinutes(), TimeUnit.MINUTES)
            .setConstraints(constraints)
            .addTag("evening_reminder")
            .setInputData(workDataOf(WindowCheckWorker.KEY_REMINDER_TYPE to ReminderType.EVENING.name))
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            EVENING_WORK_NAME,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            workRequest
        )
    }
    
    /**
     * Plant den Fallback-Worker
     * 
     * Strategie:
     * - Startet um 22:30
     * - Läuft einmal pro Tag
     * - Prüft ob Tag unvollständig ist
     * - Tägliche Wiederholung
     */
    private suspend fun scheduleFallbackWorker(settings: ReminderSettings) {
        if (!settings.fallbackEnabled) {
            workManager.cancelUniqueWork(FALLBACK_WORK_NAME)
            return
        }

        // Berechne Verzögerung bis Fallback-Zeit
        val now = LocalTime.now()
        val fallbackTime = settings.fallbackTime
        val initialDelay = delayToTime(now, fallbackTime)
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(false)
            .build()
        
        val workRequest = PeriodicWorkRequestBuilder<WindowCheckWorker>(
            repeatInterval = 1, TimeUnit.DAYS
        )
            .setInitialDelay(initialDelay.toMinutes(), TimeUnit.MINUTES)
            .setConstraints(constraints)
            .addTag("fallback_reminder")
            .setInputData(workDataOf(WindowCheckWorker.KEY_REMINDER_TYPE to ReminderType.FALLBACK.name))
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            FALLBACK_WORK_NAME,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            workRequest
        )
    }

    private suspend fun scheduleDailyWorker(settings: ReminderSettings) {
        if (!settings.dailyReminderEnabled) {
            workManager.cancelUniqueWork(DAILY_WORK_NAME)
            return
        }

        val now = LocalTime.now()
        val dailyTime = settings.dailyReminderTime
        val initialDelay = delayToTime(now, dailyTime)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(false)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<WindowCheckWorker>(
            repeatInterval = 1, TimeUnit.DAYS
        )
            .setInitialDelay(initialDelay.toMinutes(), TimeUnit.MINUTES)
            .setConstraints(constraints)
            .addTag("daily_reminder")
            .setInputData(workDataOf(WindowCheckWorker.KEY_REMINDER_TYPE to ReminderType.DAILY.name))
            .build()

        workManager.enqueueUniquePeriodicWork(
            DAILY_WORK_NAME,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            workRequest
        )
    }

    private fun delayToWindowStart(
        now: LocalTime,
        windowStart: LocalTime,
        windowEnd: LocalTime
    ): Duration {
        return when {
            now.isBefore(windowStart) -> Duration.between(now, windowStart)
            now.isAfter(windowEnd) -> Duration.between(now, windowStart).plusDays(1)
            else -> Duration.ZERO
        }
    }

    private fun delayToTime(now: LocalTime, target: LocalTime): Duration {
        return if (now.isBefore(target)) {
            Duration.between(now, target)
        } else {
            Duration.between(now, target).plusDays(1)
        }
    }
}
