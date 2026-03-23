package de.montagezeit.app.ui.screen.today

import de.montagezeit.app.data.local.entity.TravelLeg
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.local.entity.WorkEntryWithTravelLegs
import java.time.LocalDate

data class TodayScreenState(
    val uiState: TodayUiState,
    val selectedEntry: WorkEntry?,
    val selectedEntryWithTravel: WorkEntryWithTravelLegs? = null,
    val selectedDate: LocalDate,
    val todayDate: LocalDate,
    val weekDaysUi: List<WeekDayUi>,
    val weekStats: WeekStats?,
    val monthStats: MonthStats?,
    val isOvertimeConfigured: Boolean,
    val overtimeYearDisplay: String,
    val overtimeMonthDisplay: String?,
    val overtimeYearActualDisplay: String,
    val overtimeYearTargetDisplay: String,
    val overtimeYearCountedDays: Int,
    val overtimeYearOffDayTravelDisplay: String,
    val overtimeYearOffDayTravelDays: Int,
    val loadingActions: Set<TodayAction>
) {
    val currentEntry: WorkEntry?
        get() {
            val successEntry = (uiState as? TodayUiState.Success)?.entry
            return when {
                successEntry != null -> successEntry
                uiState is TodayUiState.Success -> null
                selectedEntry?.date == selectedDate -> selectedEntry
                else -> null
            }
        }

    val currentTravelLegs: List<TravelLeg>
        get() = selectedEntryWithTravel?.orderedTravelLegs ?: emptyList()

    val errorState: TodayUiState.Error?
        get() = uiState as? TodayUiState.Error

    val isViewingPastDay: Boolean
        get() = selectedDate != todayDate

    val showInitialLoading: Boolean
        get() = uiState is TodayUiState.Loading && currentEntry == null

    val showFullscreenError: Boolean
        get() = errorState != null && currentEntry == null

    val isDailyCheckInLoading: Boolean
        get() = loadingActions.contains(TodayAction.DAILY_MANUAL_CHECK_IN)

    val isConfirmOffdayLoading: Boolean
        get() = loadingActions.contains(TodayAction.CONFIRM_OFFDAY)
}

data class TodayDialogState(
    val showDailyCheckInDialog: Boolean,
    val dailyCheckInLocationInput: String,
    val dailyCheckInIsArrivalDeparture: Boolean,
    val dailyCheckInBreakfastIncluded: Boolean,
    val dailyCheckInAllowancePreviewCents: Int,
    val showDayLocationDialog: Boolean,
    val dayLocationInput: String,
    val showDeleteDayDialog: Boolean,
    val loadingActions: Set<TodayAction>
) {
    val isDailyCheckInLoading: Boolean
        get() = loadingActions.contains(TodayAction.DAILY_MANUAL_CHECK_IN)

    val isUpdateDayLocationLoading: Boolean
        get() = loadingActions.contains(TodayAction.UPDATE_DAY_LOCATION)

    val isDeleteDayLoading: Boolean
        get() = loadingActions.contains(TodayAction.DELETE_DAY)
}
