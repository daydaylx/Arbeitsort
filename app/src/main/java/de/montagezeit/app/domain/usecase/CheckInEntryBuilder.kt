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
        snapshot: Snapshot
    ): WorkEntry {
        val now = System.currentTimeMillis()
        val normalizedEntry = existingEntry?.copy(dayType = DayType.WORK)
        val dayLocation = DayLocationResolver.resolve(normalizedEntry)

        return if (snapshot == Snapshot.MORNING) {
            normalizedEntry?.copy(
                morningCapturedAt = now,
                morningLocationStatus = LocationStatus.UNAVAILABLE,
                morningLat = null,
                morningLon = null,
                morningAccuracyMeters = null,
                morningLocationLabel = null,
                outsideLeipzigMorning = null,
                dayLocationLabel = dayLocation.label,
                dayLocationSource = dayLocation.source,
                dayLocationLat = null,
                dayLocationLon = null,
                dayLocationAccuracyMeters = null,
                needsReview = false,
                updatedAt = now
            ) ?: createDefaultEntry(
                date = date,
                morningCapturedAt = now,
                morningLocationStatus = LocationStatus.UNAVAILABLE,
                dayLocationLabel = dayLocation.label,
                dayLocationSource = dayLocation.source
            )
        } else {
            normalizedEntry?.copy(
                eveningCapturedAt = now,
                eveningLocationStatus = LocationStatus.UNAVAILABLE,
                eveningLat = null,
                eveningLon = null,
                eveningAccuracyMeters = null,
                eveningLocationLabel = null,
                outsideLeipzigEvening = null,
                dayLocationLabel = dayLocation.label,
                dayLocationSource = dayLocation.source,
                dayLocationLat = null,
                dayLocationLon = null,
                dayLocationAccuracyMeters = null,
                needsReview = false,
                updatedAt = now
            ) ?: createDefaultEntry(
                date = date,
                eveningCapturedAt = now,
                eveningLocationStatus = LocationStatus.UNAVAILABLE,
                dayLocationLabel = dayLocation.label,
                dayLocationSource = dayLocation.source
            )
        }
    }

    private fun createDefaultEntry(
        date: LocalDate,
        morningCapturedAt: Long? = null,
        morningLocationStatus: LocationStatus = LocationStatus.UNAVAILABLE,
        eveningCapturedAt: Long? = null,
        eveningLocationStatus: LocationStatus = LocationStatus.UNAVAILABLE,
        dayLocationLabel: String = DEFAULT_DAY_LOCATION_LABEL,
        dayLocationSource: de.montagezeit.app.data.local.entity.DayLocationSource = de.montagezeit.app.data.local.entity.DayLocationSource.FALLBACK
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
            needsReview = false,
            createdAt = now,
            updatedAt = now
        )
    }
}
