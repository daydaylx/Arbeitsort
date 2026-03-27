package de.montagezeit.app.notification

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class ReminderNotificationManagerTest {

    private lateinit var manager: ReminderNotificationManager
    private lateinit var systemNm: NotificationManager
    private lateinit var shadowNm: org.robolectric.shadows.ShadowNotificationManager

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        manager = ReminderNotificationManager(context)
        systemNm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        shadowNm = Shadows.shadowOf(systemNm)
    }

    private fun notification(notificationId: Int): Notification {
        return requireNotNull(shadowNm.getNotification(notificationId))
    }

    @Test
    fun `showMorningReminder creates notification with correct ID`() {
        manager.showMorningReminder(LocalDate.of(2026, 3, 15))
        assertNotNull(shadowNm.getNotification(ReminderNotificationIds.MORNING_REMINDER))
    }

    @Test
    fun `showEveningReminder creates notification with correct ID`() {
        manager.showEveningReminder(LocalDate.of(2026, 3, 15))
        assertNotNull(shadowNm.getNotification(ReminderNotificationIds.EVENING_REMINDER))
    }

    @Test
    fun `showFallbackReminder creates notification with correct ID`() {
        manager.showFallbackReminder(LocalDate.of(2026, 3, 15))
        assertNotNull(shadowNm.getNotification(ReminderNotificationIds.FALLBACK_REMINDER))
    }

    @Test
    fun `showDailyConfirmationNotification creates notification with correct ID`() {
        manager.showDailyConfirmationNotification(LocalDate.of(2026, 3, 15))
        assertNotNull(shadowNm.getNotification(ReminderNotificationIds.DAILY_REMINDER))
    }

    @Test
    fun `showMorningReminder uses new morning check in action`() {
        manager.showMorningReminder(LocalDate.of(2026, 3, 15))

        val notification = notification(ReminderNotificationIds.MORNING_REMINDER)
        val actionIntent = Shadows.shadowOf(notification.actions.first().actionIntent).savedIntent

        assertEquals(ReminderActions.ACTION_MORNING_CHECK_IN, actionIntent.action)
    }

    @Test
    fun `showEveningReminder uses new evening check in action`() {
        manager.showEveningReminder(LocalDate.of(2026, 3, 15))

        val notification = notification(ReminderNotificationIds.EVENING_REMINDER)
        val actionIntent = Shadows.shadowOf(notification.actions.first().actionIntent).savedIntent

        assertEquals(ReminderActions.ACTION_EVENING_CHECK_IN, actionIntent.action)
    }

    @Test
    fun `showMorningReminder opens app on edit action for notification tap`() {
        val date = LocalDate.of(2026, 3, 15)
        manager.showMorningReminder(date)

        val notification = notification(ReminderNotificationIds.MORNING_REMINDER)
        val contentIntent = Shadows.shadowOf(notification.contentIntent).savedIntent

        assertEquals(ReminderActions.ACTION_EDIT_ENTRY, contentIntent.action)
        assertEquals(date.toString(), contentIntent.getStringExtra(ReminderActions.EXTRA_DATE))
    }

    @Test
    fun `cancelMorningReminder removes notification`() {
        manager.showMorningReminder(LocalDate.of(2026, 3, 15))
        manager.cancelMorningReminder()
        org.junit.Assert.assertNull(shadowNm.getNotification(ReminderNotificationIds.MORNING_REMINDER))
    }

    @Test
    fun `cancelAllReminders removes all notification IDs`() {
        val date = LocalDate.of(2026, 3, 15)
        manager.showMorningReminder(date)
        manager.showEveningReminder(date)
        manager.showFallbackReminder(date)
        manager.showDailyConfirmationNotification(date)

        manager.cancelAllReminders()

        org.junit.Assert.assertNull(shadowNm.getNotification(ReminderNotificationIds.MORNING_REMINDER))
        org.junit.Assert.assertNull(shadowNm.getNotification(ReminderNotificationIds.EVENING_REMINDER))
        org.junit.Assert.assertNull(shadowNm.getNotification(ReminderNotificationIds.FALLBACK_REMINDER))
        org.junit.Assert.assertNull(shadowNm.getNotification(ReminderNotificationIds.DAILY_REMINDER))
    }
}
