package de.montagezeit.app.ui.screen.today

import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.TravelLeg
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.local.entity.WorkEntryWithTravelLegs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class TodayDashboardSupportTest {

    @Test
    fun `calculateWeekStats includes off-day travel in paid hours`() {
        val result = calculateWeekStats(
            entries = listOf(
                entry(LocalDate.of(2026, 3, 23), DayType.OFF, travelMinutes = 120),
                entry(LocalDate.of(2026, 3, 24), DayType.WORK, travelMinutes = 30)
            ),
            targetHours = 8.0
        )

        assertEquals(8.0, result.totalHours, 0.001)
        assertEquals(10.5, result.totalPaidHours, 0.001)
        assertEquals(1.0f, result.progress, 0.001f)
        assertTrue(result.isOverTarget)
    }

    @Test
    fun `calculateMonthStats includes off-day travel in paid hours`() {
        val result = calculateMonthStats(
            entries = listOf(
                entry(LocalDate.of(2026, 3, 23), DayType.OFF, travelMinutes = 120),
                entry(LocalDate.of(2026, 3, 24), DayType.WORK, travelMinutes = 30)
            ),
            targetHours = 16.0
        )

        assertEquals(8.0, result.totalHours, 0.001)
        assertEquals(10.5, result.totalPaidHours, 0.001)
        assertEquals(0.65625f, result.progress, 0.001f)
        assertTrue(result.isUnderTarget)
    }

    private fun entry(date: LocalDate, dayType: DayType, travelMinutes: Int): WorkEntryWithTravelLegs {
        return WorkEntryWithTravelLegs(
            workEntry = WorkEntry(
                date = date,
                dayType = dayType,
                confirmedWorkDay = true,
                workStart = if (dayType == DayType.WORK) LocalTime.of(8, 0) else null,
                workEnd = if (dayType == DayType.WORK) LocalTime.of(17, 0) else null,
                breakMinutes = if (dayType == DayType.WORK) 60 else 0
            ),
            travelLegs = listOf(
                TravelLeg(
                    workEntryDate = date,
                    sortOrder = 0,
                    paidMinutesOverride = travelMinutes
                )
            )
        )
    }
}
