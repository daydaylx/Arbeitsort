package de.montagezeit.app.ui.screen.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.montagezeit.app.R
import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.preferences.ReminderSettingsManager
import de.montagezeit.app.domain.usecase.CalculateOvertimeForRange
import de.montagezeit.app.domain.usecase.ConfirmOffDay
import de.montagezeit.app.domain.usecase.DeleteDayEntry
import de.montagezeit.app.domain.usecase.DailyManualCheckInInput
import de.montagezeit.app.domain.usecase.RecordDailyManualCheckIn
import de.montagezeit.app.domain.usecase.ResolveDayLocationPrefill
import de.montagezeit.app.domain.usecase.SetDayLocation
import de.montagezeit.app.domain.usecase.WorkEntryFactory
import de.montagezeit.app.domain.util.MealAllowanceCalculator
import de.montagezeit.app.domain.util.NonWorkingDayChecker
import de.montagezeit.app.domain.util.TimeCalculator
import de.montagezeit.app.ui.util.UiText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale
import javax.inject.Inject

enum class WeekDayStatus {
    CONFIRMED_WORK, CONFIRMED_OFF, PARTIAL, EMPTY
}

data class WeekDayUi(
    val date: LocalDate,
    val isToday: Boolean,
    val isSelected: Boolean,
    val dayLabel: String,
    val dayNumber: String,
    val status: WeekDayStatus,
    val workHours: Double?
)

data class WeekStats(
    val totalHours: Double,
    val totalPaidHours: Double,
    val workDaysCount: Int,
    val targetHours: Double
) {
    val progress: Float
        get() = (totalHours / targetHours).coerceIn(0.0, 1.0).toFloat()

    val isOverTarget: Boolean
        get() = totalHours > targetHours

    val isUnderTarget: Boolean
        get() = totalHours < targetHours * 0.9 // 10% Toleranz
}

data class MonthStats(
    val totalHours: Double,
    val totalPaidHours: Double,
    val workDaysCount: Int,
    val targetHours: Double,
    val mealAllowanceTotalCents: Int = 0
) {
    val progress: Float
        get() = (totalHours / targetHours).coerceIn(0.0, 1.0).toFloat()

    val isOverTarget: Boolean
        get() = totalHours > targetHours

    val isUnderTarget: Boolean
        get() = totalHours < targetHours * 0.9 // 10% Toleranz
}

