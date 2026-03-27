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
    fun `COMP_TIME ohne Reise ergibt FREI`() {
        val result = classifier(
            dayType = DayType.COMP_TIME,
            workMinutes = 0,
            travelMinutes = 0
        )
        assertEquals(DayClassification.FREI, result)
    }

    @Test
    fun `COMP_TIME mit Reise ergibt FREI`() {
        // COMP_TIME mit Reise wird aktuell nicht unterstützt,
        // daher immer FREI
        val result = classifier(
            dayType = DayType.COMP_TIME,
            workMinutes = 0,
            travelMinutes = 120
        )
        assertEquals(DayClassification.FREI, result)
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
    fun `Verpflegungspauschale nur für Arbeitstage`() {
        assert(DayClassification.ARBEITSTAG_MIT_ARBEIT.isMealAllowanceEligible)
        assert(DayClassification.ARBEITSTAG_NUR_REISE.isMealAllowanceEligible)
        assert(!DayClassification.ARBEITSTAG_LEER.isMealAllowanceEligible)
        assert(!DayClassification.FREI.isMealAllowanceEligible)
        assert(!DayClassification.FREI_MIT_REISE.isMealAllowanceEligible)
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
    // ClassifiedDay
    // -------------------------------------------------------------------------

    @Test
    fun `ClassifiedDay from entry berechnet alle Werte korrekt`() {
        val entry = createEntry(
            date = LocalDate.of(2026, 3, 30),
            dayType = DayType.WORK,
            workStart = LocalTime.of(8, 0),
            workEnd = LocalTime.of(17, 0),
            breakMinutes = 60,
            travelMinutes = 120,
            mealAllowanceCents = 820,
            confirmed = true
        )

        val classified = ClassifiedDay.from(entry)

        assertEquals(LocalDate.of(2026, 3, 30), classified.date)
        assertEquals(DayClassification.ARBEITSTAG_MIT_ARBEIT, classified.classification)
        assertEquals(480, classified.workMinutes)
        assertEquals(120, classified.travelMinutes)
        assertEquals(600, classified.paidMinutes)
        assertEquals(820, classified.mealAllowanceCents)
        assert(classified.confirmed)
    }

    @Test
    fun `ClassifiedDay Stundenberechnung korrekt`() {
        val classified = ClassifiedDay(
            date = LocalDate.of(2026, 3, 31),
            classification = DayClassification.ARBEITSTAG_MIT_ARBEIT,
            workMinutes = 480,
            travelMinutes = 120,
            paidMinutes = 600,
            mealAllowanceCents = 0,
            confirmed = true
        )

        assertEquals(8.0, classified.workHours, 0.001)
        assertEquals(2.0, classified.travelHours, 0.001)
        assertEquals(10.0, classified.paidHours, 0.001)
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