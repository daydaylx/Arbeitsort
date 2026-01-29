package de.montagezeit.app.export

import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.domain.util.TimeCalculator
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.ZoneId
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
    fun formatTravelWindow(startAt: Long?, arriveAt: Long?): String {
        if (startAt == null || arriveAt == null) {
            return ""
        }
        val zoneId = ZoneId.systemDefault()
        val startTime = Instant.ofEpochMilli(startAt).atZone(zoneId).toLocalTime()
        val arriveTime = Instant.ofEpochMilli(arriveAt).atZone(zoneId).toLocalTime()
        return "${formatTime(startTime)}–${formatTime(arriveTime)}"
    }
    
    /**
     * Formatiert LocalTime als String (HH:mm)
     */
    fun formatTime(time: java.time.LocalTime): String {
        return time.format(timeFormatter)
    }
    
    /**
     * Formatiert LocalDate als String (dd.MM.yyyy)
     */
    fun formatDate(date: java.time.LocalDate): String {
        return date.format(dateFormatter)
    }
    
    /**
     * Holt den Ort für einen WorkEntry
     * Priority: morningLocationLabel > eveningLocationLabel > ""
     */
    fun getLocation(entry: WorkEntry): String {
        return entry.morningLocationLabel ?: entry.eveningLocationLabel ?: ""
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
    fun sumWorkHours(entries: List<WorkEntry>): Double {
        return entries.sumOf { calculateWorkHours(it) }
    }
    
    /**
     * Berechnet die Summe der Reisezeit in Minuten für eine Liste von WorkEntries
     */
    fun sumTravelMinutes(entries: List<WorkEntry>): Int {
        return entries.sumOf { TimeCalculator.calculateTravelMinutes(it) }
    }
    
    /**
     * Filtert WORK-Tage aus einer Liste von WorkEntries
     */
    fun filterWorkDays(entries: List<WorkEntry>): List<WorkEntry> {
        return entries.filter { it.dayType == DayType.WORK }
    }
}
