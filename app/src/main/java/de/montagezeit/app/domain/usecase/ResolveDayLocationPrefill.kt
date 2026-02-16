package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.preferences.ReminderSettingsManager
import kotlinx.coroutines.flow.first

/**
 * Ermittelt den bevorzugten Tagesort für manuelle Daily-Check-ins.
 *
 * Reihenfolge:
 * 1) Heutiger Eintrag
 * 2) Letzter WORK-Eintrag
 * 3) Letzter Eintrag unabhängig vom Tagtyp
 * 4) Settings-Default
 */
class ResolveDayLocationPrefill(
    private val workEntryDao: WorkEntryDao,
    private val reminderSettingsManager: ReminderSettingsManager
) {

    suspend operator fun invoke(existingEntry: WorkEntry?): String {
        val todayLabel = existingEntry?.dayLocationLabel?.trim().orEmpty()
        if (todayLabel.isNotEmpty()) {
            return todayLabel
        }

        val latestWorkLabel = workEntryDao.getLatestDayLocationLabelByDayType(DayType.WORK)?.trim().orEmpty()
        if (latestWorkLabel.isNotEmpty()) {
            return latestWorkLabel
        }

        val latestAnyLabel = workEntryDao.getLatestDayLocationLabel()?.trim().orEmpty()
        if (latestAnyLabel.isNotEmpty()) {
            return latestAnyLabel
        }

        val settings = reminderSettingsManager.settings.first()
        return settings.defaultDayLocationLabel.ifBlank { DEFAULT_DAY_LOCATION_LABEL }
    }
}
