package de.montagezeit.app.export

import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.TravelLeg
import de.montagezeit.app.data.local.entity.TravelLegCategory
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.local.entity.WorkEntryWithTravelLegs
import de.montagezeit.app.domain.util.MealAllowanceCalculator
import de.montagezeit.app.domain.util.TimeCalculator
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Utility-Funktionen für PDF-Export
 * Berechnet Arbeitszeiten und formatiert Werte für PDF-Tabellen
 */
object PdfUtilities {

    private val hoursFormatter = DecimalFormat("0.00", DecimalFormatSymbols(Locale.GERMAN))
    private val timeFormatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
    private val dateFormatter = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")
    private val dateShortFormatter = java.time.format.DateTimeFormatter.ofPattern("dd.MM.")
    private val monthFormatter = java.time.format.DateTimeFormatter.ofPattern("MMMM", Locale.GERMAN)

    /**
     * Formatiert Arbeitszeit als String (z.B. "8.00")
     */
    fun formatWorkHours(hours: Double): String {
        return hoursFormatter.format(hours)
    }
    
    /**
     * Formatiert Reisezeit (Minuten zu Stunden, z.B. 60 → "1.00")
     * Wenn keine Reisezeit: leerer String
     */
    fun formatTravelTime(minutes: Int?): String {
        if (minutes == null || minutes <= 0) {
            return ""
        }
        val hours = minutes / 60.0
        return hoursFormatter.format(hours)
    }

    /**
     * Sammelt die Wegpunkt-Labels einer Route, dedupliziert geteilte Zwischenpunkte.
     */
    private fun collectRouteLabels(travelLegs: List<TravelLeg>): List<String> {
        if (travelLegs.isEmpty()) return emptyList()
        val routeLabels = mutableListOf<String>()
        travelLegs.sortedBy(TravelLeg::sortOrder).forEach { leg ->
            val startLabel = leg.startLabel?.trim().orEmpty()
            val endLabel = leg.endLabel?.trim().orEmpty()
            if (startLabel.isNotEmpty() && routeLabels.lastOrNull() != startLabel) {
                routeLabels += startLabel
            }
            if (endLabel.isNotEmpty() && routeLabels.lastOrNull() != endLabel) {
                routeLabels += endLabel
            }
        }
        return routeLabels
    }

    /**
     * Formatiert die Route als String (z.B. "Dresden→Leipzig"), ohne Leerzeichen um den Pfeil.
     * Wenn keine Labels vorhanden: leerer String
     */
    fun buildTravelRouteSummary(travelLegs: List<TravelLeg>): String {
        return collectRouteLabels(travelLegs).joinToString("→")
    }

    /**
     * Formatiert die Route mit Leerzeichen um den Pfeil (z.B. "Dresden → Leipzig") für die
     * kompakte "Reise / Art"-Spalte im PDF-Export.
     */
    fun buildTravelRouteLabel(travelLegs: List<TravelLeg>): String {
        return collectRouteLabels(travelLegs).joinToString(" → ")
    }

    /**
     * Kurzcode für die Reiseart, passend zur Legende im PDF-Export.
     * A = Anreise, AB = Abreise, A/AB = An- und Abreise am selben Tag, W = Weiterreise, R = Reise
     */
    fun travelTypeCode(travelTypeKey: String): String = when (travelTypeKey) {
        "ARRIVAL" -> "A"
        "DEPARTURE" -> "AB"
        "ARRIVAL_DEPARTURE" -> "A/AB"
        "CONTINUATION" -> "W"
        "TRAVEL" -> "R"
        else -> ""
    }

    /**
     * Kombiniert Route und Reiseart-Kurzcode für die "Reise / Art"-Spalte, z.B.
     * "Leipzig → Braunschweig · A". Ohne Reisevorgang wird [dash] zurückgegeben.
     */
    fun buildTravelCellText(travelLegs: List<TravelLeg>, dash: String): String {
        if (travelLegs.isEmpty()) return dash
        val route = buildTravelRouteLabel(travelLegs)
        val code = travelTypeCode(determineTravelTypeKey(travelLegs))
        return when {
            route.isNotBlank() && code.isNotBlank() -> "$route · $code"
            route.isNotBlank() -> route
            code.isNotBlank() -> code
            else -> dash
        }
    }

    /**
     * Baut die Legende der im Zeitraum tatsächlich verwendeten Reiseart-Kurzcodes,
     * z.B. "A = Anreise · AB = Abreise". Ungenutzte Codes werden nicht aufgeführt.
     */
    fun buildTravelLegend(
        entries: List<WorkEntryWithTravelLegs>,
        arrivalLabel: String,
        departureLabel: String,
        continuationLabel: String,
        travelLabel: String
    ): String {
        val keys = entries.map { determineTravelTypeKey(it.orderedTravelLegs) }.toSet()
        val parts = mutableListOf<String>()
        if ("ARRIVAL" in keys || "ARRIVAL_DEPARTURE" in keys) parts += "A = $arrivalLabel"
        if ("DEPARTURE" in keys || "ARRIVAL_DEPARTURE" in keys) parts += "AB = $departureLabel"
        if ("CONTINUATION" in keys) parts += "W = $continuationLabel"
        if ("TRAVEL" in keys) parts += "R = $travelLabel"
        return parts.joinToString(" · ")
    }

