package de.montagezeit.app.ui.screen.history

import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.domain.util.TimeCalculator
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class WeekGroupTest {

    @Test
    fun `totalHours should calculate correct work hours for work days`() {
        val entries = listOf(
            WorkEntry(
                date = LocalDate.of(2026, 1, 5),
                workStart = LocalTime.of(8, 0),
                workEnd = LocalTime.of(17, 0),  // 9 hours
                breakMinutes = 60,  // -1 hour
                dayType = DayType.WORK
            ),
            WorkEntry(
                date = LocalDate.of(2026, 1, 6),
                workStart = LocalTime.of(8, 0),
                workEnd = LocalTime.of(18, 0),  // 10 hours
                breakMinutes = 60,  // -1 hour
                dayType = DayType.WORK
            )
        )

        val weekGroup = createWeekGroup(year = 2026, week = 1, entries = entries)

        // 8 + 9 = 17 hours total
        assertEquals(17.0, weekGroup.totalHours, 0.1)
    }

    @Test
    fun `totalHours should exclude OFF days from calculation`() {
        val entries = listOf(
            WorkEntry(
                date = LocalDate.of(2026, 1, 5),
                workStart = LocalTime.of(8, 0),
                workEnd = LocalTime.of(17, 0),
                breakMinutes = 60,
                dayType = DayType.WORK
            ),
            WorkEntry(
                date = LocalDate.of(2026, 1, 6),
                workStart = LocalTime.of(8, 0),
                workEnd = LocalTime.of(17, 0),
                breakMinutes = 60,
                dayType = DayType.OFF  // Should not be counted
            )
        )

        val weekGroup = createWeekGroup(year = 2026, week = 1, entries = entries)

        // Only 8 hours from work day
        assertEquals(8.0, weekGroup.totalHours, 0.1)
    }

    @Test
    fun `averageHoursPerDay should calculate correct average`() {
        val entries = listOf(
            WorkEntry(
                date = LocalDate.of(2026, 1, 5),
                workStart = LocalTime.of(8, 0),
                workEnd = LocalTime.of(17, 0),
                breakMinutes = 60,
                dayType = DayType.WORK
            ),
            WorkEntry(
                date = LocalDate.of(2026, 1, 6),
                workStart = LocalTime.of(8, 0),
                workEnd = LocalTime.of(19, 0),  // 11 hours
                breakMinutes = 60,
                dayType = DayType.WORK
            )
        )

        val weekGroup = createWeekGroup(year = 2026, week = 1, entries = entries)

        // (8 + 10) / 2 = 9.0
        assertEquals(9.0, weekGroup.averageHoursPerDay, 0.1)
    }

    @Test
    fun `averageHoursPerDay should return 0 when no work days`() {
        val entries = listOf(
            WorkEntry(
                date = LocalDate.of(2026, 1, 5),
                workStart = LocalTime.of(8, 0),
                workEnd = LocalTime.of(17, 0),
                breakMinutes = 60,
                dayType = DayType.OFF
            )
        )

        val weekGroup = createWeekGroup(year = 2026, week = 1, entries = entries)

        assertEquals(0.0, weekGroup.averageHoursPerDay, 0.1)
    }

    @Test
    fun `workDaysCount should count only WORK days`() {
        val entries = listOf(
            WorkEntry(
                date = LocalDate.of(2026, 1, 5),
                dayType = DayType.WORK,
                needsReview = false
            ),
            WorkEntry(
                date = LocalDate.of(2026, 1, 6),
                dayType = DayType.WORK,
                needsReview = false
            ),
            WorkEntry(
                date = LocalDate.of(2026, 1, 7),
                dayType = DayType.OFF,
                needsReview = false
            )
        )

        val weekGroup = createWeekGroup(year = 2026, week = 1, entries = entries)

        assertEquals(2, weekGroup.workDaysCount)
    }

    @Test
    fun `offDaysCount should count only OFF days`() {
        val entries = listOf(
            WorkEntry(
                date = LocalDate.of(2026, 1, 5),
                dayType = DayType.WORK,
                needsReview = false
            ),
            WorkEntry(
                date = LocalDate.of(2026, 1, 6),
                dayType = DayType.OFF,
                needsReview = false
            ),
            WorkEntry(
                date = LocalDate.of(2026, 1, 7),
                dayType = DayType.OFF,
                needsReview = false
            )
        )

        val weekGroup = createWeekGroup(year = 2026, week = 1, entries = entries)

        assertEquals(2, weekGroup.offDaysCount)
    }

    @Test
    fun `entriesNeedingReview should count entries with needsReview flag`() {
        val entries = listOf(
            WorkEntry(
                date = LocalDate.of(2026, 1, 5),
                dayType = DayType.WORK,
                needsReview = true
            ),
            WorkEntry(
                date = LocalDate.of(2026, 1, 6),
                dayType = DayType.WORK,
                needsReview = false
            ),
            WorkEntry(
                date = LocalDate.of(2026, 1, 7),
                dayType = DayType.WORK,
                needsReview = true
            )
        )

        val weekGroup = createWeekGroup(year = 2026, week = 1, entries = entries)

        assertEquals(2, weekGroup.entriesNeedingReview)
    }

    @Test
    fun `week should keep assigned value`() {
        val weekGroup = createWeekGroup(year = 2026, week = 42, entries = emptyList())

        assertEquals(42, weekGroup.week)
    }

    @Test
    fun `yearText should be empty for current year`() {
        val currentYear = LocalDate.now().year
        val weekGroup = createWeekGroup(year = currentYear, week = 1, entries = emptyList())

        assertEquals("", weekGroup.yearText)
    }

    @Test
    fun `yearText should show year for non-current year`() {
        val currentYear = LocalDate.now().year
        val weekGroup = createWeekGroup(year = currentYear - 1, week = 1, entries = emptyList())

        assertEquals("${currentYear - 1}", weekGroup.yearText)
    }

    @Test
    fun `totalHours should handle fractional hours correctly`() {
        val entries = listOf(
            WorkEntry(
                date = LocalDate.of(2026, 1, 5),
                workStart = LocalTime.of(8, 0),
                workEnd = LocalTime.of(12, 30),  // 4.5 hours
                breakMinutes = 30,  // -0.5 hour
                dayType = DayType.WORK
            )
        )

        val weekGroup = createWeekGroup(year = 2026, week = 1, entries = entries)

        // 4.5 - 0.5 = 4.0 hours
        assertEquals(4.0, weekGroup.totalHours, 0.1)
    }

    private fun createWeekGroup(year: Int, week: Int, entries: List<WorkEntry>): WeekGroup {
        val workEntries = entries.filter { it.dayType == DayType.WORK }
        val workDaysCount = workEntries.size
        val offDaysCount = entries.count { it.dayType == DayType.OFF }
        val totalHours = workEntries.sumOf { TimeCalculator.calculateWorkHours(it) }
        val totalPaidHours = workEntries.sumOf { TimeCalculator.calculatePaidTotalHours(it) }
        val averageHoursPerDay = if (workDaysCount == 0) 0.0 else totalHours / workDaysCount
        val entriesNeedingReview = entries.count { it.needsReview }

        return WeekGroup(
            year = year,
            week = week,
            entries = entries,
            workDaysCount = workDaysCount,
            offDaysCount = offDaysCount,
            totalHours = totalHours,
            totalPaidHours = totalPaidHours,
            averageHoursPerDay = averageHoursPerDay,
            entriesNeedingReview = entriesNeedingReview
        )
    }
}
