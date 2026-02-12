package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.entity.DayLocationSource
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.LocationStatus
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.preferences.ReminderSettings
import de.montagezeit.app.domain.location.LocationCalculator
import de.montagezeit.app.domain.model.LocationResult
import java.time.LocalDate

internal object CheckInEntryBuilder {

    enum class Snapshot {
        MORNING,
        EVENING
    }

    fun build(
        date: LocalDate,
        existingEntry: WorkEntry?,
        locationResult: LocationResult,
        snapshot: Snapshot,
        radiusKm: Double,
        settings: ReminderSettings,
        locationCalculator: LocationCalculator
    ): WorkEntry {
        val now = System.currentTimeMillis()
        val normalizedEntry = existingEntry?.copy(dayType = DayType.WORK)

        return when (locationResult) {
            is LocationResult.Success -> {
                val locationCheck = locationCalculator.checkLeipzigLocation(
                    locationResult.lat,
                    locationResult.lon,
                    radiusKm
                )

                val outsideLeipzig = when (locationCheck.isInside) {
                    true -> false
                    false -> true
                    null -> null
                }

                val dayLocationNeedsReview = locationCheck.isInside == false
                val needsReview = (normalizedEntry?.needsReview ?: false) ||
                    locationCheck.confirmRequired || dayLocationNeedsReview

                val locationLabel = if (locationCheck.isInside == true) {
                    DEFAULT_DAY_LOCATION_LABEL
                } else {
                    null
                }

                val dayLocation = DayLocationResolver.resolve(
                    existingEntry = normalizedEntry,
                    settings = settings,
                    locationResult = locationResult,
                    locationCheck = locationCheck
                )

                if (snapshot == Snapshot.MORNING) {
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
                val needsReview = true
                val dayLocation = DayLocationResolver.resolve(
                    existingEntry = normalizedEntry,
                    settings = settings,
                    locationResult = locationResult
                )

                if (snapshot == Snapshot.MORNING) {
                    normalizedEntry?.copy(
                        morningCapturedAt = now,
                        morningLocationStatus = LocationStatus.LOW_ACCURACY,
                        morningAccuracyMeters = locationResult.accuracyMeters,
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

            LocationResult.Unavailable, LocationResult.Timeout, LocationResult.SkippedByUser -> {
                val needsReview = if (locationResult == LocationResult.SkippedByUser) {
                    normalizedEntry?.needsReview ?: false
                } else {
                    true
                }
                val dayLocation = DayLocationResolver.resolve(
                    existingEntry = normalizedEntry,
                    settings = settings,
                    locationResult = locationResult
                )

                if (snapshot == Snapshot.MORNING) {
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

    private fun createDefaultEntry(
        date: LocalDate,
        morningCapturedAt: Long? = null,
        morningLat: Double? = null,
        morningLon: Double? = null,
        morningAccuracyMeters: Float? = null,
        morningLocationLabel: String? = null,
        outsideLeipzigMorning: Boolean? = null,
        morningLocationStatus: LocationStatus = LocationStatus.UNAVAILABLE,
        dayLocationLabel: String = DEFAULT_DAY_LOCATION_LABEL,
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
