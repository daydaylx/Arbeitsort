package de.montagezeit.app.ui.screen.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.montagezeit.app.R
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.local.entity.WorkEntryWithTravelLegs
import de.montagezeit.app.data.preferences.ReminderSettingsManager
import de.montagezeit.app.data.repository.WorkEntryRepository
import de.montagezeit.app.diagnostics.AppDiagnosticsRuntime
import de.montagezeit.app.diagnostics.DiagnosticCategory
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

enum class TodayAction {
    DAILY_MANUAL_CHECK_IN,
    CONFIRM_OFFDAY,
    SET_DAY_TYPE,
    UPDATE_DAY_LOCATION,
    DELETE_DAY
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TodayViewModel @Inject constructor(
    private val workEntryRepository: WorkEntryRepository,
    private val dateCoordinator: TodayDateCoordinator,
    private val actionsHandler: TodayActionsHandler,
    private val reminderSettingsManager: ReminderSettingsManager,
    private val clock: Clock
) : ViewModel() {

    private val currentSystemDate: LocalDate
        get() = LocalDate.now(clock)

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

    private val _dialogState = MutableStateFlow(initialDialogState())
    private var dailyCheckInDate: LocalDate = LocalDate.now()

    val loadingActions: StateFlow<Set<TodayAction>> = actionsHandler.loadingActions
    val snackbarMessage: StateFlow<UiText?> = actionsHandler.snackbarMessage
    val deletedEntryForUndo = actionsHandler.deletedEntryForUndo

    private val dailyTargetHours: StateFlow<Double> = reminderSettingsManager.settings
        .map { it.dailyTargetHours }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DEFAULT_DAILY_TARGET_HOURS)

    private val entryLoadRequests = MutableSharedFlow<EntryLoadRequest>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private val selectedEntryState = combine(
        uiState,
        selectedEntry,
        selectedEntryWithTravel
    ) { ui, entry, entryWithTravel ->
        Triple(ui, entry, entryWithTravel)
    }

