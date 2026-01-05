package de.montagezeit.app.work

import android.content.Context
import android.content.SharedPreferences
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.preferences.ReminderSettingsManager
import de.montagezeit.app.notification.ReminderNotificationManager
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime

/**
 * WorkManager Worker für Window-Check
 * 
 * Prüft regelmäßig ob Reminder notwendig sind:
 * - Morning Window (06:00-13:00): erinnert solange bis morning snapshot gesetzt oder dayType != WORK
 * - Evening Window (16:00-22:30): analog
 * - Fallback 22:30: wenn Tag unvollständig, 1x Reminder
 * 
 * Der Worker wird mehrfach im Fenster ausgeführt (deferrable scheduling über WorkManager)
 */
@HiltWorker
class WindowCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val workEntryDao: WorkEntryDao,
    private val reminderSettingsManager: ReminderSettingsManager,
    private val notificationManager: ReminderNotificationManager
) : CoroutineWorker(context, workerParams) {
    
    override suspend fun doWork(): Result {
        val today = LocalDate.now()
        val currentTime = LocalTime.now()
        val settings = reminderSettingsManager.settings.first()
        val sharedPreferences = applicationContext.getSharedPreferences("reminder_flags", Context.MODE_PRIVATE)
        
        try {
            // Prüfe Morning Reminder
            if (settings.morningReminderEnabled && isInMorningWindow(currentTime, settings)) {
                checkAndShowMorningReminder(today, settings, sharedPreferences)
            }
            
            // Prüfe Evening Reminder
            if (settings.eveningReminderEnabled && isInEveningWindow(currentTime, settings)) {
                checkAndShowEveningReminder(today, settings, sharedPreferences)
            }
            
            // Prüfe Fallback Reminder
            if (settings.fallbackEnabled && currentTime.isAfter(settings.fallbackTime)) {
                checkAndShowFallbackReminder(today, sharedPreferences)
            }
            
            return Result.success()
        } catch (e: Exception) {
            return Result.retry()
        }
    }
    
    /**
     * Prüft ob die aktuelle Zeit im Morning-Fenster liegt
     */
    private fun isInMorningWindow(currentTime: LocalTime, settings: de.montagezeit.app.data.preferences.ReminderSettings): Boolean {
        return !currentTime.isBefore(settings.morningWindowStart) && 
               currentTime.isBefore(settings.morningWindowEnd)
    }
    
    /**
     * Prüft ob die aktuelle Zeit im Evening-Fenster liegt
     */
    private fun isInEveningWindow(currentTime: LocalTime, settings: de.montagezeit.app.data.preferences.ReminderSettings): Boolean {
        return !currentTime.isBefore(settings.eveningWindowStart) && 
               currentTime.isBefore(settings.eveningWindowEnd)
    }
    
    /**
     * Prüft ob Morning Reminder nötig ist und zeigt ihn an
     */
    private suspend fun checkAndShowMorningReminder(
        date: LocalDate,
        settings: de.montagezeit.app.data.preferences.ReminderSettings,
        sharedPreferences: SharedPreferences
    ) {
        // Prüfe ob schon heute erinnert wurde
        val alreadyReminded = sharedPreferences.getBoolean("morning_reminded_$date", false)
        if (alreadyReminded) {
            return
        }
        
        // Hole heutigen WorkEntry
        val entry = workEntryDao.getByDate(date)
        
        // Prüfe ob Reminder nötig ist
        val needsReminder = entry == null || 
                           (entry.dayType == DayType.WORK && entry.morningCapturedAt == null)
        
        if (needsReminder) {
            notificationManager.showMorningReminder(date)
            
            // Setze Flag dass erinnert wurde
            sharedPreferences.edit()
                .putBoolean("morning_reminded_$date", true)
                .apply()
        }
    }
    
    /**
     * Prüft ob Evening Reminder nötig ist und zeigt ihn an
     */
    private suspend fun checkAndShowEveningReminder(
        date: LocalDate,
        settings: de.montagezeit.app.data.preferences.ReminderSettings,
        sharedPreferences: SharedPreferences
    ) {
        // Prüfe ob schon heute erinnert wurde
        val alreadyReminded = sharedPreferences.getBoolean("evening_reminded_$date", false)
        if (alreadyReminded) {
            return
        }
        
        // Hole heutigen WorkEntry
        val entry = workEntryDao.getByDate(date)
        
        // Prüfe ob Reminder nötig ist
        val needsReminder = entry == null || 
                           (entry.dayType == DayType.WORK && entry.eveningCapturedAt == null)
        
        if (needsReminder) {
            notificationManager.showEveningReminder(date)
            
            // Setze Flag dass erinnert wurde
            sharedPreferences.edit()
                .putBoolean("evening_reminded_$date", true)
                .apply()
        }
    }
    
    /**
     * Prüft ob Fallback Reminder nötig ist und zeigt ihn an
     */
    private suspend fun checkAndShowFallbackReminder(
        date: LocalDate,
        sharedPreferences: SharedPreferences
    ) {
        // Prüfe ob schon heute Fallback erinnert wurde
        val alreadyReminded = sharedPreferences.getBoolean("fallback_reminded_$date", false)
        if (alreadyReminded) {
            return
        }
        
        // Hole heutigen WorkEntry
        val entry = workEntryDao.getByDate(date)
        
        // Prüfe ob Tag unvollständig ist
        val isIncomplete = entry == null || 
                          (entry.dayType == DayType.WORK && 
                           (entry.morningCapturedAt == null || entry.eveningCapturedAt == null))
        
        if (isIncomplete) {
            notificationManager.showFallbackReminder(date)
            
            // Setze Flag dass erinnert wurde
            sharedPreferences.edit()
                .putBoolean("fallback_reminded_$date", true)
                .apply()
        }
    }
}
