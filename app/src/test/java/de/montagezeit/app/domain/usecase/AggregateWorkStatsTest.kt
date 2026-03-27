package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.TravelLeg
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.local.entity.WorkEntryWithTravelLegs
import de.montagezeit.app.domain.model.DayClassification
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
        workStart: LocalTime? = LocalTime.of(8, 0),
        workEnd: LocalTime? = LocalTime.of(17, 0),
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
        // Neue Metriken
        assertEquals(0, result.workDaysWithWork)
        assertEquals(0, result.workDaysTravelOnly)
        assertEquals(0, result.workDaysEmpty)
        assertEquals(0, result.freeDaysWithTravel)
        assertEquals(0, result.freeDaysWithoutTravel)
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

    // -------------------------------------------------------------------------
    // Neue Tests für DayClassification-basierte Metriken
    // -------------------------------------------------------------------------

    @Test
    fun `ARBEITSTAG_MIT_ARBEIT wird korrekt klassifiziert`() {
        val result = useCase(
            listOf(
                entry(LocalDate.of(2026, 3, 1), DayType.WORK,
                    workStart = LocalTime.of(8, 0), workEnd = LocalTime.of(17, 0),
                    breakMinutes = 60, travelMinutes = 0)
            )
        )
        assertEquals(1, result.workDaysWithWork)
        assertEquals(0, result.workDaysTravelOnly)
        assertEquals(0, result.workDaysEmpty)
    }

    @Test
    fun `ARBEITSTAG_MIT_ARBEIT mit Reise wird korrekt klassifiziert`() {
        val result = useCase(
            listOf(
                entry(LocalDate.of(2026, 3, 2), DayType.WORK,
                    workStart = LocalTime.of(8, 0), workEnd = LocalTime.of(17, 0),
                    breakMinutes = 60, travelMinutes = 120)
            )
        )
        assertEquals(1, result.workDaysWithWork)
        assertEquals(0, result.workDaysTravelOnly)
        assertEquals(0, result.workDaysEmpty)
    }

    @Test
    fun `ARBEITSTAG_NUR_REISE wird korrekt klassifiziert`() {
        // Arbeitstag ohne Arbeitszeit, aber mit Reisezeit
        val result = useCase(
            listOf(
                entry(LocalDate.of(2026, 3, 3), DayType.WORK,
                    workStart = null, workEnd = null,
                    breakMinutes = 0, travelMinutes = 180, mealAllowanceCents = 820)
            )
        )
        assertEquals(0, result.workDaysWithWork)
        assertEquals(1, result.workDaysTravelOnly)
        assertEquals(0, result.workDaysEmpty)
        // Sollte immer noch als workDay zählen
        assertEquals(1, result.workDays)
        // Verpflegungspauschale ist erlaubt
        assertEquals(820, result.mealAllowanceCents)
        // Reisezeit wird berücksichtigt
        assertEquals(180, result.totalTravelMinutes)
        assertEquals(180, result.totalPaidMinutes)
    }

    @Test
    fun `ARBEITSTAG_LEER wird korrekt klassifiziert`() {
        // Arbeitstag ohne Arbeitszeit und ohne Reisezeit
        val result = useCase(
            listOf(
                entry(LocalDate.of(2026, 3, 4), DayType.WORK,
                    workStart = null, workEnd = null,
                    breakMinutes = 0, travelMinutes = 0, mealAllowanceCents = 500)
            )
        )
        assertEquals(0, result.workDaysWithWork)
        assertEquals(0, result.workDaysTravelOnly)
        assertEquals(1, result.workDaysEmpty)
        // Sollte als workDay zählen
        assertEquals(1, result.workDays)
        // Keine Verpflegungspauschale bei leerem Tag
        assertEquals(0, result.mealAllowanceCents)
    }

    @Test
    fun `FREI wird korrekt klassifiziert`() {
        val result = useCase(
            listOf(
                entry(LocalDate.of(2026, 3, 5), DayType.OFF,
                    workStart = null, workEnd = null,
                    breakMinutes = 0, travelMinutes = 0)
            )
        )
        assertEquals(0, result.freeDaysWithTravel)
        assertEquals(1, result.freeDaysWithoutTravel)
        assertEquals(1, result.offDays)
    }

    @Test
    fun `FREI_MIT_REISE wird korrekt klassifiziert`() {
        val result = useCase(
            listOf(
                entry(LocalDate.of(2026, 3, 6), DayType.OFF,
                    workStart = null, workEnd = null,
                    breakMinutes = 0, travelMinutes = 90)
            )
        )
        assertEquals(1, result.freeDaysWithTravel)
        assertEquals(0, result.freeDaysWithoutTravel)
        assertEquals(1, result.offDays)
        // Reisezeit wird berücksichtigt
        assertEquals(90, result.totalTravelMinutes)
    }

    @Test
    fun `alle Klassifikationen in einem Mix`() {
        val result = useCase(
            listOf(
                // ARBEITSTAG_MIT_ARBEIT (nur Arbeit)
                entry(LocalDate.of(2026, 3, 7), DayType.WORK,
                    workStart = LocalTime.of(8, 0), workEnd = LocalTime.of(17, 0),
                    breakMinutes = 60, travelMinutes = 0),
                // ARBEITSTAG_MIT_ARBEIT (Arbeit + Reise)
                entry(LocalDate.of(2026, 3, 8), DayType.WORK,
                    workStart = LocalTime.of(8, 0), workEnd = LocalTime.of(17, 0),
                    breakMinutes = 60, travelMinutes = 60),
                // ARBEITSTAG_NUR_REISE
                entry(LocalDate.of(2026, 3, 9), DayType.WORK,
                    workStart = null, workEnd = null,
                    breakMinutes = 0, travelMinutes = 180, mealAllowanceCents = 820),
                // ARBEITSTAG_LEER
                entry(LocalDate.of(2026, 3, 10), DayType.WORK,
                    workStart = null, workEnd = null,
                    breakMinutes = 0, travelMinutes = 0),
                // FREI
                entry(LocalDate.of(2026, 3, 11), DayType.OFF,
                    workStart = null, workEnd = null,
                    breakMinutes = 0, travelMinutes = 0),
                // FREI_MIT_REISE
                entry(LocalDate.of(2026, 3, 12), DayType.OFF,
                    workStart = null, workEnd = null,
                    breakMinutes = 0, travelMinutes = 120)
            )
        )
        
        // Klassifikationen
        assertEquals(2, result.workDaysWithWork)
        assertEquals(1, result.workDaysTravelOnly)
        assertEquals(1, result.workDaysEmpty)
        assertEquals(1, result.freeDaysWithTravel)
        assertEquals(1, result.freeDaysWithoutTravel)
        
        // Klassische Metriken
        assertEquals(4, result.workDays) // 2 + 1 + 1
        assertEquals(2, result.offDays)  // 1 + 1
        
        // Zeitwerte
        assertEquals(960, result.totalWorkMinutes) // 480 + 480
        assertEquals(360, result.totalTravelMinutes) // 60 + 180 + 120
        assertEquals(1320, result.totalPaidMinutes)
        
        // Verpflegungspauschale nur für ARBEITSTAG_NUR_REISE
        assertEquals(820, result.mealAllowanceCents)
    }

    @Test
    fun `averagePaidHoursPerWorkDay berechnet korrekt`() {
        // 2 Arbeitstage: einer mit 8h Arbeit, einer mit 3h Reise
        val result = useCase(
            listOf(
                entry(LocalDate.of(2026, 3, 13), DayType.WORK,
                    workStart = LocalTime.of(8, 0), workEnd = LocalTime.of(17, 0),
                    breakMinutes = 60, travelMinutes = 0),
                entry(LocalDate.of(2026, 3, 14), DayType.WORK,
                    workStart = null, workEnd = null,
                    breakMinutes = 0, travelMinutes = 180)
            )
        )
        // (480 + 180) / 60 / 2 = 11.0 hours / 2 = 5.5
        assertEquals(5.5, result.averagePaidHoursPerWorkDay, 0.001)
    }

    @Test
    fun `averageWorkHoursPerWorkDayWithWork exkludiert Reisetage`() {
        // 3 Arbeitstage: 2 mit Arbeit, 1 nur Reise
        val result = useCase(
            listOf(
                entry(LocalDate.of(2026, 3, 15), DayType.WORK,
                    workStart = LocalTime.of(8, 0), workEnd = LocalTime.of(17, 0),
                    breakMinutes = 60, travelMinutes = 0),
                entry(LocalDate.of(2026, 3, 16), DayType.WORK,
                    workStart = LocalTime.of(8, 0), workEnd = LocalTime.of(16, 0),
                    breakMinutes = 60, travelMinutes = 0),
                entry(LocalDate.of(2026, 3, 17), DayType.WORK,
                    workStart = null, workEnd = null,
                    breakMinutes = 0, travelMinutes = 180)
            )
        )
        // (480 + 420) / 60 / 2 = 15 hours / 2 = 7.5
        assertEquals(7.5, result.averageWorkHoursPerWorkDayWithWork, 0.001)
    }
}
