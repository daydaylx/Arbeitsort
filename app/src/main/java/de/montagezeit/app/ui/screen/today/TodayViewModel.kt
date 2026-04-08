package de.montagezeit.app.ui.screen.today

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.montagezeit.app.R
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.local.entity.WorkEntryWithTravelLegs
import de.montagezeit.app.domain.usecase.GetWorkEntriesByDateRange
import de.montagezeit.app.domain.usecase.GetWorkEntryByDate
import de.montagezeit.app.domain.usecase.ObserveWorkEntryByDate
import de.montagezeit.app.domain.usecase.ObserveWorkEntryWithTravelByDate
import de.montagezeit.app.domain.usecase.DailyManualCheckInInput
import de.montagezeit.app.domain.usecase.ResolveDayLocationPrefill
import de.montagezeit.app.domain.util.MealAllowanceCalculator
import de.montagezeit.app.ui.util.UiText
import de.montagezeit.app.ui.util.toUiText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

enum class WeekDayStatus {
    CONFIRMED_WORK, CONFIRMED_OFF, PARTIAL, EMPTY
}

@Immutable
data class WeekDayUi(
    val date: LocalDate,
    val isToday: Boolean,
    val isSelected: Boolean,
    val dayLabel: String,
    val dayNumber: String,
    val status: WeekDayStatus,
    val workHours: Double?
)

