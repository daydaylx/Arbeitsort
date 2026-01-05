package de.montagezeit.app.domain.location

import kotlin.math.*

/**
 * Berechnet Distanzen und prüft ob ein Standort innerhalb des Leipzig-Radius liegt
 */
class LocationCalculator {
    
    companion object {
        // Leipzig Zentrum als Referenzpunkt
        private const val LEIPZIG_LAT = 51.340
        private const val LEIPZIG_LON = 12.374
        
        // Erdradius in Kilometern
        private const val EARTH_RADIUS_KM = 6371.0
        
        // Standard Radius um Leipzig (konfigurierbar)
        const val DEFAULT_RADIUS_KM = 30.0
        
        // Grenzzone: ±2km um Radius (28-32km)
        private const val BORDER_ZONE_KM = 2.0
    }
    
    /**
     * Berechnet die Distanz zwischen zwei Koordinaten mittels Haversine-Formel
     * 
     * @param lat1 Breitengrad Punkt 1
     * @param lon1 Längengrad Punkt 1
     * @param lat2 Breitengrad Punkt 2
     * @param lon2 Längengrad Punkt 2
     * @return Distanz in Metern
     */
    fun calculateDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val latDistance = Math.toRadians(lat2 - lat1)
        val lonDistance = Math.toRadians(lon2 - lon1)
        
        val a = (sin(latDistance / 2) * sin(latDistance / 2)
                + (cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2))
                * sin(lonDistance / 2) * sin(lonDistance / 2)))
        
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        return EARTH_RADIUS_KM * c * 1000 // Umrechnung in Meter
    }
    
    /**
     * Berechnet die Distanz eines Punktes zum Leipziger Zentrum
     * 
     * @param lat Breitengrad
     * @param lon Längengrad
     * @return Distanz in Metern
     */
    fun calculateDistanceToLeipzig(lat: Double, lon: Double): Double {
        return calculateDistance(lat, lon, LEIPZIG_LAT, LEIPZIG_LON)
    }
    
    /**
     * Prüft, ob ein Standort innerhalb des Leipziger Radius liegt
     * 
     * @param lat Breitengrad
     * @param lon Längengrad
     * @param radiusKm Radius in Kilometern (Standard 30km)
     * @return LocationCheckResult mit Details
     */
    fun checkLeipzigLocation(
        lat: Double,
        lon: Double,
        radiusKm: Double = DEFAULT_RADIUS_KM
    ): LocationCheckResult {
        val distanceMeters = calculateDistanceToLeipzig(lat, lon)
        val distanceKm = distanceMeters / 1000.0
        
        return when {
            distanceKm <= radiusKm - BORDER_ZONE_KM -> {
                // Eindeutig innerhalb
                LocationCheckResult(
                    isInside = true,
                    distanceKm = distanceKm,
                    confirmRequired = false
                )
            }
            distanceKm <= radiusKm + BORDER_ZONE_KM -> {
                // Grenzzone - 28-32km bei 30km Radius
                LocationCheckResult(
                    isInside = null, // Unklar
                    distanceKm = distanceKm,
                    confirmRequired = true
                )
            }
            else -> {
                // Eindeutig außerhalb
                LocationCheckResult(
                    isInside = false,
                    distanceKm = distanceKm,
                    confirmRequired = false
                )
            }
        }
    }
    
    /**
     * Prüft, ob die Genauigkeit akzeptabel ist
     * 
     * @param accuracyMeters Genauigkeit in Metern
     * @return true wenn Genauigkeit ≤ 3000m
     */
    fun isAccuracyAcceptable(accuracyMeters: Float): Boolean {
        return accuracyMeters <= 3000.0f
    }
}

/**
 * Ergebnis des Leipzig-Standort-Checks
 */
data class LocationCheckResult(
    /**
     * true = eindeutig innerhalb, false = eindeutig außerhalb, null = Grenzzone
     */
    val isInside: Boolean?,
    
    /**
     * Distanz zum Leipziger Zentrum in Kilometern
     */
    val distanceKm: Double,
    
    /**
     * true wenn Benutzer manuell bestätigen soll (Grenzzone)
     */
    val confirmRequired: Boolean
)
