package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.entity.WorkEntry
internal const val DEFAULT_DAY_LOCATION_LABEL = ""

internal object DayLocationResolver {

    /**
     * Löst den Tagesort auf Basis des bestehenden Eintrags auf.
     */
    fun resolve(existingEntry: WorkEntry?): String {
        return existingEntry?.dayLocationLabel?.takeIf { it.isNotBlank() } ?: ""
    }
}
