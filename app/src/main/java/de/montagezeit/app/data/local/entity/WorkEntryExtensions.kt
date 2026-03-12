package de.montagezeit.app.data.local.entity

import java.time.LocalDate

data class DayTypeConfirmationState(
    val confirmedWorkDay: Boolean,
    val confirmationAt: Long?,
    val confirmationSource: String?
)

fun WorkEntry.withTravelCleared(now: Long): WorkEntry {
    // travelPaidMinutes = null bedeutet: kein Override, Timestamps werden genutzt.
    // null statt 0, damit TimeCalculator nicht durch einen veralteten Override blockiert wird.
    return copy(
        travelStartAt = null,
        travelArriveAt = null,
        travelLabelStart = null,
        travelLabelEnd = null,
        travelFromLabel = null,
        travelToLabel = null,
        travelDistanceKm = null,
        travelPaidMinutes = null,
        travelSource = null,
        travelUpdatedAt = now,
        updatedAt = now
    )
}

fun WorkEntry.withMealAllowanceCleared(): WorkEntry {
    return copy(
        mealIsArrivalDeparture = false,
        mealBreakfastIncluded = false,
        mealAllowanceBaseCents = 0,
        mealAllowanceAmountCents = 0
    )
}

fun WorkEntry.confirmationStateForDayType(dayType: DayType, now: Long): DayTypeConfirmationState {
    return when {
        dayType == DayType.COMP_TIME -> DayTypeConfirmationState(
            confirmedWorkDay = true,
            confirmationAt = now,
            confirmationSource = DayType.COMP_TIME.name
        )
        this.dayType == DayType.COMP_TIME -> DayTypeConfirmationState(
            confirmedWorkDay = false,
            confirmationAt = null,
            confirmationSource = null
        )
        else -> DayTypeConfirmationState(
            confirmedWorkDay = confirmedWorkDay,
            confirmationAt = confirmationAt,
            confirmationSource = confirmationSource
        )
    }
}

fun WorkEntry.withConfirmedOffDay(source: String, now: Long, fallbackDayLocationLabel: String): WorkEntry {
    return withTravelCleared(now).copy(
        dayType = DayType.OFF,
        confirmedWorkDay = true,
        confirmationAt = now,
        confirmationSource = source,
        dayLocationLabel = dayLocationLabel.ifBlank { fallbackDayLocationLabel.ifBlank { "" } },
        dayLocationSource = dayLocationSource,
        updatedAt = now
    ).withMealAllowanceCleared()
}

fun createConfirmedOffDayEntry(
    date: LocalDate,
    source: String,
    now: Long,
    fallbackDayLocationLabel: String
): WorkEntry {
    return WorkEntry(
        date = date,
        dayType = DayType.OFF,
        dayLocationLabel = fallbackDayLocationLabel.ifBlank { "" },
        dayLocationSource = DayLocationSource.FALLBACK,
        travelPaidMinutes = null,
        travelUpdatedAt = now,
        confirmedWorkDay = true,
        confirmationAt = now,
        confirmationSource = source,
        createdAt = now,
        updatedAt = now
    )
}
