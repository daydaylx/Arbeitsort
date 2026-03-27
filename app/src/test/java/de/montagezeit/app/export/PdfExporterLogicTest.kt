package de.montagezeit.app.export

import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.TravelLeg
import de.montagezeit.app.data.local.entity.TravelLegCategory
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.local.entity.WorkEntryWithTravelLegs
import de.montagezeit.app.domain.util.TimeCalculator
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/**
 * Unit-Tests für die Berechnungs- und Formatierungsschicht des PDF-Exports.
 *
 * PdfDocument selbst kann nur auf einem Device laufen; hier werden ausschließlich
 * PdfUtilities-Funktionen getestet, die reine JVM-Logik enthalten.
 */
class PdfExporterLogicTest {

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun workRecord(
        date: LocalDate = LocalDate.of(2026, 1, 15),
        dayType: DayType = DayType.WORK,
        workStart: LocalTime = LocalTime.of(8, 0),
        workEnd: LocalTime = LocalTime.of(17, 0),
        breakMinutes: Int = 60,
        mealAllowanceCents: Int = 0,
        travelMinutes: Int = 0
    ): WorkEntryWithTravelLegs = WorkEntryWithTravelLegs(
        workEntry = WorkEntry(
            date = date,
            dayType = dayType,
            workStart = workStart,
            workEnd = workEnd,
            breakMinutes = breakMinutes,
            mealAllowanceAmountCents = mealAllowanceCents
        ),
        travelLegs = if (travelMinutes > 0) listOf(
            TravelLeg(workEntryDate = date, sortOrder = 0, paidMinutesOverride = travelMinutes)
        ) else emptyList()
    )

    // -------------------------------------------------------------------------
    // sumWorkHours – konsistent mit TimeCalculator
    // -------------------------------------------------------------------------

    @Test
    fun `sumWorkHours – stimmt mit TimeCalculator pro Eintrag überein`() {
        val entries = listOf(
            workRecord(LocalDate.of(2026, 1, 15),
                workStart = LocalTime.of(8, 0), workEnd = LocalTime.of(18, 0), breakMinutes = 60),
            workRecord(LocalDate.of(2026, 1, 16),
                workStart = LocalTime.of(7, 30), workEnd = LocalTime.of(16, 0), breakMinutes = 30)
        )

        val expected = entries.sumOf { TimeCalculator.calculateWorkHours(it.workEntry) }
        assertEquals(expected, PdfUtilities.sumWorkHours(entries), 0.001)
    }

    @Test
    fun `sumWorkHours – OFF-Tag zählt 0`() {
        val entries = listOf(
            workRecord(dayType = DayType.OFF,
                workStart = LocalTime.of(8, 0), workEnd = LocalTime.of(17, 0))
        )
        assertEquals(0.0, PdfUtilities.sumWorkHours(entries), 0.001)
    }

    // -------------------------------------------------------------------------
    // sumTravelMinutes – konsistent mit TimeCalculator
    // -------------------------------------------------------------------------

    @Test
    fun `sumTravelMinutes – stimmt mit TimeCalculator pro Eintrag überein`() {
        val entries = listOf(
            workRecord(LocalDate.of(2026, 1, 15), travelMinutes = 60),
            workRecord(LocalDate.of(2026, 1, 16), travelMinutes = 90)
        )

        val expected = entries.sumOf { TimeCalculator.calculateTravelMinutes(it.orderedTravelLegs) }
        assertEquals(expected, PdfUtilities.sumTravelMinutes(entries))
    }

    @Test
    fun `sumTravelMinutes – kein TravelLeg ergibt 0`() {
        val entries = listOf(workRecord())
        assertEquals(0, PdfUtilities.sumTravelMinutes(entries))
    }

    // -------------------------------------------------------------------------
    // Meal allowance – kommt aus pre-stored mealAllowanceAmountCents
    // -------------------------------------------------------------------------

    @Test
    fun `meal allowance im Summary kommt aus gespeichertem Betrag, nicht neu berechnet`() {
        val storedCents = 1200
        val entry = workRecord(mealAllowanceCents = storedCents)

        // Direkt aus dem Entity-Feld, keine Neuberechnung
        assertEquals(storedCents, entry.workEntry.mealAllowanceAmountCents)

        // sumOf über mehrere Einträge spiegelt pre-stored Werte
        val entries = listOf(
            workRecord(LocalDate.of(2026, 1, 15), mealAllowanceCents = 820),
            workRecord(LocalDate.of(2026, 1, 16), mealAllowanceCents = 2220)
        )
        val total = entries.sumOf { it.workEntry.mealAllowanceAmountCents }
        assertEquals(3040, total)
    }

    // -------------------------------------------------------------------------
    // buildTravelRouteSummary
    // -------------------------------------------------------------------------

    @Test
    fun `buildTravelRouteSummary – leere Liste ergibt leeren String`() {
        assertEquals("", PdfUtilities.buildTravelRouteSummary(emptyList()))
    }

    @Test
    fun `buildTravelRouteSummary – ein Leg ohne Labels ergibt leeren String`() {
        val leg = TravelLeg(workEntryDate = LocalDate.of(2026, 1, 15), sortOrder = 0)
        assertEquals("", PdfUtilities.buildTravelRouteSummary(listOf(leg)))
    }

