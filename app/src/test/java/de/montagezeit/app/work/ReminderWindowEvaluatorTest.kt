package de.montagezeit.app.work

import de.montagezeit.app.data.preferences.ReminderSettings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class ReminderWindowEvaluatorTest {

    private val settings = ReminderSettings()

    @Test
    fun `morning window boundaries are respected`() {
        assertFalse(ReminderWindowEvaluator.isInMorningWindow(LocalTime.of(5, 59), settings))
        assertTrue(ReminderWindowEvaluator.isInMorningWindow(LocalTime.of(6, 0), settings))
        assertTrue(ReminderWindowEvaluator.isInMorningWindow(LocalTime.of(12, 59), settings))
        assertFalse(ReminderWindowEvaluator.isInMorningWindow(LocalTime.of(13, 0), settings))
    }

    @Test
    fun `evening window boundaries are respected`() {
        assertFalse(ReminderWindowEvaluator.isInEveningWindow(LocalTime.of(15, 59), settings))
        assertTrue(ReminderWindowEvaluator.isInEveningWindow(LocalTime.of(16, 0), settings))
        assertTrue(ReminderWindowEvaluator.isInEveningWindow(LocalTime.of(22, 29), settings))
        assertFalse(ReminderWindowEvaluator.isInEveningWindow(LocalTime.of(22, 30), settings))
    }

    @Test
    fun `fallback time is inclusive`() {
        assertFalse(ReminderWindowEvaluator.isAfterFallbackTime(LocalTime.of(22, 29), settings))
        assertTrue(ReminderWindowEvaluator.isAfterFallbackTime(LocalTime.of(22, 30), settings))
        assertTrue(ReminderWindowEvaluator.isAfterFallbackTime(LocalTime.of(23, 0), settings))
    }
}
