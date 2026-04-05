package de.montagezeit.app.domain.util

import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.TravelLeg
import de.montagezeit.app.data.local.entity.WorkEntry
import java.util.Locale

object TimeCalculator {

    private const val MILLIS_PER_DAY = 24 * 60 * 60 * 1000L

    /**
     * Berechnet die Netto-Arbeitszeit in Minuten.
     * (Ende - Start - Pause).
     * Nachtschichten (Ende < Start) werden korrekt über Mitternacht berechnet.
     * Bei DayType.OFF/COMP_TIME oder fehlenden Zeiten wird 0 zurückgegeben.
     * Wenn Pause > Arbeitszeit, wird 0 zurückgegeben.
     */
    fun calculateWorkMinutes(entry: WorkEntry): Int {
        if (entry.dayType == DayType.OFF || entry.dayType == DayType.COMP_TIME) return 0

        val workStart = entry.workStart ?: return 0
        val workEnd = entry.workEnd ?: return 0
        val startMinutes = workStart.hour * 60 + workStart.minute
        val endMinutes = workEnd.hour * 60 + workEnd.minute

        // Nachtschicht-Unterstützung: wenn Ende < Start, über Mitternacht gerechnet
        val rawWork = if (endMinutes < startMinutes) {
            (24 * 60 - startMinutes) + endMinutes
        } else {
            endMinutes - startMinutes
        }
        
        // Subtract break, clamp to 0
        val netWork = rawWork - entry.breakMinutes
        return if (netWork < 0) 0 else netWork
    }

    fun calculateTravelMinutes(travelLegs: List<TravelLeg>): Int {
        return travelLegs.sumOf(::calculateTravelLegMinutes)
    }

    /**
     * Berechnet die gesamte bezahlte Zeit in Minuten.
     * Arbeitszeit + Reisezeit.
     * Hinweis: An OFF-Tagen ist die Arbeitszeit 0, daher ist die Gesamtzeit = Reisezeit.
     */
    fun calculatePaidTotalMinutes(entry: WorkEntry, travelLegs: List<TravelLeg> = emptyList()): Int {
        return calculateWorkMinutes(entry) + calculateTravelMinutes(travelLegs)
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
    fun calculatePaidTotalHours(entry: WorkEntry, travelLegs: List<TravelLeg> = emptyList()): Double {
        return calculatePaidTotalMinutes(entry, travelLegs) / 60.0
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

    private fun calculateTravelLegMinutes(leg: TravelLeg): Int {
        val start = leg.startAt
        val arrive = leg.arriveAt
        if (start != null && arrive != null) {
            val diffMs = arrive - start
            val normalizedDiffMs = if (diffMs < 0) diffMs + MILLIS_PER_DAY else diffMs
            return if (normalizedDiffMs < 0) 0 else (normalizedDiffMs / 60_000L).toInt()
        }
        return leg.paidMinutesOverride?.coerceAtLeast(0) ?: 0
    }

}