    fun formatTravelWindow(startAt: Long?, arriveAt: Long?): String {
        if (startAt == null || arriveAt == null) return ""
        val start = java.time.Instant.ofEpochMilli(startAt)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalTime()
        val arrive = java.time.Instant.ofEpochMilli(arriveAt)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalTime()
        return "${formatTime(start)}–${formatTime(arrive)}"
    }
    
    /**
     * Formatiert LocalTime als String (HH:mm)
     */
    fun formatTime(time: java.time.LocalTime?): String {
        return time?.format(timeFormatter) ?: ""
    }
    
    /**
     * Formatiert LocalDate als String (dd.MM.yyyy)
     */
    fun formatDate(date: java.time.LocalDate): String {
        return date.format(dateFormatter)
    }

    /**
     * Formatiert LocalDate ohne Jahr (dd.MM.) für kompakte Tabellenzeilen im PDF-Export.
     */
    fun formatDateShort(date: java.time.LocalDate): String {
        return date.format(dateShortFormatter)
    }

    /**
     * Formatiert Minuten als Stunden:Minuten (z.B. 60 → "1:00", 90 → "1:30").
     */
    fun formatDurationHm(minutes: Int): String {
        val hours = minutes / 60
        val remainder = minutes % 60
        return "%d:%02d".format(hours, remainder)
    }

    /**
     * Formatiert Cent-Beträge kompakt: ohne Nachkommastellen, wenn der Betrag glatt ist
     * (z.B. 1400 → "14 €"), sonst mit zwei Nachkommastellen (z.B. 840 → "8,40 €").
     */
    fun formatEuroCompact(cents: Int): String {
        require(cents >= 0) { "cents must be non-negative" }
        val euros = cents / 100
        val remainder = cents % 100
        return if (remainder == 0) "$euros €" else "%d,%02d €".format(euros, remainder)
    }

    /**
     * Formatiert den Titel-Zeitraum eines PDF-Exports als Monat/Jahr (z.B. "August 2026").
     * Erstreckt sich der Zeitraum über mehrere Monate oder Jahre, wird ein Bereich gebildet
     * (z.B. "Juli–August 2026" bzw. "Dezember 2026–Januar 2027").
     */
    fun formatPeriodLabel(startDate: java.time.LocalDate, endDate: java.time.LocalDate): String {
        val startMonth = capitalizeGerman(startDate.format(monthFormatter))
        return when {
            startDate.year == endDate.year && startDate.month == endDate.month ->
                "$startMonth ${startDate.year}"
            startDate.year == endDate.year ->
                "$startMonth–${capitalizeGerman(endDate.format(monthFormatter))} ${startDate.year}"
            else ->
                "$startMonth ${startDate.year}–${capitalizeGerman(endDate.format(monthFormatter))} ${endDate.year}"
        }
    }

