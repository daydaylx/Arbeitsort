package de.montagezeit.app.data.location

import de.montagezeit.app.domain.model.LocationResult

/**
 * Interface für Standortanbieter
 * 
 * Verantwortlichkeiten:
 * - Standort-Request (einmalig beim Check-in)
 * - Timeout-Handling (10-15s)
 * - Accuracy-Prüfung (≤3000m)
 */
interface LocationProvider {
    
    /**
     * Ermittelt den aktuellen Standort per Best-Effort Strategie.
     *
     * Darf intern mehrere Prioritäten/Versuche verwenden.
     *
     * @param timeoutMs Gesamt-Timeout in Millisekunden
     * @return LocationResult mit Status und Daten
     */
    suspend fun getCurrentLocation(timeoutMs: Long): LocationResult
}
