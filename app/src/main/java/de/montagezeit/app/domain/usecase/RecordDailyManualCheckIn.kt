package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.DayLocationSource
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.LocationStatus
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.preferences.ReminderSettingsManager
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * Speichert einen einmaligen manuellen Tages-Check-in inkl. Tagesabschluss.
 */
class RecordDailyManualCheckIn(
    private val workEntryDao: WorkEntryDao,
    private val reminderSettingsManager: ReminderSettingsManager
) {

    companion object {
        private const val CONFIRMATION_SOURCE_UI = "UI"
    }

    suspend operator fun invoke(date: LocalDate, dayLocationLabel: String): WorkEntry {
        val now = System.currentTimeMillis()
        val existingEntry = workEntryDao.getByDate(date)
        val settings = reminderSettingsManager.settings.first()

        val resolvedLabel = resolveDayLocationLabel(
            submittedLabel = dayLocationLabel,
            existingEntry = existingEntry,
            settingsDefault = settings.defaultDayLocationLabel
        )

        val updatedEntry = if (existingEntry != null) {
            val morningAlreadyCaptured = existingEntry.morningCapturedAt != null
            val eveningAlreadyCaptured = existingEntry.eveningCapturedAt != null
            existingEntry.copy(
                dayType = DayType.WORK,
                workStart = settings.workStart,
                workEnd = settings.workEnd,
                breakMinutes = settings.breakMinutes,
                dayLocationLabel = resolvedLabel,
                dayLocationSource = DayLocationSource.MANUAL,
                dayLocationLat = null,
                dayLocationLon = null,
                dayLocationAccuracyMeters = null,
                morningCapturedAt = existingEntry.morningCapturedAt ?: now,
                morningLocationLabel = if (morningAlreadyCaptured) {
                    existingEntry.morningLocationLabel ?: resolvedLabel
                } else {
                    resolvedLabel
                },
                morningLat = existingEntry.morningLat,
                morningLon = existingEntry.morningLon,
                morningAccuracyMeters = existingEntry.morningAccuracyMeters,
                outsideLeipzigMorning = existingEntry.outsideLeipzigMorning,
                morningLocationStatus = if (morningAlreadyCaptured) {
                    existingEntry.morningLocationStatus
                } else {
                    LocationStatus.UNAVAILABLE
                },
                eveningCapturedAt = existingEntry.eveningCapturedAt ?: now,
                eveningLocationLabel = if (eveningAlreadyCaptured) {
                    existingEntry.eveningLocationLabel ?: resolvedLabel
                } else {
                    resolvedLabel
                },
                eveningLat = existingEntry.eveningLat,
                eveningLon = existingEntry.eveningLon,
                eveningAccuracyMeters = existingEntry.eveningAccuracyMeters,
                outsideLeipzigEvening = existingEntry.outsideLeipzigEvening,
                eveningLocationStatus = if (eveningAlreadyCaptured) {
                    existingEntry.eveningLocationStatus
                } else {
                    LocationStatus.UNAVAILABLE
                },
                confirmedWorkDay = true,
                confirmationAt = now,
                confirmationSource = CONFIRMATION_SOURCE_UI,
                needsReview = false,
                updatedAt = now
            )
        } else {
            WorkEntry(
                date = date,
                dayType = DayType.WORK,
                workStart = settings.workStart,
                workEnd = settings.workEnd,
                breakMinutes = settings.breakMinutes,
                dayLocationLabel = resolvedLabel,
                dayLocationSource = DayLocationSource.MANUAL,
                dayLocationLat = null,
                dayLocationLon = null,
                dayLocationAccuracyMeters = null,
                morningCapturedAt = now,
                morningLocationLabel = resolvedLabel,
                morningLocationStatus = LocationStatus.UNAVAILABLE,
                eveningCapturedAt = now,
                eveningLocationLabel = resolvedLabel,
                eveningLocationStatus = LocationStatus.UNAVAILABLE,
                confirmedWorkDay = true,
                confirmationAt = now,
                confirmationSource = CONFIRMATION_SOURCE_UI,
                needsReview = false,
                createdAt = now,
                updatedAt = now
            )
        }

        workEntryDao.upsert(updatedEntry)
        return updatedEntry
    }

    private suspend fun resolveDayLocationLabel(
        submittedLabel: String,
        existingEntry: WorkEntry?,
        settingsDefault: String
    ): String {
        val trimmedSubmitted = submittedLabel.trim()
        if (trimmedSubmitted.isNotEmpty()) {
            return trimmedSubmitted
        }

        val todayLabel = existingEntry?.dayLocationLabel?.trim().orEmpty()
        if (todayLabel.isNotEmpty()) {
            return todayLabel
        }

        val lastWorkLabel = workEntryDao.getLatestDayLocationLabelByDayType(DayType.WORK)?.trim().orEmpty()
        if (lastWorkLabel.isNotEmpty()) {
            return lastWorkLabel
        }

        val lastAnyLabel = workEntryDao.getLatestDayLocationLabel()?.trim().orEmpty()
        if (lastAnyLabel.isNotEmpty()) {
            return lastAnyLabel
        }

        return settingsDefault.ifBlank { DEFAULT_DAY_LOCATION_LABEL }
    }
}
