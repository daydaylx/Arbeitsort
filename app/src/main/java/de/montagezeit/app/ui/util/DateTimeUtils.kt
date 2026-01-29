package de.montagezeit.app.ui.util

import java.time.Duration
import java.time.Instant
import java.time.LocalTime

/**
 * Zentrale Utility-Funktionen für Datum/Zeit-Berechnungen und -Formatierungen.
 *
 * Verhindert Inkonsistenzen durch duplizierte Implementierungen in verschiedenen Screens.
 */
object DateTimeUtils {

    /**
     * Berechnet die Dauer zwischen zwei Zeitpunkten (LocalTime).
     * Behandelt Übernacht-Reisen korrekt durch Hinzufügen von 24 Stunden.
     *
     * Beispiel:
     * - Start: 23:00, Ende: 01:00 → 2 Stunden (nicht -22 Stunden)
     *
     * @param start Startzeitpunkt
     * @param end Endzeitpunkt
     * @return Duration oder null wenn einer der Parameter null ist
     */
    fun calculateTravelDuration(start: LocalTime?, end: LocalTime?): Duration? {
        if (start == null || end == null) return null

        var duration = Duration.between(start, end)

        // Bei negativer Dauer: Übernacht-Reise (z.B. 23:00 -> 01:00)
        if (duration.isNegative) {
            duration = duration.plusHours(24)
        }

        return duration
    }

    /**
     * Berechnet die Dauer zwischen zwei Zeitpunkten (Epoch Millis).
     * Behandelt Übernacht-Reisen korrekt durch Hinzufügen von 24 Stunden.
     *
     * @param startMillis Startzeitpunkt in Epoch Millisekunden
     * @param endMillis Endzeitpunkt in Epoch Millisekunden
     * @return Duration oder null wenn einer der Parameter null ist
     */
    fun calculateTravelDuration(startMillis: Long?, endMillis: Long?): Duration? {
        if (startMillis == null || endMillis == null) return null

        var duration = Duration.between(
            Instant.ofEpochMilli(startMillis),
            Instant.ofEpochMilli(endMillis)
        )

        // Bei negativer Dauer: Übernacht-Reise
        if (duration.isNegative) {
            duration = duration.plusHours(24)
        }

        return duration
    }
}
