package de.montagezeit.app.work

import de.montagezeit.app.data.preferences.ReminderSettings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DefaultNonWorkingDayCheckerTest {

    private val checker = DefaultNonWorkingDayChecker()

    @Test
    fun `isNonWorkingDay returns true for weekend when auto off weekends is enabled`() {
        val saturday = LocalDate.of(2026, 5, 9)

        assertTrue(
            checker.isNonWorkingDay(
                date = saturday,
                settings = ReminderSettings(autoOffWeekends = true)
            )
        )
    }

    @Test
    fun `isNonWorkingDay returns false for weekend when auto off weekends is disabled`() {
        val saturday = LocalDate.of(2026, 5, 9)

        assertFalse(
            checker.isNonWorkingDay(
                date = saturday,
                settings = ReminderSettings(autoOffWeekends = false, autoOffHolidays = false)
            )
        )
    }

    @Test
    fun `isNonWorkingDay returns true for configured holiday when auto off holidays is enabled`() {
        val holiday = LocalDate.of(2026, 5, 1)

        assertTrue(
            checker.isNonWorkingDay(
                date = holiday,
                settings = ReminderSettings(
                    autoOffWeekends = false,
                    autoOffHolidays = true,
                    holidayDates = setOf(holiday)
                )
            )
        )
    }

    @Test
    fun `isNonWorkingDay returns false for configured holiday when auto off holidays is disabled`() {
        val holiday = LocalDate.of(2026, 5, 1)

        assertFalse(
            checker.isNonWorkingDay(
                date = holiday,
                settings = ReminderSettings(
                    autoOffWeekends = false,
                    autoOffHolidays = false,
                    holidayDates = setOf(holiday)
                )
            )
        )
    }
}
