package de.montagezeit.app.data.local.entity

import java.time.LocalDate

private const val DEFAULT_CITY_LABEL = "Leipzig"

fun WorkEntry.withTravelCleared(now: Long): WorkEntry {
    return copy(
        travelStartAt = null,
        travelArriveAt = null,
        travelLabelStart = null,
        travelLabelEnd = null,
        travelFromLabel = null,
        travelToLabel = null,
        travelDistanceKm = null,
        travelPaidMinutes = 0,
        travelSource = null,
        travelUpdatedAt = now,
        updatedAt = now
    )
}

fun WorkEntry.withConfirmedOffDay(source: String, now: Long, fallbackDayLocationLabel: String): WorkEntry {
    return withTravelCleared(now).copy(
        dayType = DayType.OFF,
        confirmedWorkDay = true,
        confirmationAt = now,
        confirmationSource = source,
        dayLocationLabel = dayLocationLabel.ifBlank { fallbackDayLocationLabel.ifBlank { DEFAULT_CITY_LABEL } },
        dayLocationSource = dayLocationSource,
        updatedAt = now
    )
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
        dayLocationLabel = fallbackDayLocationLabel.ifBlank { DEFAULT_CITY_LABEL },
        dayLocationSource = DayLocationSource.FALLBACK,
        travelPaidMinutes = 0,
        travelUpdatedAt = now,
        confirmedWorkDay = true,
        confirmationAt = now,
        confirmationSource = source,
        createdAt = now,
        updatedAt = now
    )
}
