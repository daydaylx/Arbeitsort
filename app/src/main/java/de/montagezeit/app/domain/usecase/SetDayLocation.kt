package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.DayLocationSource
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.preferences.ReminderSettingsManager
import de.montagezeit.app.domain.util.NonWorkingDayChecker
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * Setzt den Tagesort manuell für einen Tag.
 */
class SetDayLocation(
    private val workEntryDao: WorkEntryDao,
    private val reminderSettingsManager: ReminderSettingsManager,
    private val nonWorkingDayChecker: NonWorkingDayChecker
) {
    suspend operator fun invoke(date: LocalDate, label: String): WorkEntry {
        val now = System.currentTimeMillis()
        val settings = reminderSettingsManager.settings.first()

        var result: WorkEntry? = null
        workEntryDao.readModifyWrite(date) { existing ->
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
                val dayType = if (nonWorkingDayChecker.isNonWorkingDay(date, settings)) {
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
            result = updated
            updated
        }
        return result!!
    }
}
