package de.montagezeit.app.ui.screen.history

import androidx.compose.runtime.Immutable
import de.montagezeit.app.ui.screen.overview.OverviewPeriod
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.WeekFields

private const val HISTORY_GROUPED_CONTENT_START_INDEX = 2
private val historyRequestWeekFields = WeekFields.ISO

@Immutable
data class HistoryOpenRequest(
    val requestId: Long,
    val anchorDate: LocalDate,
    val grouping: HistoryGrouping
)

enum class HistoryGrouping {
    WEEK,
    MONTH
}

internal data class HistoryUiSelectionSeed(
    val showCalendar: Boolean,
    val showMonths: Boolean,
    val calendarMode: CalendarMode,
    val selectedDate: LocalDate,
    val selectedMonth: YearMonth
)

internal fun createHistoryOpenRequest(
    requestId: Long,
    anchorDate: LocalDate,
    overviewPeriod: OverviewPeriod
): HistoryOpenRequest = HistoryOpenRequest(
    requestId = requestId,
    anchorDate = anchorDate,
    grouping = historyGroupingForOverviewPeriod(overviewPeriod)
)

internal fun historyGroupingForOverviewPeriod(period: OverviewPeriod): HistoryGrouping =
    when (period) {
        OverviewPeriod.DAY, OverviewPeriod.WEEK -> HistoryGrouping.WEEK
        OverviewPeriod.MONTH, OverviewPeriod.YEAR -> HistoryGrouping.MONTH
    }

internal fun historySelectionSeedForRequest(request: HistoryOpenRequest): HistoryUiSelectionSeed =
    HistoryUiSelectionSeed(
        showCalendar = false,
        showMonths = request.grouping == HistoryGrouping.MONTH,
        calendarMode = if (request.grouping == HistoryGrouping.MONTH) {
            CalendarMode.MONTH
        } else {
            CalendarMode.WEEK
        },
        selectedDate = request.anchorDate,
        selectedMonth = YearMonth.from(request.anchorDate)
    )

internal fun historyGroupedScrollTargetIndex(
    request: HistoryOpenRequest,
    weeks: List<WeekGroup>,
    months: List<MonthGroup>
): Int? = when (request.grouping) {
    HistoryGrouping.WEEK -> weekScrollTargetIndex(request.anchorDate, weeks)
    HistoryGrouping.MONTH -> monthScrollTargetIndex(request.anchorDate, months)
}

private fun weekScrollTargetIndex(anchorDate: LocalDate, weeks: List<WeekGroup>): Int? {
    if (weeks.isEmpty()) return null
    val anchorWeekStart = anchorDate.with(historyRequestWeekFields.dayOfWeek(), 1)
    val exactIndex = weeks.indexOfFirst { it.weekStart == anchorWeekStart }
    val targetIndex = when {
        exactIndex >= 0 -> exactIndex
        else -> weeks.indexOfFirst { !it.weekStart.isAfter(anchorWeekStart) }.takeIf { it >= 0 } ?: 0
    }
    return HISTORY_GROUPED_CONTENT_START_INDEX + weeks
        .take(targetIndex)
        .sumOf { 1 + it.entries.size }
}

private fun monthScrollTargetIndex(anchorDate: LocalDate, months: List<MonthGroup>): Int? {
    if (months.isEmpty()) return null
    val anchorMonth = YearMonth.from(anchorDate)
    val exactIndex = months.indexOfFirst {
        it.year == anchorMonth.year && it.month == anchorMonth.monthValue
    }
    val targetIndex = when {
        exactIndex >= 0 -> exactIndex
        else -> months.indexOfFirst {
            YearMonth.of(it.year, it.month) <= anchorMonth
        }.takeIf { it >= 0 } ?: 0
    }
    return HISTORY_GROUPED_CONTENT_START_INDEX + months
        .take(targetIndex)
        .sumOf { 1 + it.entries.size }
}