    val screenState: StateFlow<TodayScreenState> = combine(
        combine(selectedEntryState, selectedDate, todayDate) { e, d, t -> Triple(e, d, t) },
        combine(loadingActions, dailyTargetHours) { l, h -> Pair(l, h) }
    ) { (entryState, date, today), (loading, targetHours) ->
        val (ui, entry, entryWithTravel) = entryState
        TodayScreenState(
            uiState = ui,
            selectedEntry = entry,
            selectedEntryWithTravel = entryWithTravel,
            selectedDate = date,
            todayDate = today,
            loadingActions = loading,
            dailyTargetHours = targetHours
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialScreenState())

    val dialogState: StateFlow<TodayDialogState> = combine(
        _dialogState,
        loadingActions
    ) { ds, loading ->
        ds.copy(loadingActions = loading)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialDialogState())

    init {
        observeTodayDate()
        observeEntryLoadRequests()
        loadTodayEntry()
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

    internal fun syncTodayDate(systemToday: LocalDate = currentSystemDate): Boolean {
        if (!dateCoordinator.syncWithSystemDate(systemToday)) return false

        val selectedDateSnapshot = selectedDate.value
        loadEntryForDate(selectedDateSnapshot, preferCachedSelection = false)
        return true
    }

    fun syncWithSystemDate(): Boolean = syncTodayDate()

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

    fun selectDate(date: LocalDate) {
        val wasAlreadySelected = selectedDate.value == date

        dateCoordinator.selectDate(date)

        loadEntryForDate(date, preferCachedSelection = wasAlreadySelected)
    }

    fun openDailyCheckInDialog() {
        viewModelScope.launch {
            try {
                val existingEntry = currentSelectedEntryOrNull() ?: workEntryRepository.getByDate(selectedDate.value)
                dailyCheckInDate = selectedDate.value
                val location = existingEntry?.dayLocationLabel?.trim().orEmpty()
                val isArrival = existingEntry?.mealIsArrivalDeparture ?: false
                val breakfast = existingEntry?.mealBreakfastIncluded ?: false
                _dialogState.update { state ->
                    state.copy(
                        showDailyCheckInDialog = true,
                        dailyCheckInLocationInput = location,
                        dailyCheckInIsArrivalDeparture = isArrival,
                        dailyCheckInBreakfastIncluded = breakfast,
                        dailyCheckInAllowancePreviewCents = computeAllowancePreview(location, isArrival, breakfast)
                    )
                }
            } catch (e: Exception) {
                actionsHandler.publishSnackbar(e.toUiText(R.string.today_error_day_location_save_failed))
            }
        }
    }

    fun onDailyCheckInLocationChanged(label: String) {
        _dialogState.update { state ->
            state.copy(
                dailyCheckInLocationInput = label,
                dailyCheckInAllowancePreviewCents = computeAllowancePreview(
                    label, state.dailyCheckInIsArrivalDeparture, state.dailyCheckInBreakfastIncluded
                )
            )
        }
    }

    fun onDailyCheckInArrivalDepartureChanged(value: Boolean) {
        _dialogState.update { state ->
            state.copy(
                dailyCheckInIsArrivalDeparture = value,
                dailyCheckInAllowancePreviewCents = computeAllowancePreview(
                    state.dailyCheckInLocationInput, value, state.dailyCheckInBreakfastIncluded
                )
            )
        }
    }

    fun onDailyCheckInBreakfastIncludedChanged(value: Boolean) {
        _dialogState.update { state ->
            state.copy(
                dailyCheckInBreakfastIncluded = value,
                dailyCheckInAllowancePreviewCents = computeAllowancePreview(
                    state.dailyCheckInLocationInput, state.dailyCheckInIsArrivalDeparture, value
                )
            )
        }
    }

    fun onDismissDailyCheckInDialog() = _dialogState.update { it.copy(showDailyCheckInDialog = false) }

    fun openDayLocationDialog() {
        viewModelScope.launch {
            try {
                val existingEntry = currentSelectedEntryOrNull() ?: workEntryRepository.getByDate(selectedDate.value)
                _dialogState.update {
                    it.copy(
                        showDayLocationDialog = true,
                        dayLocationInput = existingEntry?.dayLocationLabel?.trim().orEmpty()
                    )
                }
            } catch (e: Exception) {
                actionsHandler.publishSnackbar(e.toUiText(R.string.today_error_day_location_save_failed))
            }
        }
    }

    fun onDayLocationInputChanged(label: String) = _dialogState.update { it.copy(dayLocationInput = label) }
    fun onDismissDayLocationDialog() = _dialogState.update { it.copy(showDayLocationDialog = false) }

    fun submitDailyManualCheckIn(label: String = _dialogState.value.dailyCheckInLocationInput) {
        if (label.trim().isBlank()) {
            actionsHandler.publishSnackbar(UiText.StringResource(R.string.error_day_location_required))
            return
        }
        viewModelScope.launch {
            val state = _dialogState.value
            val input = DailyManualCheckInInput(
                date = dailyCheckInDate,
                dayLocationLabel = label,
                isArrivalDeparture = state.dailyCheckInIsArrivalDeparture,
                breakfastIncluded = state.dailyCheckInBreakfastIncluded
            )
            val entry = actionsHandler.submitDailyManualCheckIn(input)
            if (entry != null) {
                _uiState.value = TodayUiState.Success(entry)
                _dialogState.update {
                    it.copy(dailyCheckInLocationInput = entry.dayLocationLabel, showDailyCheckInDialog = false)
                }
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
                _dialogState.update { it.copy(showDailyCheckInDialog = false) }
            }
        }
    }

    fun setDayType(dayType: DayType) {
        viewModelScope.launch {
            val entry = actionsHandler.setDayType(selectedDate.value, dayType)
            if (entry != null) {
                _uiState.value = TodayUiState.Success(entry)
            }
        }
    }

    fun submitDayLocationUpdate(label: String = _dialogState.value.dayLocationInput) {
        val trimmed = label.trim()
        if (trimmed.isBlank()) {
            actionsHandler.publishSnackbar(UiText.StringResource(R.string.error_day_location_required))
            return
        }
        viewModelScope.launch {
            val entry = actionsHandler.submitDayLocationUpdate(selectedDate.value, trimmed)
            if (entry != null) {
                _uiState.value = TodayUiState.Success(entry)
                _dialogState.update {
                    it.copy(dayLocationInput = entry.dayLocationLabel, showDayLocationDialog = false)
                }
            }
        }
    }

    fun openDeleteDayDialog() = _dialogState.update { it.copy(showDeleteDayDialog = true) }
    fun dismissDeleteDayDialog() = _dialogState.update { it.copy(showDeleteDayDialog = false) }

    fun confirmDeleteDay() {
        viewModelScope.launch {
            if (actionsHandler.confirmDeleteDay(selectedDate.value)) {
                _dialogState.update { it.copy(showDeleteDayDialog = false) }
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

    private fun computeAllowancePreview(location: String, isArrival: Boolean, breakfast: Boolean): Int =
        MealAllowanceCalculator.resolveForActivity(
            dayType = DayType.WORK,
            isArrivalDeparture = isArrival,
            breakfastIncluded = breakfast,
            workMinutes = 1,
            travelMinutes = 0,
            locationLabel = location
        ).amountCents

    private fun currentSelectedEntryOrNull(): WorkEntry? =
        selectedEntry.value?.takeIf { it.date == selectedDate.value }

    private companion object {
        private const val DEFAULT_DAILY_TARGET_HOURS = 8.0

        private data class EntryLoadRequest(
            val targetDate: LocalDate,
            val preferCachedSelection: Boolean
        )

        fun initialScreenState() = TodayScreenState(
            uiState = TodayUiState.Loading,
            selectedEntry = null,
            selectedDate = LocalDate.now(),
            todayDate = LocalDate.now(),
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
