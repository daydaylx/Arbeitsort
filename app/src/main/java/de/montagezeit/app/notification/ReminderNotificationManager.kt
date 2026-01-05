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
import de.montagezeit.app.R
import de.montagezeit.app.handler.CheckInActionService
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
    
    // Common Actions
    const val ACTION_EDIT_ENTRY = "action_edit_entry"
    
    // Extras
    const val EXTRA_DATE = "extra_date"
    const val EXTRA_ACTION_TYPE = "extra_action_type"
}

/**
 * Konstanten für Notification IDs
 */
object ReminderNotificationIds {
    const val MORNING_REMINDER = 1001
    const val EVENING_REMINDER = 1002
    const val FALLBACK_REMINDER = 1003
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
        
        // Request Codes für PendingIntents (müssen einzigartig sein)
        private const val REQUEST_CODE_MORNING_WITH_LOCATION = 2001
        private const val REQUEST_CODE_MORNING_WITHOUT_LOCATION = 2002
        private const val REQUEST_CODE_EVENING_WITH_LOCATION = 2003
        private const val REQUEST_CODE_EVENING_WITHOUT_LOCATION = 2004
        private const val REQUEST_CODE_EDIT = 2005
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
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.notification_morning_title))
            .setContentText(context.getString(R.string.notification_morning_text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
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
            .build()
        
        notificationManager.notify(ReminderNotificationIds.MORNING_REMINDER, notification)
    }
    
    /**
     * Zeigt eine Evening-Reminder-Notification an
     */
    fun showEveningReminder(date: LocalDate) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.notification_evening_title))
            .setContentText(context.getString(R.string.notification_evening_text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
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
            .build()
        
        notificationManager.notify(ReminderNotificationIds.EVENING_REMINDER, notification)
    }
    
    /**
     * Zeigt eine Fallback-Reminder-Notification an
     */
    fun showFallbackReminder(date: LocalDate) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.notification_fallback_title))
            .setContentText(context.getString(R.string.notification_fallback_text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(
                R.drawable.ic_launcher_foreground,
                context.getString(R.string.action_edit_entry),
                createEditPendingIntent(date, REQUEST_CODE_EDIT)
            )
            .build()
        
        notificationManager.notify(ReminderNotificationIds.FALLBACK_REMINDER, notification)
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
    
    /**
     * Cancel alle Reminder-Notifications
     */
    fun cancelAllReminders() {
        notificationManager.cancel(ReminderNotificationIds.MORNING_REMINDER)
        notificationManager.cancel(ReminderNotificationIds.EVENING_REMINDER)
        notificationManager.cancel(ReminderNotificationIds.FALLBACK_REMINDER)
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
        // TODO: Implementiere Edit-Action (öffnet MainActivity mit Edit-Route)
        // Vorläufig verwenden wir einen Service der nur den Intent loggt
        val intent = Intent(context, CheckInActionService::class.java).apply {
            action = ReminderActions.ACTION_EDIT_ENTRY
            putExtra(ReminderActions.EXTRA_DATE, date.toString())
            putExtra(ReminderActions.EXTRA_ACTION_TYPE, ReminderActions.ACTION_EDIT_ENTRY)
        }
        
        return PendingIntent.getService(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
