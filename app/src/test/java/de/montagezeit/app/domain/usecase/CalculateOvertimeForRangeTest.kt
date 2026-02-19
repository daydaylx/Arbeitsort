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
    }

    @Test
    fun `freier tag mit ist zeit erhoeht ueberstunden`() {
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

        assertEquals(2.0, result.totalOvertimeHours, 0.0001)
        assertEquals(2.0, result.totalActualHours, 0.0001)
        assertEquals(0.0, result.totalTargetHours, 0.0001)
        assertEquals(1, result.countedDays)
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
    }

    @Test
    fun `mix aus mehreren tagen`() {
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

        assertEquals(1.5, result.totalOvertimeHours, 0.0001)
        assertEquals(17.5, result.totalActualHours, 0.0001)
        assertEquals(16.0, result.totalTargetHours, 0.0001)
        assertEquals(3, result.countedDays)
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
