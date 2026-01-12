package de.montagezeit.app.ui.screen.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.preferences.ReminderSettingsManager
import de.montagezeit.app.export.CsvExporter
import de.montagezeit.app.export.JsonExporter
import de.montagezeit.app.notification.ReminderNotificationManager
import de.montagezeit.app.work.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val reminderSettingsManager: ReminderSettingsManager,
    private val workEntryDao: WorkEntryDao,
    private val csvExporter: CsvExporter,
    private val jsonExporter: JsonExporter,
    private val reminderScheduler: ReminderScheduler,
    private val notificationManager: ReminderNotificationManager
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
            reminderScheduler.scheduleAll()
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
            reminderScheduler.scheduleAll()
        }
    }

    fun updateRadiusMeters(meters: Int) {
        viewModelScope.launch {
            reminderSettingsManager.updateSettings(
                locationRadiusKm = meters / 1000
            )
        }
    }

    fun updateMorningReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            reminderSettingsManager.updateSettings(morningReminderEnabled = enabled)
            reminderScheduler.scheduleAll()
        }
    }

    fun updateEveningReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            reminderSettingsManager.updateSettings(eveningReminderEnabled = enabled)
            reminderScheduler.scheduleAll()
        }
    }

    fun updateFallbackReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            reminderSettingsManager.updateSettings(fallbackEnabled = enabled)
            reminderScheduler.scheduleAll()
        }
    }

    fun updateFallbackTime(time: LocalTime) {
        viewModelScope.launch {
            reminderSettingsManager.updateSettings(fallbackTime = time)
            reminderScheduler.scheduleAll()
        }
    }

    fun updateDailyReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            reminderSettingsManager.updateSettings(dailyReminderEnabled = enabled)
            reminderScheduler.scheduleAll()
        }
    }

    fun updateDailyReminderTime(time: LocalTime) {
        viewModelScope.launch {
            reminderSettingsManager.updateSettings(dailyReminderTime = time)
            reminderScheduler.scheduleAll()
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun updateLocationMode(mode: LocationMode) {
        viewModelScope.launch {
            // Location mode is not used in the new settings, so we ignore it
            // or you could add it to ReminderSettings if needed
        }
    }

    fun updateAutoOffWeekends(enabled: Boolean) {
        viewModelScope.launch {
            reminderSettingsManager.updateSettings(autoOffWeekends = enabled)
        }
    }

    fun updateAutoOffHolidays(enabled: Boolean) {
        viewModelScope.launch {
            reminderSettingsManager.updateSettings(autoOffHolidays = enabled)
        }
    }

    fun addHolidayDate(date: LocalDate) {
        viewModelScope.launch {
            val settings = reminderSettingsManager.settings.first()
            val updated = settings.holidayDates + date
            reminderSettingsManager.updateSettings(holidayDates = updated)
        }
    }

    fun removeHolidayDate(date: LocalDate) {
        viewModelScope.launch {
            val settings = reminderSettingsManager.settings.first()
            val updated = settings.holidayDates - date
            reminderSettingsManager.updateSettings(holidayDates = updated)
        }
    }

    fun sendTestReminder() {
        viewModelScope.launch {
            notificationManager.showDailyReminder(LocalDate.now())
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
                val fileUri = csvExporter.exportToCsv(entries)
                if (fileUri != null) {
                    _uiState.value = SettingsUiState.ExportSuccess(
                        fileUri = fileUri,
                        format = ExportFormat.CSV
                    )
                } else {
                    _uiState.value = SettingsUiState.ExportError("CSV Export fehlgeschlagen")
                }
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.ExportError(e.message ?: "Export fehlgeschlagen")
            }
        }
    }

    fun exportToJson() {
        viewModelScope.launch {
            _uiState.value = SettingsUiState.Exporting

            try {
                val entries = workEntryDao.getByDateRange(
                    java.time.LocalDate.now().minusDays(365),
                    java.time.LocalDate.now()
                )
                val fileUri = jsonExporter.exportToJson(entries)
                if (fileUri != null) {
                    _uiState.value = SettingsUiState.ExportSuccess(
                        fileUri = fileUri,
                        format = ExportFormat.JSON
                    )
                } else {
                    _uiState.value = SettingsUiState.ExportError("JSON Export fehlgeschlagen")
                }
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.ExportError(e.message ?: "Export fehlgeschlagen")
            }
        }
    }

    fun resetExportState() {
        _uiState.value = SettingsUiState.Initial
    }
}

enum class LocationMode(val value: String, val displayName: String) {
    CHECK_IN_ONLY("check_in_only", "Nur beim Check-in"),
    BACKGROUND("background", "Hintergrund")
}

enum class ExportFormat {
    CSV,
    JSON
}

sealed class SettingsUiState {
    object Initial : SettingsUiState()
    object Exporting : SettingsUiState()
    data class ExportSuccess(val fileUri: Uri, val format: ExportFormat) : SettingsUiState()
    data class ExportError(val message: String) : SettingsUiState()
}
