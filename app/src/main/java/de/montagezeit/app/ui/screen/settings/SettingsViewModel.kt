package de.montagezeit.app.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.preferences.ReminderSettings
import de.montagezeit.app.data.preferences.ReminderSettingsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val reminderSettingsManager: ReminderSettingsManager,
    private val workEntryDao: WorkEntryDao
) : ViewModel() {
    
    val reminderSettings = reminderSettingsManager.settings
    
    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Initial)
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    fun updateMorningWindow(startHour: Int, startMinute: Int, endHour: Int, endMinute: Int) {
        viewModelScope.launch {
            val startTime = LocalTime.of(startHour, startMinute)
            val endTime = LocalTime.of(endHour, endMinute)
            reminderSettingsManager.updateSettings(
                morningWindowStart = startTime,
                morningWindowEnd = endTime
            )
        }
    }

    fun updateEveningWindow(startHour: Int, startMinute: Int, endHour: Int, endMinute: Int) {
        viewModelScope.launch {
            val startTime = LocalTime.of(startHour, startMinute)
            val endTime = LocalTime.of(endHour, endMinute)
            reminderSettingsManager.updateSettings(
                eveningWindowStart = startTime,
                eveningWindowEnd = endTime
            )
        }
    }

    fun updateRadiusMeters(meters: Int) {
        viewModelScope.launch {
            reminderSettingsManager.updateSettings(
                locationRadiusKm = meters / 1000
            )
        }
    }

    fun updateLocationMode(mode: LocationMode) {
        viewModelScope.launch {
            // Location mode is not used in the new settings, so we ignore it
            // or you could add it to ReminderSettings if needed
        }
    }
    
    fun exportToCsv() {
        viewModelScope.launch {
            _uiState.value = SettingsUiState.Exporting
            
            try {
                val entries = workEntryDao.getByDateRange(
                    java.time.LocalDate.now().minusDays(365),
                    java.time.LocalDate.now()
                )
                val csv = generateCsv(entries)
                _uiState.value = SettingsUiState.ExportSuccess(csv)
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.ExportError(e.message ?: "Export fehlgeschlagen")
            }
        }
    }
    
    fun resetExportState() {
        _uiState.value = SettingsUiState.Initial
    }
    
    private fun generateCsv(entries: List<WorkEntry>): String {
        val csv = StringBuilder()
        csv.appendLine("Datum;Arbeitstag;Startzeit;Endzeit;Pause(min);Morgens-Check-in;Morgens-Ort;Morgens-Status;Abends-Check-in;Abends-Ort;Abends-Status;Reisestart;Reiseende;Ort Start;Ort Ende;Überprüfung erforderlich;Notiz")
        
        entries.forEach { entry ->
            csv.appendLine(
                listOf(
                    entry.date.toString(),
                    entry.dayType.name,
                    entry.workStart.toString(),
                    entry.workEnd.toString(),
                    entry.breakMinutes.toString(),
                    entry.morningCapturedAt?.toIsoDateTime() ?: "",
                    entry.morningLocationLabel ?: "",
                    entry.morningLocationStatus.name,
                    entry.eveningCapturedAt?.toIsoDateTime() ?: "",
                    entry.eveningLocationLabel ?: "",
                    entry.eveningLocationStatus.name,
                    entry.travelStartAt?.toIsoDateTime() ?: "",
                    entry.travelArriveAt?.toIsoDateTime() ?: "",
                    entry.travelLabelStart ?: "",
                    entry.travelLabelEnd ?: "",
                    entry.needsReview.toString(),
                    entry.note ?: ""
                ).joinToString(";")
            )
        }
        
        return csv.toString()
    }
    
    private fun Long.toIsoDateTime(): String {
        return java.time.Instant.ofEpochMilli(this)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDateTime()
            .toString()
    }
    
    private fun StringBuilder.appendLine(line: String) = appendLine(line, true)
    
    private fun StringBuilder.appendLine(line: String, newLine: Boolean): StringBuilder {
        append(line)
        if (newLine) append("\n")
        return this
    }
}

enum class LocationMode(val value: String, val displayName: String) {
    CHECK_IN_ONLY("check_in_only", "Nur beim Check-in"),
    BACKGROUND("background", "Hintergrund")
}

sealed class SettingsUiState {
    object Initial : SettingsUiState()
    object Exporting : SettingsUiState()
    data class ExportSuccess(val csv: String) : SettingsUiState()
    data class ExportError(val message: String) : SettingsUiState()
}
