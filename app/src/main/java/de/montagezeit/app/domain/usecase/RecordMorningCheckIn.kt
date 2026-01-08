package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.LocationStatus
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.location.LocationProvider
import de.montagezeit.app.data.preferences.ReminderSettingsManager
import de.montagezeit.app.domain.location.LocationCalculator
import de.montagezeit.app.domain.model.LocationResult
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * UseCase für den Morgen-Check-in
 * 
 * Logik:
 * 1. Location abrufen (optional)
 * 2. Radius-Check Leipzig (mit Benutzereinstellung)
 * 3. WorkEntry upserten
 * 4. needsReview setzen wenn nötig
 */
class RecordMorningCheckIn(
    private val workEntryDao: WorkEntryDao,
    private val locationProvider: LocationProvider,
    private val locationCalculator: LocationCalculator,
    private val reminderSettingsManager: ReminderSettingsManager
) {
    
    companion object {
        // Default Timeout für Location: 15 Sekunden
        private const val LOCATION_TIMEOUT_MS = 15000L
    }
    
    /**
     * Führt den Morgen-Check-in durch
     * 
     * @param date Datum des Check-ins
     * @param forceWithoutLocation true wenn ohne Standort gespeichert werden soll
     * @return Aktualisierter WorkEntry
     */
    suspend operator fun invoke(
        date: LocalDate,
        forceWithoutLocation: Boolean = false
    ): WorkEntry {
        val existingEntry = workEntryDao.getByDate(date)
        
        // Settings lesen für konfigurierten Radius
        val settings = reminderSettingsManager.settings.first()
        val locationRadiusKm = settings.locationRadiusKm.toDouble()
        
        val locationResult = if (forceWithoutLocation) {
            LocationResult.Unavailable
        } else {
            locationProvider.getCurrentLocation(LOCATION_TIMEOUT_MS)
        }
        
        val updatedEntry = processLocationResult(
            existingEntry = existingEntry,
            locationResult = locationResult,
            isMorning = true,
            radiusKm = locationRadiusKm
        )
        
        workEntryDao.upsert(updatedEntry)
        return updatedEntry
    }
    
    /**
     * Verarbeitet das LocationResult und aktualisiert den WorkEntry
     */
    private fun processLocationResult(
        existingEntry: WorkEntry?,
        locationResult: LocationResult,
        isMorning: Boolean,
        radiusKm: Double
    ): WorkEntry {
        val now = System.currentTimeMillis()
        
        return when (locationResult) {
            is LocationResult.Success -> {
                // Location OK - Radius-Check mit konfiguriertem Radius
                val locationCheck = locationCalculator.checkLeipzigLocation(
                    locationResult.lat,
                    locationResult.lon,
                    radiusKm
                )
                
                val outsideLeipzig = when (locationCheck.isInside) {
                    true -> false
                    false -> true
                    null -> null // Unklar (Grenzzone)
                }
                
                val needsReview = existingEntry?.needsReview ?: false || 
                                   locationCheck.confirmRequired
                
                val locationLabel = if (locationCheck.isInside == true) {
                    "Leipzig"
                } else {
                    null // UI fragt nach Ortsname
                }
                
                if (isMorning) {
                    existingEntry?.copy(
                        morningCapturedAt = now,
                        morningLat = locationResult.lat,
                        morningLon = locationResult.lon,
                        morningAccuracyMeters = locationResult.accuracyMeters,
                        morningLocationLabel = locationLabel,
                        outsideLeipzigMorning = outsideLeipzig,
                        morningLocationStatus = LocationStatus.OK,
                        needsReview = needsReview,
                        updatedAt = now
                    ) ?: createDefaultEntry(
                        morningCapturedAt = now,
                        morningLat = locationResult.lat,
                        morningLon = locationResult.lon,
                        morningAccuracyMeters = locationResult.accuracyMeters,
                        morningLocationLabel = locationLabel,
                        outsideLeipzigMorning = outsideLeipzig,
                        morningLocationStatus = LocationStatus.OK,
                        needsReview = needsReview
                    )
                } else {
                    existingEntry?.copy(
                        eveningCapturedAt = now,
                        eveningLat = locationResult.lat,
                        eveningLon = locationResult.lon,
                        eveningAccuracyMeters = locationResult.accuracyMeters,
                        eveningLocationLabel = locationLabel,
                        outsideLeipzigEvening = outsideLeipzig,
                        eveningLocationStatus = LocationStatus.OK,
                        needsReview = needsReview,
                        updatedAt = now
                    ) ?: createDefaultEntry(
                        eveningCapturedAt = now,
                        eveningLat = locationResult.lat,
                        eveningLon = locationResult.lon,
                        eveningAccuracyMeters = locationResult.accuracyMeters,
                        eveningLocationLabel = locationLabel,
                        outsideLeipzigEvening = outsideLeipzig,
                        eveningLocationStatus = LocationStatus.OK,
                        needsReview = needsReview
                    )
                }
            }
            
            is LocationResult.LowAccuracy -> {
                // Accuracy zu niedrig
                val needsReview = true // Immer needsReview bei niedriger Accuracy
                
                if (isMorning) {
                    existingEntry?.copy(
                        morningCapturedAt = now,
                        morningLocationStatus = LocationStatus.LOW_ACCURACY,
                        morningAccuracyMeters = locationResult.accuracyMeters,
                        outsideLeipzigMorning = null, // Unbekannt
                        needsReview = needsReview,
                        updatedAt = now
                    ) ?: createDefaultEntry(
                        morningCapturedAt = now,
                        morningLocationStatus = LocationStatus.LOW_ACCURACY,
                        morningAccuracyMeters = locationResult.accuracyMeters,
                        outsideLeipzigMorning = null,
                        needsReview = needsReview
                    )
                } else {
                    existingEntry?.copy(
                        eveningCapturedAt = now,
                        eveningLocationStatus = LocationStatus.LOW_ACCURACY,
                        eveningAccuracyMeters = locationResult.accuracyMeters,
                        outsideLeipzigEvening = null,
                        needsReview = needsReview,
                        updatedAt = now
                    ) ?: createDefaultEntry(
                        eveningCapturedAt = now,
                        eveningLocationStatus = LocationStatus.LOW_ACCURACY,
                        eveningAccuracyMeters = locationResult.accuracyMeters,
                        outsideLeipzigEvening = null,
                        needsReview = needsReview
                    )
                }
            }
            
            LocationResult.Unavailable, LocationResult.Timeout -> {
                // Location nicht verfügbar
                val needsReview = true
                
                if (isMorning) {
                    existingEntry?.copy(
                        morningCapturedAt = now,
                        morningLocationStatus = LocationStatus.UNAVAILABLE,
                        outsideLeipzigMorning = null,
                        needsReview = needsReview,
                        updatedAt = now
                    ) ?: createDefaultEntry(
                        morningCapturedAt = now,
                        morningLocationStatus = LocationStatus.UNAVAILABLE,
                        outsideLeipzigMorning = null,
                        needsReview = needsReview
                    )
                } else {
                    existingEntry?.copy(
                        eveningCapturedAt = now,
                        eveningLocationStatus = LocationStatus.UNAVAILABLE,
                        outsideLeipzigEvening = null,
                        needsReview = needsReview,
                        updatedAt = now
                    ) ?: createDefaultEntry(
                        eveningCapturedAt = now,
                        eveningLocationStatus = LocationStatus.UNAVAILABLE,
                        outsideLeipzigEvening = null,
                        needsReview = needsReview
                    )
                }
            }
        }
    }
    
    /**
     * Erstellt einen neuen WorkEntry mit Defaults
     */
    private fun createDefaultEntry(
        morningCapturedAt: Long? = null,
        morningLat: Double? = null,
        morningLon: Double? = null,
        morningAccuracyMeters: Float? = null,
        morningLocationLabel: String? = null,
        outsideLeipzigMorning: Boolean? = null,
        morningLocationStatus: LocationStatus = LocationStatus.UNAVAILABLE,
        eveningCapturedAt: Long? = null,
        eveningLat: Double? = null,
        eveningLon: Double? = null,
        eveningAccuracyMeters: Float? = null,
        eveningLocationLabel: String? = null,
        outsideLeipzigEvening: Boolean? = null,
        eveningLocationStatus: LocationStatus = LocationStatus.UNAVAILABLE,
        needsReview: Boolean = false
    ): WorkEntry {
        val now = System.currentTimeMillis()
        return WorkEntry(
            date = LocalDate.now(),
            dayType = DayType.WORK,
            morningCapturedAt = morningCapturedAt,
            morningLat = morningLat,
            morningLon = morningLon,
            morningAccuracyMeters = morningAccuracyMeters,
            morningLocationLabel = morningLocationLabel,
            outsideLeipzigMorning = outsideLeipzigMorning,
            morningLocationStatus = morningLocationStatus,
            eveningCapturedAt = eveningCapturedAt,
            eveningLat = eveningLat,
            eveningLon = eveningLon,
            eveningAccuracyMeters = eveningAccuracyMeters,
            eveningLocationLabel = eveningLocationLabel,
            outsideLeipzigEvening = outsideLeipzigEvening,
            eveningLocationStatus = eveningLocationStatus,
            needsReview = needsReview,
            createdAt = now,
            updatedAt = now
        )
    }
}
