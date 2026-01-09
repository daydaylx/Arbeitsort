package de.montagezeit.app.ui.screen.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.TravelSource
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.domain.usecase.CalculateTravelCompensation
import de.montagezeit.app.domain.usecase.FetchRouteDistance
import de.montagezeit.app.domain.usecase.RecordEveningCheckIn
import de.montagezeit.app.domain.usecase.RecordMorningCheckIn
import de.montagezeit.app.ui.screen.travel.TravelStatus
import de.montagezeit.app.ui.screen.travel.TravelUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val workEntryDao: WorkEntryDao,
    private val recordMorningCheckIn: RecordMorningCheckIn,
    private val recordEveningCheckIn: RecordEveningCheckIn,
    private val fetchRouteDistance: FetchRouteDistance,
    private val calculateTravelCompensation: CalculateTravelCompensation
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
    
    init {
        loadTodayEntry()
        observeEntryUpdates()
    }
    
    private fun loadTodayEntry() {
        viewModelScope.launch {
            _uiState.value = TodayUiState.Loading
            
            try {
                val entry = workEntryDao.getByDate(LocalDate.now())
                _uiState.value = TodayUiState.Success(entry)
            } catch (e: Exception) {
                _uiState.value = TodayUiState.Error(e.message ?: "Unbekannter Fehler")
            }
        }
    }

    private fun observeEntryUpdates() {
        viewModelScope.launch {
            todayEntry.collect { entry ->
                _travelUiState.value = buildTravelUiState(entry)
            }
        }
    }
    
    fun onMorningCheckIn(forceWithoutLocation: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = TodayUiState.LoadingLocation
            
            try {
                val entry = recordMorningCheckIn(LocalDate.now(), forceWithoutLocation)
                _uiState.value = TodayUiState.Success(entry)
            } catch (e: Exception) {
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
        }
    }
    
    fun onEveningCheckIn(forceWithoutLocation: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = TodayUiState.LoadingLocation
            
            try {
                val entry = recordEveningCheckIn(LocalDate.now(), forceWithoutLocation)
                _uiState.value = TodayUiState.Success(entry)
            } catch (e: Exception) {
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
        }
    }
    
    fun onSkipLocation() {
        // Wird beim Location-Error verwendet, um ohne Location zu speichern
        val currentState = _uiState.value
        if (currentState is TodayUiState.Success) {
            // Prüfen welcher Check-In gerade versucht wurde
            if (currentState.entry?.morningCapturedAt == null) {
                onMorningCheckIn(forceWithoutLocation = true)
            } else if (currentState.entry?.eveningCapturedAt == null) {
                onEveningCheckIn(forceWithoutLocation = true)
            }
        }
    }
    
    fun onResetError() {
        val currentEntry = todayEntry.value
        _uiState.value = TodayUiState.Success(currentEntry)
    }
    
    fun openEditSheet(date: LocalDate) {
        // Wird von UI verwendet, um EditSheet zu öffnen
        // Implementierung in Navigation
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
        viewModelScope.launch {
            val current = _travelUiState.value
            if (current.fromLabel.isBlank() || current.toLabel.isBlank()) {
                _travelUiState.value = current.copy(
                    status = TravelStatus.Error,
                    errorMessage = "Bitte Start- und Zieladresse eingeben"
                )
                return@launch
            }

            _travelUiState.value = current.copy(status = TravelStatus.Loading, errorMessage = null)

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
                    _travelUiState.value = buildTravelUiState(updatedEntry).copy(
                        status = TravelStatus.Success
                    )
                }
                is FetchRouteDistance.Result.Error -> {
                    val message = when (result.reason) {
                        FetchRouteDistance.ErrorReason.NETWORK -> "Offline oder Netzwerkfehler"
                        FetchRouteDistance.ErrorReason.GEOCODE_FAILED -> "Adresse nicht gefunden"
                        FetchRouteDistance.ErrorReason.API_FAILED -> "Routing-Service nicht verfügbar"
                    }
                    _travelUiState.value = current.copy(
                        status = TravelStatus.Error,
                        errorMessage = message
                    )
                }
            }
        }
    }

    fun saveManualDistance() {
        viewModelScope.launch {
            val current = _travelUiState.value
            val distanceKm = current.manualDistanceKm.replace(',', '.').toDoubleOrNull()
            if (distanceKm == null || distanceKm <= 0.0) {
                _travelUiState.value = current.copy(
                    status = TravelStatus.Error,
                    errorMessage = "Bitte gültige Km eingeben"
                )
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
            _travelUiState.value = buildTravelUiState(updatedEntry).copy(
                status = TravelStatus.Success
            )
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
