package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.DayLocationSource
import de.montagezeit.app.data.local.entity.LocationStatus
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.location.LocationProvider
import de.montagezeit.app.data.preferences.ReminderSettings
import de.montagezeit.app.data.preferences.ReminderSettingsManager
import de.montagezeit.app.domain.location.LocationCheckResult
import de.montagezeit.app.domain.location.LocationCalculator
import de.montagezeit.app.domain.model.LocationResult
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * UseCase für den Abend-Check-in
 * 
 * Logik analog zu RecordMorningCheckIn, aber für Abend-Snapshot
 */
class RecordEveningCheckIn(
    private val workEntryDao: WorkEntryDao,
    private val locationProvider: LocationProvider,
    private val locationCalculator: LocationCalculator,
    private val reminderSettingsManager: ReminderSettingsManager
) {
    
    companion object {
        // Default Timeout für Location: 15 Sekunden
        private const val LOCATION_TIMEOUT_MS = 15000L
        private const val DEFAULT_CITY_LABEL = "Leipzig"
    }

    private data class DayLocationData(
        val label: String,
        val source: DayLocationSource,
        val lat: Double?,
        val lon: Double?,
        val accuracyMeters: Float?
    )
    
    /**
     * Führt den Abend-Check-in durch
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
        
        val locationResult = if (forceWithoutLocation || !settings.preferGpsLocation) {
            LocationResult.Unavailable
        } else {
            locationProvider.getCurrentLocation(LOCATION_TIMEOUT_MS)
        }
        
        val updatedEntry = processLocationResult(
            date = date,
            existingEntry = existingEntry,
            locationResult = locationResult,
            isMorning = false, // Abend
            radiusKm = locationRadiusKm,
            settings = settings
        )
        
        workEntryDao.upsert(updatedEntry)
        return updatedEntry
    }
    
    /**
     * Verarbeitet das LocationResult und aktualisiert den WorkEntry
     */
    private fun processLocationResult(
        date: LocalDate,
        existingEntry: WorkEntry?,
        locationResult: LocationResult,
        isMorning: Boolean,
        radiusKm: Double,
        settings: ReminderSettings
    ): WorkEntry {
        val now = System.currentTimeMillis()
        val normalizedEntry = existingEntry?.copy(dayType = DayType.WORK)

        fun resolveDayLocation(
            locationCheck: LocationCheckResult? = null
        ): DayLocationData {
            val manual = normalizedEntry?.takeIf { it.dayLocationSource == DayLocationSource.MANUAL }
            if (manual != null) {
                return DayLocationData(
                    label = manual.dayLocationLabel,
                    source = manual.dayLocationSource,
                    lat = manual.dayLocationLat,
                    lon = manual.dayLocationLon,
                    accuracyMeters = manual.dayLocationAccuracyMeters
                )
            }

            val fallbackLabel = normalizedEntry?.dayLocationLabel?.takeIf { it.isNotBlank() }
                ?: settings.defaultDayLocationLabel.ifBlank { DEFAULT_CITY_LABEL }

            if (locationResult is LocationResult.LowAccuracy && !settings.fallbackOnLowAccuracy) {
                val existing = normalizedEntry
                if (existing != null) {
                    return DayLocationData(
                        label = existing.dayLocationLabel,
                        source = existing.dayLocationSource,
                        lat = existing.dayLocationLat,
                        lon = existing.dayLocationLon,
                        accuracyMeters = existing.dayLocationAccuracyMeters
                    )
                }
            }

            return if (locationResult is LocationResult.Success && locationCheck?.isInside == true) {
                DayLocationData(
                    label = DEFAULT_CITY_LABEL,
                    source = DayLocationSource.GPS,
                    lat = locationResult.lat,
                    lon = locationResult.lon,
                    accuracyMeters = locationResult.accuracyMeters
                )
            } else {
                DayLocationData(
                    label = fallbackLabel,
                    source = DayLocationSource.FALLBACK,
                    lat = null,
                    lon = null,
                    accuracyMeters = null
                )
            }
        }
        
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
                
                val dayLocationNeedsReview = locationCheck.isInside == false
                val needsReview = (normalizedEntry?.needsReview ?: false) ||
                    locationCheck.confirmRequired || dayLocationNeedsReview
                
                val locationLabel = if (locationCheck.isInside == true) {
                    "Leipzig"
                } else {
                    null // UI fragt nach Ortsname
                }
                
                val dayLocation = resolveDayLocation(locationCheck)

                if (isMorning) {
                    normalizedEntry?.copy(
                        morningCapturedAt = now,
                        morningLat = locationResult.lat,
                        morningLon = locationResult.lon,
                        morningAccuracyMeters = locationResult.accuracyMeters,
                        morningLocationLabel = locationLabel,
                        outsideLeipzigMorning = outsideLeipzig,
                        morningLocationStatus = LocationStatus.OK,
                        dayLocationLabel = dayLocation.label,
                        dayLocationSource = dayLocation.source,
                        dayLocationLat = dayLocation.lat,
                        dayLocationLon = dayLocation.lon,
                        dayLocationAccuracyMeters = dayLocation.accuracyMeters,
                        needsReview = needsReview,
                        updatedAt = now
                    ) ?: createDefaultEntry(
                        date = date,
                        morningCapturedAt = now,
                        morningLat = locationResult.lat,
                        morningLon = locationResult.lon,
                        morningAccuracyMeters = locationResult.accuracyMeters,
                        morningLocationLabel = locationLabel,
                        outsideLeipzigMorning = outsideLeipzig,
                        morningLocationStatus = LocationStatus.OK,
                        dayLocationLabel = dayLocation.label,
                        dayLocationSource = dayLocation.source,
                        dayLocationLat = dayLocation.lat,
                        dayLocationLon = dayLocation.lon,
                        dayLocationAccuracyMeters = dayLocation.accuracyMeters,
                        needsReview = needsReview
                    )
                } else {
                    normalizedEntry?.copy(
                        eveningCapturedAt = now,
                        eveningLat = locationResult.lat,
                        eveningLon = locationResult.lon,
                        eveningAccuracyMeters = locationResult.accuracyMeters,
                        eveningLocationLabel = locationLabel,
                        outsideLeipzigEvening = outsideLeipzig,
                        eveningLocationStatus = LocationStatus.OK,
                        dayLocationLabel = dayLocation.label,
                        dayLocationSource = dayLocation.source,
                        dayLocationLat = dayLocation.lat,
                        dayLocationLon = dayLocation.lon,
                        dayLocationAccuracyMeters = dayLocation.accuracyMeters,
                        needsReview = needsReview,
                        updatedAt = now
                    ) ?: createDefaultEntry(
                        date = date,
                        eveningCapturedAt = now,
                        eveningLat = locationResult.lat,
                        eveningLon = locationResult.lon,
                        eveningAccuracyMeters = locationResult.accuracyMeters,
                        eveningLocationLabel = locationLabel,
                        outsideLeipzigEvening = outsideLeipzig,
                        eveningLocationStatus = LocationStatus.OK,
                        dayLocationLabel = dayLocation.label,
                        dayLocationSource = dayLocation.source,
                        dayLocationLat = dayLocation.lat,
                        dayLocationLon = dayLocation.lon,
                        dayLocationAccuracyMeters = dayLocation.accuracyMeters,
                        needsReview = needsReview
                    )
                }
            }
            
            is LocationResult.LowAccuracy -> {
                // Accuracy zu niedrig
                val needsReview = true // Immer needsReview bei niedriger Accuracy
                
                val dayLocation = resolveDayLocation()

                if (isMorning) {
                    normalizedEntry?.copy(
                        morningCapturedAt = now,
                        morningLocationStatus = LocationStatus.LOW_ACCURACY,
                        morningAccuracyMeters = locationResult.accuracyMeters,
                        outsideLeipzigMorning = null, // Unbekannt
                        dayLocationLabel = dayLocation.label,
                        dayLocationSource = dayLocation.source,
                        dayLocationLat = dayLocation.lat,
                        dayLocationLon = dayLocation.lon,
                        dayLocationAccuracyMeters = dayLocation.accuracyMeters,
                        needsReview = needsReview,
                        updatedAt = now
                    ) ?: createDefaultEntry(
                        date = date,
                        morningCapturedAt = now,
                        morningLocationStatus = LocationStatus.LOW_ACCURACY,
                        morningAccuracyMeters = locationResult.accuracyMeters,
                        outsideLeipzigMorning = null,
                        dayLocationLabel = dayLocation.label,
                        dayLocationSource = dayLocation.source,
                        dayLocationLat = dayLocation.lat,
                        dayLocationLon = dayLocation.lon,
                        dayLocationAccuracyMeters = dayLocation.accuracyMeters,
                        needsReview = needsReview
                    )
                } else {
                    normalizedEntry?.copy(
                        eveningCapturedAt = now,
                        eveningLocationStatus = LocationStatus.LOW_ACCURACY,
                        eveningAccuracyMeters = locationResult.accuracyMeters,
                        outsideLeipzigEvening = null,
                        dayLocationLabel = dayLocation.label,
                        dayLocationSource = dayLocation.source,
                        dayLocationLat = dayLocation.lat,
                        dayLocationLon = dayLocation.lon,
                        dayLocationAccuracyMeters = dayLocation.accuracyMeters,
                        needsReview = needsReview,
                        updatedAt = now
                    ) ?: createDefaultEntry(
                        date = date,
                        eveningCapturedAt = now,
                        eveningLocationStatus = LocationStatus.LOW_ACCURACY,
                        eveningAccuracyMeters = locationResult.accuracyMeters,
                        outsideLeipzigEvening = null,
                        dayLocationLabel = dayLocation.label,
                        dayLocationSource = dayLocation.source,
                        dayLocationLat = dayLocation.lat,
                        dayLocationLon = dayLocation.lon,
                        dayLocationAccuracyMeters = dayLocation.accuracyMeters,
                        needsReview = needsReview
                    )
                }
            }
            
            LocationResult.Unavailable, LocationResult.Timeout -> {
                // Location nicht verfügbar
                val needsReview = true
                
                val dayLocation = resolveDayLocation()

                if (isMorning) {
                    normalizedEntry?.copy(
                        morningCapturedAt = now,
                        morningLocationStatus = LocationStatus.UNAVAILABLE,
                        outsideLeipzigMorning = null,
                        dayLocationLabel = dayLocation.label,
                        dayLocationSource = dayLocation.source,
                        dayLocationLat = dayLocation.lat,
                        dayLocationLon = dayLocation.lon,
                        dayLocationAccuracyMeters = dayLocation.accuracyMeters,
                        needsReview = needsReview,
                        updatedAt = now
                    ) ?: createDefaultEntry(
                        date = date,
                        morningCapturedAt = now,
                        morningLocationStatus = LocationStatus.UNAVAILABLE,
                        outsideLeipzigMorning = null,
                        dayLocationLabel = dayLocation.label,
                        dayLocationSource = dayLocation.source,
                        dayLocationLat = dayLocation.lat,
                        dayLocationLon = dayLocation.lon,
                        dayLocationAccuracyMeters = dayLocation.accuracyMeters,
                        needsReview = needsReview
                    )
                } else {
                    normalizedEntry?.copy(
                        eveningCapturedAt = now,
                        eveningLocationStatus = LocationStatus.UNAVAILABLE,
                        outsideLeipzigEvening = null,
                        dayLocationLabel = dayLocation.label,
                        dayLocationSource = dayLocation.source,
                        dayLocationLat = dayLocation.lat,
                        dayLocationLon = dayLocation.lon,
                        dayLocationAccuracyMeters = dayLocation.accuracyMeters,
                        needsReview = needsReview,
                        updatedAt = now
                    ) ?: createDefaultEntry(
                        date = date,
                        eveningCapturedAt = now,
                        eveningLocationStatus = LocationStatus.UNAVAILABLE,
                        outsideLeipzigEvening = null,
                        dayLocationLabel = dayLocation.label,
                        dayLocationSource = dayLocation.source,
                        dayLocationLat = dayLocation.lat,
                        dayLocationLon = dayLocation.lon,
                        dayLocationAccuracyMeters = dayLocation.accuracyMeters,
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
        date: LocalDate,
        morningCapturedAt: Long? = null,
        morningLat: Double? = null,
        morningLon: Double? = null,
        morningAccuracyMeters: Float? = null,
        morningLocationLabel: String? = null,
        outsideLeipzigMorning: Boolean? = null,
        morningLocationStatus: LocationStatus = LocationStatus.UNAVAILABLE,
        dayLocationLabel: String = DEFAULT_CITY_LABEL,
        dayLocationSource: DayLocationSource = DayLocationSource.FALLBACK,
        dayLocationLat: Double? = null,
        dayLocationLon: Double? = null,
        dayLocationAccuracyMeters: Float? = null,
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
            date = date,
            dayType = DayType.WORK,
            dayLocationLabel = dayLocationLabel,
            dayLocationSource = dayLocationSource,
            dayLocationLat = dayLocationLat,
            dayLocationLon = dayLocationLon,
            dayLocationAccuracyMeters = dayLocationAccuracyMeters,
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
