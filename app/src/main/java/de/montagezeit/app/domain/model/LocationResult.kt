package de.montagezeit.app.domain.model

/**
 * Ergebnis einer Standortabfrage durch LocationProvider
 */
sealed class LocationResult {
    /**
     * Erfolgreiche Standortermittlung mit akzeptabler Genauigkeit
     */
    data class Success(
        val lat: Double,
        val lon: Double,
        val accuracyMeters: Float
    ) : LocationResult()

    /**
     * Standort nicht verfügbar (GPS aus / Permission denied)
     */
    object Unavailable : LocationResult()

    /**
     * Timeout bei Standortabfrage
     */
    object Timeout : LocationResult()

    /**
     * Standort ermittelt, aber Genauigkeit zu niedrig
     */
    data class LowAccuracy(val accuracyMeters: Float) : LocationResult()
}
