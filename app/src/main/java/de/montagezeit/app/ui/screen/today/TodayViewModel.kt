package de.montagezeit.app.ui.screen.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.montagezeit.app.R
import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.preferences.ReminderSettingsManager
import de.montagezeit.app.domain.usecase.ConfirmOffDay
import de.montagezeit.app.domain.usecase.DEFAULT_DAY_LOCATION_LABEL
import de.montagezeit.app.domain.usecase.RecordDailyManualCheckIn
import de.montagezeit.app.domain.usecase.ResolveDayLocationPrefill
import de.montagezeit.app.domain.usecase.ResolveReview
import de.montagezeit.app.domain.usecase.ReviewScope
import de.montagezeit.app.domain.usecase.SetDayLocation
import de.montagezeit.app.ui.util.UiText
import de.montagezeit.app.work.ReminderWindowEvaluator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject

import de.montagezeit.app.domain.util.TimeCalculator

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
    RESOLVE_REVIEW
}

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val workEntryDao: WorkEntryDao,
    private val recordDailyManualCheckIn: RecordDailyManualCheckIn,
    private val resolveDayLocationPrefill: ResolveDayLocationPrefill,
    private val confirmOffDay: ConfirmOffDay,
    private val resolveReview: ResolveReview,
    private val setDayLocation: SetDayLocation,
    private val reminderSettingsManager: ReminderSettingsManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<TodayUiState>(TodayUiState.Loading)
    val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()
    
    val todayEntry: StateFlow<WorkEntry?> = workEntryDao.getByDateFlow(LocalDate.now())
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _weekStats = MutableStateFlow<WeekStats?>(null)
    val weekStats: StateFlow<WeekStats?> = _weekStats.asStateFlow()
    
    private val _monthStats = MutableStateFlow<MonthStats?>(null)
    val monthStats: StateFlow<MonthStats?> = _monthStats.asStateFlow()
    
    private val _showReviewSheet = MutableStateFlow(false)
    val showReviewSheet: StateFlow<Boolean> = _showReviewSheet.asStateFlow()
    
    private val _reviewScope = MutableStateFlow<ReviewScope?>(null)
    val reviewScope: StateFlow<ReviewScope?> = _reviewScope.asStateFlow()

    private val _showDailyCheckInDialog = MutableStateFlow(false)
    val showDailyCheckInDialog: StateFlow<Boolean> = _showDailyCheckInDialog.asStateFlow()

    private val _dailyCheckInLocationInput = MutableStateFlow("")
    val dailyCheckInLocationInput: StateFlow<String> = _dailyCheckInLocationInput.asStateFlow()

    private val _loadingActions = MutableStateFlow<Set<TodayAction>>(emptySet())
    val loadingActions: StateFlow<Set<TodayAction>> = _loadingActions.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<UiText?>(null)
    val snackbarMessage: StateFlow<UiText?> = _snackbarMessage.asStateFlow()
    
    init {
        loadTodayEntry()
        observeEntryUpdates()
        loadStatistics()
    }
    
    private fun loadTodayEntry() {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                _uiState.value = TodayUiState.Loading
            }

            try {
                val entry = workEntryDao.getByDate(LocalDate.now())
                withContext(Dispatchers.Main) {
                    _uiState.value = TodayUiState.Success(entry)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.value = TodayUiState.Error(
                        e.toUiText(R.string.today_error_unknown)
                    )
                }
            }
        }
    }

    private fun observeEntryUpdates() {
        viewModelScope.launch {
            todayEntry.collect {
                // Statistiken neu laden, wenn sich Einträge ändern
                loadStatistics()
            }
        }
    }
    
    private fun loadStatistics() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val now = LocalDate.now()

                // Aktuelle Woche berechnen
                val weekFields = java.time.temporal.WeekFields.of(Locale.GERMAN)
                val weekStart = now.with(weekFields.dayOfWeek(), 1) // Montag
                val weekEnd = now.with(weekFields.dayOfWeek(), 7) // Sonntag
                val weekEntries = workEntryDao.getByDateRange(weekStart, weekEnd)
                val weekStats = calculateWeekStats(weekEntries)

                // Aktueller Monat berechnen
                val monthStart = now.withDayOfMonth(1)
                val monthEnd = now.withDayOfMonth(now.lengthOfMonth())
                val monthEntries = workEntryDao.getByDateRange(monthStart, monthEnd)
                val monthStats = calculateMonthStats(monthEntries)

                withContext(Dispatchers.Main) {
                    _weekStats.value = weekStats
                    _monthStats.value = monthStats
                }
            } catch (e: Exception) {
                android.util.Log.w("TodayViewModel", "loadStatistics failed: ${e.message}")
            }
        }
    }
    
    private fun calculateWeekStats(entries: List<WorkEntry>): WeekStats {
        val workEntries = entries.filter { it.dayType == de.montagezeit.app.data.local.entity.DayType.WORK }
        val totalHours = workEntries.sumOf { TimeCalculator.calculateWorkHours(it) }
        val totalPaidHours = workEntries.sumOf { TimeCalculator.calculatePaidTotalHours(it) }
        val workDaysCount = workEntries.size
        val targetHours: Double = 40.0 // Standard: 40 Stunden/Woche
        
        return WeekStats(
            totalHours = totalHours,
            totalPaidHours = totalPaidHours,
            workDaysCount = workDaysCount,
            targetHours = targetHours
        )
    }
    
    private fun calculateMonthStats(entries: List<WorkEntry>): MonthStats {
        val workEntries = entries.filter { it.dayType == de.montagezeit.app.data.local.entity.DayType.WORK }
        val totalHours = workEntries.sumOf { TimeCalculator.calculateWorkHours(it) }
        val totalPaidHours = workEntries.sumOf { TimeCalculator.calculatePaidTotalHours(it) }
        val workDaysCount = workEntries.size
        val targetHours: Double = 160.0 // Standard: 160 Stunden/Monat (40h/Woche * 4)
        
        return MonthStats(
            totalHours = totalHours,
            totalPaidHours = totalPaidHours,
            workDaysCount = workDaysCount,
            targetHours = targetHours
        )
    }

    fun openDailyCheckInDialog() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val existingEntry = todayEntry.value ?: workEntryDao.getByDate(LocalDate.now())
                val prefill = resolveDayLocationPrefill(existingEntry)
                withContext(Dispatchers.Main) {
                    _dailyCheckInLocationInput.value = prefill
                    _showDailyCheckInDialog.value = true
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _snackbarMessage.value = e.toUiText(R.string.today_error_day_location_save_failed)
                }
            }
        }
    }

    fun onDailyCheckInLocationChanged(label: String) {
        _dailyCheckInLocationInput.value = label
    }

    fun onDismissDailyCheckInDialog() {
        _showDailyCheckInDialog.value = false
    }

    fun submitDailyManualCheckIn(label: String = _dailyCheckInLocationInput.value) {
        viewModelScope.launch(Dispatchers.IO) {
            addLoadingAction(TodayAction.DAILY_MANUAL_CHECK_IN)

            try {
                val entry = recordDailyManualCheckIn(LocalDate.now(), label)
                withContext(Dispatchers.Main) {
                    _uiState.value = TodayUiState.Success(entry)
                    _dailyCheckInLocationInput.value = entry.dayLocationLabel
                    _showDailyCheckInDialog.value = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _snackbarMessage.value = e.toUiText(R.string.today_error_confirm_day_failed)
                }
            } finally {
                removeLoadingAction(TodayAction.DAILY_MANUAL_CHECK_IN)
            }
        }
    }

    fun onResetError() {
        val currentEntry = todayEntry.value
        _uiState.value = TodayUiState.Success(currentEntry)
    }

    fun ensureTodayEntryThen(onDone: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val today = LocalDate.now()
            try {
                val existing = workEntryDao.getByDate(today)
                if (existing == null) {
                    val settings = reminderSettingsManager.settings.first()
                    val isNonWorking = ReminderWindowEvaluator.isNonWorkingDay(today, settings)
                    val dayType = if (isNonWorking) DayType.OFF else DayType.WORK
                    val now = System.currentTimeMillis()
                    val defaultLocation = settings.defaultDayLocationLabel.ifBlank { DEFAULT_DAY_LOCATION_LABEL }
                    val entry = WorkEntry(
                        date = today,
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
                withContext(Dispatchers.Main) {
                    onDone()
                }
            }
        }
    }
    
    fun onConfirmOffDay() {
        viewModelScope.launch(Dispatchers.IO) {
            addLoadingAction(TodayAction.CONFIRM_OFFDAY)

            try {
                val entry = confirmOffDay(LocalDate.now(), source = "UI")
                withContext(Dispatchers.Main) {
                    _uiState.value = TodayUiState.Success(entry)
                    _showDailyCheckInDialog.value = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _snackbarMessage.value = e.toUiText(R.string.today_error_confirm_day_failed)
                }
            } finally {
                removeLoadingAction(TodayAction.CONFIRM_OFFDAY)
            }
        }
    }
    
    fun onOpenReviewSheet() {
        val entry = todayEntry.value ?: return
        
        // Scope basierend auf vorhanden Check-Ins ermitteln
        val scope = when {
            entry.morningCapturedAt == null && entry.eveningCapturedAt == null -> ReviewScope.BOTH
            entry.morningCapturedAt != null && entry.eveningCapturedAt != null -> ReviewScope.BOTH
            entry.morningCapturedAt != null -> ReviewScope.MORNING
            else -> ReviewScope.EVENING
        }
        
        _reviewScope.value = scope
        _showReviewSheet.value = true
    }
    
    fun onResolveReview(label: String, isLeipzig: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val scope = _reviewScope.value ?: return@launch
            addLoadingAction(TodayAction.RESOLVE_REVIEW)

            try {
                val entry = resolveReview(
                    date = LocalDate.now(),
                    scope = scope,
                    resolvedLabel = label,
                    isLeipzig = isLeipzig
                )
                withContext(Dispatchers.Main) {
                    _uiState.value = TodayUiState.Success(entry)
                    _showReviewSheet.value = false
                    _reviewScope.value = null
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _snackbarMessage.value = e.toUiText(R.string.today_error_review_save_failed)
                }
            } finally {
                removeLoadingAction(TodayAction.RESOLVE_REVIEW)
            }
        }
    }
    
    fun onDismissReviewSheet() {
        _showReviewSheet.value = false
        _reviewScope.value = null
    }

    fun onUpdateDayLocation(label: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val trimmed = label.trim()
            if (trimmed.isBlank()) {
                return@launch
            }
            addLoadingAction(TodayAction.RESOLVE_REVIEW)
            try {
                val entry = setDayLocation(LocalDate.now(), trimmed)
                withContext(Dispatchers.Main) {
                    _uiState.value = TodayUiState.Success(entry)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _snackbarMessage.value = e.toUiText(R.string.today_error_day_location_save_failed)
                }
            } finally {
                removeLoadingAction(TodayAction.RESOLVE_REVIEW)
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
