package de.montagezeit.app.ui.screen.today

import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.local.entity.WorkEntryWithTravelLegs
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
            entry.morningCapturedAt != null || entry.eveningCapturedAt != null -> WeekDayStatus.PARTIAL
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
    val workEntries = entries.filter { it.workEntry.dayType == DayType.WORK && it.workEntry.confirmedWorkDay }
    val totalHours = workEntries.sumOf { TimeCalculator.calculateWorkHours(it.workEntry) }
    val totalPaidHours = workEntries.sumOf {
        TimeCalculator.calculatePaidTotalHours(it.workEntry, it.orderedTravelLegs)
    }

    return WeekStats(
        totalHours = totalHours,
        totalPaidHours = totalPaidHours,
        workDaysCount = workEntries.size,
        targetHours = targetHours
    )
}

fun calculateMonthStats(entries: List<WorkEntryWithTravelLegs>, targetHours: Double): MonthStats {
    val workEntries = entries.filter { it.workEntry.dayType == DayType.WORK && it.workEntry.confirmedWorkDay }
    val totalHours = workEntries.sumOf { TimeCalculator.calculateWorkHours(it.workEntry) }
    val totalPaidHours = workEntries.sumOf {
        TimeCalculator.calculatePaidTotalHours(it.workEntry, it.orderedTravelLegs)
    }
    val mealAllowanceTotalCents = workEntries.sumOf { it.workEntry.mealAllowanceAmountCents }

    return MonthStats(
        totalHours = totalHours,
        totalPaidHours = totalPaidHours,
        workDaysCount = workEntries.size,
        targetHours = targetHours,
        mealAllowanceTotalCents = mealAllowanceTotalCents
    )
}
