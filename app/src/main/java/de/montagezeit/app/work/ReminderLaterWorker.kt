package de.montagezeit.app.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.notification.ReminderActions
import de.montagezeit.app.notification.ReminderNotificationManager
import java.time.LocalDate

/**
 * Worker für "Später erinnern" - zeigt Reminder-Notification nach X Stunden
 */
@HiltWorker
class ReminderLaterWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val notificationManager: ReminderNotificationManager,
    private val workEntryDao: WorkEntryDao
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        val dateStr = inputData.getString("date")
        val date = dateStr?.let { LocalDate.parse(it) } ?: LocalDate.now()
        val reminderTypeRaw = inputData.getString(ReminderActions.EXTRA_REMINDER_TYPE)
        val reminderType = reminderTypeRaw?.let { runCatching { ReminderType.valueOf(it) }.getOrNull() }

        if (reminderType == ReminderType.DAILY) {
            val entry = workEntryDao.getByDate(date)
            if (entry?.confirmedWorkDay == true) {
                return Result.success()
            }
        }
        
        return try {
            when (reminderType) {
                ReminderType.MORNING -> notificationManager.showMorningReminder(date)
                ReminderType.EVENING -> notificationManager.showEveningReminder(date)
                ReminderType.FALLBACK -> notificationManager.showFallbackReminder(date)
                ReminderType.DAILY -> notificationManager.showDailyConfirmationNotification(date)
                null -> notificationManager.showMorningReminder(date)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
