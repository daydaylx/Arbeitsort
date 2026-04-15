package de.montagezeit.app.ui.screen.today

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.montagezeit.app.R
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.local.entity.WorkEntryWithTravelLegs
import de.montagezeit.app.data.repository.WorkEntryRepository
import de.montagezeit.app.diagnostics.AppDiagnosticsRuntime
import de.montagezeit.app.diagnostics.DiagnosticCategory
import de.montagezeit.app.diagnostics.DiagnosticDateRange
import de.montagezeit.app.diagnostics.DiagnosticStatus
import de.montagezeit.app.diagnostics.DiagnosticTraceRequest
import de.montagezeit.app.domain.usecase.DailyManualCheckInInput
import de.montagezeit.app.domain.util.MealAllowanceCalculator
import de.montagezeit.app.ui.util.UiText
import de.montagezeit.app.ui.util.toUiText
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
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
    private val workEntryRepository: WorkEntryRepository,
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
        .flatMapLatest { date -> workEntryRepository.getByDateFlow(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedEntry: StateFlow<WorkEntry?> = selectedDate
        .flatMapLatest { date -> workEntryRepository.getByDateFlow(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedEntryWithTravel: StateFlow<WorkEntryWithTravelLegs?> = selectedDate
        .flatMapLatest { date -> workEntryRepository.getByDateWithTravelFlow(date) }
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

    private val entryLoadRequests = MutableSharedFlow<EntryLoadRequest>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private val weekOverviewRefreshRequests = MutableSharedFlow<LocalDate>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private var lastExplicitWeekRefreshMs = 0L

    private val selectedEntryState = combine(
        uiState,
        selectedEntry,
        selectedEntryWithTravel
    ) { ui, entry, entryWithTravel ->
        Triple(ui, entry, entryWithTravel)
    }

    val screenState: StateFlow<TodayScreenState> = combine(
        selectedEntryState,
        selectedDate,
        todayDate,
        weekDaysUi,
        loadingActions
    ) { entryState, date, today, days, loading ->
        val (ui, entry, entryWithTravel) = entryState
        TodayScreenState(
            uiState = ui,
            selectedEntry = entry,
            selectedEntryWithTravel = entryWithTravel,
            selectedDate = date,
            todayDate = today,
            weekDaysUi = days,
            loadingActions = loading
        )
    }.onEach {
        AppDiagnosticsRuntime.startTrace(
            DiagnosticTraceRequest(
                category = DiagnosticCategory.STATE_MUTATION,
                name = "screenState_emit",
                sourceClass = "TodayViewModel",
                screenOrWorker = "TodayScreen",
                payload = mapOf("uiState" to (it.uiState::class.simpleName ?: "unknown"))
            )
        ).finish()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialScreenState())

    private val dailyCheckInDialogState = combine(
        showDailyCheckInDialog,
        dailyCheckInLocationInput,
        dailyCheckInIsArrivalDeparture,
        dailyCheckInBreakfastIncluded,
        dailyCheckInAllowancePreviewCents
    ) { showCheckIn, locationInput, isArrivalDeparture, breakfastIncluded, allowancePreviewCents ->
        (showCheckIn to locationInput) to Triple(
            isArrivalDeparture,
            breakfastIncluded,
            allowancePreviewCents
        )
    }

    private val secondaryDialogState = combine(
        showDayLocationDialog,
        dayLocationInput,
        showDeleteDayDialog
    ) { showLocationDialog, dayLocationValue, showDeleteDialog ->
        Triple(showLocationDialog, dayLocationValue, showDeleteDialog)
    }

    val dialogState: StateFlow<TodayDialogState> = combine(
        dailyCheckInDialogState,
        secondaryDialogState,
        loadingActions
    ) { checkInState, secondaryState, loading ->
        val (visibilityAndLocation, mealState) = checkInState
        val (showCheckIn, locationInput) = visibilityAndLocation
        val (isArrivalDeparture, breakfastIncluded, allowancePreviewCents) = mealState
        val (showLocationDialog, dayLocationValue, showDeleteDialog) = secondaryState
        TodayDialogState(
            showDailyCheckInDialog = showCheckIn,
            dailyCheckInLocationInput = locationInput,
            dailyCheckInIsArrivalDeparture = isArrivalDeparture,
            dailyCheckInBreakfastIncluded = breakfastIncluded,
            dailyCheckInAllowancePreviewCents = allowancePreviewCents,
            showDayLocationDialog = showLocationDialog,
            dayLocationInput = dayLocationValue,
            showDeleteDayDialog = showDeleteDialog,
            loadingActions = loading
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialDialogState())

    init {
        observeTodayDate()
        observeEntryLoadRequests()
        loadTodayEntry()
        observeEntryUpdates()
        observeWeekOverviewRefreshes()
        loadWeekOverview()
    }

    private fun loadTodayEntry() {
        loadEntryForDate(selectedDate.value, preferCachedSelection = true)
    }

    private fun observeTodayDate() {
        viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                syncTodayDate()
                delay(dateCoordinator.millisUntilNextDay())
            }
        }
    }

    internal fun syncTodayDate(systemToday: LocalDate = LocalDate.now()): Boolean {
        if (!dateCoordinator.syncWithSystemDate(systemToday)) return false

        val selectedDateSnapshot = selectedDate.value
        _weekDaysUi.update { days ->
            days.map { day ->
                day.copy(
                    isToday = day.date == todayDate.value,
                    isSelected = day.date == selectedDateSnapshot
                )
            }
        }
        loadEntryForDate(selectedDateSnapshot, preferCachedSelection = false)
        lastExplicitWeekRefreshMs = System.currentTimeMillis()
        requestWeekOverviewRefresh(selectedDateSnapshot)
        return true
    }

    private fun observeEntryUpdates() {
        viewModelScope.launch {
            combine(selectedDate, selectedEntry) { date, entry ->
                EntryRefreshKey(
                    selectedDate = date,
                    updatedAt = entry?.takeIf { it.date == date }?.updatedAt
                )
            }
                .debounce(ENTRY_UPDATE_DEBOUNCE_MS)
                .distinctUntilChanged()
                .collectLatest { refreshKey ->
                    val remainingGuardMs = remainingWeekRefreshGuardMs()
                    if (remainingGuardMs > 0) {
                        delay(remainingGuardMs)
                    }
                    requestWeekOverviewRefresh(refreshKey.selectedDate)
                }
        }
    }

    private fun observeEntryLoadRequests() {
        viewModelScope.launch {
            entryLoadRequests.collectLatest { request ->
                val trace = AppDiagnosticsRuntime.startTrace(
                    DiagnosticTraceRequest(
                        category = DiagnosticCategory.STATE_MUTATION,
                        name = "today_entry_load",
                        sourceClass = "TodayViewModel",
                        screenOrWorker = "TodayScreen",
                        entityDate = request.targetDate,
                        payload = mapOf("preferCachedSelection" to request.preferCachedSelection)
                    )
                )
                try {
                    val cachedEntry = if (request.preferCachedSelection) {
                        selectedEntry.value?.takeIf { it.date == request.targetDate }
                    } else {
                        null
                    }

                    if (cachedEntry != null) {
                        trace.event("entry_cache_hit")
                        if (selectedDate.value == request.targetDate) {
                            _uiState.value = TodayUiState.Success(cachedEntry)
                        }
                        trace.finish(payload = mapOf("source" to "cache"))
                        return@collectLatest
                    }

                    trace.event("entry_cache_miss")
                    val dbStart = System.currentTimeMillis()
                    val entry = withContext(Dispatchers.IO) {
                        workEntryRepository.getByDate(request.targetDate)
                    }
                    trace.event("db_query_done", payload = mapOf("durationMs" to (System.currentTimeMillis() - dbStart)))
                    if (selectedDate.value == request.targetDate) {
                        _uiState.value = TodayUiState.Success(entry)
                    }
                    trace.finish(payload = mapOf("source" to "db", "entryPresent" to (entry != null)))
                } catch (e: CancellationException) {
                    trace.finish(status = DiagnosticStatus.CANCELLED)
                    throw e
                } catch (e: Exception) {
                    if (selectedDate.value == request.targetDate) {
                        _uiState.value = TodayUiState.Error(e.toUiText(R.string.today_error_unknown))
                    }
                    trace.error("entry_load_failed", e)
                    trace.finish(status = DiagnosticStatus.ERROR)
                }
            }
        }
    }

    private fun loadWeekOverview() {
        lastExplicitWeekRefreshMs = System.currentTimeMillis()
        requestWeekOverviewRefresh(selectedDate.value)
    }

    private fun requestWeekOverviewRefresh(selectedDateSnapshot: LocalDate) {
        weekOverviewRefreshRequests.tryEmit(selectedDateSnapshot)
    }

    private fun observeWeekOverviewRefreshes() {
        viewModelScope.launch {
            weekOverviewRefreshRequests.collectLatest { selectedDateSnapshot ->
                val trace = AppDiagnosticsRuntime.startTrace(
                    DiagnosticTraceRequest(
                        category = DiagnosticCategory.STATE_MUTATION,
                        name = "today_week_refresh",
                        sourceClass = "TodayViewModel",
                        screenOrWorker = "TodayScreen",
                        entityDate = selectedDateSnapshot,
                        payload = emptyMap()
                    )
                )
                try {
                    val weekDates = de.montagezeit.app.domain.util.WeekCalculator.weekDays(
                        de.montagezeit.app.domain.util.WeekCalculator.weekStart(selectedDateSnapshot)
                    )
                    val dbStart = System.currentTimeMillis()
                    val entries = withContext(Dispatchers.IO) {
                        workEntryRepository.getByDateRange(weekDates.first(), weekDates.last())
                    }
                    trace.event("db_query_done", payload = mapOf(
                        "durationMs" to (System.currentTimeMillis() - dbStart),
                        "entryCount" to entries.size
                    ))
                    if (selectedDate.value == selectedDateSnapshot) {
                        _weekDaysUi.value = weekOverviewUseCase(
                            selectedDate = selectedDateSnapshot,
                            todayDate = todayDate.value,
                            entries = entries
                        )
                    }
                    trace.finish(payload = mapOf("stale" to (selectedDate.value != selectedDateSnapshot)))
                } catch (e: CancellationException) {
                    trace.finish(status = DiagnosticStatus.CANCELLED)
                    throw e
                } catch (e: Exception) {
                    android.util.Log.w("TodayViewModel", "loadWeekOverview failed: ${e.message}")
                    trace.error("week_refresh_failed", e)
                    trace.finish(status = DiagnosticStatus.ERROR)
                }
            }
        }
    }

    fun selectDate(date: LocalDate) {
        val wasAlreadySelected = selectedDate.value == date
        val isDateInCurrentWeek = _weekDaysUi.value.any { it.date == date }

        dateCoordinator.selectDate(date)

        _weekDaysUi.update { days ->
            if (days.all { it.isSelected == (it.date == date) }) days
            else days.map { it.copy(isSelected = it.date == date) }
        }

        if (!isDateInCurrentWeek) {
            loadWeekOverview()
        }

        loadEntryForDate(date, preferCachedSelection = wasAlreadySelected)
    }

    fun openDailyCheckInDialog() {
        viewModelScope.launch {
            try {
                val existingEntry = currentSelectedEntryOrNull() ?: workEntryRepository.getByDate(selectedDate.value)
                dialogsStateHolder.openDailyCheckInDialog(
                    selectedDate = selectedDate.value,
                    locationPrefill = existingEntry?.dayLocationLabel?.trim().orEmpty(),
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
                val existingEntry = currentSelectedEntryOrNull() ?: workEntryRepository.getByDate(selectedDate.value)
                dialogsStateHolder.openDayLocationDialog(existingEntry?.dayLocationLabel?.trim().orEmpty())
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
        val hasCachedSelection = preferCachedSelection &&
            selectedEntry.value?.date == targetDate
        if (!hasCachedSelection) {
            _uiState.value = TodayUiState.Loading
        }
        entryLoadRequests.tryEmit(EntryLoadRequest(targetDate, preferCachedSelection))
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

    private fun remainingWeekRefreshGuardMs(now: Long = System.currentTimeMillis()): Long =
        (lastExplicitWeekRefreshMs + WEEK_REFRESH_GUARD_MS - now).coerceAtLeast(0L)

    private companion object {
        private const val ENTRY_UPDATE_DEBOUNCE_MS = 250L
        private const val WEEK_REFRESH_GUARD_MS = 500L

        private data class EntryLoadRequest(
            val targetDate: LocalDate,
            val preferCachedSelection: Boolean
        )

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
