package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.LocationStatus
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.preferences.ReminderSettingsManager
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * UseCase für die Daily Confirmation (JA - heute auf Arbeit, ohne GPS-Erfassung)
 */
class ConfirmWorkDay(
    private val workEntryDao: WorkEntryDao,
    private val reminderSettingsManager: ReminderSettingsManager
) {

    companion object {
        private const val CONFIRMATION_SOURCE_NOTIFICATION = "NOTIFICATION"
    }

    suspend operator fun invoke(
        date: LocalDate,
        source: String = CONFIRMATION_SOURCE_NOTIFICATION
    ): WorkEntry {
        val existingEntry = workEntryDao.getByDate(date)
        val settings = reminderSettingsManager.settings.first()
        val workStart = settings.workStart
        val workEnd = settings.workEnd
        val breakMinutes = settings.breakMinutes
        val now = System.currentTimeMillis()

        val dayLocation = DayLocationResolver.resolve(existingEntry)

        val updatedEntry = existingEntry?.copy(
            dayType = DayType.WORK,
            workStart = workStart,
            workEnd = workEnd,
            breakMinutes = breakMinutes,
            morningCapturedAt = existingEntry.morningCapturedAt ?: now,
            morningLocationStatus = existingEntry.morningLocationStatus.let {
                if (it == LocationStatus.UNAVAILABLE) LocationStatus.UNAVAILABLE else it
            },
            dayLocationLabel = dayLocation.label,
            dayLocationSource = dayLocation.source,
            dayLocationLat = null,
            dayLocationLon = null,
            dayLocationAccuracyMeters = null,
            confirmedWorkDay = true,
            confirmationAt = now,
            confirmationSource = source,
            needsReview = false,
            updatedAt = now
        ) ?: WorkEntry(
            date = date,
            dayType = DayType.WORK,
            workStart = workStart,
            workEnd = workEnd,
            breakMinutes = breakMinutes,
            dayLocationLabel = dayLocation.label,
            dayLocationSource = dayLocation.source,
            dayLocationLat = null,
            dayLocationLon = null,
            dayLocationAccuracyMeters = null,
            morningCapturedAt = now,
            morningLocationStatus = LocationStatus.UNAVAILABLE,
            confirmedWorkDay = true,
            confirmationAt = now,
            confirmationSource = source,
            needsReview = false,
            createdAt = now,
            updatedAt = now
        )

        workEntryDao.upsert(updatedEntry)
        return updatedEntry
    }
}
