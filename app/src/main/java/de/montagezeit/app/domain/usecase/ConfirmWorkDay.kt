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
 * UseCase für die Daily Confirmation (JA - heute auf Arbeit)
 * 
 * Logik:
 * 1. Settings lesen für Standard-Arbeitszeiten
 * 2. Best-effort Location-Erfassung (15s timeout)
 * 3. WorkEntry upserten mit Standard-Werten
 * 4. confirmedWorkDay=true, confirmationAt=now setzen
 * 5. needsReview=true bei Location-Failure
 */
class ConfirmWorkDay(
    private val workEntryDao: WorkEntryDao,
    private val locationProvider: LocationProvider,
    private val locationCalculator: LocationCalculator,
    private val reminderSettingsManager: ReminderSettingsManager
) {
    
    companion object {
        // Default Timeout für Location: 15 Sekunden
        private const val LOCATION_TIMEOUT_MS = 15000L
        private const val CONFIRMATION_SOURCE_NOTIFICATION = "NOTIFICATION"
        private const val CONFIRMATION_SOURCE_UI = "UI"
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
     * Bestätigt, dass heute ein Arbeitstag ist
     * 
     * @param date Datum des Tages
     * @param forceWithoutLocation true wenn ohne Standort gespeichert werden soll
     * @param source "NOTIFICATION" oder "UI"
     * @return Aktualisierter WorkEntry
     */
    suspend operator fun invoke(
        date: LocalDate,
        forceWithoutLocation: Boolean = false,
        source: String = CONFIRMATION_SOURCE_NOTIFICATION
    ): WorkEntry {
        val existingEntry = workEntryDao.getByDate(date)
        
        // Settings lesen für Standard-Arbeitszeiten und Radius
        val settings = reminderSettingsManager.settings.first()
        val workStart = settings.workStart
        val workEnd = settings.workEnd
        val breakMinutes = settings.breakMinutes
        val locationRadiusKm = settings.locationRadiusKm.toDouble()
        
        val locationResult = if (forceWithoutLocation || !settings.preferGpsLocation) {
            LocationResult.Unavailable
        } else {
            locationProvider.getCurrentLocation(LOCATION_TIMEOUT_MS)
        }
        
        val now = System.currentTimeMillis()
        
        val updatedEntry = processLocationAndCreateEntry(
            date = date,
            existingEntry = existingEntry,
            locationResult = locationResult,
            workStart = workStart,
            workEnd = workEnd,
            breakMinutes = breakMinutes,
            radiusKm = locationRadiusKm,
            settings = settings,
            confirmationAt = now,
            confirmationSource = source
        )
        
        workEntryDao.upsert(updatedEntry)
        return updatedEntry
    }
    
    /**
     * Verarbeitet das LocationResult und erstellt/aktualisiert den WorkEntry
     */
    private fun processLocationAndCreateEntry(
        date: LocalDate,
        existingEntry: WorkEntry?,
        locationResult: LocationResult,
        workStart: java.time.LocalTime,
        workEnd: java.time.LocalTime,
        breakMinutes: Int,
        radiusKm: Double,
        settings: ReminderSettings,
        confirmationAt: Long,
        confirmationSource: String
    ): WorkEntry {
        val now = System.currentTimeMillis()

        fun resolveDayLocation(
            locationCheck: LocationCheckResult? = null
        ): DayLocationData {
            val manual = existingEntry?.takeIf { it.dayLocationSource == DayLocationSource.MANUAL }
            if (manual != null) {
                return DayLocationData(
                    label = manual.dayLocationLabel,
                    source = manual.dayLocationSource,
                    lat = manual.dayLocationLat,
                    lon = manual.dayLocationLon,
                    accuracyMeters = manual.dayLocationAccuracyMeters
                )
            }

            val fallbackLabel = existingEntry?.dayLocationLabel?.takeIf { it.isNotBlank() }
                ?: settings.defaultDayLocationLabel.ifBlank { DEFAULT_CITY_LABEL }

            if (locationResult is LocationResult.LowAccuracy && !settings.fallbackOnLowAccuracy) {
                val existing = existingEntry
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
                val needsReview = (existingEntry?.needsReview ?: false) ||
                    locationCheck.confirmRequired || dayLocationNeedsReview
                
                val locationLabel = if (locationCheck.isInside == true) {
                    "Leipzig"
                } else {
                    null // UI fragt nach Ortsname
                }
                
                val dayLocation = resolveDayLocation(locationCheck)

                existingEntry?.copy(
                    dayType = DayType.WORK,
                    workStart = workStart,
                    workEnd = workEnd,
                    breakMinutes = breakMinutes,
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
                    confirmedWorkDay = true,
                    confirmationAt = confirmationAt,
                    confirmationSource = confirmationSource,
                    needsReview = needsReview,
                    updatedAt = now
                ) ?: WorkEntry(
                    date = date,
                    dayType = DayType.WORK,
                    workStart = workStart,
                    workEnd = workEnd,
                    breakMinutes = breakMinutes,
                    dayLocationLabel = dayLocation.label,
                    dayLocationSource = dayLocation.source,
                    dayLocationLat = dayLocation.lat,
                    dayLocationLon = dayLocation.lon,
                    dayLocationAccuracyMeters = dayLocation.accuracyMeters,
                    morningLat = locationResult.lat,
                    morningLon = locationResult.lon,
                    morningAccuracyMeters = locationResult.accuracyMeters,
                    morningLocationLabel = locationLabel,
                    outsideLeipzigMorning = outsideLeipzig,
                    morningLocationStatus = LocationStatus.OK,
                    confirmedWorkDay = true,
                    confirmationAt = confirmationAt,
                    confirmationSource = confirmationSource,
                    createdAt = now,
                    updatedAt = now
                )
            }
            
            is LocationResult.LowAccuracy -> {
                // Accuracy zu niedrig
                val needsReview = true // Immer needsReview bei niedriger Accuracy
                
                val dayLocation = resolveDayLocation()

                existingEntry?.copy(
                    dayType = DayType.WORK,
                    workStart = workStart,
                    workEnd = workEnd,
                    breakMinutes = breakMinutes,
                    morningLocationStatus = LocationStatus.LOW_ACCURACY,
                    morningAccuracyMeters = locationResult.accuracyMeters,
                    outsideLeipzigMorning = null,
                    dayLocationLabel = dayLocation.label,
                    dayLocationSource = dayLocation.source,
                    dayLocationLat = dayLocation.lat,
                    dayLocationLon = dayLocation.lon,
                    dayLocationAccuracyMeters = dayLocation.accuracyMeters,
                    confirmedWorkDay = true,
                    confirmationAt = confirmationAt,
                    confirmationSource = confirmationSource,
                    needsReview = needsReview,
                    updatedAt = now
                ) ?: WorkEntry(
                    date = date,
                    dayType = DayType.WORK,
                    workStart = workStart,
                    workEnd = workEnd,
                    breakMinutes = breakMinutes,
                    dayLocationLabel = dayLocation.label,
                    dayLocationSource = dayLocation.source,
                    dayLocationLat = dayLocation.lat,
                    dayLocationLon = dayLocation.lon,
                    dayLocationAccuracyMeters = dayLocation.accuracyMeters,
                    morningLocationStatus = LocationStatus.LOW_ACCURACY,
                    morningAccuracyMeters = locationResult.accuracyMeters,
                    outsideLeipzigMorning = null,
                    confirmedWorkDay = true,
                    confirmationAt = confirmationAt,
                    confirmationSource = confirmationSource,
                    needsReview = needsReview,
                    createdAt = now,
                    updatedAt = now
                )
            }
            
            LocationResult.Unavailable, LocationResult.Timeout -> {
                // Location nicht verfügbar
                val needsReview = true
                
                val dayLocation = resolveDayLocation()

                existingEntry?.copy(
                    dayType = DayType.WORK,
                    workStart = workStart,
                    workEnd = workEnd,
                    breakMinutes = breakMinutes,
                    morningLocationStatus = LocationStatus.UNAVAILABLE,
                    outsideLeipzigMorning = null,
                    dayLocationLabel = dayLocation.label,
                    dayLocationSource = dayLocation.source,
                    dayLocationLat = dayLocation.lat,
                    dayLocationLon = dayLocation.lon,
                    dayLocationAccuracyMeters = dayLocation.accuracyMeters,
                    confirmedWorkDay = true,
                    confirmationAt = confirmationAt,
                    confirmationSource = confirmationSource,
                    needsReview = needsReview,
                    updatedAt = now
                ) ?: WorkEntry(
                    date = date,
                    dayType = DayType.WORK,
                    workStart = workStart,
                    workEnd = workEnd,
                    breakMinutes = breakMinutes,
                    dayLocationLabel = dayLocation.label,
                    dayLocationSource = dayLocation.source,
                    dayLocationLat = dayLocation.lat,
                    dayLocationLon = dayLocation.lon,
                    dayLocationAccuracyMeters = dayLocation.accuracyMeters,
                    morningLocationStatus = LocationStatus.UNAVAILABLE,
                    outsideLeipzigMorning = null,
                    confirmedWorkDay = true,
                    confirmationAt = confirmationAt,
                    confirmationSource = confirmationSource,
                    needsReview = needsReview,
                    createdAt = now,
                    updatedAt = now
                )
            }
        }
    }
}
