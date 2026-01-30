package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.DayLocationSource
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.preferences.ReminderSettingsManager
import de.montagezeit.app.work.ReminderWindowEvaluator
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
        val now = System.currentTimeMillis()
        val existing = workEntryDao.getByDate(date)

        val updated = if (existing != null) {
            existing.copy(
                dayLocationLabel = label,
                dayLocationSource = DayLocationSource.MANUAL,
                dayLocationLat = null,
                dayLocationLon = null,
                dayLocationAccuracyMeters = null,
                updatedAt = now
            )
        } else {
            val settings = reminderSettingsManager.settings.first()
            val dayType = if (ReminderWindowEvaluator.isNonWorkingDay(date, settings)) {
                DayType.OFF
            } else {
                DayType.WORK
            }

            WorkEntry(
                date = date,
                dayType = dayType,
                workStart = settings.workStart,
                workEnd = settings.workEnd,
                breakMinutes = settings.breakMinutes,
                dayLocationLabel = label,
                dayLocationSource = DayLocationSource.MANUAL,
                createdAt = now,
                updatedAt = now
            )
        }

        workEntryDao.upsert(updated)
        return updated
    }
}
