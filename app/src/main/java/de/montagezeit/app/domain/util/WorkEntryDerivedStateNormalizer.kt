package de.montagezeit.app.domain.util

import de.montagezeit.app.data.local.entity.TravelLeg
import de.montagezeit.app.data.local.entity.WorkEntry

object WorkEntryDerivedStateNormalizer {

    private const val CONFIRMATION_SOURCE_DERIVED_STATE = "DERIVED_STATE"

    fun normalize(
        entry: WorkEntry,
        travelLegs: List<TravelLeg>,
        now: Long = System.currentTimeMillis()
    ): WorkEntry {
        val workMinutes = TimeCalculator.calculateWorkMinutes(entry)
        val travelMinutes = TimeCalculator.calculateTravelMinutes(travelLegs)
        val hasActivity = workMinutes > 0 || travelMinutes > 0

        val mealSnapshot = MealAllowanceCalculator.resolveForActivity(
            dayType = entry.dayType,
            isArrivalDeparture = entry.mealIsArrivalDeparture,
            breakfastIncluded = entry.mealBreakfastIncluded,
            workMinutes = workMinutes,
            travelMinutes = travelMinutes,
            locationLabel = entry.dayLocationLabel
        )

        val normalized = if (entry.dayType.isWorkLike) {
            if (hasActivity) {
                entry.copy(
                    confirmedWorkDay = true,
                    confirmationAt = entry.confirmationAt ?: now,
                    confirmationSource = entry.confirmationSource
                        ?.takeIf(String::isNotBlank)
                        ?: CONFIRMATION_SOURCE_DERIVED_STATE,
                    mealIsArrivalDeparture = mealSnapshot.isArrivalDeparture,
                    mealBreakfastIncluded = mealSnapshot.breakfastIncluded,
                    mealAllowanceBaseCents = mealSnapshot.baseCents,
                    mealAllowanceAmountCents = mealSnapshot.amountCents
                )
            } else {
                entry.copy(
                    confirmedWorkDay = false,
                    confirmationAt = null,
                    confirmationSource = null,
                    mealIsArrivalDeparture = mealSnapshot.isArrivalDeparture,
                    mealBreakfastIncluded = mealSnapshot.breakfastIncluded,
                    mealAllowanceBaseCents = mealSnapshot.baseCents,
                    mealAllowanceAmountCents = mealSnapshot.amountCents
                )
            }
        } else {
            entry.copy(
                confirmedWorkDay = true,
                confirmationAt = entry.confirmationAt ?: now,
                confirmationSource = entry.confirmationSource
                    ?.takeIf(String::isNotBlank)
                    ?: entry.dayType.name,
                mealIsArrivalDeparture = mealSnapshot.isArrivalDeparture,
                mealBreakfastIncluded = mealSnapshot.breakfastIncluded,
                mealAllowanceBaseCents = mealSnapshot.baseCents,
                mealAllowanceAmountCents = mealSnapshot.amountCents
            )
        }

        return if (normalized == entry) entry else normalized
    }
}
