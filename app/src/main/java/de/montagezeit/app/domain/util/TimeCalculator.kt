package de.montagezeit.app.domain.util

import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntry
import java.util.Locale

object TimeCalculator {

    /**
     * Berechnet die Netto-Arbeitszeit in Minuten.
     * (Ende - Start - Pause).
     * Bei DayType.OFF oder ungültigen Zeiten (Ende < Start, Pause > Dauer) wird 0 zurückgegeben.
     */
    fun calculateWorkMinutes(entry: WorkEntry): Int {
        if (entry.dayType == DayType.OFF) return 0

        val startMinutes = entry.workStart.hour * 60 + entry.workStart.minute
        val endMinutes = entry.workEnd.hour * 60 + entry.workEnd.minute
        val duration = endMinutes - startMinutes

        // Clamp to 0 if end is before start
        val rawWork = if (duration < 0) 0 else duration
        
        // Subtract break, clamp to 0
        val netWork = rawWork - entry.breakMinutes
        return if (netWork < 0) 0 else netWork
    }

    /**
     * Gibt die bezahlten Reiseminuten zurück.
     * Nutzt travelPaidMinutes falls vorhanden.
     * Andernfalls wird die Differenz zwischen travelStartAt und travelArriveAt berechnet.
     */
    fun calculateTravelMinutes(entry: WorkEntry): Int {
        if (entry.travelPaidMinutes != null) return entry.travelPaidMinutes
        val start = entry.travelStartAt ?: return 0
        val arrive = entry.travelArriveAt ?: return 0
        var diffMs = arrive - start
        if (diffMs < 0) diffMs += 24 * 60 * 60 * 1000L // overnight crossing: both timestamps use entry date
        return (diffMs / 60_000L).toInt().coerceAtLeast(0)
    }

    /**
     * Berechnet die gesamte bezahlte Zeit in Minuten.
     * Arbeitszeit + Reisezeit.
     * Hinweis: An OFF-Tagen ist die Arbeitszeit 0, daher ist die Gesamtzeit = Reisezeit.
     */
    fun calculatePaidTotalMinutes(entry: WorkEntry): Int {
        return calculateWorkMinutes(entry) + calculateTravelMinutes(entry)
    }

    /**
     * Berechnet die Arbeitszeit in Stunden (für Exports/Anzeige).
     */
    fun calculateWorkHours(entry: WorkEntry): Double {
        return calculateWorkMinutes(entry) / 60.0
    }

    /**
     * Berechnet die gesamte bezahlte Zeit in Stunden.
     */
    fun calculatePaidTotalHours(entry: WorkEntry): Double {
        return calculatePaidTotalMinutes(entry) / 60.0
    }
    
    /**
     * Formatiert Stunden als String (z.B. "8,50")
     */
    fun formatHours(hours: Double): String {
        return String.format(Locale.GERMAN, "%.2f", hours)
    }
    
    /**
     * Formatiert Minuten als Stunden-String (z.B. 90 -> "1,50")
     */
    fun formatMinutesAsHours(minutes: Int): String {
        return formatHours(minutes / 60.0)
    }
}
