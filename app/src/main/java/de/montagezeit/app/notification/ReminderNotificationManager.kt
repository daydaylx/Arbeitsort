package de.montagezeit.app.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import de.montagezeit.app.MainActivity
import de.montagezeit.app.R
import de.montagezeit.app.handler.CheckInActionService
import de.montagezeit.app.work.ReminderType
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Konstanten für Notification Actions
 */
object ReminderActions {
    // Morning Actions
    const val ACTION_MORNING_CHECK_IN_WITH_LOCATION = "action_morning_check_in_with_location"
    const val ACTION_MORNING_CHECK_IN_WITHOUT_LOCATION = "action_morning_check_in_without_location"
    
    // Evening Actions
    const val ACTION_EVENING_CHECK_IN_WITH_LOCATION = "action_evening_check_in_with_location"
    const val ACTION_EVENING_CHECK_IN_WITHOUT_LOCATION = "action_evening_check_in_without_location"
    
    // Daily Confirmation Actions
    const val ACTION_CONFIRM_WORK_DAY = "action_confirm_work_day"
    const val ACTION_CONFIRM_OFF_DAY = "action_confirm_off_day"
    const val ACTION_REMIND_LATER_CONFIRMATION = "action_remind_later_confirmation"
    
    // Common Actions
    const val ACTION_EDIT_ENTRY = "action_edit_entry"
    const val ACTION_REMIND_LATER = "action_remind_later"
    const val ACTION_MARK_DAY_OFF = "action_mark_day_off"
    
    // Extras
    const val EXTRA_DATE = "extra_date"
    const val EXTRA_ACTION_TYPE = "extra_action_type"
    const val EXTRA_HOURS_LATER = "extra_hours_later"
    const val EXTRA_REMINDER_TYPE = "extra_reminder_type"
    const val EXTRA_CONFIRMATION_SOURCE = "extra_confirmation_source"
}

/**
 * Konstanten für Notification IDs
 */
object ReminderNotificationIds {
    const val MORNING_REMINDER = 1001
    const val EVENING_REMINDER = 1002
    const val FALLBACK_REMINDER = 1003
    const val DAILY_REMINDER = 1004
}

/**
 * Manager für Reminder-Notifications
 */
@Singleton
class ReminderNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    companion object {
        private const val CHANNEL_ID = "reminder_notifications"
        private const val CHANNEL_NAME = "Check-in Erinnerungen"
        private const val CHANNEL_DESCRIPTION = "Erinnerungen für morgendlichen und abendlichen Check-in"
        private const val NOTIFICATION_GROUP = "reminder_group"
        