enum class TodayAction {
    DAILY_MANUAL_CHECK_IN,
    CONFIRM_OFFDAY,
    UPDATE_DAY_LOCATION,
    DELETE_DAY
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class TodayViewModel @Inject constructor(
    private val observeWorkEntryByDate: ObserveWorkEntryByDate,
    private val observeWorkEntryWithTravelByDate: ObserveWorkEntryWithTravelByDate,
    private val getWorkEntryByDate: GetWorkEntryByDate,
    private val getWorkEntriesByDateRange: GetWorkEntriesByDateRange,
    private val resolveDayLocationPrefill: ResolveDayLocationPrefill,
    private val dateCoordinator: TodayDateCoordinator,
    private val weekOverviewUseCase: TodayWeekOverviewUseCase,
    private val dialogsStateHolder: TodayDialogsStateHolder,
    private val actionsHandler: TodayActionsHandler
) : ViewModel() {

    private val _uiState = MutableStateFlow<TodayUiState>(TodayUiState.Loading)
    val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()

    val selectedDate: StateFlow<LocalDate> = dateCoordinator.selectedDate
    private val todayDate: StateFlow<LocalDate> = dateCoordinator.todayDate

    val todayEntry: StateFlow<WorkEntry?> = todayDate
        .flatMapLatest { date -> observeWorkEntryByDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedEntry: StateFlow<WorkEntry?> = selectedDate
        .flatMapLatest { date -> observeWorkEntryByDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedEntryWithTravel: StateFlow<WorkEntryWithTravelLegs?> = selectedDate
        .flatMapLatest { date -> observeWorkEntryWithTravelByDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _weekDaysUi = MutableStateFlow<List<WeekDayUi>>(emptyList())
    val weekDaysUi: StateFlow<List<WeekDayUi>> = _weekDaysUi.asStateFlow()

    val showDailyCheckInDialog: StateFlow<Boolean> = dialogsStateHolder.showDailyCheckInDialog
    val dailyCheckInLocationInput: StateFlow<String> = dialogsStateHolder.dailyCheckInLocationInput
    val dailyCheckInIsArrivalDeparture: StateFlow<Boolean> = dialogsStateHolder.dailyCheckInIsArrivalDeparture
    val dailyCheckInBreakfastIncluded: StateFlow<Boolean> = dialogsStateHolder.dailyCheckInBreakfastIncluded
    val showDayLocationDialog: StateFlow<Boolean> = dialogsStateHolder.showDayLocationDialog
    val dayLocationInput: StateFlow<String> = dialogsStateHolder.dayLocationInput
    val showDeleteDayDialog: StateFlow<Boolean> = dialogsStateHolder.showDeleteDayDialog

    val dailyCheckInAllowancePreviewCents: StateFlow<Int> = dialogsStateHolder.dailyCheckInAllowancePreviewCents
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MealAllowanceCalculator.BASE_NORMAL_CENTS
        )

    val loadingActions: StateFlow<Set<TodayAction>> = actionsHandler.loadingActions
    val snackbarMessage: StateFlow<UiText?> = actionsHandler.snackbarMessage
    val deletedEntryForUndo = actionsHandler.deletedEntryForUndo

    private var selectDateJob: Job? = null
    private var weekOverviewJob: Job? = null
    private var entryLoadRequestId = 0L
    private var weekOverviewRequestId = 0L

    private data class ScreenCorePart(
        val uiState: TodayUiState,
        val selectedEntry: WorkEntry?,
        val selectedDate: LocalDate,
        val todayDate: LocalDate,
        val loadingActions: Set<TodayAction>
    )

    private data class ScreenWeekMonthPart(
        val weekDaysUi: List<WeekDayUi>,
        val selectedEntryWithTravel: WorkEntryWithTravelLegs?
    )

    private data class DialogCheckInPart(
        val showDailyCheckInDialog: Boolean,
        val locationInput: String,
        val isArrivalDeparture: Boolean,
        val breakfastIncluded: Boolean,
        val allowancePreviewCents: Int
    )

    private data class DialogLocationDeletePart(
        val showDayLocationDialog: Boolean,
        val dayLocationInput: String,
        val showDeleteDayDialog: Boolean,
        val loadingActions: Set<TodayAction>
    )

    val screenState: StateFlow<TodayScreenState> = combine(
        combine(uiState, selectedEntry, selectedDate, todayDate, loadingActions) { ui, entry, date, today, loading ->
            ScreenCorePart(ui, entry, date, today, loading)
        },
        combine(weekDaysUi, selectedEntryWithTravel) { days, entryWithTravel ->
            ScreenWeekMonthPart(days, entryWithTravel)
        }
    ) { core, weekMonth ->
        TodayScreenState(
            uiState = core.uiState,
            selectedEntry = core.selectedEntry,
            selectedEntryWithTravel = weekMonth.selectedEntryWithTravel,
            selectedDate = core.selectedDate,
            todayDate = core.todayDate,
            weekDaysUi = weekMonth.weekDaysUi,
            loadingActions = core.loadingActions
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialScreenState())

    val dialogState: StateFlow<TodayDialogState> = combine(
        combine(
            showDailyCheckInDialog,
            dailyCheckInLocationInput,
            dailyCheckInIsArrivalDeparture,
            dailyCheckInBreakfastIncluded,
            dailyCheckInAllowancePreviewCents
        ) { show, loc, arrival, breakfast, allowance ->
            DialogCheckInPart(show, loc, arrival, breakfast, allowance)
        },
        combine(showDayLocationDialog, dayLocationInput, showDeleteDayDialog, loadingActions) { showLoc, locInput, showDel, loading ->
            DialogLocationDeletePart(showLoc, locInput, showDel, loading)
        }
    ) { checkIn, locDel ->
        TodayDialogState(
            showDailyCheckInDialog = checkIn.showDailyCheckInDialog,
            dailyCheckInLocationInput = checkIn.locationInput,
            dailyCheckInIsArrivalDeparture = checkIn.isArrivalDeparture,
            dailyCheckInBreakfastIncluded = checkIn.breakfastIncluded,
            dailyCheckInAllowancePreviewCents = checkIn.allowancePreviewCents,
            showDayLocationDialog = locDel.showDayLocationDialog,
            dayLocationInput = locDel.dayLocationInput,
            showDeleteDayDialog = locDel.showDeleteDayDialog,
            loadingActions = locDel.loadingActions
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialDialogState())

    init {
        observeTodayDate()
        loadTodayEntry()
        observeEntryUpdates()
        loadWeekOverview()
    }

    private fun loadTodayEntry() {
        loadEntryForDate(selectedDate.value, preferCachedSelection = true)
    }

    private fun observeTodayDate() {
        viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                if (dateCoordinator.syncWithSystemDate()) {
                    requestWeekOverviewRefresh(selectedDate.value)
                }
                delay(dateCoordinator.millisUntilNextDay())
            }
        }
    }

    private fun observeEntryUpdates() {
        viewModelScope.launch {
            combine(selectedDate, selectedEntry) { date, entry ->
                EntryRefreshKey(selectedDate = date, updatedAt = entry?.updatedAt)
            }
                .debounce(ENTRY_UPDATE_DEBOUNCE_MS)
                .distinctUntilChanged()
                .collectLatest { refreshKey ->
                    requestWeekOverviewRefresh(refreshKey.selectedDate)
                }
        }
    }

    private fun loadWeekOverview() {
        requestWeekOverviewRefresh(selectedDate.value)
    }

    private fun requestWeekOverviewRefresh(selectedDateSnapshot: LocalDate) {
        weekOverviewJob?.cancel()
        val requestId = ++weekOverviewRequestId
        weekOverviewJob = viewModelScope.launch {
            loadWeekOverviewInternal(
                selectedDateSnapshot = selectedDateSnapshot,
                requestId = requestId
            )
        }
    }

    private suspend fun loadWeekOverviewInternal(
        selectedDateSnapshot: LocalDate,
        requestId: Long
    ) {
        try {
            val weekDates = de.montagezeit.app.domain.util.WeekCalculator.weekDays(
                de.montagezeit.app.domain.util.WeekCalculator.weekStart(selectedDateSnapshot)
            )
            val entries = withContext(Dispatchers.IO) {
                getWorkEntriesByDateRange(weekDates.first(), weekDates.last())
            }
            if (!isLatestWeekOverviewRequest(selectedDateSnapshot, requestId)) return
            _weekDaysUi.value = weekOverviewUseCase(
                selectedDate = selectedDateSnapshot,
                todayDate = todayDate.value,
                entries = entries
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.w("TodayViewModel", "loadWeekOverview failed: ${e.message}")
        }
    }

    fun selectDate(date: LocalDate) {
        val wasAlreadySelected = selectedDate.value == date
        val isDateInCurrentWeek = _weekDaysUi.value.any { it.date == date }

        dateCoordinator.selectDate(date)
        _uiState.value = TodayUiState.Loading
        _weekDaysUi.update { days -> days.map { it.copy(isSelected = it.date == date) } }

        if (!isDateInCurrentWeek) {
            loadWeekOverview()
        }

        loadEntryForDate(date, preferCachedSelection = wasAlreadySelected)
    }

    fun openDailyCheckInDialog() {
        viewModelScope.launch {
            try {
                val existingEntry = currentSelectedEntryOrNull() ?: getWorkEntryByDate(selectedDate.value)
                val prefill = resolveDayLocationPrefill(existingEntry)
                dialogsStateHolder.openDailyCheckInDialog(
                    selectedDate = selectedDate.value,
                    locationPrefill = prefill,
                    isArrivalDeparture = existingEntry?.mealIsArrivalDeparture ?: false,
                    breakfastIncluded = existingEntry?.mealBreakfastIncluded ?: false
                )
            } catch (e: Exception) {
                actionsHandler.publishSnackbar(e.toUiText(R.string.today_error_day_location_save_failed))
            }
        }
    }

    fun onDailyCheckInLocationChanged(label: String) = dialogsStateHolder.updateDailyCheckInLocation(label)
    fun onDailyCheckInArrivalDepartureChanged(value: Boolean) = dialogsStateHolder.updateDailyCheckInArrivalDeparture(value)
    fun onDailyCheckInBreakfastIncludedChanged(value: Boolean) = dialogsStateHolder.updateDailyCheckInBreakfastIncluded(value)
    fun onDismissDailyCheckInDialog() = dialogsStateHolder.dismissDailyCheckInDialog()

    fun openDayLocationDialog() {
        viewModelScope.launch {
            try {
                val existingEntry = currentSelectedEntryOrNull() ?: getWorkEntryByDate(selectedDate.value)
                val prefill = resolveDayLocationPrefill(existingEntry)
                dialogsStateHolder.openDayLocationDialog(prefill)
            } catch (e: Exception) {
                actionsHandler.publishSnackbar(e.toUiText(R.string.today_error_day_location_save_failed))
            }
        }
    }

    fun onDayLocationInputChanged(label: String) = dialogsStateHolder.updateDayLocationInput(label)
    fun onDismissDayLocationDialog() = dialogsStateHolder.dismissDayLocationDialog()

    fun submitDailyManualCheckIn(label: String = dailyCheckInLocationInput.value) {
        if (label.trim().isBlank()) {
            actionsHandler.publishSnackbar(UiText.StringResource(R.string.error_day_location_required))
            return
        }
        viewModelScope.launch {
            val input = DailyManualCheckInInput(
                date = dialogsStateHolder.dailyCheckInDate.value,
                dayLocationLabel = label,
                isArrivalDeparture = dailyCheckInIsArrivalDeparture.value,
                breakfastIncluded = dailyCheckInBreakfastIncluded.value
            )
            val entry = actionsHandler.submitDailyManualCheckIn(input)
            if (entry != null) {
                _uiState.value = TodayUiState.Success(entry)
                dialogsStateHolder.updateDailyCheckInLocation(entry.dayLocationLabel)
                dialogsStateHolder.dismissDailyCheckInDialog()
            }
        }
    }

    fun onResetError() = loadSelectedDateEntry()

    private fun loadSelectedDateEntry() {
        loadEntryForDate(selectedDate.value, preferCachedSelection = true)
    }

    private fun loadEntryForDate(
        targetDate: LocalDate,
        preferCachedSelection: Boolean
    ) {
        selectDateJob?.cancel()
        val requestId = ++entryLoadRequestId
        val cachedEntry = if (preferCachedSelection) {
            selectedEntry.value?.takeIf { it.date == targetDate }
        } else {
            null
        }

        _uiState.value = TodayUiState.Loading
        selectDateJob = viewModelScope.launch {
            try {
                val entry = cachedEntry ?: withContext(Dispatchers.IO) { getWorkEntryByDate(targetDate) }
                if (isLatestEntryLoad(targetDate, requestId)) {
                    _uiState.value = TodayUiState.Success(entry)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (isLatestEntryLoad(targetDate, requestId)) {
                    _uiState.value = TodayUiState.Error(e.toUiText(R.string.today_error_unknown))
                }
            }
        }
    }

    fun ensureTodayEntryThen(onDone: () -> Unit) = onDone()

    fun onConfirmOffDay() {
        viewModelScope.launch {
            val entry = actionsHandler.confirmOffDay(selectedDate.value)
            if (entry != null) {
                _uiState.value = TodayUiState.Success(entry)
                dialogsStateHolder.dismissDailyCheckInDialog()
            }
        }
    }

    fun submitDayLocationUpdate(label: String = dayLocationInput.value) {
        val trimmed = label.trim()
        if (trimmed.isBlank()) {
            actionsHandler.publishSnackbar(UiText.StringResource(R.string.error_day_location_required))
            return
        }
        viewModelScope.launch {
            val entry = actionsHandler.submitDayLocationUpdate(selectedDate.value, trimmed)
            if (entry != null) {
                _uiState.value = TodayUiState.Success(entry)
                dialogsStateHolder.updateDayLocationInput(entry.dayLocationLabel)
                dialogsStateHolder.dismissDayLocationDialog()
            }
        }
    }

    fun openDeleteDayDialog() = dialogsStateHolder.openDeleteDayDialog()
    fun dismissDeleteDayDialog() = dialogsStateHolder.dismissDeleteDayDialog()

    fun confirmDeleteDay() {
        viewModelScope.launch {
            if (actionsHandler.confirmDeleteDay(selectedDate.value)) {
                dialogsStateHolder.dismissDeleteDayDialog()
                _uiState.value = TodayUiState.Success(null)
            }
        }
    }

    fun undoDeleteDay() {
        viewModelScope.launch {
            val restored = actionsHandler.undoDeleteDay { restoredDate -> selectDate(restoredDate) }
            if (restored != null) _uiState.value = TodayUiState.Success(restored)
        }
    }

    fun onUndoWindowClosed() = actionsHandler.onUndoWindowClosed()
    fun onSnackbarShown() = actionsHandler.onSnackbarShown()

    private fun currentSelectedEntryOrNull(): WorkEntry? =
        selectedEntry.value?.takeIf { it.date == selectedDate.value }

    private fun isLatestEntryLoad(targetDate: LocalDate, requestId: Long): Boolean {
        return requestId == entryLoadRequestId && selectedDate.value == targetDate
    }

    private fun isLatestWeekOverviewRequest(selectedDateSnapshot: LocalDate, requestId: Long): Boolean {
        return requestId == weekOverviewRequestId && selectedDate.value == selectedDateSnapshot
    }

    private companion object {
        private const val ENTRY_UPDATE_DEBOUNCE_MS = 250L

        private data class EntryRefreshKey(
            val selectedDate: LocalDate,
            val updatedAt: Long?
        )

        fun initialScreenState() = TodayScreenState(
            uiState = TodayUiState.Loading,
            selectedEntry = null,
            selectedDate = LocalDate.now(),
            todayDate = LocalDate.now(),
            weekDaysUi = emptyList(),
            loadingActions = emptySet()
        )

        fun initialDialogState() = TodayDialogState(
            showDailyCheckInDialog = false,
            dailyCheckInLocationInput = "",
            dailyCheckInIsArrivalDeparture = false,
            dailyCheckInBreakfastIncluded = false,
            dailyCheckInAllowancePreviewCents = MealAllowanceCalculator.BASE_NORMAL_CENTS,
            showDayLocationDialog = false,
            dayLocationInput = "",
            showDeleteDayDialog = false,
            loadingActions = emptySet()
        )
    }
}

sealed class TodayUiState {
    object Loading : TodayUiState()
    data class Success(val entry: WorkEntry?) : TodayUiState()
    data class Error(val message: UiText) : TodayUiState()
}
