package de.montagezeit.app.ui.screen.overview

import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntryWithTravelLegs
import de.montagezeit.app.data.preferences.ReminderSettings
import de.montagezeit.app.domain.usecase.AggregateWorkStats
import de.montagezeit.app.domain.usecase.CalculateOvertimeForRange
import java.time.LocalDate
import java.time.temporal.WeekFields

data class OverviewDateRange(
    val startDate: LocalDate,
    val endDate: LocalDate
)

internal fun OverviewPeriod.rangeFor(referenceDate: LocalDate): OverviewDateRange =
    when (this) {
        OverviewPeriod.DAY -> OverviewDateRange(referenceDate, referenceDate)
        OverviewPeriod.WEEK -> {
            val weekStart = referenceDate.with(WeekFields.ISO.dayOfWeek(), 1)
            OverviewDateRange(weekStart, weekStart.plusDays(6))
        }
        OverviewPeriod.MONTH -> {
            val monthStart = referenceDate.withDayOfMonth(1)
            OverviewDateRange(monthStart, monthStart.withDayOfMonth(monthStart.lengthOfMonth()))
        }
        OverviewPeriod.YEAR -> {
            val yearStart = referenceDate.withDayOfYear(1)
            OverviewDateRange(yearStart, yearStart.withDayOfYear(yearStart.lengthOfYear()))
        }
    }

internal fun OverviewPeriod.shiftReferenceDate(referenceDate: LocalDate, step: Long): LocalDate =
    when (this) {
        OverviewPeriod.DAY -> referenceDate.plusDays(step)
        OverviewPeriod.WEEK -> referenceDate.plusWeeks(step)
        OverviewPeriod.MONTH -> referenceDate.plusMonths(step)
        OverviewPeriod.YEAR -> referenceDate.plusYears(step)
    }

internal fun targetHoursForPeriod(
    period: OverviewPeriod,
    settings: ReminderSettings
): Double = when (period) {
    OverviewPeriod.DAY -> settings.dailyTargetHours
    OverviewPeriod.WEEK -> settings.weeklyTargetHours
    OverviewPeriod.MONTH -> settings.monthlyTargetHours
    OverviewPeriod.YEAR -> settings.monthlyTargetHours * 12
}

internal fun buildOverviewMetrics(
    period: OverviewPeriod,
    entries: List<WorkEntryWithTravelLegs>,
    settings: ReminderSettings
): OverviewMetrics {
    val stats = AggregateWorkStats()(entries)
    val overtime = CalculateOvertimeForRange()(entries, settings.dailyTargetHours)
    val unconfirmedCount = entries.count { 
        it.workEntry.dayType == DayType.WORK && !it.workEntry.confirmedWorkDay 
    }

    return OverviewMetrics(
        overtimeHours = overtime.totalOvertimeHours,
        targetHours = targetHoursForPeriod(period, settings),
        actualHours = overtime.totalActualHours,
        travelHours = stats.totalTravelMinutes / 60.0,
        mealAllowanceCents = stats.mealAllowanceCents,
        countedDays = overtime.countedDays,
        unconfirmedDaysCount = unconfirmedCount
    )
}