        // Request Codes für PendingIntents (müssen einzigartig sein)
        private const val REQUEST_CODE_MORNING_WITH_LOCATION = 2001
        private const val REQUEST_CODE_MORNING_WITHOUT_LOCATION = 2002
        private const val REQUEST_CODE_EVENING_WITH_LOCATION = 2003
        private const val REQUEST_CODE_EVENING_WITHOUT_LOCATION = 2004
        private const val REQUEST_CODE_EDIT = 2005
        private const val REQUEST_CODE_REMIND_LATER_1H_MORNING = 2006
        private const val REQUEST_CODE_REMIND_LATER_2H_MORNING = 2007
        private const val REQUEST_CODE_REMIND_LATER_1H_EVENING = 2015
        private const val REQUEST_CODE_REMIND_LATER_2H_EVENING = 2016
        private const val REQUEST_CODE_REMIND_LATER_1H_FALLBACK = 2017
        private const val REQUEST_CODE_REMIND_LATER_2H_FALLBACK = 2018
        private const val REQUEST_CODE_MARK_DAY_OFF_MORNING = 2008
        private const val REQUEST_CODE_MARK_DAY_OFF_EVENING = 2009
        private const val REQUEST_CODE_MARK_DAY_OFF_FALLBACK = 2010
        private const val REQUEST_CODE_MARK_DAY_OFF_DAILY = 2011
        private const val REQUEST_CODE_DAILY_EDIT = 2012
        private const val REQUEST_CODE_REMIND_LATER_DAILY_1H = 2013
        private const val REQUEST_CODE_REMIND_LATER_DAILY_2H = 2014
    }
    
    init {
        createNotificationChannel()
    }
    
    /**
     * Erstellt den Notification Channel
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                setShowBadge(true)
            }
            
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Zeigt eine Morning-Reminder-Notification an
     */
    fun showMorningReminder(date: LocalDate) {
        // Erstelle Group Summary Notification (Android 7.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val groupSummary = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Check-in Erinnerungen")
                .setContentText("Tägliche Check-in Erinnerungen")
                .setGroupSummary(true)
                .setGroup(NOTIFICATION_GROUP)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
            notificationManager.notify(ReminderNotificationIds.MORNING_REMINDER - 1, groupSummary)
        }
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.notification_morning_title))
            .setContentText(context.getString(R.string.notification_morning_text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true) // Persistente Notification
            .setAutoCancel(false) // Wird manuell entfernt
            .setGroup(NOTIFICATION_GROUP)
            .addAction(
                R.drawable.ic_launcher_foreground,
                context.getString(R.string.action_morning_check_in),
                createCheckInPendingIntent(
                    date,
                    ReminderActions.ACTION_MORNING_CHECK_IN_WITH_LOCATION,
                    REQUEST_CODE_MORNING_WITH_LOCATION
                )
            )
            .addAction(
                R.drawable.ic_launcher_foreground,
                context.getString(R.string.action_morning_check_in_no_location),
                createCheckInPendingIntent(
                    date,
                    ReminderActions.ACTION_MORNING_CHECK_IN_WITHOUT_LOCATION,
                    REQUEST_CODE_MORNING_WITHOUT_LOCATION
                )
            )
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Später (1h)",
                createRemindLaterPendingIntent(
                    date,
                    1,
                    ReminderType.MORNING,
                    REQUEST_CODE_REMIND_LATER_1H_MORNING
                )
            )
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Später (2h)",
                createRemindLaterPendingIntent(
                    date,
                    2,
                    ReminderType.MORNING,
                    REQUEST_CODE_REMIND_LATER_2H_MORNING
                )
            )
            .addAction(
                R.drawable.ic_launcher_foreground,
                context.getString(R.string.action_mark_day_off),
                createMarkDayOffPendingIntent(
                    date,
                    REQUEST_CODE_MARK_DAY_OFF_MORNING
                )
            )
            .build()
        
        notificationManager.notify(ReminderNotificationIds.MORNING_REMINDER, notification)
    }
    
    /**
     * Zeigt eine Evening-Reminder-Notification an
     */
    fun showEveningReminder(date: LocalDate) {
        // Erstelle Group Summary Notification (Android 7.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val groupSummary = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Check-in Erinnerungen")
                .setContentText("Tägliche Check-in Erinnerungen")
                .setGroupSummary(true)
                .setGroup(NOTIFICATION_GROUP)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
            notificationManager.notify(ReminderNotificationIds.EVENING_REMINDER - 1, groupSummary)
        }
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.notification_evening_title))
            .setContentText(context.getString(R.string.notification_evening_text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true) // Persistente Notification
            .setAutoCancel(false) // Wird manuell entfernt
            .setGroup(NOTIFICATION_GROUP)
            .addAction(
                R.drawable.ic_launcher_foreground,
                context.getString(R.string.action_evening_check_in),
                createCheckInPendingIntent(
                    date,
                    ReminderActions.ACTION_EVENING_CHECK_IN_WITH_LOCATION,
                    REQUEST_CODE_EVENING_WITH_LOCATION
                )
            )
            .addAction(
                R.drawable.ic_launcher_foreground,
                context.getString(R.string.action_evening_check_in_no_location),
                createCheckInPendingIntent(
                    date,
                    ReminderActions.ACTION_EVENING_CHECK_IN_WITHOUT_LOCATION,
                    REQUEST_CODE_EVENING_WITHOUT_LOCATION
                )
            )
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Später (1h)",
                createRemindLaterPendingIntent(
                    date,
                    1,
                    ReminderType.EVENING,
                    REQUEST_CODE_REMIND_LATER_1H_EVENING
                )
            )
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Später (2h)",
                createRemindLaterPendingIntent(
                    date,
                    2,
                    ReminderType.EVENING,
                    REQUEST_CODE_REMIND_LATER_2H_EVENING
                )
            )
            .addAction(
                R.drawable.ic_launcher_foreground,
                context.getString(R.string.action_mark_day_off),
                createMarkDayOffPendingIntent(
                    date,
                    REQUEST_CODE_MARK_DAY_OFF_EVENING
                )
            )
            .build()
        
        notificationManager.notify(ReminderNotificationIds.EVENING_REMINDER, notification)
    }
    
    /**
     * Zeigt eine Fallback-Reminder-Notification an
     */
    fun showFallbackReminder(date: LocalDate) {
        // Erstelle Group Summary Notification (Android 7.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val groupSummary = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Check-in Erinnerungen")
                .setContentText("Tägliche Check-in Erinnerungen")
                .setGroupSummary(true)
                .setGroup(NOTIFICATION_GROUP)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
            notificationManager.notify(ReminderNotificationIds.FALLBACK_REMINDER - 1, groupSummary)
        }
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.notification_fallback_title))
            .setContentText(context.getString(R.string.notification_fallback_text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true) // Persistente Notification
            .setAutoCancel(false) // Wird manuell entfernt
            .setGroup(NOTIFICATION_GROUP)
            .addAction(
                R.drawable.ic_launcher_foreground,
                context.getString(R.string.action_edit_entry),
                createEditPendingIntent(date, REQUEST_CODE_EDIT)
            )
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Später (1h)",
                createRemindLaterPendingIntent(
                    date,
                    1,
                    ReminderType.FALLBACK,
                    REQUEST_CODE_REMIND_LATER_1H_FALLBACK
                )
            )
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Später (2h)",
                createRemindLaterPendingIntent(
                    date,
                    2,
                    ReminderType.FALLBACK,
                    REQUEST_CODE_REMIND_LATER_2H_FALLBACK
                )
            )
            .addAction(
                R.drawable.ic_launcher_foreground,
                context.getString(R.string.action_mark_day_off),
                createMarkDayOffPendingIntent(
                    date,
                    REQUEST_CODE_MARK_DAY_OFF_FALLBACK
                )
            )
            .build()
        
        notificationManager.notify(ReminderNotificationIds.FALLBACK_REMINDER, notification)
    }

    fun showDailyConfirmationNotification(date: LocalDate) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val groupSummary = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Check-in Erinnerungen")
                .setContentText("Tägliche Check-in Erinnerungen")
                .setGroupSummary(true)
                .setGroup(NOTIFICATION_GROUP)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
            notificationManager.notify(ReminderNotificationIds.DAILY_REMINDER - 1, groupSummary)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.notification_daily_confirmation_title))
            .setContentText(context.getString(R.string.notification_daily_confirmation_text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setAutoCancel(false)
            .setGroup(NOTIFICATION_GROUP)
            .addAction(
                R.drawable.ic_launcher_foreground,
                context.getString(R.string.action_confirm_yes),
                createConfirmWorkDayPendingIntent(date)
            )
            .addAction(
                R.drawable.ic_launcher_foreground,
                context.getString(R.string.action_confirm_no),
                createConfirmOffDayPendingIntent(date)
            )
            .addAction(
                R.drawable.ic_launcher_foreground,
                context.getString(R.string.action_confirm_later),
                createRemindLaterConfirmationPendingIntent(date)
            )
            .build()

        notificationManager.notify(ReminderNotificationIds.DAILY_REMINDER, notification)
    }
    
    /**
     * Cancel die Morning-Reminder-Notification
     */
    fun cancelMorningReminder() {
        notificationManager.cancel(ReminderNotificationIds.MORNING_REMINDER)
    }
    
    /**
     * Cancel die Evening-Reminder-Notification
     */
    fun cancelEveningReminder() {
        notificationManager.cancel(ReminderNotificationIds.EVENING_REMINDER)
    }
    
    /**
     * Cancel die Fallback-Reminder-Notification
     */
    fun cancelFallbackReminder() {
        notificationManager.cancel(ReminderNotificationIds.FALLBACK_REMINDER)
    }

    fun cancelDailyReminder() {
        notificationManager.cancel(ReminderNotificationIds.DAILY_REMINDER)
    }
    
    /**
     * Cancel alle Reminder-Notifications
     */
    fun cancelAllReminders() {
        notificationManager.cancel(ReminderNotificationIds.MORNING_REMINDER)
        notificationManager.cancel(ReminderNotificationIds.EVENING_REMINDER)
        notificationManager.cancel(ReminderNotificationIds.FALLBACK_REMINDER)
        notificationManager.cancel(ReminderNotificationIds.DAILY_REMINDER)
    }
    
    /**
     * Erstellt einen PendingIntent für Check-In Actions
     */
    private fun createCheckInPendingIntent(
        date: LocalDate,
        action: String,
        requestCode: Int
    ): PendingIntent {
        val intent = Intent(context, CheckInActionService::class.java).apply {
            this.action = action
            putExtra(ReminderActions.EXTRA_DATE, date.toString())
            putExtra(ReminderActions.EXTRA_ACTION_TYPE, action)
        }
        
        return PendingIntent.getService(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
    
    /**
     * Erstellt einen PendingIntent für Edit Action
     */
    private fun createEditPendingIntent(
        date: LocalDate,
        requestCode: Int
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = ReminderActions.ACTION_EDIT_ENTRY
            putExtra(ReminderActions.EXTRA_DATE, date.toString())
            putExtra(ReminderActions.EXTRA_ACTION_TYPE, ReminderActions.ACTION_EDIT_ENTRY)
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
        }
        
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
    
    /**
     * Erstellt einen PendingIntent für "Später erinnern" Action
     */
    private fun createRemindLaterPendingIntent(
        date: LocalDate,
        hoursLater: Int,
        reminderType: ReminderType,
        requestCode: Int
    ): PendingIntent {
        val intent = Intent(context, CheckInActionService::class.java).apply {
            action = ReminderActions.ACTION_REMIND_LATER
            putExtra(ReminderActions.EXTRA_DATE, date.toString())
            putExtra(ReminderActions.EXTRA_HOURS_LATER, hoursLater)
            putExtra(ReminderActions.EXTRA_REMINDER_TYPE, reminderType.name)
        }
        
        return PendingIntent.getService(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createMarkDayOffPendingIntent(
        date: LocalDate,
        requestCode: Int
    ): PendingIntent {
        val intent = Intent(context, CheckInActionService::class.java).apply {
            action = ReminderActions.ACTION_MARK_DAY_OFF
            putExtra(ReminderActions.EXTRA_DATE, date.toString())
            putExtra(ReminderActions.EXTRA_ACTION_TYPE, ReminderActions.ACTION_MARK_DAY_OFF)
        }

        return PendingIntent.getService(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Erstellt einen PendingIntent für "Ja" Action (Daily Confirmation)
     */
    private fun createConfirmWorkDayPendingIntent(
        date: LocalDate,
        requestCode: Int = 3001
    ): PendingIntent {
        val intent = Intent(context, CheckInActionService::class.java).apply {
            action = ReminderActions.ACTION_CONFIRM_WORK_DAY
            putExtra(ReminderActions.EXTRA_DATE, date.toString())
            putExtra(ReminderActions.EXTRA_ACTION_TYPE, ReminderActions.ACTION_CONFIRM_WORK_DAY)
            putExtra(ReminderActions.EXTRA_CONFIRMATION_SOURCE, "NOTIFICATION")
        }

        return PendingIntent.getService(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Erstellt einen PendingIntent für "Nein" Action (Daily Confirmation)
     */
    private fun createConfirmOffDayPendingIntent(
        date: LocalDate,
        requestCode: Int = 3002
    ): PendingIntent {
        val intent = Intent(context, CheckInActionService::class.java).apply {
            action = ReminderActions.ACTION_CONFIRM_OFF_DAY
            putExtra(ReminderActions.EXTRA_DATE, date.toString())
            putExtra(ReminderActions.EXTRA_ACTION_TYPE, ReminderActions.ACTION_CONFIRM_OFF_DAY)
            putExtra(ReminderActions.EXTRA_CONFIRMATION_SOURCE, "NOTIFICATION")
        }

        return PendingIntent.getService(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Erstellt einen PendingIntent für "Später" Action (Daily Confirmation)
     */
    private fun createRemindLaterConfirmationPendingIntent(
        date: LocalDate,
        requestCode: Int = 3003
    ): PendingIntent {
        val intent = Intent(context, CheckInActionService::class.java).apply {
            action = ReminderActions.ACTION_REMIND_LATER_CONFIRMATION
            putExtra(ReminderActions.EXTRA_DATE, date.toString())
            putExtra(ReminderActions.EXTRA_REMINDER_TYPE, ReminderType.DAILY.name)
        }

        return PendingIntent.getService(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
