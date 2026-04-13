package de.montagezeit.app.work

import de.montagezeit.app.data.preferences.ReminderSettings
import de.montagezeit.app.data.preferences.ReminderSettingsManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.Duration
import java.time.LocalTime
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderSchedulerTest {

    @Test
    fun `scheduleAll enqueues enabled reminders and cancels disabled ones`() = runTest {
        val settings = ReminderSettings(
            morningReminderEnabled = true,
            eveningReminderEnabled = false,
            fallbackEnabled = true,
            dailyReminderEnabled = false
        )
        val settingsManager = mockk<ReminderSettingsManager>()
        val enqueuer = mockk<ReminderWorkEnqueuer>(relaxed = true)

        every { settingsManager.settings } returns flowOf(settings)

        ReminderScheduler(
            reminderSettingsManager = settingsManager,
            reminderWorkEnqueuer = enqueuer
        ).scheduleAll()

        verify(exactly = 2) { enqueuer.enqueue(any()) }
        verify { enqueuer.cancel(ReminderScheduler.EVENING_WORK_NAME) }
        verify { enqueuer.cancel(ReminderScheduler.DAILY_WORK_NAME) }
    }

    @Test
    fun `buildMorningReminderWorkSpec keeps configured window and interval`() {
        val settings = ReminderSettings(
            morningWindowStart = LocalTime.of(6, 0),
            morningWindowEnd = LocalTime.of(12, 0),
            morningCheckIntervalMinutes = 90
        )

        val spec = buildMorningReminderWorkSpec(settings, now = LocalTime.of(5, 30))

        assertEquals(ReminderScheduler.MORNING_WORK_NAME, spec.uniqueWorkName)
        assertEquals(ReminderType.MORNING, spec.reminderType)
        assertEquals(Duration.ofMinutes(30).toMillis(), spec.initialDelayMillis)
        assertEquals(90L, spec.repeatInterval)
        assertEquals(ReminderScheduler.WINDOW_FLEX_MINUTES, spec.flexInterval)
    }

    @Test
    fun `buildEveningReminderWorkSpec clamps interval to workmanager minimum`() {
        val settings = ReminderSettings(
            eveningCheckIntervalMinutes = 5
        )

        val spec = buildEveningReminderWorkSpec(settings, now = LocalTime.of(15, 0))

        assertEquals(15L, spec.repeatInterval)
        assertEquals(Duration.ofHours(1).toMillis(), spec.initialDelayMillis)
    }

    @Test
    fun `daily and fallback specs stay once per day without flex override`() {
        val settings = ReminderSettings(
            fallbackTime = LocalTime.of(22, 30),
            dailyReminderTime = LocalTime.of(18, 0)
        )

        val fallbackSpec = buildFallbackReminderWorkSpec(settings, now = LocalTime.of(21, 0))
        val dailySpec = buildDailyReminderWorkSpec(settings, now = LocalTime.of(17, 0))

        assertEquals(1L, fallbackSpec.repeatInterval)
        assertEquals(1L, dailySpec.repeatInterval)
        assertTrue(fallbackSpec.initialDelayMillis > 0)
        assertTrue(dailySpec.initialDelayMillis > 0)
        assertNull(fallbackSpec.flexInterval)
        assertNull(dailySpec.flexInterval)
    }

    @Test
    fun `cancelAll cancels every unique work name`() {
        val settingsManager = mockk<ReminderSettingsManager>()
        val enqueuer = mockk<ReminderWorkEnqueuer>(relaxed = true)

        every { settingsManager.settings } returns flowOf(ReminderSettings())

        ReminderScheduler(
            reminderSettingsManager = settingsManager,
            reminderWorkEnqueuer = enqueuer
        ).cancelAll()

        verify { enqueuer.cancel(ReminderScheduler.MORNING_WORK_NAME) }
        verify { enqueuer.cancel(ReminderScheduler.EVENING_WORK_NAME) }
        verify { enqueuer.cancel(ReminderScheduler.FALLBACK_WORK_NAME) }
        verify { enqueuer.cancel(ReminderScheduler.DAILY_WORK_NAME) }
    }
}
