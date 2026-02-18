package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.entity.DayLocationSource
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.domain.util.AppDefaults

internal const val DEFAULT_DAY_LOCATION_LABEL = AppDefaults.DEFAULT_CITY

internal data class DayLocationData(
    val label: String,
    val source: DayLocationSource,
    val lat: Double?,
    val lon: Double?,
    val accuracyMeters: Float?
)

internal object DayLocationResolver {

    /**
     * Löst den Tagesort auf Basis des bestehenden Eintrags auf.
     * GPS-Koordinaten werden nicht mehr verwendet.
     */
    fun resolve(existingEntry: WorkEntry?): DayLocationData {
        val manual = existingEntry?.takeIf { it.dayLocationSource == DayLocationSource.MANUAL }
        if (manual != null) {
            return DayLocationData(
                label = manual.dayLocationLabel,
                source = manual.dayLocationSource,
                lat = null,
                lon = null,
                accuracyMeters = null
            )
        }

        val existingLabel = existingEntry?.dayLocationLabel?.takeIf { it.isNotBlank() }
            ?: DEFAULT_DAY_LOCATION_LABEL

        return DayLocationData(
            label = existingLabel,
            source = DayLocationSource.FALLBACK,
            lat = null,
            lon = null,
            accuracyMeters = null
        )
    }
}
