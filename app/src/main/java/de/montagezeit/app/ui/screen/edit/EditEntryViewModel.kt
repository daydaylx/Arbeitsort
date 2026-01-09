package de.montagezeit.app.ui.screen.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.TravelSource
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.domain.usecase.CalculateTravelCompensation
import de.montagezeit.app.domain.usecase.FetchRouteDistance
import de.montagezeit.app.ui.screen.travel.TravelStatus
import de.montagezeit.app.ui.screen.travel.TravelUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class EditEntryViewModel @Inject constructor(
    private val workEntryDao: WorkEntryDao,
    private val fetchRouteDistance: FetchRouteDistance,
    private val calculateTravelCompensation: CalculateTravelCompensation,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val date: LocalDate = LocalDate.parse(
        savedStateHandle.get<String>("date") ?: LocalDate.now().toString()
    )
    
    private val _uiState = MutableStateFlow<EditUiState>(EditUiState.Loading)
    val uiState: StateFlow<EditUiState> = _uiState.asStateFlow()
    
    private val _formData = MutableStateFlow(EditFormData())
    val formData: StateFlow<EditFormData> = _formData.asStateFlow()

    private val _travelUiState = MutableStateFlow(TravelUiState())
    val travelUiState: StateFlow<TravelUiState> = _travelUiState.asStateFlow()
    
    init {
        loadEntry()
    }
    
    fun loadEntry() {
        viewModelScope.launch {
            _uiState.value = EditUiState.Loading
            
            try {
                val entry = workEntryDao.getByDate(date)
                if (entry != null) {
                    _formData.value = EditFormData.fromEntry(entry)
                    _travelUiState.value = buildTravelUiState(entry)
                    _uiState.value = EditUiState.Success(entry)
                } else {
                    _uiState.value = EditUiState.NotFound
                }
            } catch (e: Exception) {
                _uiState.value = EditUiState.Error(e.message ?: "Unbekannter Fehler")
            }
        }
    }
    
    fun updateDayType(dayType: DayType) {
        _formData.value = _formData.value.copy(dayType = dayType)
    }
    
    fun updateWorkStart(hour: Int, minute: Int) {
        _formData.value = _formData.value.copy(
            workStart = LocalTime.of(hour, minute)
        )
    }
    
    fun updateWorkEnd(hour: Int, minute: Int) {
        _formData.value = _formData.value.copy(
            workEnd = LocalTime.of(hour, minute)
        )
    }
    
    fun updateBreakMinutes(minutes: Int) {
        _formData.value = _formData.value.copy(
            breakMinutes = minutes
        )
    }
    
    fun updateMorningLocationLabel(label: String) {
        _formData.value = _formData.value.copy(
            morningLocationLabel = label.takeIf { it.isNotBlank() }
        )
    }
    
    fun updateEveningLocationLabel(label: String) {
        _formData.value = _formData.value.copy(
            eveningLocationLabel = label.takeIf { it.isNotBlank() }
        )
    }
    
    fun updateNote(note: String) {
        _formData.value = _formData.value.copy(
            note = note.takeIf { it.isNotBlank() }
        )
    }

    fun updateTravelFromLabel(label: String) {
        _formData.value = _formData.value.copy(
            travelFromLabel = label.takeIf { it.isNotBlank() }
        )
        _travelUiState.value = _travelUiState.value.copy(fromLabel = label)
    }

    fun updateTravelToLabel(label: String) {
        _formData.value = _formData.value.copy(
            travelToLabel = label.takeIf { it.isNotBlank() }
        )
        _travelUiState.value = _travelUiState.value.copy(toLabel = label)
    }

    fun updateManualDistance(value: String) {
        _travelUiState.value = _travelUiState.value.copy(manualDistanceKm = value)
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
                    updateTravelFields(
                        fromLabel = current.fromLabel,
                        toLabel = current.toLabel,
                        distanceKm = result.distanceKm,
                        paidMinutes = compensation.paidMinutes,
                        source = TravelSource.ROUTED
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
            updateTravelFields(
                fromLabel = current.fromLabel,
                toLabel = current.toLabel,
                distanceKm = distanceKm,
                paidMinutes = compensation.paidMinutes,
                source = TravelSource.MANUAL
            )
        }
    }
    
    fun resetNeedsReview() {
        _formData.value = _formData.value.copy(
            needsReview = false
        )
    }
    
    fun save(confirmBorderzone: Boolean = false) {
        viewModelScope.launch {
            val data = _formData.value
            val currentState = _uiState.value
            
            if (currentState !is EditUiState.Success) return@launch
            
            val originalEntry = currentState.entry
            
            // Prüfen ob Borderzone-Confirm erforderlich
            val isBorderzone = (originalEntry.morningLocationLabel == null || 
                             originalEntry.eveningLocationLabel == null)
            
            if (isBorderzone && !confirmBorderzone) {
                _uiState.value = currentState.copy(showConfirmDialog = true)
                return@launch
            }
            
            val updatedEntry = originalEntry.copy(
                dayType = data.dayType,
                workStart = data.workStart,
                workEnd = data.workEnd,
                breakMinutes = data.breakMinutes,
                morningLocationLabel = data.morningLocationLabel,
                eveningLocationLabel = data.eveningLocationLabel,
                travelFromLabel = data.travelFromLabel,
                travelToLabel = data.travelToLabel,
                travelDistanceKm = data.travelDistanceKm,
                travelPaidMinutes = data.travelPaidMinutes,
                travelSource = data.travelSource,
                travelUpdatedAt = data.travelUpdatedAt,
                note = data.note,
                needsReview = data.needsReview,
                updatedAt = System.currentTimeMillis()
            )
            
            try {
                workEntryDao.upsert(updatedEntry)
                _uiState.value = EditUiState.Saved
            } catch (e: Exception) {
                _uiState.value = EditUiState.Error(e.message ?: "Speichern fehlgeschlagen")
            }
        }
    }
    
    fun confirmAndSave() {
        save(confirmBorderzone = true)
    }

    fun dismissConfirmDialog() {
        val currentState = _uiState.value
        if (currentState is EditUiState.Success) {
            _uiState.value = currentState.copy(showConfirmDialog = false)
        }
    }

    private fun updateTravelFields(
        fromLabel: String,
        toLabel: String,
        distanceKm: Double,
        paidMinutes: Int,
        source: TravelSource
    ) {
        val now = System.currentTimeMillis()
        _formData.value = _formData.value.copy(
            travelFromLabel = fromLabel.takeIf { it.isNotBlank() },
            travelToLabel = toLabel.takeIf { it.isNotBlank() },
            travelDistanceKm = distanceKm,
            travelPaidMinutes = paidMinutes,
            travelSource = source,
            travelUpdatedAt = now
        )
        _travelUiState.value = _travelUiState.value.copy(
            fromLabel = fromLabel,
            toLabel = toLabel,
            manualDistanceKm = String.format(Locale.GERMAN, "%.2f", distanceKm),
            distanceKm = distanceKm,
            paidMinutes = paidMinutes,
            paidHoursDisplay = CalculateTravelCompensation.formatPaidHours(paidMinutes),
            source = source,
            status = TravelStatus.Success,
            errorMessage = null
        )
    }

    private fun buildTravelUiState(entry: WorkEntry): TravelUiState {
        val distanceKm = entry.travelDistanceKm
        val paidMinutes = entry.travelPaidMinutes
        return TravelUiState(
            fromLabel = entry.travelFromLabel.orEmpty(),
            toLabel = entry.travelToLabel.orEmpty(),
            manualDistanceKm = distanceKm?.let { String.format(Locale.GERMAN, "%.2f", it) } ?: "",
            distanceKm = distanceKm,
            paidMinutes = paidMinutes,
            paidHoursDisplay = paidMinutes?.let { CalculateTravelCompensation.formatPaidHours(it) },
            source = entry.travelSource,
            status = TravelStatus.Idle,
            errorMessage = null
        )
    }

    companion object {
        private const val ROUNDING_STEP_MINUTES = 15
    }
}

