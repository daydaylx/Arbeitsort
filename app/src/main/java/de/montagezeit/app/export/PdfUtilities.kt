package de.montagezeit.app.export

import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.TravelLeg
import de.montagezeit.app.data.local.entity.TravelLegCategory
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.local.entity.WorkEntryWithTravelLegs
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

    /**
     * Berechnet die Arbeitszeit in Stunden für einen WorkEntry
     * Arbeitszeit = (workEnd - workStart - pause) / 60
     * Für OFF-Tage: 0.0
     */
    fun calculateWorkHours(entry: WorkEntry): Double {
        return TimeCalculator.calculateWorkHours(entry)
    }
    
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
     * Formatiert Reisezeit von–bis (HH:mm–HH:mm)
     * Wenn Start/Ende fehlen: leerer String
     */
    fun buildTravelRouteSummary(travelLegs: List<TravelLeg>): String {
        if (travelLegs.isEmpty()) return ""
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
        return routeLabels.joinToString("→")
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
        return entries.sumOf { calculateWorkHours(it.workEntry) }
    }

    fun sumWorkHours(entries: Collection<WorkEntry>): Double {
        return entries.sumOf(::calculateWorkHours)
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
}
