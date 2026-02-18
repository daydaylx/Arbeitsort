package de.montagezeit.app.ui.screen.settings

import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.montagezeit.app.R
import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.preferences.ReminderSettingsManager
import de.montagezeit.app.export.PdfExporter
import de.montagezeit.app.notification.ReminderNotificationManager
import de.montagezeit.app.ui.util.UiText
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
    private val pdfExporter: PdfExporter,
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

    fun updateWorkStart(time: LocalTime) {
        viewModelScope.launch {
            reminderSettingsManager.updateSettings(workStart = time)
        }
    }

    fun updateWorkEnd(time: LocalTime) {
        viewModelScope.launch {
            reminderSettingsManager.updateSettings(workEnd = time)
        }
    }

    fun updateBreakMinutes(minutes: Int) {
        viewModelScope.launch {
            reminderSettingsManager.updateSettings(
                breakMinutes = minutes.coerceIn(0, 180)
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
            notificationManager.showDailyConfirmationNotification(LocalDate.now())
        }
    }

    fun resetExportState() {
        _uiState.value = SettingsUiState.Initial
    }
    
    /**
     * Exportiert PDF f�r den aktuellen Monat
     */
    fun exportPdfCurrentMonth() {
        viewModelScope.launch {
            _uiState.value = SettingsUiState.Exporting
            
            try {
                val settings = reminderSettingsManager.settings.first()
                
                // Name-Validierung
                if (settings.pdfEmployeeName.isNullOrBlank()) {
                    _uiState.value = SettingsUiState.ExportError(
                        UiText.StringResource(R.string.settings_error_name_missing)
                    )
                    return@launch
                }
                
                // Zeitraum: Aktueller Monat
                val now = LocalDate.now()
                val startDate = now.withDayOfMonth(1)
                val endDate = now.withDayOfMonth(now.lengthOfMonth())
                
                // Eintr�ge laden
                val entries = workEntryDao.getByDateRange(startDate, endDate)
                
                if (entries.isEmpty()) {
                    _uiState.value = SettingsUiState.ExportError(
                        UiText.StringResource(R.string.settings_error_no_entries_current_month)
                    )
                    return@launch
                }
                
                val fileUri = pdfExporter.exportToPdf(
                    entries = entries,
                    employeeName = settings.pdfEmployeeName,
                    company = settings.pdfCompany,
                    project = settings.pdfProject,
                    personnelNumber = settings.pdfPersonnelNumber,
                    startDate = startDate,
                    endDate = endDate
                )
                
                if (fileUri != null) {
                    _uiState.value = SettingsUiState.ExportSuccess(
                        fileUri = fileUri,
                        format = ExportFormat.PDF
                    )
                } else {
                    _uiState.value = SettingsUiState.ExportError(
                        UiText.StringResource(R.string.settings_error_pdf_export_failed)
                    )
                }
            } catch (e: Exception) {
                _uiState.value = buildExportError(
                    message = e.message,
                    fallbackRes = R.string.settings_error_export_failed
                )
            }
        }
    }
    
    /**
     * Exportiert PDF f�r die letzten 30 Tage
     */
    fun exportPdfLast30Days() {
        viewModelScope.launch {
            _uiState.value = SettingsUiState.Exporting
            
            try {
                val settings = reminderSettingsManager.settings.first()
                
                // Name-Validierung
                if (settings.pdfEmployeeName.isNullOrBlank()) {
                    _uiState.value = SettingsUiState.ExportError(
                        UiText.StringResource(R.string.settings_error_name_missing)
                    )
                    return@launch
                }
                
                // Zeitraum: Letzte 30 Tage
                val endDate = LocalDate.now()
                val startDate = endDate.minusDays(30)
                
                // Eintr�ge laden
                val entries = workEntryDao.getByDateRange(startDate, endDate)
                
                if (entries.isEmpty()) {
                    _uiState.value = SettingsUiState.ExportError(
                        UiText.StringResource(R.string.settings_error_no_entries_last_30_days)
                    )
                    return@launch
                }
                
                val fileUri = pdfExporter.exportToPdf(
                    entries = entries,
                    employeeName = settings.pdfEmployeeName,
                    company = settings.pdfCompany,
                    project = settings.pdfProject,
                    personnelNumber = settings.pdfPersonnelNumber,
                    startDate = startDate,
                    endDate = endDate
                )
                
                if (fileUri != null) {
                    _uiState.value = SettingsUiState.ExportSuccess(
                        fileUri = fileUri,
                        format = ExportFormat.PDF
                    )
                } else {
                    _uiState.value = SettingsUiState.ExportError(
                        UiText.StringResource(R.string.settings_error_pdf_export_failed)
                    )
                }
            } catch (e: Exception) {
                _uiState.value = buildExportError(
                    message = e.message,
                    fallbackRes = R.string.settings_error_export_failed
                )
            }
        }
    }
    
    /**
     * Exportiert PDF f�r einen benutzerdefinierten Zeitraum
     */
    fun exportPdfCustomRange(startDate: LocalDate, endDate: LocalDate) {
        viewModelScope.launch {
            _uiState.value = SettingsUiState.Exporting
            
            try {
                val settings = reminderSettingsManager.settings.first()
                
                // Name-Validierung
                if (settings.pdfEmployeeName.isNullOrBlank()) {
                    _uiState.value = SettingsUiState.ExportError(
                        UiText.StringResource(R.string.settings_error_name_missing)
                    )
                    return@launch
                }
                
                // Eintr�ge laden
                val entries = workEntryDao.getByDateRange(startDate, endDate)
                
                if (entries.isEmpty()) {
                    _uiState.value = SettingsUiState.ExportError(
                        UiText.StringResource(R.string.settings_error_no_entries_custom_range)
                    )
                    return@launch
                }
                
                val fileUri = pdfExporter.exportToPdf(
                    entries = entries,
                    employeeName = settings.pdfEmployeeName,
                    company = settings.pdfCompany,
                    project = settings.pdfProject,
                    personnelNumber = settings.pdfPersonnelNumber,
                    startDate = startDate,
                    endDate = endDate
                )
                
                if (fileUri != null) {
                    _uiState.value = SettingsUiState.ExportSuccess(
                        fileUri = fileUri,
                        format = ExportFormat.PDF
                    )
                } else {
                    _uiState.value = SettingsUiState.ExportError(
                        UiText.StringResource(R.string.settings_error_pdf_export_failed)
                    )
                }
            } catch (e: Exception) {
                _uiState.value = buildExportError(
                    message = e.message,
                    fallbackRes = R.string.settings_error_export_failed
                )
            }
        }
    }
    
    /**
     * Aktualisiert PDF-Settings
     */
    fun updatePdfSettings(
        employeeName: String?,
        company: String?,
        project: String?,
        personnelNumber: String?
    ) {
        viewModelScope.launch {
            reminderSettingsManager.updateSettings(
                pdfEmployeeName = employeeName.orEmpty(),
                pdfCompany = company.orEmpty(),
                pdfProject = project.orEmpty(),
                pdfPersonnelNumber = personnelNumber.orEmpty()
            )
        }
    }
}

private fun buildExportError(
    message: String?,
    @StringRes fallbackRes: Int
): SettingsUiState.ExportError {
    val normalizedMessage = message?.takeIf { it.isNotBlank() }
    return SettingsUiState.ExportError(
        normalizedMessage?.let(UiText::DynamicString)
            ?: UiText.StringResource(fallbackRes)
    )
}

enum class ExportFormat {
    PDF
}

sealed class SettingsUiState {
    object Initial : SettingsUiState()
    object Exporting : SettingsUiState()
    data class ExportSuccess(val fileUri: Uri, val format: ExportFormat) : SettingsUiState()
    data class ExportError(val message: UiText) : SettingsUiState()
}
