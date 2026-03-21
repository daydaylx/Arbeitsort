package de.montagezeit.app.domain.util

import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntry
import java.util.Locale

object TimeCalculator {

    /**
     * Berechnet die Netto-Arbeitszeit in Minuten.
     * (Ende - Start - Pause).
     * Bei DayType.OFF/COMP_TIME oder ungültigen Zeiten (Ende < Start, Pause > Dauer) wird 0 zurückgegeben.
     */
    fun calculateWorkMinutes(entry: WorkEntry): Int {
        if (entry.dayType == DayType.OFF || entry.dayType == DayType.COMP_TIME) return 0

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
     * Gibt die Hinfahrt-Minuten zurück, oder null wenn kein vollständiges Zeitpaar vorhanden.
     */
    private fun calculateOutboundMinutes(entry: WorkEntry): Int? {
        val start = entry.travelStartAt
        val arrive = entry.travelArriveAt
        if (start != null && arrive != null) {
            var diffMs = arrive - start
            if (diffMs < 0) diffMs += 24 * 60 * 60 * 1000L
            return (diffMs / 60_000L).toInt().coerceAtLeast(0)
        }
        return null
    }

    /**
     * Gibt die Rückfahrt-Minuten zurück, oder null wenn kein vollständiges Zeitpaar vorhanden.
     */
    private fun calculateReturnMinutes(entry: WorkEntry): Int? {
        val start = entry.returnStartAt
        val arrive = entry.returnArriveAt
        if (start != null && arrive != null) {
            var diffMs = arrive - start
            if (diffMs < 0) diffMs += 24 * 60 * 60 * 1000L
            return (diffMs / 60_000L).toInt().coerceAtLeast(0)
        }
        return null
    }

    /**
     * Gibt die bezahlten Reiseminuten zurück (Hinfahrt + Rückfahrt).
     * Nutzt bevorzugt die manuell gepflegten Travel-Timestamps.
     * travelPaidMinutes bleibt nur Fallback, wenn KEINE Timestamps für Hin- oder Rückfahrt vorliegen.
     *
     * Bei Mitternachtsüberschreitung: Wenn arrive < start (zeitlich), werden 24h addiert.
     */
    fun calculateTravelMinutes(entry: WorkEntry): Int {
        val outbound = calculateOutboundMinutes(entry)
        val returnTrip = calculateReturnMinutes(entry)

        if (outbound == null && returnTrip == null) {
            return entry.travelPaidMinutes?.coerceAtLeast(0) ?: 0
        }
        return (outbound ?: 0) + (returnTrip ?: 0)
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
