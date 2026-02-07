package de.montagezeit.app.work

import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.preferences.ReminderSettings
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
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

    @Test
    fun `custom windows remain start inclusive and end exclusive`() {
        val customSettings = settings.copy(
            morningWindowStart = LocalTime.of(7, 30),
            morningWindowEnd = LocalTime.of(8, 45),
            eveningWindowStart = LocalTime.of(17, 15),
            eveningWindowEnd = LocalTime.of(20, 0)
        )

        assertTrue(ReminderWindowEvaluator.isInMorningWindow(LocalTime.of(7, 30), customSettings))
        assertFalse(ReminderWindowEvaluator.isInMorningWindow(LocalTime.of(8, 45), customSettings))
        assertTrue(ReminderWindowEvaluator.isInEveningWindow(LocalTime.of(19, 59), customSettings))
        assertFalse(ReminderWindowEvaluator.isInEveningWindow(LocalTime.of(20, 0), customSettings))
    }

    @Test
    fun `manual WORK override beats weekend auto off`() = runTest {
        val date = LocalDate.of(2026, 2, 7) // Saturday
        val dao = mockk<WorkEntryDao>()
        val settings = ReminderSettings(autoOffWeekends = true, autoOffHolidays = false)
        coEvery { dao.getByDate(date) } returns WorkEntry(date = date, dayType = DayType.WORK)

        val nonWorking = ReminderWindowEvaluator.isNonWorkingDay(date, settings, dao)

        assertFalse(nonWorking)
    }

    @Test
    fun `manual OFF override beats working day defaults`() = runTest {
        val date = LocalDate.of(2026, 2, 9) // Monday
        val dao = mockk<WorkEntryDao>()
        val settings = ReminderSettings(autoOffWeekends = false, autoOffHolidays = false)
        coEvery { dao.getByDate(date) } returns WorkEntry(date = date, dayType = DayType.OFF)

        val nonWorking = ReminderWindowEvaluator.isNonWorkingDay(date, settings, dao)

        assertTrue(nonWorking)
    }
}
