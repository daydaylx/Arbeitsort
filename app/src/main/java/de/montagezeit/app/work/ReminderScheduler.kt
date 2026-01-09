package de.montagezeit.app.work

import android.content.Context
import androidx.work.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import de.montagezeit.app.data.preferences.ReminderSettings
import de.montagezeit.app.data.preferences.ReminderSettingsManager

/**
 * Scheduler für Reminder-Worker
 * 
 * Plant WindowCheckWorker mit UniqueWork für Reboot-Resilienz:
 * - Morning Worker: Läuft im Morning Window (06:00-13:00) alle 2 Stunden
 * - Evening Worker: Läuft im Evening Window (16:00-22:30) alle 3 Stunden
 * - Fallback Worker: Läuft nach 22:30 einmal
 * 
 * Verwendet PeriodicWorkRequest mit Fenster-Intervallen
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val reminderSettingsManager: ReminderSettingsManager
) {
    
    private val workManager = WorkManager.getInstance(context)
    
    companion object {
        private const val MORNING_WORK_NAME = "morning_reminder_work"
        private const val EVENING_WORK_NAME = "evening_reminder_work"
        private const val FALLBACK_WORK_NAME = "fallback_reminder_work"
    }
    
    /**
     * Plant alle Reminder-Worker
     */
    suspend fun scheduleAll() {
        val settings = reminderSettingsManager.settings.first()

        if (settings.morningReminderEnabled) {
            scheduleMorningWorker(settings)
        } else {
            workManager.cancelUniqueWork(MORNING_WORK_NAME)
        }

        if (settings.eveningReminderEnabled) {
            scheduleEveningWorker(settings)
        } else {
            workManager.cancelUniqueWork(EVENING_WORK_NAME)
        }

        if (settings.fallbackEnabled) {
            scheduleFallbackWorker(settings)
        } else {
            workManager.cancelUniqueWork(FALLBACK_WORK_NAME)
        }
    }
    
    /**
     * Cancel alle Reminder-Worker
     */
    fun cancelAll() {
        workManager.cancelUniqueWork(MORNING_WORK_NAME)
        workManager.cancelUniqueWork(EVENING_WORK_NAME)
        workManager.cancelUniqueWork(FALLBACK_WORK_NAME)
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
    private fun scheduleMorningWorker(settings: ReminderSettings) {
        // Berechne Verzögerung bis 06:00 heute oder morgen
        val now = LocalTime.now()
        val initialDelay = initialDelayForWindowStart(
            now = now,
            windowStart = settings.morningWindowStart,
            windowEnd = settings.morningWindowEnd
        )
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(false)
            .build()
        
        val workRequest = PeriodicWorkRequestBuilder<WindowCheckWorker>(
            repeatInterval = settings.morningCheckIntervalMinutes.toLong(),
            repeatIntervalTimeUnit = TimeUnit.MINUTES
        )
            .setInitialDelay(initialDelay.toMinutes(), TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setInputData(workDataOf(WindowCheckWorker.KEY_REMINDER_TYPE to ReminderType.MORNING.name))
            .addTag("morning_reminder")
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            MORNING_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
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
    private fun scheduleEveningWorker(settings: ReminderSettings) {
        // Berechne Verzögerung bis 16:00 heute oder morgen
        val now = LocalTime.now()
        val initialDelay = initialDelayForWindowStart(
            now = now,
            windowStart = settings.eveningWindowStart,
            windowEnd = settings.eveningWindowEnd
        )
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(false)
            .build()
        
        val workRequest = PeriodicWorkRequestBuilder<WindowCheckWorker>(
            repeatInterval = settings.eveningCheckIntervalMinutes.toLong(),
            repeatIntervalTimeUnit = TimeUnit.MINUTES
        )
            .setInitialDelay(initialDelay.toMinutes(), TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setInputData(workDataOf(WindowCheckWorker.KEY_REMINDER_TYPE to ReminderType.EVENING.name))
            .addTag("evening_reminder")
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            EVENING_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
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
    private fun scheduleFallbackWorker(settings: ReminderSettings) {
        // Berechne Verzögerung bis 22:30 heute oder morgen
        val now = LocalTime.now()
        val initialDelay = initialDelayForFallback(now, settings.fallbackTime)
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(false)
            .build()
        
        val workRequest = PeriodicWorkRequestBuilder<WindowCheckWorker>(
            repeatInterval = 1, TimeUnit.DAYS
        )
            .setInitialDelay(initialDelay.toMinutes(), TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setInputData(workDataOf(WindowCheckWorker.KEY_REMINDER_TYPE to ReminderType.FALLBACK.name))
            .addTag("fallback_reminder")
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            FALLBACK_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    private fun initialDelayForWindowStart(
        now: LocalTime,
        windowStart: LocalTime,
        windowEnd: LocalTime
    ): Duration {
        return when {
            now.isBefore(windowStart) -> Duration.between(now, windowStart)
            now.isBefore(windowEnd) -> Duration.ZERO
            else -> Duration.between(now, windowStart).plusDays(1)
        }
    }

    private fun initialDelayForFallback(now: LocalTime, fallbackTime: LocalTime): Duration {
        return if (now.isBefore(fallbackTime)) {
            Duration.between(now, fallbackTime)
        } else {
            Duration.between(now, fallbackTime).plusDays(1)
        }
    }
}
