package de.montagezeit.app.ui.screen.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.TravelSource
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.preferences.ReminderSettingsManager
import de.montagezeit.app.domain.usecase.CalculateTravelCompensation
import de.montagezeit.app.domain.usecase.FetchRouteDistance
import de.montagezeit.app.domain.usecase.RecordEveningCheckIn
import de.montagezeit.app.domain.usecase.RecordMorningCheckIn
import de.montagezeit.app.domain.usecase.ConfirmWorkDay
import de.montagezeit.app.domain.usecase.ConfirmOffDay
import de.montagezeit.app.domain.usecase.ResolveReview
import de.montagezeit.app.domain.usecase.ReviewScope
import de.montagezeit.app.domain.usecase.SetDayLocation
import de.montagezeit.app.ui.screen.travel.TravelStatus
import de.montagezeit.app.ui.screen.travel.TravelUiState
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
    MORNING_CHECK_IN,
    EVENING_CHECK_IN,
    CONFIRM_WORKDAY,
    CONFIRM_OFFDAY,
    RESOLVE_REVIEW
}

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val workEntryDao: WorkEntryDao,
    private val recordMorningCheckIn: RecordMorningCheckIn,
    private val recordEveningCheckIn: RecordEveningCheckIn,
    private val confirmWorkDay: ConfirmWorkDay,
    private val confirmOffDay: ConfirmOffDay,
    private val fetchRouteDistance: FetchRouteDistance,
    private val calculateTravelCompensation: CalculateTravelCompensation,
    private val resolveReview: ResolveReview,
    private val setDayLocation: SetDayLocation,
    private val reminderSettingsManager: ReminderSettingsManager
) : ViewModel() {
    
    companion object {
        private const val LOCATION_TIMEOUT_MS = 15000L
        private const val ROUNDING_STEP_MINUTES = 15
    }
    
    private val _uiState = MutableStateFlow<TodayUiState>(TodayUiState.Loading)
    val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()
    
    val todayEntry: StateFlow<WorkEntry?> = workEntryDao.getByDateFlow(LocalDate.now())
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _travelUiState = MutableStateFlow(TravelUiState())
    val travelUiState: StateFlow<TravelUiState> = _travelUiState.asStateFlow()
    
    private val _weekStats = MutableStateFlow<WeekStats?>(null)
    val weekStats: StateFlow<WeekStats?> = _weekStats.asStateFlow()
    
    private val _monthStats = MutableStateFlow<MonthStats?>(null)
    val monthStats: StateFlow<MonthStats?> = _monthStats.asStateFlow()
    
    private val _showReviewSheet = MutableStateFlow(false)
    val showReviewSheet: StateFlow<Boolean> = _showReviewSheet.asStateFlow()
    
    private val _reviewScope = MutableStateFlow<ReviewScope?>(null)
    val reviewScope: StateFlow<ReviewScope?> = _reviewScope.asStateFlow()

    private val _loadingActions = MutableStateFlow<Set<TodayAction>>(emptySet())
    val loadingActions: StateFlow<Set<TodayAction>> = _loadingActions.asStateFlow()
    
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
                    _uiState.value = TodayUiState.Error(e.message ?: "Unbekannter Fehler")
                }
            }
        }
    }

    private fun observeEntryUpdates() {
        viewModelScope.launch {
            todayEntry.collect { entry ->
                _travelUiState.value = buildTravelUiState(entry)
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
                // Fehler ignorieren, Statistiken bleiben null
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
    
    fun onMorningCheckIn(forceWithoutLocation: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            addLoadingAction(TodayAction.MORNING_CHECK_IN)
            withContext(Dispatchers.Main) {
                _uiState.value = TodayUiState.LoadingLocation
            }

            try {
                val entry = recordMorningCheckIn(LocalDate.now(), forceWithoutLocation)
                withContext(Dispatchers.Main) {
                    _uiState.value = TodayUiState.Success(entry)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    when (e) {
                        is SecurityException -> {
                            _uiState.value = TodayUiState.LocationError(
                                "Standortberechtigung fehlt",
                                canRetry = false
                            )
                        }
                        else -> {
                            _uiState.value = TodayUiState.LocationError(
                                e.message ?: "Standort konnte nicht ermittelt werden",
                                canRetry = true
                            )
                        }
                    }
                }
            } finally {
                removeLoadingAction(TodayAction.MORNING_CHECK_IN)
            }
        }
    }
    
    fun onEveningCheckIn(forceWithoutLocation: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            addLoadingAction(TodayAction.EVENING_CHECK_IN)
            withContext(Dispatchers.Main) {
                _uiState.value = TodayUiState.LoadingLocation
            }

            try {
                val entry = recordEveningCheckIn(LocalDate.now(), forceWithoutLocation)
                withContext(Dispatchers.Main) {
                    _uiState.value = TodayUiState.Success(entry)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    when (e) {
                        is SecurityException -> {
                            _uiState.value = TodayUiState.LocationError(
                                "Standortberechtigung fehlt",
                                canRetry = false
                            )
                        }
                        else -> {
                            _uiState.value = TodayUiState.LocationError(
                                e.message ?: "Standort konnte nicht ermittelt werden",
                                canRetry = true
                            )
                        }
                    }
                }
            } finally {
                removeLoadingAction(TodayAction.EVENING_CHECK_IN)
            }
        }
    }
    
    fun onSkipLocation() {
        // Wird beim Location-Error verwendet, um ohne Location zu speichern
        val entry = todayEntry.value
        // Prüfen welcher Check-In fehlt
        if (entry == null || entry.morningCapturedAt == null) {
            onMorningCheckIn(forceWithoutLocation = true)
        } else if (entry.eveningCapturedAt == null) {
            onEveningCheckIn(forceWithoutLocation = true)
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
                    val defaultLocation = settings.defaultDayLocationLabel.ifBlank { "Leipzig" }
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
    
    @Suppress("UNUSED_PARAMETER")
    fun openEditSheet(date: LocalDate) {
        // Wird von UI verwendet, um EditSheet zu öffnen
        // Implementierung in Navigation
    }

    fun onConfirmWorkDay() {
        viewModelScope.launch(Dispatchers.IO) {
            addLoadingAction(TodayAction.CONFIRM_WORKDAY)
            withContext(Dispatchers.Main) {
                _uiState.value = TodayUiState.LoadingLocation
            }

            try {
                val entry = confirmWorkDay(LocalDate.now(), source = "UI")
                withContext(Dispatchers.Main) {
                    _uiState.value = TodayUiState.Success(entry)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    when (e) {
                        is SecurityException -> {
                            _uiState.value = TodayUiState.LocationError(
                                "Standortberechtigung fehlt",
                                canRetry = false
                            )
                        }
                        else -> {
                            _uiState.value = TodayUiState.LocationError(
                                e.message ?: "Standort konnte nicht ermittelt werden",
                                canRetry = true
                            )
                        }
                    }
                }
            } finally {
                removeLoadingAction(TodayAction.CONFIRM_WORKDAY)
            }
        }
    }

    fun onConfirmOffDay() {
        viewModelScope.launch(Dispatchers.IO) {
            addLoadingAction(TodayAction.CONFIRM_OFFDAY)
            withContext(Dispatchers.Main) {
                _uiState.value = TodayUiState.Loading
            }

            try {
                val entry = confirmOffDay(LocalDate.now(), source = "UI")
                withContext(Dispatchers.Main) {
                    _uiState.value = TodayUiState.Success(entry)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.value = TodayUiState.Error(e.message ?: "Konnte Tag nicht bestätigen")
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
            
            withContext(Dispatchers.Main) {
                _uiState.value = TodayUiState.Loading
            }
            
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
                    _uiState.value = TodayUiState.Error(e.message ?: "Prüfung konnte nicht gespeichert werden")
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
                    _uiState.value = TodayUiState.Error(e.message ?: "Ort konnte nicht gespeichert werden")
                }
            } finally {
                removeLoadingAction(TodayAction.RESOLVE_REVIEW)
            }
        }
    }

    fun updateTravelFromLabel(value: String) {
        _travelUiState.value = _travelUiState.value.copy(
            fromLabel = value
        )
    }

    fun updateTravelToLabel(value: String) {
        _travelUiState.value = _travelUiState.value.copy(
            toLabel = value
        )
    }

    fun updateManualDistance(value: String) {
        _travelUiState.value = _travelUiState.value.copy(
            manualDistanceKm = value
        )
    }

    fun calculateRouteDistance() {
        viewModelScope.launch(Dispatchers.IO) {
            val current = _travelUiState.value
            if (current.fromLabel.isBlank() || current.toLabel.isBlank()) {
                withContext(Dispatchers.Main) {
                    _travelUiState.value = current.copy(
                        status = TravelStatus.Error,
                        errorMessage = "Bitte Start- und Zieladresse eingeben"
                    )
                }
                return@launch
            }

            withContext(Dispatchers.Main) {
                _travelUiState.value = current.copy(status = TravelStatus.Loading, errorMessage = null)
            }

            when (val result = fetchRouteDistance(current.fromLabel, current.toLabel)) {
                is FetchRouteDistance.Result.Success -> {
                    val compensation = calculateTravelCompensation(
                        fromLabel = current.fromLabel,
                        toLabel = current.toLabel,
                        distanceKm = result.distanceKm,
                        roundingStepMinutes = ROUNDING_STEP_MINUTES
                    )
                    val updatedEntry = upsertTravelEntry(
                        fromLabel = current.fromLabel,
                        toLabel = current.toLabel,
                        distanceKm = result.distanceKm,
                        paidMinutes = compensation.paidMinutes,
                        source = TravelSource.ROUTED
                    )
                    withContext(Dispatchers.Main) {
                        _travelUiState.value = buildTravelUiState(updatedEntry).copy(
                            status = TravelStatus.Success
                        )
                    }
                }
                is FetchRouteDistance.Result.Error -> {
                    val message = when (result.reason) {
                        FetchRouteDistance.ErrorReason.NETWORK -> "Offline oder Netzwerkfehler"
                        FetchRouteDistance.ErrorReason.GEOCODE_FAILED -> "Adresse nicht gefunden"
                        FetchRouteDistance.ErrorReason.API_FAILED -> "Routing-Service nicht verfügbar"
                    }
                    withContext(Dispatchers.Main) {
                        _travelUiState.value = current.copy(
                            status = TravelStatus.Error,
                            errorMessage = message
                        )
                    }
                }
            }
        }
    }

    fun saveManualDistance() {
        viewModelScope.launch(Dispatchers.IO) {
            val current = _travelUiState.value
            val distanceKm = current.manualDistanceKm.replace(',', '.').toDoubleOrNull()
            if (distanceKm == null || distanceKm <= 0.0) {
                withContext(Dispatchers.Main) {
                    _travelUiState.value = current.copy(
                        status = TravelStatus.Error,
                        errorMessage = "Bitte gültige Km eingeben"
                    )
                }
                return@launch
            }

            val compensation = calculateTravelCompensation(
                fromLabel = current.fromLabel,
                toLabel = current.toLabel,
                distanceKm = distanceKm,
                roundingStepMinutes = ROUNDING_STEP_MINUTES
            )
            val updatedEntry = upsertTravelEntry(
                fromLabel = current.fromLabel,
                toLabel = current.toLabel,
                distanceKm = distanceKm,
                paidMinutes = compensation.paidMinutes,
                source = TravelSource.MANUAL
            )
            withContext(Dispatchers.Main) {
                _travelUiState.value = buildTravelUiState(updatedEntry).copy(
                    status = TravelStatus.Success
                )
            }
        }
    }

    private suspend fun upsertTravelEntry(
        fromLabel: String,
        toLabel: String,
        distanceKm: Double,
        paidMinutes: Int,
        source: TravelSource
    ): WorkEntry {
        val now = System.currentTimeMillis()
        val date = LocalDate.now()
        val existing = workEntryDao.getByDate(date)
        val updated = if (existing != null) {
            existing.copy(
                travelFromLabel = fromLabel.takeIf { it.isNotBlank() },
                travelToLabel = toLabel.takeIf { it.isNotBlank() },
                travelDistanceKm = distanceKm,
                travelPaidMinutes = paidMinutes,
                travelSource = source,
                travelUpdatedAt = now,
                updatedAt = now
            )
        } else {
            WorkEntry(
                date = date,
                travelFromLabel = fromLabel.takeIf { it.isNotBlank() },
                travelToLabel = toLabel.takeIf { it.isNotBlank() },
                travelDistanceKm = distanceKm,
                travelPaidMinutes = paidMinutes,
                travelSource = source,
                travelUpdatedAt = now,
                createdAt = now,
                updatedAt = now
            )
        }
        workEntryDao.upsert(updated)
        return updated
    }

    private fun addLoadingAction(action: TodayAction) {
        _loadingActions.update { it + action }
    }

    private fun removeLoadingAction(action: TodayAction) {
        _loadingActions.update { it - action }
    }

    private fun buildTravelUiState(entry: WorkEntry?): TravelUiState {
        if (entry == null) {
            return TravelUiState()
        }
        val distanceKm = entry.travelDistanceKm
        val paidMinutes = entry.travelPaidMinutes
        val paidHoursDisplay = paidMinutes?.let { CalculateTravelCompensation.formatPaidHours(it) }
        return TravelUiState(
            fromLabel = entry.travelFromLabel.orEmpty(),
            toLabel = entry.travelToLabel.orEmpty(),
            manualDistanceKm = distanceKm?.let { String.format(Locale.GERMAN, "%.2f", it) } ?: "",
            distanceKm = distanceKm,
            paidMinutes = paidMinutes,
            paidHoursDisplay = paidHoursDisplay,
            source = entry.travelSource,
            status = TravelStatus.Idle,
            errorMessage = null
        )
    }
}

sealed class TodayUiState {
    object Loading : TodayUiState()
    object LoadingLocation : TodayUiState()
    data class Success(val entry: WorkEntry?) : TodayUiState()
    data class Error(val message: String) : TodayUiState()
    data class LocationError(val message: String, val canRetry: Boolean) : TodayUiState()
}
