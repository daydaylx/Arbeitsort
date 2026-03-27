package de.montagezeit.app.data.local.entity

import java.time.LocalDate

data class DayTypeConfirmationState(
    val confirmedWorkDay: Boolean,
    val confirmationAt: Long?,
    val confirmationSource: String?
)

fun WorkEntry.withMealAllowanceCleared(): WorkEntry {
    return copy(
        mealIsArrivalDeparture = false,
        mealBreakfastIncluded = false,
        mealAllowanceBaseCents = 0,
        mealAllowanceAmountCents = 0
    )
}

fun WorkEntry.confirmationStateForDayType(dayType: DayType, now: Long): DayTypeConfirmationState {
    return when (dayType) {
        DayType.COMP_TIME -> {
            val source = DayType.COMP_TIME.name
            if (this.dayType == DayType.COMP_TIME && confirmedWorkDay) {
                DayTypeConfirmationState(
                    confirmedWorkDay = true,
                    confirmationAt = confirmationAt ?: now,
                    confirmationSource = confirmationSource ?: source
                )
            } else {
                DayTypeConfirmationState(
                    confirmedWorkDay = true,
                    confirmationAt = now,
                    confirmationSource = source
                )
            }
        }
        DayType.OFF -> {
            val source = DayType.OFF.name
            if (this.dayType == DayType.OFF && confirmedWorkDay) {
                DayTypeConfirmationState(
                    confirmedWorkDay = true,
                    confirmationAt = confirmationAt ?: now,
                    confirmationSource = confirmationSource ?: source
                )
            } else {
                DayTypeConfirmationState(
                    confirmedWorkDay = true,
                    confirmationAt = now,
                    confirmationSource = source
                )
            }
        }
        DayType.WORK -> {
            if (this.dayType == DayType.COMP_TIME) {
                DayTypeConfirmationState(
                    confirmedWorkDay = false,
                    confirmationAt = null,
                    confirmationSource = null
                )
            } else {
                DayTypeConfirmationState(
                    confirmedWorkDay = confirmedWorkDay,
                    confirmationAt = confirmationAt,
                    confirmationSource = confirmationSource
                )
            }
        }
    }
}

fun WorkEntry.transitionToDayType(dayType: DayType, now: Long): WorkEntry {
    val transitioned = when (dayType) {
        DayType.COMP_TIME -> {
            val confirmationState = confirmationStateForDayType(dayType = dayType, now = now)
            copy(
                dayType = DayType.COMP_TIME,
                workStart = null,
                workEnd = null,
                breakMinutes = 0,
                confirmedWorkDay = confirmationState.confirmedWorkDay,
                confirmationAt = confirmationState.confirmationAt,
                confirmationSource = confirmationState.confirmationSource
            )
            .withMealAllowanceCleared()
        }

        DayType.OFF -> {
            val confirmationState = confirmationStateForDayType(dayType = dayType, now = now)
            copy(
                dayType = dayType,
                workStart = null,
                workEnd = null,
                breakMinutes = 0,
                confirmedWorkDay = confirmationState.confirmedWorkDay,
                confirmationAt = confirmationState.confirmationAt,
                confirmationSource = confirmationState.confirmationSource
            )
                .withMealAllowanceCleared()
        }

        DayType.WORK -> {
            val confirmationState = confirmationStateForDayType(dayType = dayType, now = now)
            val transitioned = copy(
                dayType = dayType,
                confirmedWorkDay = confirmationState.confirmedWorkDay,
                confirmationAt = confirmationState.confirmationAt,
                confirmationSource = confirmationState.confirmationSource
            )
            val shouldClearMealAllowance = this.dayType != DayType.WORK
            if (shouldClearMealAllowance) transitioned.withMealAllowanceCleared() else transitioned
        }
    }
    return if (transitioned == this) this else transitioned.copy(updatedAt = now)
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
