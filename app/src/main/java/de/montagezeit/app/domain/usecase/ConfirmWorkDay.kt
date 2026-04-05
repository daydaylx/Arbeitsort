package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.repository.WorkEntryRepository
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.preferences.ReminderSettingsManager
import de.montagezeit.app.domain.util.resolveWorkScheduleDefaults
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * UseCase für die Daily Confirmation (JA - heute auf Arbeit, ohne GPS-Erfassung)
 */
class ConfirmWorkDay(
    private val workEntryDao: WorkEntryRepository,
    private val reminderSettingsManager: ReminderSettingsManager
) {

    companion object {
        private const val CONFIRMATION_SOURCE_NOTIFICATION = "NOTIFICATION"
    }

    suspend operator fun invoke(
        date: LocalDate,
        source: String = CONFIRMATION_SOURCE_NOTIFICATION
    ): WorkEntry {
        val settings = reminderSettingsManager.settings.first()
        val defaults = resolveWorkScheduleDefaults(settings)
        val workStart = defaults.workStart
        val workEnd = defaults.workEnd
        val breakMinutes = defaults.breakMinutes
        val now = System.currentTimeMillis()
        var result: WorkEntry? = null
        workEntryDao.readModifyWrite(date) { existingEntry ->
            val dayLocation = DayLocationResolver.resolve(existingEntry)
            val updatedEntry = existingEntry?.let { entry ->
                val hasExistingWorkSchedule = entry.workStart != null && entry.workEnd != null
                entry.copy(
                    dayType = DayType.WORK,
                    workStart = entry.workStart ?: workStart,
                    workEnd = entry.workEnd ?: workEnd,
                    breakMinutes = if (hasExistingWorkSchedule) entry.breakMinutes else breakMinutes,
                    morningCapturedAt = entry.morningCapturedAt ?: now,
                    dayLocationLabel = dayLocation,
                    confirmedWorkDay = true,
                    confirmationAt = now,
                    confirmationSource = source,
                    updatedAt = now
                )
            } ?: WorkEntry(
                date = date,
                dayType = DayType.WORK,
                workStart = workStart,
                workEnd = workEnd,
                breakMinutes = breakMinutes,
                dayLocationLabel = dayLocation,
                morningCapturedAt = now,
                confirmedWorkDay = true,
                confirmationAt = now,
                confirmationSource = source,
                createdAt = now,
                updatedAt = now
            )
            result = updatedEntry
            updatedEntry
        }
        return requireNotNull(result) { "readModifyWrite hat kein Ergebnis geliefert" }
    }
}