    private fun capitalizeGerman(text: String): String {
        return text.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.GERMAN) else it.toString() }
    }

    /**
     * Holt den Ort für einen WorkEntry.
     * Leere Labels werden übersprungen.
     */
    fun getLocation(entry: WorkEntry, travelLegs: List<TravelLeg> = emptyList()): String {
        return entry.dayLocationLabel.takeIf { it.isNotBlank() }
            ?: travelLegs.asReversed().firstNotNullOfOrNull { it.endLabel?.takeIf(String::isNotBlank) }
            ?: travelLegs.firstNotNullOfOrNull { it.startLabel?.takeIf(String::isNotBlank) }
            ?: ""
    }
    
    /**
     * Holt die Notiz für einen WorkEntry
     */
    fun getNote(entry: WorkEntry): String {
        return entry.note ?: ""
    }
    
    /**
     * Berechnet die Summe der Arbeitsstunden für eine Liste von WorkEntries
     */
    fun sumWorkHours(entries: List<WorkEntryWithTravelLegs>): Double {
        return entries.sumOf { TimeCalculator.calculateWorkHours(it.workEntry) }
    }

    fun sumWorkHours(entries: Collection<WorkEntry>): Double {
        return entries.sumOf { TimeCalculator.calculateWorkHours(it) }
    }
    
    /**
     * Berechnet die Summe der Reisezeit in Minuten für eine Liste von WorkEntries
     */
    fun sumTravelMinutes(entries: List<WorkEntryWithTravelLegs>): Int {
        return entries.sumOf { TimeCalculator.calculateTravelMinutes(it.orderedTravelLegs) }
    }

    
    /**
     * Filtert WORK-Tage aus einer Liste von WorkEntries
     */
    fun filterWorkDays(entries: List<WorkEntryWithTravelLegs>): List<WorkEntryWithTravelLegs> {
        return entries.filter { it.workEntry.dayType == DayType.WORK }
    }

    fun filterWorkDays(entries: Collection<WorkEntry>): List<WorkEntry> {
        return entries.filter { it.dayType == DayType.WORK }
    }

    /**
     * Bestimmt den Reiseart-Schlüssel basierend auf den TravelLeg-Kategorien.
     * Rückgabewerte: "ARRIVAL", "DEPARTURE", "ARRIVAL_DEPARTURE", "CONTINUATION", "TRAVEL", "NONE"
     * Lokalisierung erfolgt im aufrufenden Code über String-Ressourcen.
     */
    fun determineTravelTypeKey(travelLegs: List<TravelLeg>): String {
        if (travelLegs.isEmpty()) return "NONE"
        val categories = travelLegs.map { it.category }.toSet()
        val hasOutbound = TravelLegCategory.OUTBOUND in categories
        val hasReturn = TravelLegCategory.RETURN in categories
        val hasIntersite = TravelLegCategory.INTERSITE in categories
        return when {
            hasOutbound && hasReturn -> "ARRIVAL_DEPARTURE"
            hasOutbound              -> "ARRIVAL"
            hasReturn                -> "DEPARTURE"
            hasIntersite             -> "CONTINUATION"
            else                     -> "TRAVEL"
        }
    }

    /**
     * Baut die neun Tabellenzellen-Texte einer PDF-Zeile in Spaltenreihenfolge
     * (Datum, Einsatzort, Start, Ende, Pause, Arbeit, Reise/Art, Reisezeit, VP).
     *
     * Reine Datentypen bleiben je Spalte eindeutig: Zeitfelder enthalten ausschließlich
     * eine Uhrzeit/Dauer oder [dash] – niemals Textlabel wie "Reisetag" oder "Frei".
     */
    fun buildTableRowTexts(
        record: WorkEntryWithTravelLegs,
        dash: String = "–",
        hoursUnitSuffix: String = " h"
    ): List<String> {
        val entry = record.workEntry
        val travelLegs = record.orderedTravelLegs
        val travelMinutes = TimeCalculator.calculateTravelMinutes(travelLegs)
        val workMinutes = TimeCalculator.calculateWorkMinutes(entry)
        val workHours = workMinutes / 60.0
        val hasTimes = entry.dayType.isWorkLike && entry.workStart != null && entry.workEnd != null
        val mealSnapshot = MealAllowanceCalculator.resolveEffectiveStoredSnapshot(record)

        val startText = if (hasTimes) formatTime(entry.workStart) else dash
        val endText = if (hasTimes) formatTime(entry.workEnd) else dash
        val breakText = if (hasTimes && entry.breakMinutes > 0) formatDurationHm(entry.breakMinutes) else dash
        val workText = if (workMinutes > 0) "${formatWorkHours(workHours)}$hoursUnitSuffix" else dash
        val travelCellText = buildTravelCellText(travelLegs, dash)
        val travelTimeText = if (travelMinutes > 0) "${formatTravelTime(travelMinutes)}$hoursUnitSuffix" else dash
        val vpText = if (mealSnapshot.amountCents > 0) formatEuroCompact(mealSnapshot.amountCents) else dash

        return listOf(
            formatDateShort(entry.date),
            getLocation(entry, travelLegs).ifBlank { dash },
            startText,
            endText,
            breakText,
            workText,
            travelCellText,
            travelTimeText,
            vpText
        )
    }

    /**
     * Baut die erste Metadatenzeile des PDF-Kopfbereichs (Mitarbeiter · Personalnr. · Firma ·
     * Projekt). Leere/fehlende Felder (Personalnr., Firma, Projekt) werden übersprungen statt
     * als leere Zeile ausgegeben – siehe Vorgabe "keine leeren Projekt-/Metadatenfelder".
     *
     * Die Templates entsprechen dem Format von `String.format` (Platzhalter "%1$s").
     */
    fun buildHeaderMetaLine1(
        employeeName: String,
        employeeTemplate: String,
        personnelNumber: String?,
        personnelNumberTemplate: String,
        company: String?,
        companyTemplate: String,
        project: String?,
        projectTemplate: String
    ): String {
        return listOfNotNull(
            employeeTemplate.format(employeeName),
            personnelNumber?.takeIf(String::isNotBlank)?.let { personnelNumberTemplate.format(it) },
            company?.takeIf(String::isNotBlank)?.let { companyTemplate.format(it) },
            project?.takeIf(String::isNotBlank)?.let { projectTemplate.format(it) }
        ).joinToString(" · ")
    }

    fun buildMealAllowanceLabel(
        amountCents: Int,
        isArrivalDeparture: Boolean,
        breakfastIncluded: Boolean,
        arrivalDepartureLabel: String = "An-/Abreise",
        breakfastIncludedLabel: String = "Frühstück"
    ): String {
        if (amountCents <= 0) return ""
        return buildList {
            add(MealAllowanceCalculator.formatEuro(amountCents))
            if (isArrivalDeparture) add(arrivalDepartureLabel)
            if (breakfastIncluded) add(breakfastIncludedLabel)
        }.joinToString(" / ")
    }
}