data class EditFormData(
    val dayType: DayType = DayType.WORK,
    val workStart: LocalTime = LocalTime.of(8, 0),
    val workEnd: LocalTime = LocalTime.of(19, 0),
    val breakMinutes: Int = 60,
    val morningLocationLabel: String? = null,
    val eveningLocationLabel: String? = null,
    val travelFromLabel: String? = null,
    val travelToLabel: String? = null,
    val travelDistanceKm: Double? = null,
    val travelPaidMinutes: Int? = null,
    val travelSource: TravelSource? = null,
    val travelUpdatedAt: Long? = null,
    val note: String? = null,
    val needsReview: Boolean = false
) {
    companion object {
        fun fromEntry(entry: WorkEntry): EditFormData {
            return EditFormData(
                dayType = entry.dayType,
                workStart = entry.workStart,
                workEnd = entry.workEnd,
                breakMinutes = entry.breakMinutes,
                morningLocationLabel = entry.morningLocationLabel,
                eveningLocationLabel = entry.eveningLocationLabel,
                travelFromLabel = entry.travelFromLabel,
                travelToLabel = entry.travelToLabel,
                travelDistanceKm = entry.travelDistanceKm,
                travelPaidMinutes = entry.travelPaidMinutes,
                travelSource = entry.travelSource,
                travelUpdatedAt = entry.travelUpdatedAt,
                note = entry.note,
                needsReview = entry.needsReview
            )
        }
    }
}

sealed class EditUiState {
    object Loading : EditUiState()
    data class Success(val entry: WorkEntry, val showConfirmDialog: Boolean = false) : EditUiState()
    object NotFound : EditUiState()
    object Saved : EditUiState()
    data class Error(val message: String) : EditUiState()
}
