package de.montagezeit.app.ui.screen.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.domain.usecase.UpdateEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class EditEntryViewModel @Inject constructor(
    private val workEntryDao: WorkEntryDao,
    private val updateEntry: UpdateEntry,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private var currentDate: LocalDate =
        savedStateHandle.get<String>("date")?.let { LocalDate.parse(it) } ?: LocalDate.now()
    
    private val _uiState = MutableStateFlow<EditUiState>(EditUiState.Loading)
    val uiState: StateFlow<EditUiState> = _uiState.asStateFlow()
    
    private val _formData = MutableStateFlow(EditFormData())
    val formData: StateFlow<EditFormData> = _formData.asStateFlow()
    
    init {
        loadEntry(currentDate)
    }
    
    fun setDate(date: LocalDate) {
        if (date == currentDate) {
            return
        }
        currentDate = date
        savedStateHandle["date"] = date.toString()
        loadEntry(date)
    }

    private fun loadEntry(date: LocalDate) {
        viewModelScope.launch {
            _uiState.value = EditUiState.Loading
            
            try {
                val entry = workEntryDao.getByDate(date)
                if (entry != null) {
                    _formData.value = EditFormData.fromEntry(entry)
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

    fun updateTravelStart(time: LocalTime?) {
        _formData.value = _formData.value.copy(travelStartTime = time)
    }

    fun updateTravelArrive(time: LocalTime?) {
        _formData.value = _formData.value.copy(travelArriveTime = time)
    }

    fun updateTravelLabelStart(label: String) {
        _formData.value = _formData.value.copy(
            travelLabelStart = label.takeIf { it.isNotBlank() }
        )
    }

    fun updateTravelLabelEnd(label: String) {
        _formData.value = _formData.value.copy(
            travelLabelEnd = label.takeIf { it.isNotBlank() }
        )
    }

    fun clearTravel() {
        _formData.value = _formData.value.copy(
            travelStartTime = null,
            travelArriveTime = null,
            travelLabelStart = null,
            travelLabelEnd = null
        )
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

            // Validate form data before saving
            val validationErrors = data.validate()
            if (validationErrors.isNotEmpty()) {
                _uiState.value = currentState.copy(validationErrors = validationErrors)
                return@launch
            }

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
                note = data.note,
                travelStartAt = data.travelStartTime?.let { toEpochMillis(originalEntry.date, it) },
                travelArriveAt = data.travelArriveTime?.let { toEpochMillis(originalEntry.date, it) },
                travelLabelStart = data.travelLabelStart,
                travelLabelEnd = data.travelLabelEnd,
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

    fun clearValidationErrors() {
        val currentState = _uiState.value
        if (currentState is EditUiState.Success && currentState.validationErrors.isNotEmpty()) {
            _uiState.value = currentState.copy(validationErrors = emptyList())
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

    private fun toEpochMillis(date: LocalDate, time: LocalTime): Long {
        return date.atTime(time)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }
}

data class EditFormData(
    val dayType: DayType = DayType.WORK,
    val workStart: LocalTime = LocalTime.of(8, 0),
    val workEnd: LocalTime = LocalTime.of(19, 0),
    val breakMinutes: Int = 60,
    val morningLocationLabel: String? = null,
    val eveningLocationLabel: String? = null,
    val note: String? = null,
    val needsReview: Boolean = false,
    val travelStartTime: LocalTime? = null,
    val travelArriveTime: LocalTime? = null,
    val travelLabelStart: String? = null,
    val travelLabelEnd: String? = null
) {
    /**
     * Validates the form data and returns a list of validation errors.
     * Returns empty list if validation passes.
     */
    fun validate(): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        // 1. Check if workEnd is after workStart
        val workTimeValid = workEnd > workStart
        if (!workTimeValid) {
            errors.add(ValidationError.WorkEndBeforeStart)
        }

        // 2. Check if breakMinutes is non-negative
        if (breakMinutes < 0) {
            errors.add(ValidationError.NegativeBreakMinutes)
        }

        // 3. Check if breakMinutes is not longer than total work time
        // Only check this if work time itself is valid
        if (workTimeValid) {
            val totalWorkMinutes = (workEnd.toSecondOfDay() - workStart.toSecondOfDay()) / 60
            if (breakMinutes > totalWorkMinutes) {
                errors.add(ValidationError.BreakLongerThanWorkTime)
            }
        }

        // 4. Check travel times if both are set
        if (travelStartTime != null && travelArriveTime != null) {
            if (travelArriveTime <= travelStartTime) {
                errors.add(ValidationError.TravelArriveBeforeStart)
            }
        }

        return errors
    }

    /**
     * Returns true if the form data is valid (no validation errors).
     */
    fun isValid(): Boolean = validate().isEmpty()

    companion object {
        fun fromEntry(entry: WorkEntry): EditFormData {
            return EditFormData(
                dayType = entry.dayType,
                workStart = entry.workStart,
                workEnd = entry.workEnd,
                breakMinutes = entry.breakMinutes,
                morningLocationLabel = entry.morningLocationLabel,
                eveningLocationLabel = entry.eveningLocationLabel,
                note = entry.note,
                needsReview = entry.needsReview,
                travelStartTime = entry.travelStartAt?.let { toLocalTime(it) },
                travelArriveTime = entry.travelArriveAt?.let { toLocalTime(it) },
                travelLabelStart = entry.travelLabelStart,
                travelLabelEnd = entry.travelLabelEnd
            )
        }

        private fun toLocalTime(timestamp: Long): LocalTime {
            return Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalTime()
        }
    }
}

/**
 * Validation errors for EditFormData
 */
sealed class ValidationError(val message: String) {
    object WorkEndBeforeStart : ValidationError("Arbeitsende muss nach Arbeitsbeginn liegen")
    object NegativeBreakMinutes : ValidationError("Pause kann nicht negativ sein")
    object BreakLongerThanWorkTime : ValidationError("Pause kann nicht länger als die Arbeitszeit sein")
    object TravelArriveBeforeStart : ValidationError("Ankunftszeit muss nach Abfahrtszeit liegen")
}

sealed class EditUiState {
    object Loading : EditUiState()
    data class Success(
        val entry: WorkEntry,
        val showConfirmDialog: Boolean = false,
        val validationErrors: List<ValidationError> = emptyList()
    ) : EditUiState()
    object NotFound : EditUiState()
    object Saved : EditUiState()
    data class Error(val message: String) : EditUiState()
}
