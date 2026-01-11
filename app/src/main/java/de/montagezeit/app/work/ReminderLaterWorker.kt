package de.montagezeit.app.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import de.montagezeit.app.notification.ReminderNotificationManager
import java.time.LocalDate

/**
 * Worker für "Später erinnern" - zeigt Reminder-Notification nach X Stunden
 */
@HiltWorker
class ReminderLaterWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val notificationManager: ReminderNotificationManager
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        val dateStr = inputData.getString("date")
        val date = dateStr?.let { LocalDate.parse(it) } ?: LocalDate.now()
        
        // Bestimme, ob es ein Morning- oder Evening-Reminder war
        // (vereinfacht: prüfe ob es vor 13:00 ist für Morning, sonst Evening)
        val currentHour = java.time.LocalTime.now().hour
        
        return try {
            if (currentHour < 13) {
                notificationManager.showMorningReminder(date)
            } else {
                notificationManager.showEveningReminder(date)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

