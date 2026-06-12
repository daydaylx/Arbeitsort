package de.montagezeit.app.ui.screen.today

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import de.montagezeit.app.data.local.entity.TravelLeg
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.local.entity.WorkEntryWithTravelLegs
import java.time.LocalDate

@Stable
data class TodayScreenState(
    val uiState: TodayUiState,
    val selectedEntry: WorkEntry?,
    val selectedEntryWithTravel: WorkEntryWithTravelLegs? = null,
    val selectedDate: LocalDate,
    val todayDate: LocalDate,
    val loadingActions: Set<TodayAction>,
    val dailyTargetHours: Double = 8.0
) {
    val currentEntry: WorkEntry?
        get() {
            // Loading ohne passenden reaktiven Eintrag → nichts zeigen (vermeidet alten Eintrag)
            if (uiState is TodayUiState.Loading && selectedEntry?.date != selectedDate) return null
            // Fehler → nichts zeigen
            if (uiState is TodayUiState.Error) return null
            // Reaktiver Flow zuerst: immer frisch (Background-Updates, Notification-Aktionen etc.)
            if (selectedEntry?.date == selectedDate) return selectedEntry
            // Fallback: uiState-Eintrag (kurzes Fenster bevor DB-Flow nach Datumswechsel emittiert)
            return (uiState as? TodayUiState.Success)?.entry?.takeIf { it.date == selectedDate }
        }

    val currentTravelLegs: List<TravelLeg>
        get() = selectedEntryWithTravel
            ?.takeIf { it.workEntry.date == selectedDate }
            ?.orderedTravelLegs
            ?: emptyList()

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

    val isSetDayTypeLoading: Boolean
        get() = loadingActions.contains(TodayAction.SET_DAY_TYPE)
}

@Immutable
data class TodayDialogState(
    val showDailyCheckInDialog: Boolean,
    val dailyCheckInLocationInput: String,
    val dailyCheckInLocationIsSuggestion: Boolean = false,
    val dailyCheckInIsArrivalDeparture: Boolean,
    val dailyCheckInBreakfastIncluded: Boolean,
    val dailyCheckInAllowancePreviewCents: Int,
    val showDayLocationDialog: Boolean,
    val dayLocationInput: String,
    val showDeleteDayDialog: Boolean,
    val loadingActions: Set<TodayAction>
) {
    val dailyCheckInIsMealEligible: Boolean
        get() = dailyCheckInAllowancePreviewCents > 0

    val isDailyCheckInLoading: Boolean
        get() = loadingActions.contains(TodayAction.DAILY_MANUAL_CHECK_IN)

    val isUpdateDayLocationLoading: Boolean
        get() = loadingActions.contains(TodayAction.UPDATE_DAY_LOCATION)

    val isDeleteDayLoading: Boolean
        get() = loadingActions.contains(TodayAction.DELETE_DAY)
}
