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

class ClassifyDayTest {

    private val classifier = ClassifyDay()

    // -------------------------------------------------------------------------
    // WORK-Tage
    // -------------------------------------------------------------------------

    @Test
    fun `WORK mit Arbeitszeit und ohne Reise ergibt ARBEITSTAG_MIT_ARBEIT`() {
        val result = classifier(
            dayType = DayType.WORK,
            workMinutes = 480,
            travelMinutes = 0
        )
        assertEquals(DayClassification.ARBEITSTAG_MIT_ARBEIT, result)
    }

    @Test
    fun `WORK mit Arbeitszeit und mit Reise ergibt ARBEITSTAG_MIT_ARBEIT`() {
        val result = classifier(
            dayType = DayType.WORK,
            workMinutes = 480,
            travelMinutes = 120
        )
        assertEquals(DayClassification.ARBEITSTAG_MIT_ARBEIT, result)
    }

    @Test
    fun `WORK ohne Arbeitszeit aber mit Reise ergibt ARBEITSTAG_NUR_REISE`() {
        val result = classifier(
            dayType = DayType.WORK,
            workMinutes = 0,
            travelMinutes = 180
        )
        assertEquals(DayClassification.ARBEITSTAG_NUR_REISE, result)
    }

    @Test
    fun `WORK ohne Arbeitszeit und ohne Reise ergibt ARBEITSTAG_LEER`() {
        val result = classifier(
            dayType = DayType.WORK,
            workMinutes = 0,
            travelMinutes = 0
        )
        assertEquals(DayClassification.ARBEITSTAG_LEER, result)
    }

    // -------------------------------------------------------------------------
    // OFF-Tage
    // -------------------------------------------------------------------------

    @Test
    fun `OFF ohne Reise ergibt FREI`() {
        val result = classifier(
            dayType = DayType.OFF,
            workMinutes = 0,
            travelMinutes = 0
        )
        assertEquals(DayClassification.FREI, result)
    }

    @Test
    fun `OFF mit Reise ergibt FREI_MIT_REISE`() {
        val result = classifier(
            dayType = DayType.OFF,
            workMinutes = 0,
            travelMinutes = 120
        )
        assertEquals(DayClassification.FREI_MIT_REISE, result)
    }

    @Test
    fun `OFF mit Arbeitszeit (untypisch) wird ignoriert und ergibt FREI_MIT_REISE wenn Reise`() {
        // Edge case: Arbeitszeit an einem OFF-Tag sollte nicht vorkommen,
        // aber wenn doch, wird die Reisezeit priorisiert für die Klassifikation
        val result = classifier(
            dayType = DayType.OFF,
            workMinutes = 480, // Wird ignoriert bei OFF
            travelMinutes = 60
        )
        assertEquals(DayClassification.FREI_MIT_REISE, result)
    }

    // -------------------------------------------------------------------------
    // COMP_TIME-Tage
    // -------------------------------------------------------------------------

    @Test
    fun `COMP_TIME ohne Reise ergibt UEBERSTUNDEN_ABBAU`() {
        val result = classifier(
            dayType = DayType.COMP_TIME,
            workMinutes = 0,
            travelMinutes = 0
        )
        assertEquals(DayClassification.UEBERSTUNDEN_ABBAU, result)
    }

    @Test
    fun `COMP_TIME mit Reise ergibt UEBERSTUNDEN_ABBAU`() {
        val result = classifier(
            dayType = DayType.COMP_TIME,
            workMinutes = 0,
            travelMinutes = 120
        )
        assertEquals(DayClassification.UEBERSTUNDEN_ABBAU, result)
    }

    // -------------------------------------------------------------------------
    // DayClassification Properties
    // -------------------------------------------------------------------------

    @Test
    fun `ARBEITSTAG_MIT_ARBEIT ist gezählter Arbeitstag`() {
        assert(DayClassification.ARBEITSTAG_MIT_ARBEIT.isCountedWorkDay)
        assert(DayClassification.ARBEITSTAG_MIT_ARBEIT.hasWorkTime)
        assert(DayClassification.ARBEITSTAG_MIT_ARBEIT.canHaveTravelTime) // kann Reisezeit haben
        assert(!DayClassification.ARBEITSTAG_MIT_ARBEIT.hasTravelTime) // muss nicht haben
    }

    @Test
    fun `ARBEITSTAG_NUR_REISE ist gezählter Arbeitstag ohne Arbeitszeit`() {
        assert(DayClassification.ARBEITSTAG_NUR_REISE.isCountedWorkDay)
        assert(!DayClassification.ARBEITSTAG_NUR_REISE.hasWorkTime)
        assert(DayClassification.ARBEITSTAG_NUR_REISE.hasTravelTime)
    }