enum class TodayAction {
    DAILY_MANUAL_CHECK_IN,
    CONFIRM_OFFDAY,
    UPDATE_DAY_LOCATION,
    DELETE_DAY
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class TodayViewModel @Inject constructor(
    private val workEntryDao: WorkEntryDao,
    private val recordDailyManualCheckIn: RecordDailyManualCheckIn,
    private val resolveDayLocationPrefill: ResolveDayLocationPrefill,
    private val confirmOffDay: ConfirmOffDay,
    private val setDayLocation: SetDayLocation,
    private val reminderSettingsManager: ReminderSettingsManager,
    private val deleteDayEntry: DeleteDayEntry,
    private val nonWorkingDayChecker: NonWorkingDayChecker
) : ViewModel() {
    private val calculateOvertimeForRange = CalculateOvertimeForRange()

    private val _uiState = MutableStateFlow<TodayUiState>(TodayUiState.Loading)
    val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    // A03: Reactive today date that refreshes on access
    private val _todayDate = MutableStateFlow(LocalDate.now())

    /** Always tracks today's entry reactively (for the action bar). */
    val todayEntry: StateFlow<WorkEntry?> = _todayDate
        .flatMapLatest { date -> workEntryDao.getByDateFlow(date) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    /** Tracks the selected date's entry reactively. */
    val selectedEntry: StateFlow<WorkEntry?> = _selectedDate
        .flatMapLatest { date -> workEntryDao.getByDateFlow(date) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _weekStats = MutableStateFlow<WeekStats?>(null)
    val weekStats: StateFlow<WeekStats?> = _weekStats.asStateFlow()

    private val _monthStats = MutableStateFlow<MonthStats?>(null)
    val monthStats: StateFlow<MonthStats?> = _monthStats.asStateFlow()

    private val _isOvertimeConfigured = MutableStateFlow(true)
    val isOvertimeConfigured: StateFlow<Boolean> = _isOvertimeConfigured.asStateFlow()

    private val _overtimeYearDisplay = MutableStateFlow(formatSignedHours(0.0))
    val overtimeYearDisplay: StateFlow<String> = _overtimeYearDisplay.asStateFlow()

    private val _overtimeMonthDisplay = MutableStateFlow<String?>(formatSignedHours(0.0))
    val overtimeMonthDisplay: StateFlow<String?> = _overtimeMonthDisplay.asStateFlow()

    private val _overtimeYearActualDisplay = MutableStateFlow(formatHours(0.0))
    val overtimeYearActualDisplay: StateFlow<String> = _overtimeYearActualDisplay.asStateFlow()

    private val _overtimeYearTargetDisplay = MutableStateFlow(formatHours(0.0))
    val overtimeYearTargetDisplay: StateFlow<String> = _overtimeYearTargetDisplay.asStateFlow()

    private val _overtimeYearCountedDays = MutableStateFlow(0)
    val overtimeYearCountedDays: StateFlow<Int> = _overtimeYearCountedDays.asStateFlow()

    private val _overtimeYearOffDayTravelDisplay = MutableStateFlow(formatHours(0.0))
    val overtimeYearOffDayTravelDisplay: StateFlow<String> = _overtimeYearOffDayTravelDisplay.asStateFlow()

    private val _overtimeYearOffDayTravelDays = MutableStateFlow(0)
    val overtimeYearOffDayTravelDays: StateFlow<Int> = _overtimeYearOffDayTravelDays.asStateFlow()

    private val _weekDaysUi = MutableStateFlow<List<WeekDayUi>>(emptyList())
    val weekDaysUi: StateFlow<List<WeekDayUi>> = _weekDaysUi.asStateFlow()

    private val _showDailyCheckInDialog = MutableStateFlow(false)
    val showDailyCheckInDialog: StateFlow<Boolean> = _showDailyCheckInDialog.asStateFlow()

    private val _dailyCheckInLocationInput = MutableStateFlow("")
    val dailyCheckInLocationInput: StateFlow<String> = _dailyCheckInLocationInput.asStateFlow()

    private val _dailyCheckInIsArrivalDeparture = MutableStateFlow(false)
    val dailyCheckInIsArrivalDeparture: StateFlow<Boolean> = _dailyCheckInIsArrivalDeparture.asStateFlow()

    private val _dailyCheckInBreakfastIncluded = MutableStateFlow(false)
    val dailyCheckInBreakfastIncluded: StateFlow<Boolean> = _dailyCheckInBreakfastIncluded.asStateFlow()

    val dailyCheckInAllowancePreviewCents: StateFlow<Int> = combine(
        _dailyCheckInIsArrivalDeparture,
        _dailyCheckInBreakfastIncluded
    ) { arrival, breakfast ->
        MealAllowanceCalculator.calculate(
            dayType = DayType.WORK,
            isArrivalDeparture = arrival,
            breakfastIncluded = breakfast
        ).amountCents
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = MealAllowanceCalculator.BASE_NORMAL_CENTS
    )

    private val _showDayLocationDialog = MutableStateFlow(false)
    val showDayLocationDialog: StateFlow<Boolean> = _showDayLocationDialog.asStateFlow()

    private val _dayLocationInput = MutableStateFlow("")
    val dayLocationInput: StateFlow<String> = _dayLocationInput.asStateFlow()

    private val _loadingActions = MutableStateFlow<Set<TodayAction>>(emptySet())
    val loadingActions: StateFlow<Set<TodayAction>> = _loadingActions.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<UiText?>(null)
    val snackbarMessage: StateFlow<UiText?> = _snackbarMessage.asStateFlow()

    private val _showDeleteDayDialog = MutableStateFlow(false)
    val showDeleteDayDialog: StateFlow<Boolean> = _showDeleteDayDialog.asStateFlow()

    /** Holds the last deleted entry until the undo window closes. */
    private val _deletedEntryForUndo = MutableStateFlow<WorkEntry?>(null)
    val deletedEntryForUndo: StateFlow<WorkEntry?> = _deletedEntryForUndo.asStateFlow()

    // B10: Capture the date at dialog-open time so racing selectDate() calls don't affect it
    private val _dailyCheckInDate = MutableStateFlow(LocalDate.now())

    // Hilfs-Typen für typsichere combine()-Gruppen (ersetzt UNCHECKED_CAST-Variante)
    private data class ScreenCorePart(
        val uiState: TodayUiState,
        val selectedEntry: WorkEntry?,
        val selectedDate: LocalDate,
        val todayDate: LocalDate,
        val loadingActions: Set<TodayAction>
    )
    private data class ScreenWeekMonthPart(
        val weekDaysUi: List<WeekDayUi>,
        val weekStats: WeekStats?,
        val monthStats: MonthStats?
    )
    private data class OvertimeDisplayPart(
        val isOvertimeConfigured: Boolean,
        val overtimeYearDisplay: String,
        val overtimeMonthDisplay: String?,
        val overtimeYearActualDisplay: String,
        val overtimeYearTargetDisplay: String
    )
    private data class OvertimeCountsPart(
        val overtimeYearCountedDays: Int,
        val overtimeYearOffDayTravelDisplay: String,
        val overtimeYearOffDayTravelDays: Int
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
        combine(uiState, selectedEntry, selectedDate, _todayDate, loadingActions) { ui, entry, date, today, loading ->
            ScreenCorePart(ui, entry, date, today, loading)
        },
        combine(weekDaysUi, weekStats, monthStats) { days, week, month ->
            ScreenWeekMonthPart(days, week, month)
        },
        combine(isOvertimeConfigured, overtimeYearDisplay, overtimeMonthDisplay, overtimeYearActualDisplay, overtimeYearTargetDisplay) { configured, year, month, actual, target ->
            OvertimeDisplayPart(configured, year, month, actual, target)
        },
        combine(overtimeYearCountedDays, overtimeYearOffDayTravelDisplay, overtimeYearOffDayTravelDays) { days, travel, travelDays ->
            OvertimeCountsPart(days, travel, travelDays)
        }
    ) { core, weekMonth, overtime, overtimeCounts ->
        TodayScreenState(
            uiState = core.uiState,
            selectedEntry = core.selectedEntry,
            selectedDate = core.selectedDate,
            todayDate = core.todayDate,
            weekDaysUi = weekMonth.weekDaysUi,
            weekStats = weekMonth.weekStats,
            monthStats = weekMonth.monthStats,
            isOvertimeConfigured = overtime.isOvertimeConfigured,
            overtimeYearDisplay = overtime.overtimeYearDisplay,
            overtimeMonthDisplay = overtime.overtimeMonthDisplay,
            overtimeYearActualDisplay = overtime.overtimeYearActualDisplay,
            overtimeYearTargetDisplay = overtime.overtimeYearTargetDisplay,
            overtimeYearCountedDays = overtimeCounts.overtimeYearCountedDays,
            overtimeYearOffDayTravelDisplay = overtimeCounts.overtimeYearOffDayTravelDisplay,
            overtimeYearOffDayTravelDays = overtimeCounts.overtimeYearOffDayTravelDays,
            loadingActions = core.loadingActions
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = initialScreenState()
    )

    val dialogState: StateFlow<TodayDialogState> = combine(
        combine(showDailyCheckInDialog, dailyCheckInLocationInput, dailyCheckInIsArrivalDeparture, dailyCheckInBreakfastIncluded, dailyCheckInAllowancePreviewCents) { show, loc, arrival, breakfast, allowance ->
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
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = initialDialogState()
    )

    init {
        observeTodayDate()
        loadTodayEntry()
        observeEntryUpdates()
        loadStatistics()
        loadWeekOverview()
    }

    private fun loadTodayEntry() {
        viewModelScope.launch {
            _uiState.value = TodayUiState.Loading
            try {
                val entry = withContext(Dispatchers.IO) { workEntryDao.getByDate(_selectedDate.value) }
                _uiState.value = TodayUiState.Success(entry)
            } catch (e: Exception) {
                _uiState.value = TodayUiState.Error(e.toUiText(R.string.today_error_unknown))
            }
        }
    }

    private fun observeTodayDate() {
        viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                val systemToday = LocalDate.now()
                if (_todayDate.value != systemToday) {
                    _todayDate.value = systemToday
                    loadStatisticsInternal()
                    loadWeekOverviewInternal()
                }
                delay(millisUntilNextDay())
            }
        }
    }

    // A04: debounce to avoid rapid re-computation
    private fun observeEntryUpdates() {
        viewModelScope.launch {
            combine(selectedDate, selectedEntry) { date, entry ->
                EntryRefreshKey(
                    selectedDate = date,
                    updatedAt = entry?.updatedAt
                )
            }
                .debounce(ENTRY_UPDATE_DEBOUNCE_MS)
                .distinctUntilChanged()
                .collectLatest {
                    loadStatisticsInternal()
                    loadWeekOverviewInternal()
                }
        }
    }

    private fun loadStatistics() {
        viewModelScope.launch { loadStatisticsInternal() }
    }

    private fun loadWeekOverview() {
        viewModelScope.launch { loadWeekOverviewInternal() }
    }

    private suspend fun loadStatisticsInternal() {
        try {
            val now = LocalDate.now()
            _todayDate.value = now
            val weekFields = java.time.temporal.WeekFields.of(Locale.GERMAN)
            val weekStart = now.with(weekFields.dayOfWeek(), 1)
            val weekEnd = now.with(weekFields.dayOfWeek(), 7)
            val monthStart = now.withDayOfMonth(1)
            val monthEnd = now.withDayOfMonth(now.lengthOfMonth())

            val settings = reminderSettingsManager.settings.first()
            _isOvertimeConfigured.value = true

            val (weekEntries, monthEntries) = withContext(Dispatchers.IO) {
                Pair(
                    workEntryDao.getByDateRangeWithTravel(weekStart, weekEnd),
                    workEntryDao.getByDateRangeWithTravel(monthStart, monthEnd)
                )
            }

            _weekStats.value = calculateWeekStats(weekEntries, settings.weeklyTargetHours)
            _monthStats.value = calculateMonthStats(monthEntries, settings.monthlyTargetHours)

            val yearStart = now.withDayOfYear(1)
            val (yearEntries, currentMonthEntries) = withContext(Dispatchers.IO) {
                Pair(
                    workEntryDao.getByDateRangeWithTravel(yearStart, now),
                    workEntryDao.getByDateRangeWithTravel(monthStart, now)
                )
            }

            val yearOvertime = calculateOvertimeForRange(yearEntries, settings.dailyTargetHours)
            val monthOvertime = calculateOvertimeForRange(currentMonthEntries, settings.dailyTargetHours)

            _overtimeYearDisplay.value = formatSignedHours(yearOvertime.totalOvertimeHours)
            _overtimeMonthDisplay.value = formatSignedHours(monthOvertime.totalOvertimeHours)
            _overtimeYearActualDisplay.value = formatHours(yearOvertime.totalActualHours)
            _overtimeYearTargetDisplay.value = formatHours(yearOvertime.totalTargetHours)
            _overtimeYearCountedDays.value = yearOvertime.countedDays
            _overtimeYearOffDayTravelDisplay.value = formatHours(yearOvertime.offDayTravelHours)
            _overtimeYearOffDayTravelDays.value = yearOvertime.offDayTravelDays
        } catch (e: Exception) {
            android.util.Log.w("TodayViewModel", "loadStatistics failed: ${e.message}")
            _snackbarMessage.value = e.toUiText(R.string.today_error_unknown)
        }
    }

    private suspend fun loadWeekOverviewInternal() {
        try {
            val selected = _selectedDate.value
            val today = _todayDate.value
            val weekDates = de.montagezeit.app.domain.util.WeekCalculator.weekDays(
                de.montagezeit.app.domain.util.WeekCalculator.weekStart(selected)
            )
            val entries = withContext(Dispatchers.IO) {
                workEntryDao.getByDateRange(weekDates.first(), weekDates.last())
            }
            _weekDaysUi.value = buildWeekDayUi(
                selectedDate = selected,
                todayDate = today,
                entries = entries
            )
        } catch (e: Exception) {
            android.util.Log.w("TodayViewModel", "loadWeekOverview failed: ${e.message}")
            _snackbarMessage.value = e.toUiText(R.string.today_error_unknown)
        }
    }

    fun selectDate(date: LocalDate) {
        val wasAlreadySelected = _selectedDate.value == date
        val isDateInCurrentWeek = _weekDaysUi.value.any { it.date == date }
        _selectedDate.value = date

        // B08: Set Loading immediately to avoid stale UI
        _uiState.value = TodayUiState.Loading

        // Update week overview highlights immediately
        _weekDaysUi.update { days ->
            days.map { it.copy(isSelected = it.date == date) }
        }

        // Skip reloading if already selected (optimization)
        if (wasAlreadySelected) {
            // Still need to restore the UI state from the flow
            viewModelScope.launch {
                val entry = selectedEntry.value ?: withContext(Dispatchers.IO) { workEntryDao.getByDate(date) }
                _uiState.value = TodayUiState.Success(entry)
            }
            return
        }

        if (!isDateInCurrentWeek) {
            loadWeekOverview()
        }

        viewModelScope.launch {
            try {
                val entry = withContext(Dispatchers.IO) { workEntryDao.getByDate(date) }
                _uiState.value = TodayUiState.Success(entry)
            } catch (e: Exception) {
                _uiState.value = TodayUiState.Error(e.toUiText(R.string.today_error_unknown))
            }
        }
    }

    fun openDailyCheckInDialog() {
        // B10: Capture selected date at dialog-open time
        _dailyCheckInDate.value = _selectedDate.value
        viewModelScope.launch {
            try {
                val existingEntry = currentSelectedEntryOrNull() ?: workEntryDao.getByDate(_selectedDate.value)
                val prefill = resolveDayLocationPrefill(existingEntry)
                _dailyCheckInLocationInput.value = prefill
                _dailyCheckInIsArrivalDeparture.value = existingEntry?.mealIsArrivalDeparture ?: false
                _dailyCheckInBreakfastIncluded.value = existingEntry?.mealBreakfastIncluded ?: false
                _showDailyCheckInDialog.value = true
            } catch (e: Exception) {
                _snackbarMessage.value = e.toUiText(R.string.today_error_day_location_save_failed)
            }
        }
    }

    fun onDailyCheckInLocationChanged(label: String) {
        _dailyCheckInLocationInput.value = label
    }

    fun onDailyCheckInArrivalDepartureChanged(value: Boolean) {
        _dailyCheckInIsArrivalDeparture.value = value
    }

    fun onDailyCheckInBreakfastIncludedChanged(value: Boolean) {
        _dailyCheckInBreakfastIncluded.value = value
    }

    fun onDismissDailyCheckInDialog() {
        _showDailyCheckInDialog.value = false
    }

    fun openDayLocationDialog() {
        viewModelScope.launch {
            try {
                val existingEntry = currentSelectedEntryOrNull() ?: workEntryDao.getByDate(_selectedDate.value)
                val prefill = resolveDayLocationPrefill(existingEntry)
                _dayLocationInput.value = prefill
                _showDayLocationDialog.value = true
            } catch (e: Exception) {
                _snackbarMessage.value = e.toUiText(R.string.today_error_day_location_save_failed)
            }
        }
    }

    fun onDayLocationInputChanged(label: String) {
        _dayLocationInput.value = label
    }

    fun onDismissDayLocationDialog() {
        _showDayLocationDialog.value = false
    }

    // B10: Use _dailyCheckInDate instead of _selectedDate for submission
    fun submitDailyManualCheckIn(label: String = _dailyCheckInLocationInput.value) {
        if (label.trim().isBlank()) {
            _snackbarMessage.value = UiText.StringResource(R.string.error_day_location_required)
            return
        }
        viewModelScope.launch {
            addLoadingAction(TodayAction.DAILY_MANUAL_CHECK_IN)
            try {
                val input = DailyManualCheckInInput(
                    date = _dailyCheckInDate.value,
                    dayLocationLabel = label,
                    isArrivalDeparture = _dailyCheckInIsArrivalDeparture.value,
                    breakfastIncluded = _dailyCheckInBreakfastIncluded.value
                )
                val entry = recordDailyManualCheckIn(input)
                _uiState.value = TodayUiState.Success(entry)
                _dailyCheckInLocationInput.value = entry.dayLocationLabel
                _showDailyCheckInDialog.value = false
            } catch (e: Exception) {
                _snackbarMessage.value = e.toUiText(R.string.today_error_confirm_day_failed)
            } finally {
                removeLoadingAction(TodayAction.DAILY_MANUAL_CHECK_IN)
            }
        }
    }

    fun onResetError() {
        val currentEntry = currentSelectedEntryOrNull()
        _uiState.value = TodayUiState.Success(currentEntry)
    }

    fun ensureTodayEntryThen(onDone: () -> Unit) {
        viewModelScope.launch {
            val date = _selectedDate.value
            try {
                val existing = workEntryDao.getByDate(date)
                if (existing == null) {
                    val settings = reminderSettingsManager.settings.first()
                    val isNonWorking = nonWorkingDayChecker.isNonWorkingDay(date, settings)
                    val now = System.currentTimeMillis()
                    val entry = WorkEntryFactory.createDefaultEntry(
                        date = date,
                        settings = settings,
                        dayType = if (isNonWorking) DayType.OFF else DayType.WORK,
                        now = now
                    )
                    workEntryDao.upsert(entry)
                }
            } finally {
                onDone()
            }
        }
    }

    fun onConfirmOffDay() {
        viewModelScope.launch {
            addLoadingAction(TodayAction.CONFIRM_OFFDAY)
            try {
                val entry = confirmOffDay(_selectedDate.value, source = "UI")
                _uiState.value = TodayUiState.Success(entry)
                _showDailyCheckInDialog.value = false
            } catch (e: Exception) {
                _snackbarMessage.value = e.toUiText(R.string.today_error_confirm_day_failed)
            } finally {
                removeLoadingAction(TodayAction.CONFIRM_OFFDAY)
            }
        }
    }

    fun submitDayLocationUpdate(label: String = _dayLocationInput.value) {
        viewModelScope.launch {
            val trimmed = label.trim()
            if (trimmed.isBlank()) {
                _snackbarMessage.value = UiText.StringResource(R.string.error_day_location_required)
                return@launch
            }
            addLoadingAction(TodayAction.UPDATE_DAY_LOCATION)
            try {
                val entry = setDayLocation(_selectedDate.value, trimmed)
                _uiState.value = TodayUiState.Success(entry)
                _dayLocationInput.value = entry.dayLocationLabel
                _showDayLocationDialog.value = false
            } catch (e: Exception) {
                _snackbarMessage.value = e.toUiText(R.string.today_error_day_location_save_failed)
            } finally {
                removeLoadingAction(TodayAction.UPDATE_DAY_LOCATION)
            }
        }
    }

    fun openDeleteDayDialog() {
        _showDeleteDayDialog.value = true
    }

    fun dismissDeleteDayDialog() {
        _showDeleteDayDialog.value = false
    }

    fun confirmDeleteDay() {
        viewModelScope.launch {
            addLoadingAction(TodayAction.DELETE_DAY)
            try {
                val deleted = deleteDayEntry(_selectedDate.value)
                _showDeleteDayDialog.value = false
                if (deleted != null) {
                    _uiState.value = TodayUiState.Success(null)
                    _deletedEntryForUndo.value = deleted
                }
            } catch (e: Exception) {
                _snackbarMessage.value = e.toUiText(R.string.today_error_delete_failed)
            } finally {
                removeLoadingAction(TodayAction.DELETE_DAY)
            }
        }
    }

    // B06: After undo, also navigate back to the restored date
    fun undoDeleteDay() {
        val entry = _deletedEntryForUndo.value ?: return
        _deletedEntryForUndo.value = null
        viewModelScope.launch {
            try {
                workEntryDao.upsert(entry)
                _uiState.value = TodayUiState.Success(entry)
                selectDate(entry.date)
            } catch (e: Exception) {
                _snackbarMessage.value = e.toUiText(R.string.today_error_delete_failed)
            }
        }
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

    private fun currentSelectedEntryOrNull(): WorkEntry? =
        selectedEntry.value?.takeIf { it.date == _selectedDate.value }

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
            weekStats = null,
            monthStats = null,
            isOvertimeConfigured = true,
            overtimeYearDisplay = formatSignedHours(0.0),
            overtimeMonthDisplay = formatSignedHours(0.0),
            overtimeYearActualDisplay = formatHours(0.0),
            overtimeYearTargetDisplay = formatHours(0.0),
            overtimeYearCountedDays = 0,
            overtimeYearOffDayTravelDisplay = formatHours(0.0),
            overtimeYearOffDayTravelDays = 0,
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

        fun millisUntilNextDay(now: LocalDateTime = LocalDateTime.now()): Long {
            val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay()
            return Duration.between(now, nextMidnight).toMillis().coerceAtLeast(1L)
        }

        fun formatSignedHours(hours: Double): String {
            return String.format(Locale.GERMAN, "%+.2fh", hours)
        }

        fun formatHours(hours: Double): String {
            return String.format(Locale.GERMAN, "%.2fh", hours)
        }
    }
}

sealed class TodayUiState {
    object Loading : TodayUiState()
    data class Success(val entry: WorkEntry?) : TodayUiState()
    data class Error(val message: UiText) : TodayUiState()
}

private fun Throwable.toUiText(@androidx.annotation.StringRes fallbackRes: Int): UiText {
    val messageValue = message?.trim().orEmpty()
    return if (messageValue.isNotEmpty()) {
        UiText.DynamicString(messageValue)
    } else {
        UiText.StringResource(fallbackRes)
    }
}
