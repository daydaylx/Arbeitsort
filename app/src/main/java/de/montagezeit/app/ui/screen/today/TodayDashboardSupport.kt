package de.montagezeit.app.ui.screen.today

import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.local.entity.WorkEntryWithTravelLegs
import de.montagezeit.app.domain.usecase.AggregateWorkStats
import de.montagezeit.app.domain.util.TimeCalculator
import de.montagezeit.app.domain.util.WeekCalculator
import java.time.LocalDate

fun buildWeekDayUi(
    selectedDate: LocalDate,
    todayDate: LocalDate,
    entries: List<WorkEntry>
): List<WeekDayUi> {
    val weekStart = WeekCalculator.weekStart(selectedDate)
    val weekDates = WeekCalculator.weekDays(weekStart)
    val entriesByDate = entries.associateBy { it.date }

    return weekDates.map { date ->
        val entry = entriesByDate[date]
        val status = when {
            entry == null -> WeekDayStatus.EMPTY
            entry.dayType == DayType.OFF && entry.confirmedWorkDay -> WeekDayStatus.CONFIRMED_OFF
            entry.dayType == DayType.COMP_TIME -> WeekDayStatus.CONFIRMED_OFF
            entry.confirmedWorkDay -> WeekDayStatus.CONFIRMED_WORK
            entry.morningCapturedAt != null || entry.eveningCapturedAt != null -> WeekDayStatus.PARTIAL // Eintrag vorhanden, aber noch nicht bestätigt
            else -> WeekDayStatus.EMPTY
        }
        val workHours = if (entry != null && entry.dayType == DayType.WORK) {
            val hours = TimeCalculator.calculateWorkHours(entry)
            if (hours > 0.0) hours else null
        } else {
            null
        }

        WeekDayUi(
            date = date,
            isToday = date == todayDate,
            isSelected = date == selectedDate,
            dayLabel = date.shortWeekDayLabel(),
            dayNumber = date.dayOfMonth.toString(),
            status = status,
            workHours = workHours
        )
    }
}

fun calculateWeekStats(entries: List<WorkEntryWithTravelLegs>, targetHours: Double): WeekStats {
    val stats = AggregateWorkStats()(entries)
    return WeekStats(
        totalHours = stats.totalWorkMinutes / 60.0,
        totalPaidHours = stats.totalPaidMinutes / 60.0,
        workDaysCount = stats.workDays,
        targetHours = targetHours
    )
}

fun calculateMonthStats(entries: List<WorkEntryWithTravelLegs>, targetHours: Double): MonthStats {
    val stats = AggregateWorkStats()(entries)
    return MonthStats(
        totalHours = stats.totalWorkMinutes / 60.0,
        totalPaidHours = stats.totalPaidMinutes / 60.0,
        workDaysCount = stats.workDays,
        targetHours = targetHours,
        mealAllowanceTotalCents = stats.mealAllowanceCents
    )
}
