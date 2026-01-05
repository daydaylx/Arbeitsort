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
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class EditEntryViewModel @Inject constructor(
    private val workEntryDao: WorkEntryDao,
    private val updateEntry: UpdateEntry,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val date: LocalDate = LocalDate.parse(
        savedStateHandle.get<String>("date") ?: LocalDate.now().toString()
    )
    
    private val _uiState = MutableStateFlow<EditUiState>(EditUiState.Loading)
    val uiState: StateFlow<EditUiState> = _uiState.asStateFlow()
    
    private val _formData = MutableStateFlow(EditFormData())
    val formData: StateFlow<EditFormData> = _formData.asStateFlow()
    
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
            
            val originalEntry = (currentState as EditUiState.Success).entry
            
            // Prüfen ob Borderzone-Confirm erforderlich
            val isBorderzone = (originalEntry.morningLocationLabel == null || 
                             originalEntry.eveningLocationLabel == null)
            
            if (isBorderzone && !confirmBorderzone) {
                _uiState.value = EditUiState.NeedConfirm
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
}

data class EditFormData(
    val dayType: DayType = DayType.WORK,
    val workStart: LocalTime = LocalTime.of(8, 0),
    val workEnd: LocalTime = LocalTime.of(19, 0),
    val breakMinutes: Int = 60,
    val morningLocationLabel: String? = null,
    val eveningLocationLabel: String? = null,
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
                note = entry.note,
                needsReview = entry.needsReview
            )
        }
    }
}

sealed class EditUiState {
    object Loading : EditUiState()
    data class Success(val entry: WorkEntry) : EditUiState()
    object NotFound : EditUiState()
    object NeedConfirm : EditUiState()
    object Saved : EditUiState()
    data class Error(val message: String) : EditUiState()
}
