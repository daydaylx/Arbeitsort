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
import de.montagezeit.app.domain.usecase.RecordDailyManualCheckIn
import de.montagezeit.app.domain.usecase.ResolveDayLocationPrefill
import de.montagezeit.app.domain.usecase.SetDayLocation
import de.montagezeit.app.domain.util.TimeCalculator
import de.montagezeit.app.domain.util.WeekCalculator
import de.montagezeit.app.ui.util.UiText
import de.montagezeit.app.work.ReminderWindowEvaluator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
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
    val targetHours: Double
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
    UPDATE_DAY_LOCATION
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TodayViewModel @Inject constructor(
    private val workEntryDao: WorkEntryDao,
    private val recordDailyManualCheckIn: RecordDailyManualCheckIn,
    private val resolveDayLocationPrefill: ResolveDayLocationPrefill,
    private val confirmOffDay: ConfirmOffDay,
    private val setDayLocation: SetDayLocation,
    private val reminderSettingsManager: ReminderSettingsManager
) : ViewModel() {
    private val calculateOvertimeForRange = CalculateOvertimeForRange()

    private val _uiState = MutableStateFlow<TodayUiState>(TodayUiState.Loading)
    val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    /** Always tracks today's entry reactively (for the action bar). */
    val todayEntry: StateFlow<WorkEntry?> = workEntryDao.getByDateFlow(LocalDate.now())
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

    private val _weekDaysUi = MutableStateFlow<List<WeekDayUi>>(emptyList())
    val weekDaysUi: StateFlow<List<WeekDayUi>> = _weekDaysUi.asStateFlow()

    private val _showDailyCheckInDialog = MutableStateFlow(false)
    val showDailyCheckInDialog: StateFlow<Boolean> = _showDailyCheckInDialog.asStateFlow()

    private val _dailyCheckInLocationInput = MutableStateFlow("")
    val dailyCheckInLocationInput: StateFlow<String> = _dailyCheckInLocationInput.asStateFlow()

    private val _showDayLocationDialog = MutableStateFlow(false)
    val showDayLocationDialog: StateFlow<Boolean> = _showDayLocationDialog.asStateFlow()

    private val _dayLocationInput = MutableStateFlow("")
    val dayLocationInput: StateFlow<String> = _dayLocationInput.asStateFlow()

    private val _loadingActions = MutableStateFlow<Set<TodayAction>>(emptySet())
    val loadingActions: StateFlow<Set<TodayAction>> = _loadingActions.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<UiText?>(null)
    val snackbarMessage: StateFlow<UiText?> = _snackbarMessage.asStateFlow()

    init {
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

    private fun observeEntryUpdates() {
        viewModelScope.launch {
            selectedEntry.collect {
                loadStatistics()
                loadWeekOverview()
            }
        }
    }

    private fun loadStatistics() {
        viewModelScope.launch {
            try {
                val now = LocalDate.now()
                val weekFields = java.time.temporal.WeekFields.of(Locale.GERMAN)
                val weekStart = now.with(weekFields.dayOfWeek(), 1)
                val weekEnd = now.with(weekFields.dayOfWeek(), 7)
                val monthStart = now.withDayOfMonth(1)
                val monthEnd = now.withDayOfMonth(now.lengthOfMonth())

                // Load settings for target hours
                val settings = reminderSettingsManager.settings.first()
                _isOvertimeConfigured.value = true

                val (weekEntries, monthEntries) = withContext(Dispatchers.IO) {
                    Pair(
                        workEntryDao.getByDateRange(weekStart, weekEnd),
                        workEntryDao.getByDateRange(monthStart, monthEnd)
                    )
                }

                _weekStats.value = calculateWeekStats(weekEntries, settings.weeklyTargetHours)
                _monthStats.value = calculateMonthStats(monthEntries, settings.monthlyTargetHours)

                val yearStart = now.withDayOfYear(1)
                val (yearEntries, currentMonthEntries) = withContext(Dispatchers.IO) {
                    Pair(
                        workEntryDao.getEntriesBetween(yearStart, now),
                        workEntryDao.getEntriesBetween(monthStart, now)
                    )
                }

                val yearOvertime = calculateOvertimeForRange(yearEntries, settings.dailyTargetHours)
                val monthOvertime = calculateOvertimeForRange(currentMonthEntries, settings.dailyTargetHours)

                _overtimeYearDisplay.value = formatSignedHours(yearOvertime.totalOvertimeHours)
                _overtimeMonthDisplay.value = formatSignedHours(monthOvertime.totalOvertimeHours)
                _overtimeYearActualDisplay.value = formatHours(yearOvertime.totalActualHours)
                _overtimeYearTargetDisplay.value = formatHours(yearOvertime.totalTargetHours)
                _overtimeYearCountedDays.value = yearOvertime.countedDays
            } catch (e: Exception) {
                android.util.Log.w("TodayViewModel", "loadStatistics failed: ${e.message}")
            }
        }
    }

    private fun loadWeekOverview() {
        viewModelScope.launch {
            try {
                val selected = _selectedDate.value
                val today = LocalDate.now()
                val weekStart = WeekCalculator.weekStart(today)
                val weekDates = WeekCalculator.weekDays(weekStart)
                val entries = withContext(Dispatchers.IO) {
                    workEntryDao.getByDateRange(weekDates.first(), weekDates.last())
                }
                val entriesByDate = entries.associateBy { it.date }
                _weekDaysUi.value = weekDates.map { date ->
                    val entry = entriesByDate[date]
                    val status = when {
                        entry == null -> WeekDayStatus.EMPTY
                        entry.dayType == DayType.OFF && entry.confirmedWorkDay -> WeekDayStatus.CONFIRMED_OFF
                        entry.confirmedWorkDay -> WeekDayStatus.CONFIRMED_WORK
                        entry.morningCapturedAt != null || entry.eveningCapturedAt != null -> WeekDayStatus.PARTIAL
                        else -> WeekDayStatus.EMPTY
                    }
                    val workHours = if (entry != null && entry.dayType == DayType.WORK) {
                        val h = TimeCalculator.calculateWorkHours(entry)
                        if (h > 0.0) h else null
                    } else null
                    WeekDayUi(
                        date = date,
                        isToday = date == today,
                        isSelected = date == selected,
                        dayLabel = date.shortWeekDayLabel(),
                        dayNumber = date.dayOfMonth.toString(),
                        status = status,
                        workHours = workHours
                    )
                }
            } catch (e: Exception) {
                android.util.Log.w("TodayViewModel", "loadWeekOverview failed: ${e.message}")
            }
        }
    }

    private fun calculateWeekStats(entries: List<WorkEntry>, targetHours: Double): WeekStats {
        val workEntries = entries.filter { it.dayType == DayType.WORK }
        val totalHours = workEntries.sumOf { TimeCalculator.calculateWorkHours(it) }
        val totalPaidHours = workEntries.sumOf { TimeCalculator.calculatePaidTotalHours(it) }
        val workDaysCount = workEntries.size

        return WeekStats(
            totalHours = totalHours,
            totalPaidHours = totalPaidHours,
            workDaysCount = workDaysCount,
            targetHours = targetHours
        )
    }

    private fun calculateMonthStats(entries: List<WorkEntry>, targetHours: Double): MonthStats {
        val workEntries = entries.filter { it.dayType == DayType.WORK }
        val totalHours = workEntries.sumOf { TimeCalculator.calculateWorkHours(it) }
        val totalPaidHours = workEntries.sumOf { TimeCalculator.calculatePaidTotalHours(it) }
        val workDaysCount = workEntries.size

        return MonthStats(
            totalHours = totalHours,
            totalPaidHours = totalPaidHours,
            workDaysCount = workDaysCount,
            targetHours = targetHours
        )
    }

    fun selectDate(date: LocalDate) {
        val wasAlreadySelected = _selectedDate.value == date
        _selectedDate.value = date

        // Update week overview highlights immediately
        _weekDaysUi.update { days ->
            days.map { it.copy(isSelected = it.date == date) }
        }

        // Skip reloading if already selected (optimization)
        if (wasAlreadySelected) return

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
        viewModelScope.launch {
            try {
                val existingEntry = selectedEntry.value ?: workEntryDao.getByDate(_selectedDate.value)
                val prefill = resolveDayLocationPrefill(existingEntry)
                _dailyCheckInLocationInput.value = prefill
                _showDailyCheckInDialog.value = true
            } catch (e: Exception) {
                _snackbarMessage.value = e.toUiText(R.string.today_error_day_location_save_failed)
            }
        }
    }

    fun onDailyCheckInLocationChanged(label: String) {
        _dailyCheckInLocationInput.value = label
    }

    fun onDismissDailyCheckInDialog() {
        _showDailyCheckInDialog.value = false
    }

    fun openDayLocationDialog() {
        viewModelScope.launch {
            try {
                val existingEntry = selectedEntry.value ?: workEntryDao.getByDate(_selectedDate.value)
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

    fun submitDailyManualCheckIn(label: String = _dailyCheckInLocationInput.value) {
        if (label.trim().isBlank()) {
            _snackbarMessage.value = UiText.StringResource(R.string.error_day_location_required)
            return
        }
        viewModelScope.launch {
            addLoadingAction(TodayAction.DAILY_MANUAL_CHECK_IN)
            try {
                val entry = recordDailyManualCheckIn(_selectedDate.value, label)
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
        val currentEntry = selectedEntry.value
        _uiState.value = TodayUiState.Success(currentEntry)
    }

    fun ensureTodayEntryThen(onDone: () -> Unit) {
        viewModelScope.launch {
            val date = _selectedDate.value
            try {
                val existing = workEntryDao.getByDate(date)
                if (existing == null) {
                    val settings = reminderSettingsManager.settings.first()
                    val isNonWorking = ReminderWindowEvaluator.isNonWorkingDay(date, settings)
                    val dayType = if (isNonWorking) DayType.OFF else DayType.WORK
                    val now = System.currentTimeMillis()
                    val defaultLocation = ""
                    val entry = WorkEntry(
                        date = date,
                        dayType = dayType,
                        workStart = settings.workStart,
                        workEnd = settings.workEnd,
                        breakMinutes = settings.breakMinutes,
                        dayLocationLabel = defaultLocation,
                        dayLocationSource = de.montagezeit.app.data.local.entity.DayLocationSource.FALLBACK,
                        createdAt = now,
                        updatedAt = now
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

    fun onSnackbarShown() {
        _snackbarMessage.value = null
    }

    private fun addLoadingAction(action: TodayAction) {
        _loadingActions.update { it + action }
    }

    private fun removeLoadingAction(action: TodayAction) {
        _loadingActions.update { it - action }
    }

    private companion object {
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
