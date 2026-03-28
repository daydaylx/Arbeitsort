package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.TravelLeg
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.local.entity.WorkEntryWithTravelLegs
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class CalculateOvertimeForRangeTest {

    private val useCase = CalculateOvertimeForRange()

    @Test
    fun `nur Arbeit`() {
        // 8h IST, 8h SOLL, Saldo 0
        val result = useCase(
            entries = listOf(
                overtimeEntry(
                    date = LocalDate.of(2026, 1, 3),
                    dayType = DayType.WORK,
                    confirmedWorkDay = true,
                    workStart = LocalTime.of(8, 0),
                    workEnd = LocalTime.of(16, 0),
                    breakMinutes = 0
                )
            ),
            dailyTargetHours = 8.0
        )

        assertEquals(0.0, result.totalOvertimeHours, 0.0001)
        assertEquals(8.0, result.totalActualHours, 0.0001)
        assertEquals(8.0, result.totalTargetHours, 0.0001)
        assertEquals(1, result.countedDays)
    }

    @Test
    fun `nur Fahrzeit - unbestaetigter WORK-Tag mit Travel wird aggregiert`() {
        // WORK-Tag unbestätigt aber mit Travel (4h IST, 8h SOLL, Saldo -4)
        val result = useCase(
            entries = listOf(
                overtimeEntry(
                    date = LocalDate.of(2026, 1, 4),
                    dayType = DayType.WORK,
                    confirmedWorkDay = false,
                    travelPaidMinutes = 240 // 4 Stunden
                )
            ),
            dailyTargetHours = 8.0
        )

        assertEquals(-4.0, result.totalOvertimeHours, 0.0001)
        assertEquals(4.0, result.totalActualHours, 0.0001)
        assertEquals(8.0, result.totalTargetHours, 0.0001)
        assertEquals(1, result.countedDays)
    }

    @Test
    fun `Arbeit und Fahrzeit kombiniert`() {
        // (12h IST, 8h SOLL, Saldo +4)
        val result = useCase(
            entries = listOf(
                overtimeEntry(
                    date = LocalDate.of(2026, 1, 5),
                    dayType = DayType.WORK,
                    confirmedWorkDay = true,
                    workStart = LocalTime.of(8, 0),
                    workEnd = LocalTime.of(16, 0), // 8h Arbeit
                    breakMinutes = 0,
                    travelPaidMinutes = 240 // 4h Fahrt
                )
            ),
            dailyTargetHours = 8.0
        )

        assertEquals(4.0, result.totalOvertimeHours, 0.0001)
        assertEquals(12.0, result.totalActualHours, 0.0001)
        assertEquals(8.0, result.totalTargetHours, 0.0001)
        assertEquals(1, result.countedDays)
    }

    @Test
    fun `frei mit Reise`() {
        // OFF-Tag (3h IST, 0h SOLL, Saldo +3)
        // Vorher: travelHours wurde nur zu offDayTravelHours addiert.
        // Jetzt: erhöht totalActualHours!
        val result = useCase(
            entries = listOf(
                overtimeEntry(
                    date = LocalDate.of(2026, 1, 6),
                    dayType = DayType.OFF,
                    confirmedWorkDay = true,
                    travelPaidMinutes = 180 // 3 Stunden
                )
            ),
            dailyTargetHours = 8.0
        )

        // 3h IST, 0 SOLL => +3 Überstunden
        assertEquals(3.0, result.totalOvertimeHours, 0.0001)
        assertEquals(3.0, result.totalActualHours, 0.0001)
        assertEquals(0.0, result.totalTargetHours, 0.0001)
        assertEquals(0, result.countedDays)
        assertEquals(3.0, result.offDayTravelHours, 0.0001)
        assertEquals(1, result.offDayTravelDays)
    }

    @Test
    fun `Ueberstunden-Abbau (COMP_TIME)`() {
        // (0h IST, 8h SOLL, Saldo -8)
        val result = useCase(
            entries = listOf(
                overtimeEntry(
                    date = LocalDate.of(2026, 1, 7),
                    dayType = DayType.COMP_TIME,
                    confirmedWorkDay = true
                )
            ),
            dailyTargetHours = 8.0
        )

        assertEquals(-8.0, result.totalOvertimeHours, 0.0001)
        assertEquals(0.0, result.totalActualHours, 0.0001)
        assertEquals(8.0, result.totalTargetHours, 0.0001)
        assertEquals(1, result.countedDays)
    }
    
    @Test
    fun `leerer Arbeitstag wird nur gezaehlt wenn explizit bestaetigt`() {
        // Unbestätigt -> Ignoriert
        val result1 = useCase(
            entries = listOf(
                overtimeEntry(
                    date = LocalDate.of(2026, 1, 8),
                    dayType = DayType.WORK,
                    confirmedWorkDay = false
                )
            ),
            dailyTargetHours = 8.0
        )
        assertEquals(0, result1.countedDays)
        
        // Bestätigt -> 0h IST, 8h SOLL, Saldo -8
        val result2 = useCase(
            entries = listOf(
                overtimeEntry(
                    date = LocalDate.of(2026, 1, 9),
                    dayType = DayType.WORK,
                    confirmedWorkDay = true
                )
            ),
            dailyTargetHours = 8.0
        )
        assertEquals(1, result2.countedDays)
        assertEquals(-8.0, result2.totalOvertimeHours, 0.0001)
    }

    private fun overtimeEntry(
        date: LocalDate,
        dayType: DayType,
        confirmedWorkDay: Boolean,
        workStart: LocalTime? = null,
        workEnd: LocalTime? = null,
        breakMinutes: Int = 0,
        travelPaidMinutes: Int = 0
    ): WorkEntryWithTravelLegs {
        val entry = WorkEntry(
            date = date,
            dayType = dayType,
            confirmedWorkDay = confirmedWorkDay,
            workStart = workStart,
            workEnd = workEnd,
            breakMinutes = breakMinutes
        )

        val travelLegs = if (travelPaidMinutes > 0) {
            listOf(
                TravelLeg(
                    workEntryDate = date,
                    sortOrder = 0,
                    paidMinutesOverride = travelPaidMinutes
                )
            )
        } else emptyList()

        return WorkEntryWithTravelLegs(workEntry = entry, travelLegs = travelLegs)
    }
}