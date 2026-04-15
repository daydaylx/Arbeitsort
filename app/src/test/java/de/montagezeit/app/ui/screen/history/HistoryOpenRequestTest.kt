package de.montagezeit.app.ui.screen.history

import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.ui.screen.overview.OverviewPeriod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HistoryOpenRequestTest {

    @Test
    fun `createHistoryOpenRequest maps overview period to expected grouping`() {
        val date = LocalDate.of(2026, 3, 18)

        assertEquals(
            HistoryGrouping.WEEK,
            createHistoryOpenRequest(1L, date, OverviewPeriod.DAY).grouping
        )
        assertEquals(
            HistoryGrouping.WEEK,
            createHistoryOpenRequest(2L, date, OverviewPeriod.WEEK).grouping
        )
        assertEquals(
            HistoryGrouping.MONTH,
            createHistoryOpenRequest(3L, date, OverviewPeriod.MONTH).grouping
        )
        assertEquals(
            HistoryGrouping.MONTH,
            createHistoryOpenRequest(4L, date, OverviewPeriod.YEAR).grouping
        )
    }

    @Test
    fun `historySelectionSeedForRequest keeps list mode and anchors date`() {
        val date = LocalDate.of(2026, 3, 18)
        val weekSeed = historySelectionSeedForRequest(
            HistoryOpenRequest(
                requestId = 1L,
                anchorDate = date,
                grouping = HistoryGrouping.WEEK
            )
        )
        val monthSeed = historySelectionSeedForRequest(
            HistoryOpenRequest(
                requestId = 2L,
                anchorDate = date,
                grouping = HistoryGrouping.MONTH
            )
        )

        assertFalse(weekSeed.showCalendar)
        assertFalse(weekSeed.showMonths)
        assertEquals(CalendarMode.WEEK, weekSeed.calendarMode)
        assertEquals(date, weekSeed.selectedDate)

        assertFalse(monthSeed.showCalendar)
        assertTrue(monthSeed.showMonths)
        assertEquals(CalendarMode.MONTH, monthSeed.calendarMode)
        assertEquals(date, monthSeed.selectedDate)
    }

    @Test
    fun `historyGroupedScrollTargetIndex returns week header index for anchor week`() {
        val request = HistoryOpenRequest(
            requestId = 1L,
            anchorDate = LocalDate.of(2026, 3, 17),
            grouping = HistoryGrouping.WEEK
        )
        val weeks = listOf(
            WeekGroup(
                year = 2026,
                week = 12,
                weekStart = LocalDate.of(2026, 3, 16),
                entries = listOf(
                    WorkEntry(date = LocalDate.of(2026, 3, 18)),
                    WorkEntry(date = LocalDate.of(2026, 3, 17))
                ),
                workDaysCount = 2,
                offDaysCount = 0,
                totalHours = 16.0,
                totalPaidHours = 16.0,
                averageHoursPerDay = 8.0
            ),
            WeekGroup(
                year = 2026,
                week = 11,
                weekStart = LocalDate.of(2026, 3, 9),
                entries = listOf(WorkEntry(date = LocalDate.of(2026, 3, 10))),
                workDaysCount = 1,
                offDaysCount = 0,
                totalHours = 8.0,
                totalPaidHours = 8.0,
                averageHoursPerDay = 8.0
            )
        )

        val targetIndex = historyGroupedScrollTargetIndex(
            request = request,
            weeks = weeks,
            months = emptyList()
        )

        assertEquals(2, targetIndex)
    }

    @Test
    fun `historyGroupedScrollTargetIndex returns month header index for anchor month`() {
        val request = HistoryOpenRequest(
            requestId = 1L,
            anchorDate = LocalDate.of(2026, 2, 20),
            grouping = HistoryGrouping.MONTH
        )
        val months = listOf(
            MonthGroup(
                year = 2026,
                month = 3,
                entries = listOf(
                    WorkEntry(date = LocalDate.of(2026, 3, 18)),
                    WorkEntry(date = LocalDate.of(2026, 3, 17))
                ),
                workDaysCount = 2,
                offDaysCount = 0,
                totalHours = 16.0,
                totalPaidHours = 16.0,
                averageHoursPerDay = 8.0
            ),
            MonthGroup(
                year = 2026,
                month = 2,
                entries = listOf(WorkEntry(date = LocalDate.of(2026, 2, 20))),
                workDaysCount = 1,
                offDaysCount = 0,
                totalHours = 8.0,
                totalPaidHours = 8.0,
                averageHoursPerDay = 8.0
            )
        )

        val targetIndex = historyGroupedScrollTargetIndex(
            request = request,
            weeks = emptyList(),
            months = months
        )

        assertEquals(5, targetIndex)
    }
}
