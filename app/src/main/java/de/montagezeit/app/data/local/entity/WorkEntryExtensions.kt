package de.montagezeit.app.data.local.entity

import java.time.LocalDate

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

fun WorkEntry.withConfirmedOffDay(source: String, now: Long, fallbackDayLocationLabel: String): WorkEntry {
    return withTravelCleared(now).copy(
        dayType = DayType.OFF,
        confirmedWorkDay = true,
        confirmationAt = now,
        confirmationSource = source,
        dayLocationLabel = dayLocationLabel.ifBlank { fallbackDayLocationLabel.ifBlank { "" } },
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
