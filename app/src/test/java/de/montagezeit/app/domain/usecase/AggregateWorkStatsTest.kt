package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.TravelLeg
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.local.entity.WorkEntryWithTravelLegs
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class AggregateWorkStatsTest {

    private val useCase = AggregateWorkStats()

    private fun entry(
        date: LocalDate,
        dayType: DayType,
        confirmed: Boolean = true,
        workStart: LocalTime = LocalTime.of(8, 0),
        workEnd: LocalTime = LocalTime.of(17, 0),
        breakMinutes: Int = 60,
        travelMinutes: Int = 0,
        mealAllowanceCents: Int = 0
    ): WorkEntryWithTravelLegs = WorkEntryWithTravelLegs(
        workEntry = WorkEntry(
            date = date,
            dayType = dayType,
            workStart = workStart,
            workEnd = workEnd,
            breakMinutes = breakMinutes,
            confirmedWorkDay = confirmed,
            mealAllowanceAmountCents = mealAllowanceCents
        ),
        travelLegs = if (travelMinutes > 0) listOf(
            TravelLeg(workEntryDate = date, sortOrder = 0, paidMinutesOverride = travelMinutes)
        ) else emptyList()
    )

    @Test
    fun `leere Liste ergibt Nullwerte`() {
        val result = useCase(emptyList())
        assertEquals(0, result.workDays)
        assertEquals(0, result.offDays)
        assertEquals(0, result.totalWorkMinutes)
        assertEquals(0, result.totalTravelMinutes)
        assertEquals(0, result.totalPaidMinutes)
        assertEquals(0, result.mealAllowanceCents)
    }

    @Test
    fun `unbestaetigte Eintraege werden ignoriert`() {
        val result = useCase(
            listOf(
                entry(LocalDate.of(2026, 1, 5), DayType.WORK, confirmed = false,
                    workStart = LocalTime.of(8, 0), workEnd = LocalTime.of(17, 0), breakMinutes = 60)
            )
        )
        assertEquals(0, result.workDays)
        assertEquals(0, result.totalWorkMinutes)
    }

    @Test
    fun `WORK Tag zaehlt workDays und Arbeitsminuten`() {
        // 08:00–17:00 – 60 min Pause = 480 min
        val result = useCase(
            listOf(
                entry(LocalDate.of(2026, 1, 6), DayType.WORK,
                    workStart = LocalTime.of(8, 0), workEnd = LocalTime.of(17, 0), breakMinutes = 60)
            )
        )
        assertEquals(1, result.workDays)
        assertEquals(480, result.totalWorkMinutes)
        assertEquals(0, result.offDays)
    }

    @Test
    fun `OFF Tag zaehlt offDays und keine Arbeitsminuten`() {
        val result = useCase(
            listOf(
                entry(LocalDate.of(2026, 1, 7), DayType.OFF,
                    workStart = LocalTime.of(8, 0), workEnd = LocalTime.of(17, 0), breakMinutes = 60)
            )
        )
        assertEquals(0, result.workDays)
        assertEquals(1, result.offDays)
        assertEquals(0, result.totalWorkMinutes)
    }

    @Test
    fun `Reisezeit auf OFF-Tag fliesst in bezahlte Wochensumme ein`() {
        val result = useCase(
            listOf(
                entry(LocalDate.of(2026, 1, 8), DayType.OFF, travelMinutes = 120)
            )
        )
        assertEquals(0, result.totalWorkMinutes)
        assertEquals(120, result.totalTravelMinutes)
        assertEquals(120, result.totalPaidMinutes)
    }

    @Test
    fun `totalPaidMinutes ist Summe aus Arbeit und Reise`() {
        // 08:00–17:00 – 60 min = 480 min work + 60 min travel = 540 paid
        val result = useCase(
            listOf(
                entry(LocalDate.of(2026, 1, 9), DayType.WORK,
                    workStart = LocalTime.of(8, 0), workEnd = LocalTime.of(17, 0),
                    breakMinutes = 60, travelMinutes = 60)
            )
        )
        assertEquals(480, result.totalWorkMinutes)
        assertEquals(60, result.totalTravelMinutes)
        assertEquals(540, result.totalPaidMinutes)
    }

    @Test
    fun `mealAllowanceCents summiert nur WORK-Eintraege`() {
        val result = useCase(
            listOf(
                entry(LocalDate.of(2026, 1, 10), DayType.WORK, mealAllowanceCents = 820),
                entry(LocalDate.of(2026, 1, 11), DayType.OFF, mealAllowanceCents = 500)
            )
        )
        assertEquals(820, result.mealAllowanceCents)
    }

    @Test
    fun `averageWorkHoursPerDay korrekt berechnet`() {
        // 2 WORK-Tage à 480 min = 960 min / 2 = 480 min = 8.0 h
        val result = useCase(
            listOf(
                entry(LocalDate.of(2026, 1, 12), DayType.WORK,
                    workStart = LocalTime.of(8, 0), workEnd = LocalTime.of(17, 0), breakMinutes = 60),
                entry(LocalDate.of(2026, 1, 13), DayType.WORK,
                    workStart = LocalTime.of(8, 0), workEnd = LocalTime.of(17, 0), breakMinutes = 60)
            )
        )
        assertEquals(8.0, result.averageWorkHoursPerDay, 0.001)
    }

    @Test
    fun `COMP_TIME Tag zaehlt in offDays und keine Arbeitsminuten`() {
        val result = useCase(
            listOf(
                entry(LocalDate.of(2026, 1, 7), DayType.COMP_TIME,
                    workStart = LocalTime.of(8, 0), workEnd = LocalTime.of(17, 0), breakMinutes = 60)
            )
        )
        assertEquals(0, result.workDays)
        assertEquals(1, result.offDays)
        assertEquals(0, result.totalWorkMinutes)
    }

    @Test
    fun `averageWorkHoursPerDay ist 0 wenn keine WORK-Tage`() {
        val result = useCase(
            listOf(entry(LocalDate.of(2026, 1, 14), DayType.OFF))
        )
        assertEquals(0.0, result.averageWorkHoursPerDay, 0.001)
    }

    @Test
    fun `Mix aus WORK OFF und unbestaetigt korrekt aggregiert`() {
        val result = useCase(
            listOf(
                // WORK, confirmed, 480 min work + 30 min travel
                entry(LocalDate.of(2026, 1, 15), DayType.WORK,
                    workStart = LocalTime.of(8, 0), workEnd = LocalTime.of(17, 0),
                    breakMinutes = 60, travelMinutes = 30, mealAllowanceCents = 820),
                // OFF, confirmed, 90 min travel (no work)
                entry(LocalDate.of(2026, 1, 16), DayType.OFF, travelMinutes = 90),
                // WORK, NOT confirmed → ignored
                entry(LocalDate.of(2026, 1, 17), DayType.WORK, confirmed = false,
                    workStart = LocalTime.of(8, 0), workEnd = LocalTime.of(20, 0), breakMinutes = 0)
            )
        )
        assertEquals(1, result.workDays)
        assertEquals(1, result.offDays)
        assertEquals(480, result.totalWorkMinutes)
        assertEquals(120, result.totalTravelMinutes)
        assertEquals(600, result.totalPaidMinutes)
        assertEquals(820, result.mealAllowanceCents)
    }
}
