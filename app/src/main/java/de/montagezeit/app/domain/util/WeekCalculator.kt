package de.montagezeit.app.domain.util

import java.time.LocalDate
import java.time.temporal.WeekFields

object WeekCalculator {
    /** Returns the Monday of the ISO week containing [date]. */
    fun weekStart(date: LocalDate): LocalDate =
        date.with(WeekFields.ISO.dayOfWeek(), 1)

    /** Returns 7 days Mon–Sun starting from [weekStart]. */
    fun weekDays(weekStart: LocalDate): List<LocalDate> =
        (0L..6L).map { weekStart.plusDays(it) }
}
