package de.montagezeit.app.ui.screen.overview

import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntryWithTravelLegs
import de.montagezeit.app.data.preferences.ReminderSettings
import de.montagezeit.app.diagnostics.DiagnosticTrace
import de.montagezeit.app.diagnostics.DiagnosticWarningCodes
import de.montagezeit.app.diagnostics.toSanitizedDiagnosticPayload
import de.montagezeit.app.domain.usecase.AggregateWorkStats
import de.montagezeit.app.domain.usecase.CalculateOvertimeForRange
import de.montagezeit.app.domain.usecase.EntryStatusResolver
import de.montagezeit.app.domain.usecase.isStatisticsEligible
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

internal fun buildOverviewMetrics(
    period: OverviewPeriod,
    entries: List<WorkEntryWithTravelLegs>,
    settings: ReminderSettings,
    trace: DiagnosticTrace? = null
): OverviewMetrics {
    trace?.event(
        name = "overview_inputs",
        payload = mapOf(
            "period" to period.name,
            "entryCount" to entries.size,
            "dailyTargetHours" to settings.dailyTargetHours
        )
    )

    val stats = AggregateWorkStats()(entries, trace)
    val overtime = CalculateOvertimeForRange()(entries, settings.dailyTargetHours, trace)
    val unconfirmedCount = entries.count {
        it.workEntry.dayType == DayType.WORK && !EntryStatusResolver.resolve(it).isConfirmed
    }
    entries
        .filter { it.workEntry.dayType == DayType.WORK }
        .filterNot(::isStatisticsEligible)
        .forEach { excluded ->
            trace?.warning(
                DiagnosticWarningCodes.UNCONFIRMED_DAY_EXCLUDED,
                payload = excluded.toSanitizedDiagnosticPayload()
            )
        }

    return OverviewMetrics(
        overtimeHours = overtime.totalOvertimeHours,
        targetHours = overtime.totalTargetHours,
        actualHours = overtime.totalActualHours,
        travelHours = stats.totalTravelMinutes / 60.0,
        mealAllowanceCents = stats.mealAllowanceCents,
        countedDays = overtime.countedDays,
        unconfirmedDaysCount = unconfirmedCount,
        compTimeDays = stats.compTimeDays
    ).also { metrics ->
        trace?.event(
            name = "overview_metrics_built",
            phase = de.montagezeit.app.diagnostics.DiagnosticPhase.RESULT,
            payload = mapOf(
                "overtimeHours" to metrics.overtimeHours,
                "targetHours" to metrics.targetHours,
                "actualHours" to metrics.actualHours,
                "travelHours" to metrics.travelHours,
                "mealAllowanceCents" to metrics.mealAllowanceCents,
                "countedDays" to metrics.countedDays,
                "unconfirmedDaysCount" to metrics.unconfirmedDaysCount,
                "compTimeDays" to metrics.compTimeDays
            )
        )
    }
}
