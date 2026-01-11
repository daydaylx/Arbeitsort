package de.montagezeit.app.work

import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

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
    @ApplicationContext private val context: Context
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
        scheduleMorningWorker()
        scheduleEveningWorker()
        scheduleFallbackWorker()
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
    private suspend fun scheduleMorningWorker() {
        // Berechne Verzögerung bis 06:00 heute oder morgen
        val now = LocalTime.now()
        val morningWindowStart = LocalTime.of(6, 0)
        
        val initialDelay = if (now.isBefore(morningWindowStart)) {
            // Heute noch vor 06:00
            Duration.between(now, morningWindowStart)
        } else {
            // Heute schon nach 06:00 → morgen 06:00
            Duration.between(now, morningWindowStart).plusDays(1)
        }
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(false)
            .build()
        
        val workRequest = PeriodicWorkRequestBuilder<WindowCheckWorker>(
            repeatInterval = 2, TimeUnit.HOURS
        )
            .setInitialDelay(initialDelay.toMinutes(), TimeUnit.MINUTES)
            .setConstraints(constraints)
            .addTag("morning_reminder")
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            MORNING_WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
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
    private suspend fun scheduleEveningWorker() {
        // Berechne Verzögerung bis 16:00 heute oder morgen
        val now = LocalTime.now()
        val eveningWindowStart = LocalTime.of(16, 0)
        
        val initialDelay = if (now.isBefore(eveningWindowStart)) {
            // Heute noch vor 16:00
            Duration.between(now, eveningWindowStart)
        } else {
            // Heute schon nach 16:00 → morgen 16:00
            Duration.between(now, eveningWindowStart).plusDays(1)
        }
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(false)
            .build()
        
        val workRequest = PeriodicWorkRequestBuilder<WindowCheckWorker>(
            repeatInterval = 3, TimeUnit.HOURS
        )
            .setInitialDelay(initialDelay.toMinutes(), TimeUnit.MINUTES)
            .setConstraints(constraints)
            .addTag("evening_reminder")
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            EVENING_WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
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
    private suspend fun scheduleFallbackWorker() {
        // Berechne Verzögerung bis 22:30 heute oder morgen
        val now = LocalTime.now()
        val fallbackTime = LocalTime.of(22, 30)
        
        val initialDelay = if (now.isBefore(fallbackTime)) {
            // Heute noch vor 22:30
            Duration.between(now, fallbackTime)
        } else {
            // Heute schon nach 22:30 → morgen 22:30
            Duration.between(now, fallbackTime).plusDays(1)
        }
        
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
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            FALLBACK_WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }
}
