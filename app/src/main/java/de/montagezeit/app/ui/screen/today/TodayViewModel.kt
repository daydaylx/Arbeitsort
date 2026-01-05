package de.montagezeit.app.ui.screen.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.domain.usecase.RecordEveningCheckIn
import de.montagezeit.app.domain.usecase.RecordMorningCheckIn
import de.montagezeit.app.domain.usecase.UpdateEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val workEntryDao: WorkEntryDao,
    private val recordMorningCheckIn: RecordMorningCheckIn,
    private val recordEveningCheckIn: RecordEveningCheckIn,
    private val updateEntry: UpdateEntry
) : ViewModel() {
    
    companion object {
        private const val LOCATION_TIMEOUT_MS = 15000L
    }
    
    private val _uiState = MutableStateFlow<TodayUiState>(TodayUiState.Loading)
    val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()
    
    val todayEntry: StateFlow<WorkEntry?> = workEntryDao.getByDateFlow(LocalDate.now())
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    
    init {
        loadTodayEntry()
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
}

sealed class TodayUiState {
    object Loading : TodayUiState()
    object LoadingLocation : TodayUiState()
    data class Success(val entry: WorkEntry?) : TodayUiState()
    data class Error(val message: String) : TodayUiState()
    data class LocationError(val message: String, val canRetry: Boolean) : TodayUiState()
}
