package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.dao.OvertimeEntryRow
import de.montagezeit.app.data.local.entity.DayType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class CalculateOvertimeForRangeTest {

    private val useCase = CalculateOvertimeForRange()

    @Test
    fun `normaler arbeitstag mit positiven ueberstunden`() {
        val result = useCase(
            entries = listOf(
                overtimeEntry(
                    date = LocalDate.of(2026, 1, 3),
                    dayType = DayType.WORK,
                    confirmedWorkDay = true,
                    workStart = LocalTime.of(8, 0),
                    workEnd = LocalTime.of(18, 0),
                    breakMinutes = 60
                )
            ),
            dailyTargetHours = 8.0
        )

        assertEquals(1.0, result.totalOvertimeHours, 0.0001)
        assertEquals(9.0, result.totalActualHours, 0.0001)
        assertEquals(8.0, result.totalTargetHours, 0.0001)
        assertEquals(1, result.countedDays)
        assertEquals(0.0, result.offDayTravelHours, 0.0001)
        assertEquals(0, result.offDayTravelDays)
    }

    @Test
    fun `minusstunden werden korrekt berechnet`() {
        val result = useCase(
            entries = listOf(
                overtimeEntry(
                    date = LocalDate.of(2026, 1, 4),
                    dayType = DayType.WORK,
                    confirmedWorkDay = true,
                    workStart = LocalTime.of(8, 0),
                    workEnd = LocalTime.of(15, 0),
                    breakMinutes = 60
                )
            ),
            dailyTargetHours = 8.0
        )

        assertEquals(-2.0, result.totalOvertimeHours, 0.0001)
        assertEquals(6.0, result.totalActualHours, 0.0001)
        assertEquals(8.0, result.totalTargetHours, 0.0001)
        assertEquals(1, result.countedDays)
        assertEquals(0.0, result.offDayTravelHours, 0.0001)
        assertEquals(0, result.offDayTravelDays)
    }

    @Test
    fun `freier tag mit fahrzeit wird separat gezaehlt`() {
        val result = useCase(
            entries = listOf(
                overtimeEntry(
                    date = LocalDate.of(2026, 1, 5),
                    dayType = DayType.OFF,
                    confirmedWorkDay = true,
                    travelPaidMinutes = 120
                )
            ),
            dailyTargetHours = 8.0
        )

        assertEquals(0.0, result.totalOvertimeHours, 0.0001)
        assertEquals(0.0, result.totalActualHours, 0.0001)
        assertEquals(0.0, result.totalTargetHours, 0.0001)
        assertEquals(0, result.countedDays)
        assertEquals(2.0, result.offDayTravelHours, 0.0001)
        assertEquals(1, result.offDayTravelDays)
    }

    @Test
    fun `unbestaetigter tag wird ignoriert`() {
        val result = useCase(
            entries = listOf(
                overtimeEntry(
                    date = LocalDate.of(2026, 1, 6),
                    dayType = DayType.WORK,
                    confirmedWorkDay = false,
                    workStart = LocalTime.of(8, 0),
                    workEnd = LocalTime.of(18, 0),
                    breakMinutes = 60
                ),
                overtimeEntry(
                    date = LocalDate.of(2026, 1, 7),
                    dayType = DayType.WORK,
                    confirmedWorkDay = true,
                    workStart = LocalTime.of(8, 0),
                    workEnd = LocalTime.of(17, 0),
                    breakMinutes = 60
                )
            ),
            dailyTargetHours = 8.0
        )

        assertEquals(0.0, result.totalOvertimeHours, 0.0001)
        assertEquals(8.0, result.totalActualHours, 0.0001)
        assertEquals(8.0, result.totalTargetHours, 0.0001)
        assertEquals(1, result.countedDays)
        assertEquals(0.0, result.offDayTravelHours, 0.0001)
        assertEquals(0, result.offDayTravelDays)
    }

    @Test
    fun `mix aus mehreren tagen trennt offday fahrzeit von ueberstunden`() {
        val result = useCase(
            entries = listOf(
                overtimeEntry(
                    date = LocalDate.of(2026, 1, 8),
                    dayType = DayType.WORK,
                    confirmedWorkDay = true,
                    workStart = LocalTime.of(8, 0),
                    workEnd = LocalTime.of(18, 0),
                    breakMinutes = 60
                ),
                overtimeEntry(
                    date = LocalDate.of(2026, 1, 9),
                    dayType = DayType.WORK,
                    confirmedWorkDay = true,
                    workStart = LocalTime.of(8, 0),
                    workEnd = LocalTime.of(16, 0),
                    breakMinutes = 60
                ),
                overtimeEntry(
                    date = LocalDate.of(2026, 1, 10),
                    dayType = DayType.OFF,
                    confirmedWorkDay = true,
                    travelPaidMinutes = 90
                ),
                overtimeEntry(
                    date = LocalDate.of(2026, 1, 11),
                    dayType = DayType.WORK,
                    confirmedWorkDay = false,
                    workStart = LocalTime.of(8, 0),
                    workEnd = LocalTime.of(20, 0),
                    breakMinutes = 60
                )
            ),
            dailyTargetHours = 8.0
        )

        assertEquals(0.0, result.totalOvertimeHours, 0.0001)
        assertEquals(16.0, result.totalActualHours, 0.0001)
        assertEquals(16.0, result.totalTargetHours, 0.0001)
        assertEquals(2, result.countedDays)
        assertEquals(1.5, result.offDayTravelHours, 0.0001)
        assertEquals(1, result.offDayTravelDays)
    }

    @Test
    fun `offday ohne fahrzeit erhoeht offday travel zaehler nicht`() {
        val result = useCase(
            entries = listOf(
                overtimeEntry(
                    date = LocalDate.of(2026, 1, 12),
                    dayType = DayType.OFF,
                    confirmedWorkDay = true
                )
            ),
            dailyTargetHours = 8.0
        )

        assertEquals(0.0, result.totalOvertimeHours, 0.0001)
        assertEquals(0.0, result.totalActualHours, 0.0001)
        assertEquals(0.0, result.totalTargetHours, 0.0001)
        assertEquals(0, result.countedDays)
        assertEquals(0.0, result.offDayTravelHours, 0.0001)
        assertEquals(0, result.offDayTravelDays)
    }

    // -------------------------------------------------------------------------
    // COMP_TIME
    // -------------------------------------------------------------------------

    @Test
    fun `COMP_TIME tag reduziert overtime bank um targetMinutes`() {
        val result = useCase(
            entries = listOf(
                overtimeEntry(
                    date = LocalDate.of(2026, 3, 1),
                    dayType = DayType.COMP_TIME,
                    confirmedWorkDay = true
                )
            ),
            dailyTargetHours = 8.0
        )

        // Net delta = 0 actual - 8 target = -8 h
        assertEquals(-8.0, result.totalOvertimeHours, 0.0001)
        assertEquals(0.0, result.totalActualHours, 0.0001)
        assertEquals(8.0, result.totalTargetHours, 0.0001)
        assertEquals(1, result.countedDays)
        assertEquals(0.0, result.offDayTravelHours, 0.0001)
        assertEquals(0, result.offDayTravelDays)
    }

    @Test
    fun `COMP_TIME tag mit anderem target korrekt berechnet`() {
        val result = useCase(
            entries = listOf(
                overtimeEntry(
                    date = LocalDate.of(2026, 3, 2),
                    dayType = DayType.COMP_TIME,
                    confirmedWorkDay = true
                )
            ),
            dailyTargetHours = 10.0
        )

        assertEquals(-10.0, result.totalOvertimeHours, 0.0001)
        assertEquals(0.0, result.totalActualHours, 0.0001)
        assertEquals(10.0, result.totalTargetHours, 0.0001)
        assertEquals(1, result.countedDays)
    }

    @Test
    fun `COMP_TIME und WORK tag kombiniert berechnet korrekt`() {
        val result = useCase(
            entries = listOf(
                overtimeEntry(
                    date = LocalDate.of(2026, 3, 3),
                    dayType = DayType.WORK,
                    confirmedWorkDay = true,
                    workStart = LocalTime.of(8, 0),
                    workEnd = LocalTime.of(18, 0),
                    breakMinutes = 60
                ),
                overtimeEntry(
                    date = LocalDate.of(2026, 3, 4),
                    dayType = DayType.COMP_TIME,
                    confirmedWorkDay = true
                )
            ),
            dailyTargetHours = 8.0
        )

        // WORK day: 9h actual - 8h target = +1h
        // COMP_TIME day: 0h actual - 8h target = -8h
        // Total: 9h actual, 16h target, -7h overtime
        assertEquals(-7.0, result.totalOvertimeHours, 0.0001)
        assertEquals(9.0, result.totalActualHours, 0.0001)
        assertEquals(16.0, result.totalTargetHours, 0.0001)
        assertEquals(2, result.countedDays)
    }

    @Test
    fun `unbestaetiger COMP_TIME tag wird ignoriert`() {
        val result = useCase(
            entries = listOf(
                overtimeEntry(
                    date = LocalDate.of(2026, 3, 5),
                    dayType = DayType.COMP_TIME,
                    confirmedWorkDay = false
                )
            ),
            dailyTargetHours = 8.0
        )

        // confirmedWorkDay == false → skip
        assertEquals(0.0, result.totalOvertimeHours, 0.0001)
        assertEquals(0, result.countedDays)
    }

    private fun overtimeEntry(
        date: LocalDate,
        dayType: DayType,
        confirmedWorkDay: Boolean,
        workStart: LocalTime = LocalTime.of(8, 0),
        workEnd: LocalTime = LocalTime.of(8, 0),
        breakMinutes: Int = 0,
        travelPaidMinutes: Int? = null
    ): OvertimeEntryRow {
        return OvertimeEntryRow(
            date = date,
            workStart = workStart,
            workEnd = workEnd,
            breakMinutes = breakMinutes,
            dayType = dayType,
            confirmedWorkDay = confirmedWorkDay,
            travelStartAt = null,
            travelArriveAt = null,
            travelPaidMinutes = travelPaidMinutes
        )
    }
}