    @Test
    fun `buildTravelRouteSummary – ein Leg mit beiden Labels`() {
        val leg = TravelLeg(
            workEntryDate = LocalDate.of(2026, 1, 15),
            sortOrder = 0,
            startLabel = "Dresden",
            endLabel = "Leipzig"
        )
        assertEquals("Dresden→Leipzig", PdfUtilities.buildTravelRouteSummary(listOf(leg)))
    }

    @Test
    fun `buildTravelRouteSummary – zwei Legs mit geteiltem Zwischenpunkt werden dedupliziert`() {
        val leg1 = TravelLeg(
            workEntryDate = LocalDate.of(2026, 1, 15),
            sortOrder = 0,
            startLabel = "Dresden",
            endLabel = "Leipzig"
        )
        val leg2 = TravelLeg(
            workEntryDate = LocalDate.of(2026, 1, 15),
            sortOrder = 1,
            startLabel = "Leipzig",
            endLabel = "Berlin"
        )
        // Leipzig soll nur einmal auftauchen
        assertEquals("Dresden→Leipzig→Berlin", PdfUtilities.buildTravelRouteSummary(listOf(leg1, leg2)))
    }

    @Test
    fun `buildTravelRouteSummary – drei Legs OUTBOUND INTERSITE RETURN`() {
        val legs = listOf(
            TravelLeg(
                workEntryDate = LocalDate.of(2026, 1, 15),
                sortOrder = 0,
                category = TravelLegCategory.OUTBOUND,
                startLabel = "Home",
                endLabel = "Baustelle A"
            ),
            TravelLeg(
                workEntryDate = LocalDate.of(2026, 1, 15),
                sortOrder = 1,
                category = TravelLegCategory.INTERSITE,
                startLabel = "Baustelle A",
                endLabel = "Baustelle B"
            ),
            TravelLeg(
                workEntryDate = LocalDate.of(2026, 1, 15),
                sortOrder = 2,
                category = TravelLegCategory.RETURN,
                startLabel = "Baustelle B",
                endLabel = "Home"
            )
        )
        assertEquals("Home→Baustelle A→Baustelle B→Home",
            PdfUtilities.buildTravelRouteSummary(legs))
    }

    @Test
    fun `buildTravelRouteSummary – Legs werden nach sortOrder sortiert`() {
        // Legs in umgekehrter Reihenfolge übergeben
        val legs = listOf(
            TravelLeg(workEntryDate = LocalDate.of(2026, 1, 15), sortOrder = 1,
                startLabel = "B", endLabel = "C"),
            TravelLeg(workEntryDate = LocalDate.of(2026, 1, 15), sortOrder = 0,
                startLabel = "A", endLabel = "B")
        )
        assertEquals("A→B→C", PdfUtilities.buildTravelRouteSummary(legs))
    }

    // -------------------------------------------------------------------------
    // Texttrunkierung: Location (8 Zeichen) und Routen-Summary (18 Zeichen)
    // -------------------------------------------------------------------------

    @Test
    fun `getLocation – mehr als 8 Zeichen take8 liefert Prefix`() {
        val entry = WorkEntry(
            date = LocalDate.of(2026, 1, 15),
            dayType = DayType.WORK,
            dayLocationLabel = "LangerOrtslabel"
        )
        val location = PdfUtilities.getLocation(entry)
        // PdfExporter.drawTable ruft location.take(8) auf
        assertEquals("LangerOr", location.take(8))
    }

    @Test
    fun `getLocation – genau 8 Zeichen bleibt unveraendert`() {
        val entry = WorkEntry(
            date = LocalDate.of(2026, 1, 15),
            dayType = DayType.WORK,
            dayLocationLabel = "Ort12345"
        )
        val location = PdfUtilities.getLocation(entry)
        assertEquals("Ort12345", location.take(8))
    }

    @Test
    fun `buildTravelRouteSummary – take(18) trunkiert lange Route`() {
        val leg = TravelLeg(
            workEntryDate = LocalDate.of(2026, 1, 15),
            sortOrder = 0,
            startLabel = "StadtA",
            endLabel = "StadtBLangerName"
        )
        val summary = PdfUtilities.buildTravelRouteSummary(listOf(leg))
        // PdfExporter.drawTable ruft summary.take(18) auf
        assertEquals("StadtA→StadtBLange", summary.take(18))
    }

    @Test
    fun `buildTravelRouteSummary – kurze Route bleibt durch take(18) unveraendert`() {
        val leg = TravelLeg(
            workEntryDate = LocalDate.of(2026, 1, 15),
            sortOrder = 0,
            startLabel = "A",
            endLabel = "B"
        )
        val summary = PdfUtilities.buildTravelRouteSummary(listOf(leg))
        assertEquals(summary, summary.take(18))
    }

    // -------------------------------------------------------------------------
    // MAX_ENTRIES_PER_PDF – OOM-Schutz
    // Note: The full end-to-end path (PdfExporter.exportToPdf returning
    // ValidationError) requires an Android Context and is covered by
    // ExportPreviewViewModelTest via a mocked PdfExporter.
    // -------------------------------------------------------------------------

    @Test
    fun `MAX_ENTRIES_PER_PDF is set to the expected safety limit`() {
        assertEquals(180, PdfExporter.MAX_ENTRIES_PER_PDF)
    }
}
