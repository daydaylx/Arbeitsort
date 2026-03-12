package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.DayLocationSource
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.LocationStatus
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.preferences.ReminderSettingsManager
import de.montagezeit.app.domain.util.MealAllowanceCalculator
import kotlinx.coroutines.flow.first
import java.time.LocalDate

data class DailyManualCheckInInput(
    val date: LocalDate,
    val dayLocationLabel: String,
    val isArrivalDeparture: Boolean = false,
    val breakfastIncluded: Boolean = false
)

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

    suspend operator fun invoke(input: DailyManualCheckInInput): WorkEntry {
        val resolvedLabel = input.dayLocationLabel.trim()
        if (resolvedLabel.isEmpty()) {
            throw IllegalArgumentException("dayLocationLabel darf nicht leer sein")
        }

        val now = System.currentTimeMillis()
        val existingEntry = workEntryDao.getByDate(input.date)
        val settings = reminderSettingsManager.settings.first()

        val mealResult = MealAllowanceCalculator.calculate(
            dayType = DayType.WORK,
            isArrivalDeparture = input.isArrivalDeparture,
            breakfastIncluded = input.breakfastIncluded
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
                eveningLocationStatus = if (eveningAlreadyCaptured) {
                    existingEntry.eveningLocationStatus
                } else {
                    LocationStatus.UNAVAILABLE
                },
                mealIsArrivalDeparture = input.isArrivalDeparture,
                mealBreakfastIncluded = input.breakfastIncluded,
                mealAllowanceBaseCents = mealResult.baseCents,
                mealAllowanceAmountCents = mealResult.amountCents,
                confirmedWorkDay = true,
                confirmationAt = now,
                confirmationSource = CONFIRMATION_SOURCE_UI,
                needsReview = false,
                updatedAt = now
            )
        } else {
            WorkEntry(
                date = input.date,
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
                mealIsArrivalDeparture = input.isArrivalDeparture,
                mealBreakfastIncluded = input.breakfastIncluded,
                mealAllowanceBaseCents = mealResult.baseCents,
                mealAllowanceAmountCents = mealResult.amountCents,
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
}
