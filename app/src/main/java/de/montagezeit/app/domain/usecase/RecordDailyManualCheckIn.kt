package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.DayType
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
                existingEntry.copy(
                    dayType = DayType.WORK,
                    workStart = if (keepExistingWorkSchedule) existingEntry.workStart ?: settings.workStart else settings.workStart,
                    workEnd = if (keepExistingWorkSchedule) existingEntry.workEnd ?: settings.workEnd else settings.workEnd,
                    breakMinutes = if (keepExistingWorkSchedule) existingEntry.breakMinutes else settings.breakMinutes,
                    dayLocationLabel = resolvedLabel,
                    mealIsArrivalDeparture = input.isArrivalDeparture,
                    mealBreakfastIncluded = input.breakfastIncluded,
                    mealAllowanceBaseCents = mealResult.baseCents,
                    mealAllowanceAmountCents = mealResult.amountCents,
                    confirmedWorkDay = true,
                    confirmationAt = now,
                    confirmationSource = CONFIRMATION_SOURCE_UI,
                    updatedAt = now
                )
            } else {
                WorkEntryFactory.createDefaultEntry(
                    date = input.date,
                    settings = settings,
                    dayType = DayType.WORK,
                    dayLocationLabel = resolvedLabel,
                    now = now
                ).copy(
                    mealIsArrivalDeparture = input.isArrivalDeparture,
                    mealBreakfastIncluded = input.breakfastIncluded,
                    mealAllowanceBaseCents = mealResult.baseCents,
                    mealAllowanceAmountCents = mealResult.amountCents,
                    confirmedWorkDay = true,
                    confirmationAt = now,
                    confirmationSource = CONFIRMATION_SOURCE_UI
                )
            }
            result = updatedEntry
            updatedEntry
        }
        return result!!
    }
}
