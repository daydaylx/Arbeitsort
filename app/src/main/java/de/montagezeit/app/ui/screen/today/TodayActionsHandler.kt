package de.montagezeit.app.ui.screen.today

import de.montagezeit.app.R
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.domain.usecase.ConfirmOffDay
import de.montagezeit.app.domain.usecase.DeletedDaySnapshot
import de.montagezeit.app.domain.usecase.DeleteDayEntry
import de.montagezeit.app.domain.usecase.DailyManualCheckInInput
import de.montagezeit.app.domain.usecase.RecordDailyManualCheckIn
import de.montagezeit.app.domain.usecase.ReplaceWorkEntryWithTravelLegs
import de.montagezeit.app.domain.usecase.SetDayLocation
import de.montagezeit.app.ui.util.UiText
import de.montagezeit.app.ui.util.toUiText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import javax.inject.Inject

class TodayActionsHandler @Inject constructor(
    private val recordDailyManualCheckIn: RecordDailyManualCheckIn,
    private val confirmOffDay: ConfirmOffDay,
    private val setDayLocation: SetDayLocation,
    private val deleteDayEntry: DeleteDayEntry,
    private val replaceWorkEntryWithTravelLegs: ReplaceWorkEntryWithTravelLegs
) {
    private val _loadingActions = MutableStateFlow<Set<TodayAction>>(emptySet())
    val loadingActions: StateFlow<Set<TodayAction>> = _loadingActions.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<UiText?>(null)
    val snackbarMessage: StateFlow<UiText?> = _snackbarMessage.asStateFlow()

    private val _deletedEntryForUndo = MutableStateFlow<DeletedDaySnapshot?>(null)
    val deletedEntryForUndo: StateFlow<DeletedDaySnapshot?> = _deletedEntryForUndo.asStateFlow()

    suspend fun submitDailyManualCheckIn(input: DailyManualCheckInInput): WorkEntry? {
        if (TodayAction.DAILY_MANUAL_CHECK_IN in _loadingActions.value) return null
        addLoadingAction(TodayAction.DAILY_MANUAL_CHECK_IN)
        return try {
            recordDailyManualCheckIn(input).also {
                _snackbarMessage.value = UiText.StringResource(R.string.toast_check_in_day_saved)
                _deletedEntryForUndo.value = null
            }
        } catch (e: Exception) {
            _snackbarMessage.value = e.toUiText(R.string.today_error_confirm_day_failed)
            null
        } finally {
            removeLoadingAction(TodayAction.DAILY_MANUAL_CHECK_IN)
        }
    }

    suspend fun confirmOffDay(selectedDate: LocalDate): WorkEntry? {
        if (TodayAction.CONFIRM_OFFDAY in _loadingActions.value) return null
        addLoadingAction(TodayAction.CONFIRM_OFFDAY)
        return try {
            confirmOffDay(selectedDate, source = "UI").also {
                _snackbarMessage.value = UiText.StringResource(R.string.toast_off_day_saved)
            }
        } catch (e: Exception) {
            _snackbarMessage.value = e.toUiText(R.string.today_error_confirm_day_failed)
            null
        } finally {
            removeLoadingAction(TodayAction.CONFIRM_OFFDAY)
        }
    }

    suspend fun submitDayLocationUpdate(selectedDate: LocalDate, label: String): WorkEntry? {
        if (TodayAction.UPDATE_DAY_LOCATION in _loadingActions.value) return null
        addLoadingAction(TodayAction.UPDATE_DAY_LOCATION)
        return try {
            setDayLocation(selectedDate, label)
        } catch (e: Exception) {
            _snackbarMessage.value = e.toUiText(R.string.today_error_day_location_save_failed)
            null
        } finally {
            removeLoadingAction(TodayAction.UPDATE_DAY_LOCATION)
        }
    }

    suspend fun confirmDeleteDay(selectedDate: LocalDate): Boolean {
        if (TodayAction.DELETE_DAY in _loadingActions.value) return false
        addLoadingAction(TodayAction.DELETE_DAY)
        return try {
            _deletedEntryForUndo.value = deleteDayEntry(selectedDate)
            true
        } catch (e: Exception) {
            _snackbarMessage.value = e.toUiText(R.string.today_error_delete_failed)
            false
        } finally {
            removeLoadingAction(TodayAction.DELETE_DAY)
        }
    }

    suspend fun undoDeleteDay(onRestoredDate: (LocalDate) -> Unit): WorkEntry? {
        val snapshot = _deletedEntryForUndo.value ?: return null
        _deletedEntryForUndo.value = null
        return try {
            replaceWorkEntryWithTravelLegs(snapshot.entry, snapshot.travelLegs)
            onRestoredDate(snapshot.entry.date)
            snapshot.entry
        } catch (e: Exception) {
            _snackbarMessage.value = e.toUiText(R.string.today_error_delete_failed)
            null
        }
    }

    fun publishSnackbar(message: UiText) {
        _snackbarMessage.value = message
    }

    fun onUndoWindowClosed() {
        _deletedEntryForUndo.value = null
    }

    fun onSnackbarShown() {
        _snackbarMessage.value = null
    }

    private fun addLoadingAction(action: TodayAction) {
        _loadingActions.update { it + action }
    }

    private fun removeLoadingAction(action: TodayAction) {
        _loadingActions.update { it - action }
    }
}
