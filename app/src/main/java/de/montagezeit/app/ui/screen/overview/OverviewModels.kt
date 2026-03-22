package de.montagezeit.app.ui.screen.overview

import androidx.annotation.StringRes
import de.montagezeit.app.R
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.ui.util.UiText
import java.time.LocalDate

enum class OverviewPeriod(@StringRes val labelRes: Int) {
    DAY(R.string.overview_period_day),
    WEEK(R.string.overview_period_week),
    MONTH(R.string.overview_period_month),
    YEAR(R.string.overview_period_year)
}

data class OverviewMetrics(
    val overtimeHours: Double = 0.0,
    val targetHours: Double = 0.0,
    val actualHours: Double = 0.0,
    val travelHours: Double = 0.0,
    val mealAllowanceCents: Int = 0,
    val countedDays: Int = 0
)

data class OverviewScreenState(
    val selectedDate: LocalDate,
    val selectedPeriod: OverviewPeriod,
    val selectedEntry: WorkEntry?,
    val metrics: OverviewMetrics?,
    val isLoading: Boolean,
    val errorMessage: UiText?
) {
    val currentEntry: WorkEntry?
        get() = selectedEntry?.takeIf { it.date == selectedDate }

    val showInitialLoading: Boolean
        get() = isLoading && metrics == null

    val showFullscreenError: Boolean
        get() = errorMessage != null && metrics == null
}
