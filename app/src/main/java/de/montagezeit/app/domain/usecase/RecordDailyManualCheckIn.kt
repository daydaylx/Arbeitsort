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
        val settings = reminderSettingsManager.settings.first()

        val mealResult = MealAllowanceCalculator.calculate(
            dayType = DayType.WORK,
            isArrivalDeparture = input.isArrivalDeparture,
            breakfastIncluded = input.breakfastIncluded
        )

        var result: WorkEntry? = null
        workEntryDao.readModifyWrite(input.date) { existingEntry ->
            val updatedEntry = if (existingEntry != null) {
                val keepExistingWorkSchedule = existingEntry.dayType == DayType.WORK

                val morningSnapshot = buildMorningSnapshot(existingEntry, resolvedLabel, now)
                val eveningSnapshot = buildEveningSnapshot(existingEntry, resolvedLabel, now)

                // B03: If entry is already confirmed, preserve existing travel data
                val preserveTravel = existingEntry.confirmedWorkDay
                existingEntry.copy(
                    dayType = DayType.WORK,
                    workStart = if (keepExistingWorkSchedule) existingEntry.workStart else settings.workStart,
                    workEnd = if (keepExistingWorkSchedule) existingEntry.workEnd else settings.workEnd,
                    breakMinutes = if (keepExistingWorkSchedule) existingEntry.breakMinutes else settings.breakMinutes,
                    dayLocationLabel = resolvedLabel,
                    dayLocationSource = DayLocationSource.MANUAL,
                    dayLocationLat = null,
                    dayLocationLon = null,
                    dayLocationAccuracyMeters = null,
                    morningCapturedAt = morningSnapshot.capturedAt,
                    morningLocationLabel = morningSnapshot.label,
                    morningLat = existingEntry.morningLat,
                    morningLon = existingEntry.morningLon,
                    morningAccuracyMeters = existingEntry.morningAccuracyMeters,
                    morningLocationStatus = morningSnapshot.status,
                    eveningCapturedAt = eveningSnapshot.capturedAt,
                    eveningLocationLabel = eveningSnapshot.label,
                    eveningLat = existingEntry.eveningLat,
                    eveningLon = existingEntry.eveningLon,
                    eveningAccuracyMeters = existingEntry.eveningAccuracyMeters,
                    eveningLocationStatus = eveningSnapshot.status,
                    travelStartAt = if (preserveTravel) existingEntry.travelStartAt else existingEntry.travelStartAt,
                    travelArriveAt = if (preserveTravel) existingEntry.travelArriveAt else existingEntry.travelArriveAt,
                    travelPaidMinutes = if (preserveTravel) existingEntry.travelPaidMinutes else existingEntry.travelPaidMinutes,
                    travelLabelStart = if (preserveTravel) existingEntry.travelLabelStart else existingEntry.travelLabelStart,
                    travelLabelEnd = if (preserveTravel) existingEntry.travelLabelEnd else existingEntry.travelLabelEnd,
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
            result = updatedEntry
            updatedEntry
        }
        return result!!
    }

    private data class SnapshotData(
        val capturedAt: Long,
        val label: String?,
        val status: LocationStatus
    )

    private fun buildMorningSnapshot(existing: WorkEntry, resolvedLabel: String, now: Long): SnapshotData {
        val alreadyCaptured = existing.morningCapturedAt != null
        return SnapshotData(
            capturedAt = existing.morningCapturedAt ?: now,
            label = if (alreadyCaptured) existing.morningLocationLabel ?: resolvedLabel else resolvedLabel,
            status = if (alreadyCaptured) existing.morningLocationStatus else LocationStatus.UNAVAILABLE
        )
    }

    private fun buildEveningSnapshot(existing: WorkEntry, resolvedLabel: String, now: Long): SnapshotData {
        val alreadyCaptured = existing.eveningCapturedAt != null
        return SnapshotData(
            capturedAt = existing.eveningCapturedAt ?: now,
            label = if (alreadyCaptured) existing.eveningLocationLabel ?: resolvedLabel else resolvedLabel,
            status = if (alreadyCaptured) existing.eveningLocationStatus else LocationStatus.UNAVAILABLE
        )
    }
}
