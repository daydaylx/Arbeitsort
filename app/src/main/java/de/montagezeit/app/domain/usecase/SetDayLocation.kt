package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.preferences.ReminderSettingsManager
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * Setzt den Tagesort manuell für einen Tag.
 */
class SetDayLocation(
    private val workEntryDao: WorkEntryDao,
    private val reminderSettingsManager: ReminderSettingsManager
) {
    suspend operator fun invoke(date: LocalDate, label: String): WorkEntry {
        val normalizedLabel = label.trim()
        require(normalizedLabel.isNotBlank()) { "dayLocationLabel darf nicht leer sein" }

        val now = System.currentTimeMillis()
        val settings = reminderSettingsManager.settings.first()

        var result: WorkEntry? = null
        workEntryDao.readModifyWrite(date) { existing ->
            val updated = if (existing != null) {
                existing.copy(
                    dayLocationLabel = normalizedLabel,
                    updatedAt = now
                )
            } else {
                // Explizites Setzen eines Tagesorts impliziert Arbeitstag, unabhängig von Wochenende/Feiertag
                WorkEntryFactory.createDefaultEntry(
                    date = date,
                    settings = settings,
                    dayType = DayType.WORK,
                    dayLocationLabel = normalizedLabel,
                    now = now
                )
            }
            result = updated
            updated
        }
        return requireNotNull(result) { "readModifyWrite must return an entry for date $date" }
    }
}
