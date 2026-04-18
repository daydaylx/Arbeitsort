package de.montagezeit.app.domain.util

import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntry
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
    // OFF/COMP_TIME auto-confirm on transition; WORK-like types start pending unless
    // we're staying on the same type and it was already confirmed.
    if (!dayType.isWorkLike) {
        val source = dayType.name
        val keepTimestamp = this.dayType == dayType && confirmedWorkDay
        return DayTypeConfirmationState(
            confirmedWorkDay = true,
            confirmationAt = if (keepTimestamp) confirmationAt ?: now else now,
            confirmationSource = if (keepTimestamp) confirmationSource ?: source else source
        )
    }
    val keepConfirmation = this.dayType == dayType && confirmedWorkDay
    return DayTypeConfirmationState(
        confirmedWorkDay = keepConfirmation,
        confirmationAt = if (keepConfirmation) confirmationAt else null,
        confirmationSource = if (keepConfirmation) confirmationSource else null
    )
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

        DayType.SCHULUNG, DayType.LEHRGANG -> {
            val confirmationState = confirmationStateForDayType(dayType = dayType, now = now)
            copy(
                dayType = dayType,
                confirmedWorkDay = confirmationState.confirmedWorkDay,
                confirmationAt = confirmationState.confirmationAt,
                confirmationSource = confirmationState.confirmationSource
            ).withMealAllowanceCleared()
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
