package de.montagezeit.app.data.local.entity

import java.time.LocalDate

data class DayTypeConfirmationState(
    val confirmedWorkDay: Boolean,
    val confirmationAt: Long?,
    val confirmationSource: String?
)

fun WorkEntry.copyWithLegacyTravel(
    travelStartAt: Long? = this.travelStartAt,
    travelArriveAt: Long? = this.travelArriveAt,
    travelLabelStart: String? = this.travelLabelStart,
    travelLabelEnd: String? = this.travelLabelEnd,
    travelFromLabel: String? = this.travelFromLabel,
    travelToLabel: String? = this.travelToLabel,
    travelDistanceKm: Double? = this.travelDistanceKm,
    travelPaidMinutes: Int? = this.travelPaidMinutes,
    travelSource: TravelSource? = this.travelSource,
    travelUpdatedAt: Long? = this.travelUpdatedAt,
    returnStartAt: Long? = this.returnStartAt,
    returnArriveAt: Long? = this.returnArriveAt
): WorkEntry {
    return copy().also {
        it.travelStartAt = travelStartAt
        it.travelArriveAt = travelArriveAt
        it.travelLabelStart = travelLabelStart
        it.travelLabelEnd = travelLabelEnd
        it.travelFromLabel = travelFromLabel
        it.travelToLabel = travelToLabel
        it.travelDistanceKm = travelDistanceKm
        it.travelPaidMinutes = travelPaidMinutes
        it.travelSource = travelSource
        it.travelUpdatedAt = travelUpdatedAt
        it.returnStartAt = returnStartAt
        it.returnArriveAt = returnArriveAt
    }
}

fun WorkEntry.withLegacyTravelFrom(source: WorkEntry): WorkEntry {
    return copyWithLegacyTravel(
        travelStartAt = source.travelStartAt,
        travelArriveAt = source.travelArriveAt,
        travelLabelStart = source.travelLabelStart,
        travelLabelEnd = source.travelLabelEnd,
        travelFromLabel = source.travelFromLabel,
        travelToLabel = source.travelToLabel,
        travelDistanceKm = source.travelDistanceKm,
        travelPaidMinutes = source.travelPaidMinutes,
        travelSource = source.travelSource,
        travelUpdatedAt = source.travelUpdatedAt,
        returnStartAt = source.returnStartAt,
        returnArriveAt = source.returnArriveAt
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

fun WorkEntry.withTravelCleared(): WorkEntry {
    return copyWithLegacyTravel(
        travelStartAt = null,
        travelArriveAt = null,
        travelLabelStart = null,
        travelLabelEnd = null,
        travelFromLabel = null,
        travelToLabel = null,
        travelDistanceKm = null,
        travelPaidMinutes = null,
        travelSource = null,
        travelUpdatedAt = null,
        returnStartAt = null,
        returnArriveAt = null
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

fun WorkEntry.transitionToDayType(dayType: DayType, now: Long): WorkEntry {
    return when (dayType) {
        DayType.COMP_TIME -> copy(
                dayType = DayType.COMP_TIME,
                workStart = null,
                workEnd = null,
                breakMinutes = 0,
                confirmedWorkDay = true,
                confirmationAt = now,
                confirmationSource = DayType.COMP_TIME.name,
                updatedAt = now
            )
            .withTravelCleared()
            .withMealAllowanceCleared()

        DayType.OFF -> {
            val confirmationState = confirmationStateForDayType(dayType = dayType, now = now)
            copy(
                dayType = dayType,
                workStart = null,
                workEnd = null,
                breakMinutes = 0,
                confirmedWorkDay = confirmationState.confirmedWorkDay,
                confirmationAt = confirmationState.confirmationAt,
                confirmationSource = confirmationState.confirmationSource,
                updatedAt = now
            )
                .withTravelCleared()
                .withMealAllowanceCleared()
        }

        DayType.WORK -> {
            val confirmationState = confirmationStateForDayType(dayType = dayType, now = now)
            val transitioned = copy(
                dayType = dayType,
                confirmedWorkDay = confirmationState.confirmedWorkDay,
                confirmationAt = confirmationState.confirmationAt,
                confirmationSource = confirmationState.confirmationSource,
                updatedAt = now
            )
            val shouldClearMealAllowance = this.dayType != DayType.WORK
            if (shouldClearMealAllowance) transitioned.withMealAllowanceCleared() else transitioned
        }
    }
}

fun WorkEntry.withConfirmedOffDay(source: String, now: Long, fallbackDayLocationLabel: String): WorkEntry {
    return copy(
        dayType = DayType.OFF,
        workStart = null,
        workEnd = null,
        breakMinutes = 0,
        confirmedWorkDay = true,
        confirmationAt = now,
        confirmationSource = source,
        dayLocationLabel = dayLocationLabel.ifBlank { fallbackDayLocationLabel.ifBlank { "" } },
        updatedAt = now
    )
        .withTravelCleared()
        .withMealAllowanceCleared()
}

fun createConfirmedOffDayEntry(
    date: LocalDate,
    source: String,
    now: Long,
    fallbackDayLocationLabel: String
): WorkEntry {
    return WorkEntry(date = date, createdAt = now)
        .withConfirmedOffDay(source = source, now = now, fallbackDayLocationLabel = fallbackDayLocationLabel)
}
