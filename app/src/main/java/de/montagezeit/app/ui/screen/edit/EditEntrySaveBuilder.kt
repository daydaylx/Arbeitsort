package de.montagezeit.app.ui.screen.edit

import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.TravelLeg
import de.montagezeit.app.data.local.entity.TravelLegCategory
import de.montagezeit.app.data.local.entity.TravelSource
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.domain.usecase.EntryStatusResolver
import de.montagezeit.app.domain.util.transitionToDayType
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

data class EditEntryPendingSave(
    val entry: WorkEntry,
    val legs: List<TravelLeg>
)

class EditEntrySaveBuilder @Inject constructor(
    private val draftRules: EditEntryDraftRules
) {
    companion object {
        private const val CONFIRMATION_SOURCE_EDIT_SAVE = "EDIT_SAVE"
    }

    fun build(
        currentState: EditUiState,
        data: EditFormData,
        zoneId: ZoneId,
        now: Long = System.currentTimeMillis()
    ): EditEntryPendingSave? {
        val date = when (currentState) {
            is EditUiState.Success -> currentState.entry.date
            is EditUiState.NewEntry -> currentState.date
            else -> return null
        }
        val mealAllowance = resolveMealAllowanceForSave(
            dayType = data.dayType,
            isArrivalDeparture = data.mealIsArrivalDeparture,
            breakfastIncluded = data.mealBreakfastIncluded,
            workMinutes = draftRules.calculateEffectiveWorkMinutes(data),
            travelMinutes = draftRules.calculateEffectiveTravelMinutes(data)
        )
        val travelLegs = buildTravelLegsToSave(data, date, zoneId, now)
        return EditEntryPendingSave(
            entry = buildEntryToSave(currentState, data, now, mealAllowance, travelLegs),
            legs = travelLegs
        )
    }

    private fun buildEntryToSave(
        currentState: EditUiState,
        data: EditFormData,
        now: Long,
        mealAllowance: ResolvedMealAllowanceForSave,
        travelLegs: List<TravelLeg>
    ): WorkEntry {
        val date = when (currentState) {
            is EditUiState.Success -> currentState.entry.date
            is EditUiState.NewEntry -> currentState.date
            else -> error("Unsupported save state: $currentState")
        }
        val originalEntry = (currentState as? EditUiState.Success)?.entry
        val baseEntry = originalEntry?.transitionToDayType(dayType = data.dayType, now = now)
            ?: WorkEntry(
                date = date,
                createdAt = now,
                updatedAt = now
            ).transitionToDayType(dayType = data.dayType, now = now)

        val persistWorkBlock = data.dayType == DayType.WORK && data.hasWorkTimes
        val draftEntry = baseEntry.copy(
            dayType = data.dayType,
            workStart = if (persistWorkBlock) data.workStart else null,
            workEnd = if (persistWorkBlock) data.workEnd else null,
            breakMinutes = if (persistWorkBlock) data.breakMinutes else 0,
            dayLocationLabel = data.dayLocationLabel.orEmpty(),
            note = data.note,
            mealIsArrivalDeparture = mealAllowance.isArrivalDeparture,
            mealBreakfastIncluded = mealAllowance.breakfastIncluded,
            mealAllowanceBaseCents = mealAllowance.baseCents,
            mealAllowanceAmountCents = mealAllowance.amountCents,
            updatedAt = now
        )
        return when (draftEntry.dayType) {
            DayType.WORK -> {
                if (EntryStatusResolver.shouldAutoConfirmWorkDay(draftEntry, travelLegs)) {
                    draftEntry.copy(
                        confirmedWorkDay = true,
                        confirmationAt = draftEntry.confirmationAt ?: now,
                        confirmationSource = draftEntry.confirmationSource ?: CONFIRMATION_SOURCE_EDIT_SAVE
                    )
                } else {
                    draftEntry.copy(
                        confirmedWorkDay = false,
                        confirmationAt = null,
                        confirmationSource = null
                    )
                }
            }
            DayType.OFF, DayType.COMP_TIME -> draftEntry
        }
    }

    private fun buildTravelLegsToSave(
        data: EditFormData,
        date: LocalDate,
        zoneId: ZoneId,
        now: Long
    ): List<TravelLeg> {
        if (data.dayType == DayType.COMP_TIME) return emptyList()

        val normalizedTravelLegs = draftRules.normalizedTravelLegs(data)
        return normalizedTravelLegs.mapIndexed { index, leg ->
            val isOvernightLeg = leg.startTime != null &&
                leg.arriveTime != null &&
                requireNotNull(leg.arriveTime).isBefore(requireNotNull(leg.startTime))
            TravelLeg(
                workEntryDate = date,
                sortOrder = index,
                category = inferCategory(index, normalizedTravelLegs.size),
                startAt = leg.startTime?.let { toEpochMillis(date, it, zoneId) },
                arriveAt = leg.arriveTime?.let {
                    val arriveDate = if (isOvernightLeg) date.plusDays(1) else date
                    toEpochMillis(arriveDate, it, zoneId)
                },
                startLabel = leg.startLabel,
                endLabel = leg.endLabel,
                paidMinutesOverride = leg.paidMinutesOverride?.takeIf {
                    leg.startTime == null && leg.arriveTime == null
                },
                source = TravelSource.MANUAL,
                createdAt = now,
                updatedAt = now
            )
        }
    }

    private fun toEpochMillis(date: LocalDate, time: LocalTime, zoneId: ZoneId): Long {
        return date.atTime(time)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
    }

    private fun inferCategory(index: Int, totalSize: Int): TravelLegCategory {
        return when {
            totalSize == 1 -> TravelLegCategory.OTHER
            index == 0 -> TravelLegCategory.OUTBOUND
            index == totalSize - 1 -> TravelLegCategory.RETURN
            else -> TravelLegCategory.INTERSITE
        }
    }
}
