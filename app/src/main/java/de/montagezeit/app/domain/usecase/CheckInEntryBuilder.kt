package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.LocationStatus
import de.montagezeit.app.data.local.entity.WorkEntry
import java.time.LocalDate

internal object CheckInEntryBuilder {

    enum class Snapshot {
        MORNING,
        EVENING
    }

    fun build(
        date: LocalDate,
        existingEntry: WorkEntry?,
        snapshot: Snapshot,
        locationStatus: LocationStatus = LocationStatus.UNAVAILABLE,
        locationLat: Double? = null,
        locationLon: Double? = null,
        locationAccuracyMeters: Float? = null,
        locationLabel: String? = null
    ): WorkEntry {
        val now = System.currentTimeMillis()
        val normalizedEntry = existingEntry?.copy(dayType = DayType.WORK)
        val dayLocation = DayLocationResolver.resolve(normalizedEntry)

        // Standort-Review ist deaktiviert: Ortsangabe wird manuell geführt.
        val needsReview = false

        return if (snapshot == Snapshot.MORNING) {
            normalizedEntry?.copy(
                morningCapturedAt = now,
                morningLocationStatus = locationStatus,
                morningLat = locationLat,
                morningLon = locationLon,
                morningAccuracyMeters = locationAccuracyMeters,
                morningLocationLabel = locationLabel,
                dayLocationLabel = dayLocation.label,
                dayLocationSource = dayLocation.source,
                dayLocationLat = null,
                dayLocationLon = null,
                dayLocationAccuracyMeters = null,
                needsReview = needsReview,
                updatedAt = now
            ) ?: createDefaultEntry(
                date = date,
                morningCapturedAt = now,
                morningLocationStatus = locationStatus,
                dayLocationLabel = dayLocation.label,
                dayLocationSource = dayLocation.source,
                needsReview = needsReview
            )
        } else {
            normalizedEntry?.copy(
                eveningCapturedAt = now,
                eveningLocationStatus = locationStatus,
                eveningLat = locationLat,
                eveningLon = locationLon,
                eveningAccuracyMeters = locationAccuracyMeters,
                eveningLocationLabel = locationLabel,
                dayLocationLabel = dayLocation.label,
                dayLocationSource = dayLocation.source,
                dayLocationLat = null,
                dayLocationLon = null,
                dayLocationAccuracyMeters = null,
                needsReview = needsReview,
                updatedAt = now
            ) ?: createDefaultEntry(
                date = date,
                eveningCapturedAt = now,
                eveningLocationStatus = locationStatus,
                dayLocationLabel = dayLocation.label,
                dayLocationSource = dayLocation.source,
                needsReview = needsReview
            )
        }
    }

    private fun createDefaultEntry(
        date: LocalDate,
        morningCapturedAt: Long? = null,
        morningLocationStatus: LocationStatus = LocationStatus.UNAVAILABLE,
        eveningCapturedAt: Long? = null,
        eveningLocationStatus: LocationStatus = LocationStatus.UNAVAILABLE,
        dayLocationLabel: String = "",
        dayLocationSource: de.montagezeit.app.data.local.entity.DayLocationSource = de.montagezeit.app.data.local.entity.DayLocationSource.FALLBACK,
        needsReview: Boolean = false
    ): WorkEntry {
        val now = System.currentTimeMillis()
        return WorkEntry(
            date = date,
            dayType = DayType.WORK,
            dayLocationLabel = dayLocationLabel,
            dayLocationSource = dayLocationSource,
            dayLocationLat = null,
            dayLocationLon = null,
            dayLocationAccuracyMeters = null,
            morningCapturedAt = morningCapturedAt,
            morningLocationStatus = morningLocationStatus,
            eveningCapturedAt = eveningCapturedAt,
            eveningLocationStatus = eveningLocationStatus,
            needsReview = needsReview,
            createdAt = now,
            updatedAt = now
        )
    }
}
