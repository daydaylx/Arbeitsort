package de.montagezeit.app.work

import de.montagezeit.app.data.preferences.ReminderSettings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class ReminderWindowEvaluatorTest {

    private val settings = ReminderSettings(
        dailyReminderTime = LocalTime.of(18, 0)
    )

    @Test
    fun `daily reminder guard blocks times before configured reminder`() {
        assertFalse(ReminderWindowEvaluator.isAfterDailyReminderTime(LocalTime.of(17, 59, 59), settings))
    }

    @Test
    fun `daily reminder guard allows configured reminder time and later`() {
        assertTrue(ReminderWindowEvaluator.isAfterDailyReminderTime(LocalTime.of(18, 0), settings))
        assertTrue(ReminderWindowEvaluator.isAfterDailyReminderTime(LocalTime.of(18, 0, 1), settings))
    }
}
