package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.entity.DayLocationSource
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.preferences.ReminderSettings
import de.montagezeit.app.domain.location.LocationCheckResult
import de.montagezeit.app.domain.model.LocationResult

internal const val DEFAULT_DAY_LOCATION_LABEL = "Leipzig"

internal data class DayLocationData(
    val label: String,
    val source: DayLocationSource,
    val lat: Double?,
    val lon: Double?,
    val accuracyMeters: Float?
)

internal object DayLocationResolver {

    fun resolve(
        existingEntry: WorkEntry?,
        settings: ReminderSettings,
        locationResult: LocationResult,
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
            ?: settings.defaultDayLocationLabel.ifBlank { DEFAULT_DAY_LOCATION_LABEL }

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
                label = DEFAULT_DAY_LOCATION_LABEL,
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
}
