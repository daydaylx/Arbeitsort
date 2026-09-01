package de.montagezeit.app.export

import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.TravelLeg
import de.montagezeit.app.data.local.entity.TravelLegCategory
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.local.entity.WorkEntryWithTravelLegs
import de.montagezeit.app.domain.usecase.AggregateWorkStats
import de.montagezeit.app.domain.util.MealAllowanceCalculator
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
    // Meal allowance – gespeicherte Werte zählen nur mit fachlicher Aktivität
    // -------------------------------------------------------------------------

    @Test
    fun `meal allowance effective snapshot ignores stored amount without activity`() {
        val ineligible = workRecord(
            mealAllowanceCents = 1200,
            workStart = LocalTime.of(8, 0),
            workEnd = LocalTime.of(8, 30),
            breakMinutes = 30
        )
        val eligible = workRecord(mealAllowanceCents = 820)

        assertEquals(0, MealAllowanceCalculator.resolveEffectiveStoredSnapshot(ineligible).amountCents)
        assertEquals(820, MealAllowanceCalculator.resolveEffectiveStoredSnapshot(eligible).amountCents)
    }

    @Test
    fun `buildMealAllowanceLabel includes arrival departure and breakfast flags`() {
        val label = PdfUtilities.buildMealAllowanceLabel(
            amountCents = 840,
            isArrivalDeparture = true,
            breakfastIncluded = true,
            arrivalDepartureLabel = "An-/Abreise",
            breakfastIncludedLabel = "Frühstück"
        )

        assertEquals("8,40 € / An-/Abreise / Frühstück", label)
    }

    @Test
    fun `buildMealAllowanceLabel stays blank without amount`() {
        val label = PdfUtilities.buildMealAllowanceLabel(
            amountCents = 0,
            isArrivalDeparture = true,
            breakfastIncluded = true
        )

        assertEquals("", label)
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
    // Texttrunkierung: Location (10 Zeichen) und Routen-Summary (22 Zeichen)
    // PdfExporter.drawTable: location.length > 11 → take(10)+"…"
    //                        route.length   > 23 → take(22)+"…"
    // -------------------------------------------------------------------------

    @Test
    fun `getLocation – mehr als 11 Zeichen take10 liefert Prefix`() {
        val entry = WorkEntry(
            date = LocalDate.of(2026, 1, 15),
            dayType = DayType.WORK,
            dayLocationLabel = "LangerOrtslabel"
        )
        val location = PdfUtilities.getLocation(entry)
        // PdfExporter.drawTable: length > 11 → take(10) + "…"
        assertEquals("LangerOrts", location.take(10))
    }

    @Test
    fun `getLocation – genau 11 Zeichen bleibt unveraendert`() {
        val entry = WorkEntry(
            date = LocalDate.of(2026, 1, 15),
            dayType = DayType.WORK,
            dayLocationLabel = "Ort12345678"
        )
        val location = PdfUtilities.getLocation(entry)
        assertEquals("Ort12345678", location.take(11))
    }

    @Test
    fun `buildTravelRouteSummary – take(22) trunkiert lange Route`() {
        val leg = TravelLeg(
            workEntryDate = LocalDate.of(2026, 1, 15),
            sortOrder = 0,
            startLabel = "StadtA",
            endLabel = "StadtBLangerNameXXXXX"
        )
        val summary = PdfUtilities.buildTravelRouteSummary(listOf(leg))
        // PdfExporter.drawTable: length > 23 → take(22) + "…"
        assertEquals("StadtA→StadtBLangerNam", summary.take(22))
    }

    @Test
    fun `buildTravelRouteSummary – kurze Route bleibt durch take(22) unveraendert`() {
        val leg = TravelLeg(
            workEntryDate = LocalDate.of(2026, 1, 15),
            sortOrder = 0,
            startLabel = "A",
            endLabel = "B"
        )
        val summary = PdfUtilities.buildTravelRouteSummary(listOf(leg))
        assertEquals(summary, summary.take(22))
    }

    // -------------------------------------------------------------------------
    // determineTravelTypeKey
    // -------------------------------------------------------------------------

    @Test
    fun `determineTravelTypeKey – leere Liste ergibt NONE`() {
        assertEquals("NONE", PdfUtilities.determineTravelTypeKey(emptyList()))
    }

    @Test
    fun `determineTravelTypeKey – nur OUTBOUND ergibt ARRIVAL`() {
        val legs = listOf(
            TravelLeg(workEntryDate = LocalDate.of(2026, 1, 15), sortOrder = 0,
                category = TravelLegCategory.OUTBOUND)
        )
        assertEquals("ARRIVAL", PdfUtilities.determineTravelTypeKey(legs))
    }

    @Test
    fun `determineTravelTypeKey – nur RETURN ergibt DEPARTURE`() {
        val legs = listOf(
            TravelLeg(workEntryDate = LocalDate.of(2026, 1, 15), sortOrder = 0,
                category = TravelLegCategory.RETURN)
        )
        assertEquals("DEPARTURE", PdfUtilities.determineTravelTypeKey(legs))
    }

    @Test
    fun `determineTravelTypeKey – OUTBOUND und RETURN ergibt ARRIVAL_DEPARTURE`() {
        val legs = listOf(
            TravelLeg(workEntryDate = LocalDate.of(2026, 1, 15), sortOrder = 0,
                category = TravelLegCategory.OUTBOUND),
            TravelLeg(workEntryDate = LocalDate.of(2026, 1, 15), sortOrder = 1,
                category = TravelLegCategory.RETURN)
        )
        assertEquals("ARRIVAL_DEPARTURE", PdfUtilities.determineTravelTypeKey(legs))
    }

    @Test
    fun `determineTravelTypeKey – nur INTERSITE ergibt CONTINUATION`() {
        val legs = listOf(
            TravelLeg(workEntryDate = LocalDate.of(2026, 1, 15), sortOrder = 0,
                category = TravelLegCategory.INTERSITE)
        )
        assertEquals("CONTINUATION", PdfUtilities.determineTravelTypeKey(legs))
    }

    @Test
    fun `determineTravelTypeKey – nur OTHER ergibt TRAVEL`() {
        val legs = listOf(
            TravelLeg(workEntryDate = LocalDate.of(2026, 1, 15), sortOrder = 0,
                category = TravelLegCategory.OTHER)
        )
        assertEquals("TRAVEL", PdfUtilities.determineTravelTypeKey(legs))
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

    // -------------------------------------------------------------------------
    // formatDateShort / formatDurationHm / formatEuroCompact / formatPeriodLabel
    // -------------------------------------------------------------------------

    @Test
    fun `formatDateShort - ohne Jahr`() {
        assertEquals("03.08.", PdfUtilities.formatDateShort(LocalDate.of(2026, 8, 3)))
    }

    @Test
    fun `formatDurationHm - 60 Minuten ergibt 1-00`() {
        assertEquals("1:00", PdfUtilities.formatDurationHm(60))
    }

    @Test
    fun `formatDurationHm - 90 Minuten ergibt 1-30`() {
        assertEquals("1:30", PdfUtilities.formatDurationHm(90))
    }

    @Test
    fun `formatDurationHm - 0 Minuten ergibt 0-00`() {
        assertEquals("0:00", PdfUtilities.formatDurationHm(0))
    }

    @Test
    fun `formatEuroCompact - glatter Betrag ohne Nachkommastellen`() {
        assertEquals("14 €", PdfUtilities.formatEuroCompact(1400))
        assertEquals("28 €", PdfUtilities.formatEuroCompact(2800))
    }

    @Test
    fun `formatEuroCompact - krummer Betrag mit Nachkommastellen`() {
        assertEquals("8,40 €", PdfUtilities.formatEuroCompact(840))
    }

    @Test
    fun `formatPeriodLabel - gleicher Monat ergibt Monat und Jahr`() {
        assertEquals(
            "August 2026",
            PdfUtilities.formatPeriodLabel(LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 20))
        )
    }

    @Test
    fun `formatPeriodLabel - unterschiedliche Monate gleiches Jahr ergibt Bereich`() {
        assertEquals(
            "Juli–August 2026",
            PdfUtilities.formatPeriodLabel(LocalDate.of(2026, 7, 28), LocalDate.of(2026, 8, 3))
        )
    }

    @Test
    fun `formatPeriodLabel - unterschiedliche Jahre ergibt vollen Bereich`() {
        assertEquals(
            "Dezember 2026–Januar 2027",
            PdfUtilities.formatPeriodLabel(LocalDate.of(2026, 12, 28), LocalDate.of(2027, 1, 3))
        )
    }

    // -------------------------------------------------------------------------
    // buildTravelRouteLabel / travelTypeCode / buildTravelCellText
    // -------------------------------------------------------------------------

    @Test
    fun `buildTravelRouteLabel - Pfeil mit Leerzeichen`() {
        val leg = TravelLeg(
            workEntryDate = LocalDate.of(2026, 8, 3),
            sortOrder = 0,
            startLabel = "Leipzig",
            endLabel = "Braunschweig"
        )
        assertEquals("Leipzig → Braunschweig", PdfUtilities.buildTravelRouteLabel(listOf(leg)))
    }

    @Test
    fun `travelTypeCode - mappt bekannte Schluessel auf Kurzcodes`() {
        assertEquals("A", PdfUtilities.travelTypeCode("ARRIVAL"))
        assertEquals("AB", PdfUtilities.travelTypeCode("DEPARTURE"))
        assertEquals("A/AB", PdfUtilities.travelTypeCode("ARRIVAL_DEPARTURE"))
        assertEquals("W", PdfUtilities.travelTypeCode("CONTINUATION"))
        assertEquals("R", PdfUtilities.travelTypeCode("TRAVEL"))
        assertEquals("", PdfUtilities.travelTypeCode("NONE"))
    }

    @Test
    fun `buildTravelCellText - kombiniert Route und Kurzcode`() {
        val leg = TravelLeg(
            workEntryDate = LocalDate.of(2026, 8, 3),
            sortOrder = 0,
            category = TravelLegCategory.OUTBOUND,
            startLabel = "Leipzig",
            endLabel = "Braunschweig"
        )
        assertEquals("Leipzig → Braunschweig · A", PdfUtilities.buildTravelCellText(listOf(leg), "–"))
    }

    @Test
    fun `buildTravelCellText - ohne Reise ergibt dash`() {
        assertEquals("–", PdfUtilities.buildTravelCellText(emptyList(), "–"))
    }

    @Test
    fun `buildTravelLegend - listet nur tatsaechlich verwendete Codes`() {
        val outboundOnly = listOf(
            workRecord(travelMinutes = 0).copy(
                travelLegs = listOf(
                    TravelLeg(
                        workEntryDate = LocalDate.of(2026, 8, 3),
                        sortOrder = 0,
                        category = TravelLegCategory.OUTBOUND
                    )
                )
            )
        )
        val legend = PdfUtilities.buildTravelLegend(
            entries = outboundOnly,
            arrivalLabel = "Anreise",
            departureLabel = "Abreise",
            continuationLabel = "Weiterreise",
            travelLabel = "Reise"
        )
        assertEquals("A = Anreise", legend)
    }

    @Test
    fun `buildTravelLegend - ohne Reisen ergibt leeren String`() {
        val legend = PdfUtilities.buildTravelLegend(
            entries = listOf(workRecord()),
            arrivalLabel = "Anreise",
            departureLabel = "Abreise",
            continuationLabel = "Weiterreise",
            travelLabel = "Reise"
        )
        assertEquals("", legend)
    }

    // -------------------------------------------------------------------------
    // buildHeaderMetaLine1 – leere Projekt-/Metadatenfelder werden nicht angezeigt (Test G)
    // -------------------------------------------------------------------------

    @Test
    fun `buildHeaderMetaLine1 - leeres Projekt wird nicht angezeigt`() {
        val line = PdfUtilities.buildHeaderMetaLine1(
            employeeName = "David Grunert",
            employeeTemplate = "Mitarbeiter: %1\$s",
            optionalFields = listOf(
                PdfUtilities.MetaField("25", "Personalnr.: %1\$s"),
                PdfUtilities.MetaField("TBM Maifarth", "Firma: %1\$s"),
                PdfUtilities.MetaField(null, "Projekt: %1\$s")
            )
        )
        assertEquals("Mitarbeiter: David Grunert · Personalnr.: 25 · Firma: TBM Maifarth", line)
    }

    @Test
    fun `buildHeaderMetaLine1 - leerer Blank-String wird wie fehlend behandelt`() {
        val line = PdfUtilities.buildHeaderMetaLine1(
            employeeName = "David Grunert",
            employeeTemplate = "Mitarbeiter: %1\$s",
            optionalFields = listOf(
                PdfUtilities.MetaField("  ", "Personalnr.: %1\$s"),
                PdfUtilities.MetaField(null, "Firma: %1\$s"),
                PdfUtilities.MetaField("   ", "Projekt: %1\$s")
            )
        )
        assertEquals("Mitarbeiter: David Grunert", line)
    }

    @Test
    fun `buildHeaderMetaLine1 - alle Felder vorhanden werden in Reihenfolge angezeigt`() {
        val line = PdfUtilities.buildHeaderMetaLine1(
            employeeName = "David Grunert",
            employeeTemplate = "Mitarbeiter: %1\$s",
            optionalFields = listOf(
                PdfUtilities.MetaField("25", "Personalnr.: %1\$s"),
                PdfUtilities.MetaField("TBM Maifarth", "Firma: %1\$s"),
                PdfUtilities.MetaField("Projekt X", "Projekt: %1\$s")
            )
        )
        assertEquals(
            "Mitarbeiter: David Grunert · Personalnr.: 25 · Firma: TBM Maifarth · Projekt: Projekt X",
            line
        )
    }

    // -------------------------------------------------------------------------
    // buildTableRowTexts – reiner Reisetag (Test C) und gemischter Arbeitstag (Test D)
    // -------------------------------------------------------------------------

    private fun travelOnlyRecord(date: LocalDate, travelMinutes: Int = 150): WorkEntryWithTravelLegs {
        return WorkEntryWithTravelLegs(
            workEntry = WorkEntry(
                date = date,
                dayType = DayType.WORK,
                workStart = null,
                workEnd = null,
                breakMinutes = 0,
                confirmedWorkDay = true,
                mealAllowanceAmountCents = 1400
            ),
            travelLegs = listOf(
                TravelLeg(
                    workEntryDate = date,
                    sortOrder = 0,
                    category = TravelLegCategory.OUTBOUND,
                    startLabel = "Leipzig",
                    endLabel = "Nürnberg",
                    paidMinutesOverride = travelMinutes
                )
            )
        )
    }

    @Test
    fun `buildTableRowTexts - reiner Reisetag enthaelt keinen Text in Uhrzeitfeldern`() {
        val record = travelOnlyRecord(LocalDate.of(2026, 8, 10))
        val texts = PdfUtilities.buildTableRowTexts(record, dash = "–")

        assertEquals("–", texts.start)
        assertEquals("–", texts.end)
        assertEquals("–", texts.breakText)
        assertEquals("–", texts.work)
        assertEquals("Leipzig → Nürnberg · A", texts.travel)
        assertEquals("2,50 h", texts.travelTime)
        assertEquals("14 €", texts.vp)
    }

    @Test
    fun `buildTableRowTexts - gemischter Arbeitstag zeigt Arbeit und Reisezeit getrennt`() {
        val date = LocalDate.of(2026, 8, 3)
        val record = WorkEntryWithTravelLegs(
            workEntry = WorkEntry(
                date = date,
                dayType = DayType.WORK,
                workStart = LocalTime.of(9, 30),
                workEnd = LocalTime.of(19, 0),
                breakMinutes = 60,
                confirmedWorkDay = true,
                mealAllowanceAmountCents = 1400
            ),
            travelLegs = listOf(
                TravelLeg(
                    workEntryDate = date,
                    sortOrder = 0,
                    category = TravelLegCategory.OUTBOUND,
                    startLabel = "Leipzig",
                    endLabel = "Braunschweig",
                    paidMinutesOverride = 150
                )
            )
        )
        val texts = PdfUtilities.buildTableRowTexts(record, dash = "–")

        assertEquals("09:30", texts.start)
        assertEquals("19:00", texts.end)
        assertEquals("1:00", texts.breakText)
        assertEquals("8,50 h", texts.work)
        assertEquals("Leipzig → Braunschweig · A", texts.travel)
        assertEquals("2,50 h", texts.travelTime)
        assertEquals("14 €", texts.vp)
    }

    @Test
    fun `buildTableRowTexts - Pause 0 Minuten zeigt dash statt 0-00`() {
        val date = LocalDate.of(2026, 8, 14)
        val record = WorkEntryWithTravelLegs(
            workEntry = WorkEntry(
                date = date,
                dayType = DayType.WORK,
                workStart = LocalTime.of(8, 0),
                workEnd = LocalTime.of(13, 0),
                breakMinutes = 0,
                confirmedWorkDay = true
            ),
            travelLegs = emptyList()
        )
        val texts = PdfUtilities.buildTableRowTexts(record, dash = "–")
        assertEquals("–", texts.breakText)
    }

    @Test
    fun `buildTableRowTexts - Einsatzort statt leerer Zeichenkette zeigt dash`() {
        val record = workRecord()
        val texts = PdfUtilities.buildTableRowTexts(record, dash = "–")
        assertEquals("–", texts.location)
    }

    // -------------------------------------------------------------------------
    // Summenpruefung: Arbeitszeit + Reisezeit = Gesamtzeit (Test E) und
    // Summe der VP-Werte = VP-Gesamtsumme (Test F)
    // -------------------------------------------------------------------------

    private fun confirmedRecord(
        date: LocalDate,
        workStart: LocalTime = LocalTime.of(8, 0),
        workEnd: LocalTime = LocalTime.of(17, 0),
        breakMinutes: Int = 60,
        travelMinutes: Int = 0,
        mealAllowanceCents: Int = 0,
        location: String = "Braunschweig"
    ): WorkEntryWithTravelLegs = WorkEntryWithTravelLegs(
        workEntry = WorkEntry(
            date = date,
            dayType = DayType.WORK,
            workStart = workStart,
            workEnd = workEnd,
            breakMinutes = breakMinutes,
            dayLocationLabel = location,
            confirmedWorkDay = true,
            mealAllowanceAmountCents = mealAllowanceCents
        ),
        travelLegs = if (travelMinutes > 0) listOf(
            TravelLeg(workEntryDate = date, sortOrder = 0, paidMinutesOverride = travelMinutes)
        ) else emptyList()
    )

    @Test
    fun `Summenpruefung - Arbeitszeit plus Reisezeit ergibt Gesamtzeit`() {
        val entries = listOf(
            confirmedRecord(LocalDate.of(2026, 8, 3), travelMinutes = 150),
            confirmedRecord(LocalDate.of(2026, 8, 4)),
            confirmedRecord(LocalDate.of(2026, 8, 20), workEnd = LocalTime.of(19, 30), travelMinutes = 0)
        )
        val stats = AggregateWorkStats()(entries)

        assertEquals(stats.totalWorkMinutes + stats.totalTravelMinutes, stats.totalPaidMinutes)

        // Die je Zeile dargestellte Reisezeit muss exakt der Summe entsprechen.
        val sumOfDisplayedTravelMinutes = entries.sumOf {
            TimeCalculator.calculateTravelMinutes(it.orderedTravelLegs)
        }
        assertEquals(stats.totalTravelMinutes, sumOfDisplayedTravelMinutes)
    }

    @Test
    fun `Verpflegung - Summe der einzelnen VP-Werte ergibt VP-Gesamtsumme`() {
        val entries = listOf(
            confirmedRecord(LocalDate.of(2026, 8, 3), mealAllowanceCents = 1400),
            confirmedRecord(LocalDate.of(2026, 8, 4), mealAllowanceCents = 2800),
            confirmedRecord(LocalDate.of(2026, 8, 5), mealAllowanceCents = 2800)
        )
        val stats = AggregateWorkStats()(entries)

        val sumOfDisplayedVp = entries.sumOf {
            MealAllowanceCalculator.resolveEffectiveStoredSnapshot(it).amountCents
        }
        assertEquals(stats.mealAllowanceCents, sumOfDisplayedVp)
        assertEquals(7000, stats.mealAllowanceCents)
    }
}
