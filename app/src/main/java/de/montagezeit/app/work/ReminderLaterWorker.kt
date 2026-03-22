package de.montagezeit.app.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.preferences.ReminderSettings
import de.montagezeit.app.data.preferences.ReminderSettingsManager
import de.montagezeit.app.notification.ReminderActions
import de.montagezeit.app.notification.ReminderNotificationManager
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * Worker für "Später erinnern" - zeigt Reminder-Notification nach X Stunden
 */
@HiltWorker
class ReminderLaterWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val notificationManager: ReminderNotificationManager,
    private val workEntryDao: WorkEntryDao,
    private val reminderSettingsManager: ReminderSettingsManager
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        val dateStr = inputData.getString("date")
        val date = dateStr?.let { LocalDate.parse(it) } ?: LocalDate.now()
        val reminderTypeRaw = inputData.getString(ReminderActions.EXTRA_REMINDER_TYPE)
        val reminderType = reminderTypeRaw?.let { runCatching { ReminderType.valueOf(it) }.getOrNull() }
        
        return try {
            val settings = reminderSettingsManager.settings.first()
            if (!shouldShowReminder(date, reminderType, workEntryDao, settings)) {
                return Result.success()
            }

            when (reminderType) {
                ReminderType.MORNING -> notificationManager.showMorningReminder(date)
                ReminderType.EVENING -> notificationManager.showEveningReminder(date)
                ReminderType.FALLBACK -> notificationManager.showFallbackReminder(date)
                ReminderType.DAILY -> notificationManager.showDailyConfirmationNotification(date)
                null -> return Result.success()
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        internal suspend fun shouldShowReminder(
            date: LocalDate,
            reminderType: ReminderType?,
            workEntryDao: WorkEntryDao,
            settings: ReminderSettings
        ): Boolean {
            val entry = workEntryDao.getByDate(date)
            val isAutoNonWorkingDay = entry == null && ReminderWindowEvaluator.isNonWorkingDay(date, settings, workEntryDao)

            return when (reminderType) {
                ReminderType.MORNING -> !isAutoNonWorkingDay && WindowCheckWorker.shouldShowMorningReminder(entry)
                ReminderType.EVENING -> !isAutoNonWorkingDay && WindowCheckWorker.shouldShowEveningReminder(entry)
                ReminderType.FALLBACK -> !isAutoNonWorkingDay && WindowCheckWorker.shouldShowFallbackReminder(entry)
                ReminderType.DAILY -> !isAutoNonWorkingDay && WindowCheckWorker.shouldShowDailyReminder(entry)
                null -> false
            }
        }
    }
}