    @Test
    fun `ARBEITSTAG_LEER ist gezählter Arbeitstag aber leer`() {
        assert(DayClassification.ARBEITSTAG_LEER.isCountedWorkDay)
        assert(!DayClassification.ARBEITSTAG_LEER.hasWorkTime)
        assert(!DayClassification.ARBEITSTAG_LEER.hasTravelTime)
    }

    @Test
    fun `FREI ist kein gezählter Arbeitstag`() {
        assert(!DayClassification.FREI.isCountedWorkDay)
        assert(!DayClassification.FREI.hasWorkTime)
        assert(!DayClassification.FREI.hasTravelTime)
    }

    @Test
    fun `FREI_MIT_REISE hat Reise aber ist kein Arbeitstag`() {
        assert(!DayClassification.FREI_MIT_REISE.isCountedWorkDay)
        assert(!DayClassification.FREI_MIT_REISE.hasWorkTime)
        assert(DayClassification.FREI_MIT_REISE.hasTravelTime)
    }

    @Test
    fun `Verpflegungspauschale fuer Arbeitstage und Reisetage`() {
        assert(DayClassification.ARBEITSTAG_MIT_ARBEIT.isMealAllowanceEligible)
        assert(DayClassification.ARBEITSTAG_NUR_REISE.isMealAllowanceEligible)
        assert(DayClassification.FREI_MIT_REISE.isMealAllowanceEligible)
        assert(!DayClassification.ARBEITSTAG_LEER.isMealAllowanceEligible)
        assert(!DayClassification.FREI.isMealAllowanceEligible)
        assert(!DayClassification.UEBERSTUNDEN_ABBAU.isMealAllowanceEligible)
    }

    // -------------------------------------------------------------------------
    // WorkEntryWithTravelLegs Integration
    // -------------------------------------------------------------------------

    @Test
    fun `klassifiziere vollstaendigen WorkEntry mit Arbeit und Reise`() {
        val entry = createEntry(
            date = LocalDate.of(2026, 3, 27),
            dayType = DayType.WORK,
            workStart = LocalTime.of(8, 0),
            workEnd = LocalTime.of(17, 0),
            breakMinutes = 60,
            travelMinutes = 120
        )

        val result = classifier(entry)

        assertEquals(DayClassification.ARBEITSTAG_MIT_ARBEIT, result)
    }

    @Test
    fun `klassifiziere WorkEntry nur mit Reise`() {
        val entry = createEntry(
            date = LocalDate.of(2026, 3, 27),
            dayType = DayType.WORK,
            workStart = null,
            workEnd = null,
            breakMinutes = 0,
            travelMinutes = 180
        )

        val result = classifier(entry)

        assertEquals(DayClassification.ARBEITSTAG_NUR_REISE, result)
    }

    @Test
    fun `klassifiziere OFF-Tag mit Heimreise`() {
        val entry = createEntry(
            date = LocalDate.of(2026, 3, 28),
            dayType = DayType.OFF,
            workStart = null,
            workEnd = null,
            breakMinutes = 0,
            travelMinutes = 90
        )

        val result = classifier(entry)

        assertEquals(DayClassification.FREI_MIT_REISE, result)
    }

    @Test
    fun `extension function classifyDay funktioniert`() {
        val entry = createEntry(
            date = LocalDate.of(2026, 3, 29),
            dayType = DayType.WORK,
            workStart = LocalTime.of(8, 0),
            workEnd = LocalTime.of(16, 0),
            breakMinutes = 30,
            travelMinutes = 0
        )

        val result = entry.classifyDay()

        assertEquals(DayClassification.ARBEITSTAG_MIT_ARBEIT, result)
    }

    // -------------------------------------------------------------------------
    // Helper Functions
    // -------------------------------------------------------------------------

    private fun createEntry(
        date: LocalDate,
        dayType: DayType,
        workStart: LocalTime?,
        workEnd: LocalTime?,
        breakMinutes: Int,
        travelMinutes: Int,
        mealAllowanceCents: Int = 0,
        confirmed: Boolean = true
    ): WorkEntryWithTravelLegs {
        return WorkEntryWithTravelLegs(
            workEntry = WorkEntry(
                date = date,
                dayType = dayType,
                workStart = workStart,
                workEnd = workEnd,
                breakMinutes = breakMinutes,
                confirmedWorkDay = confirmed,
                mealAllowanceAmountCents = mealAllowanceCents
            ),
            travelLegs = if (travelMinutes > 0) {
                listOf(TravelLeg(workEntryDate = date, sortOrder = 0, paidMinutesOverride = travelMinutes))
            } else {
                emptyList()
            }
        )
    }
}