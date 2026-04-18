package de.montagezeit.app.ui.screen.overview

import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.TravelLeg
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.local.entity.WorkEntryWithTravelLegs
import de.montagezeit.app.data.preferences.ReminderSettings
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class OverviewCalculationsTest {

    private val settings = ReminderSettings(
        dailyTargetHours = 8.0
    )

    @Test
    fun `rangeFor returns correct week month and year bounds`() {
        val date = LocalDate.of(2026, 3, 18)

        assertEquals(
            OverviewDateRange(
                startDate = LocalDate.of(2026, 3, 16),
                endDate = LocalDate.of(2026, 3, 22)
            ),
            OverviewPeriod.WEEK.rangeFor(date)
        )
        assertEquals(
            OverviewDateRange(
                startDate = LocalDate.of(2026, 3, 1),
                endDate = LocalDate.of(2026, 3, 31)
            ),
            OverviewPeriod.MONTH.rangeFor(date)
        )
        assertEquals(
            OverviewDateRange(
                startDate = LocalDate.of(2026, 1, 1),
                endDate = LocalDate.of(2026, 12, 31)
            ),
            OverviewPeriod.YEAR.rangeFor(date)
        )
    }

    @Test
    fun `shiftReferenceDate moves by the selected period`() {
        val date = LocalDate.of(2026, 3, 31)

        assertEquals(LocalDate.of(2026, 4, 1), OverviewPeriod.DAY.shiftReferenceDate(date, 1))
        assertEquals(LocalDate.of(2026, 4, 7), OverviewPeriod.WEEK.shiftReferenceDate(date, 1))
        assertEquals(LocalDate.of(2026, 4, 30), OverviewPeriod.MONTH.shiftReferenceDate(date, 1))
        assertEquals(LocalDate.of(2027, 3, 31), OverviewPeriod.YEAR.shiftReferenceDate(date, 1))
    }

    @Test
    fun `buildOverviewMetrics excludes unconfirmed work activity`() {
        val date = LocalDate.of(2026, 3, 18)
        val confirmedEntry = WorkEntryWithTravelLegs(
            workEntry = WorkEntry(
                date = date,
                workStart = LocalTime.of(8, 0),
                workEnd = LocalTime.of(17, 0),
                breakMinutes = 60,
                dayType = DayType.WORK,
                confirmedWorkDay = true,
                mealAllowanceAmountCents = 2800
            ),
            travelLegs = listOf(TravelLeg(workEntryDate = date, sortOrder = 0, paidMinutesOverride = 120))
        )
        // Hat Arbeitszeiten aber kein confirmedWorkDay → bleibt ausgeschlossen
        val unconfirmedWithData = WorkEntryWithTravelLegs(
            workEntry = confirmedEntry.workEntry.copy(
                date = date.plusDays(1),
                confirmedWorkDay = false,
                mealAllowanceAmountCents = 1400
            ),
            travelLegs = listOf(TravelLeg(workEntryDate = date.plusDays(1), sortOrder = 0, paidMinutesOverride = 120))
        )

        val metrics = buildOverviewMetrics(
            period = OverviewPeriod.DAY,
            entries = listOf(confirmedEntry, unconfirmedWithData),
            settings = settings
        )

        assertEquals(2.0, metrics.overtimeHours, 0.001)
        assertEquals(8.0, metrics.targetHours, 0.001)
        assertEquals(10.0, metrics.actualHours, 0.001)
        assertEquals(2.0, metrics.travelHours, 0.001)
        assertEquals(2800, metrics.mealAllowanceCents)
        assertEquals(1, metrics.countedDays)
        assertEquals(1, metrics.unconfirmedDaysCount)
    }

    @Test
    fun `buildOverviewMetrics excludes WORK days without data`() {
        val date = LocalDate.of(2026, 3, 18)
        val confirmedEntry = WorkEntryWithTravelLegs(
            workEntry = WorkEntry(
                date = date,
                workStart = LocalTime.of(8, 0),
                workEnd = LocalTime.of(17, 0),
                breakMinutes = 60,
                dayType = DayType.WORK,
                confirmedWorkDay = true,
                mealAllowanceAmountCents = 2800
            ),
            travelLegs = listOf(TravelLeg(workEntryDate = date, sortOrder = 0, paidMinutesOverride = 120))
        )
        val emptyEntry = WorkEntryWithTravelLegs(
            workEntry = WorkEntry(
                date = date.plusDays(1),
                dayType = DayType.WORK,
                confirmedWorkDay = false
            ),
            travelLegs = emptyList()
        )

        val metrics = buildOverviewMetrics(
            period = OverviewPeriod.DAY,
            entries = listOf(confirmedEntry, emptyEntry),
            settings = settings
        )

        assertEquals(2.0, metrics.overtimeHours, 0.001)
        assertEquals(8.0, metrics.targetHours, 0.001)
        assertEquals(10.0, metrics.actualHours, 0.001)
        assertEquals(2.0, metrics.travelHours, 0.001)
        assertEquals(2800, metrics.mealAllowanceCents)
        assertEquals(1, metrics.countedDays)
        assertEquals(1, metrics.unconfirmedDaysCount)
    }

    @Test
    fun `buildOverviewMetrics includes off day travel in actual overtime hours`() {
        val date = LocalDate.of(2026, 3, 19)
        val offEntry = WorkEntryWithTravelLegs(
            workEntry = WorkEntry(
                date = date,
                dayType = DayType.OFF,
                confirmedWorkDay = true
            ),
            travelLegs = listOf(TravelLeg(workEntryDate = date, sortOrder = 0, paidMinutesOverride = 120))
        )

        val metrics = buildOverviewMetrics(
            period = OverviewPeriod.DAY,
            entries = listOf(offEntry),
            settings = settings
        )

        assertEquals(2.0, metrics.overtimeHours, 0.001)
        assertEquals(2.0, metrics.actualHours, 0.001)
        assertEquals(2.0, metrics.travelHours, 0.001)
        assertEquals(0, metrics.countedDays)
    }

}
