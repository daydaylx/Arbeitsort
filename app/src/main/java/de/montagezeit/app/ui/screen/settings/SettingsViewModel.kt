package de.montagezeit.app.ui.screen.settings

import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.montagezeit.app.R
import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.WorkEntryWithTravelLegs
import de.montagezeit.app.data.preferences.ReminderSettingsManager
import de.montagezeit.app.export.CsvExporter
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
    private val csvExporter: CsvExporter,
    private val reminderScheduler: ReminderScheduler,
    private val notificationManager: ReminderNotificationManager
) : ViewModel() {
    
    val reminderSettings = reminderSettingsManager.settings
    
    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Initial)
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<UiText?>(null)
    val snackbarMessage: StateFlow<UiText?> = _snackbarMessage.asStateFlow()

    fun onSnackbarShown() {
        _snackbarMessage.value = null
    }

    fun updateMorningWindow(startHour: Int, startMinute: Int, endHour: Int, endMinute: Int) {
        viewModelScope.launch {
            val startTime = LocalTime.of(startHour, startMinute)
            val endTime = LocalTime.of(endHour, endMinute)
            if (!isValidWindow(startTime, endTime)) {
                _uiState.value = SettingsUiState.ReminderError(
                    UiText.StringResource(R.string.error_time_range_invalid)
                )
                return@launch
            }
            reminderSettingsManager.updateSettings(
                morningWindowStart = startTime,
                morningWindowEnd = endTime
            )
            _uiState.value = SettingsUiState.Initial
            reminderScheduler.scheduleAll()
        }
    }

    fun updateEveningWindow(startHour: Int, startMinute: Int, endHour: Int, endMinute: Int) {
        viewModelScope.launch {
            val startTime = LocalTime.of(startHour, startMinute)
            val endTime = LocalTime.of(endHour, endMinute)
            if (!isValidWindow(startTime, endTime)) {
                _uiState.value = SettingsUiState.ReminderError(
                    UiText.StringResource(R.string.error_time_range_invalid)
                )
                return@launch
            }
            reminderSettingsManager.updateSettings(
                eveningWindowStart = startTime,
                eveningWindowEnd = endTime
            )
            _uiState.value = SettingsUiState.Initial
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
            if (date in settings.holidayDates) return@launch
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
    
    fun exportPdfCurrentMonth() {
        exportCurrentMonth(ExportFormat.PDF)
    }

    fun exportCsvCurrentMonth() {
        exportCurrentMonth(ExportFormat.CSV)
    }

    fun exportPdfLast30Days() {
        exportLast30Days(ExportFormat.PDF)
    }

    fun exportCsvLast30Days() {
        exportLast30Days(ExportFormat.CSV)
    }

    fun exportPdfCustomRange(startDate: LocalDate, endDate: LocalDate) {
        exportRange(
            format = ExportFormat.PDF,
            startDate = startDate,
            endDate = endDate,
            emptyRangeRes = R.string.settings_error_no_entries_custom_range
        )
    }

    fun exportCsvCustomRange(startDate: LocalDate, endDate: LocalDate) {
        exportRange(
            format = ExportFormat.CSV,
            startDate = startDate,
            endDate = endDate,
            emptyRangeRes = R.string.settings_error_no_entries_custom_range
        )
    }

    private fun exportCurrentMonth(format: ExportFormat) {
        val now = LocalDate.now()
        exportRange(
            format = format,
            startDate = now.withDayOfMonth(1),
            endDate = now.withDayOfMonth(now.lengthOfMonth()),
            emptyRangeRes = R.string.settings_error_no_entries_current_month
        )
    }

    private fun exportLast30Days(format: ExportFormat) {
        val endDate = LocalDate.now()
        exportRange(
            format = format,
            startDate = endDate.minusDays(29),
            endDate = endDate,
            emptyRangeRes = R.string.settings_error_no_entries_last_30_days
        )
    }

    private fun exportRange(
        format: ExportFormat,
        startDate: LocalDate,
        endDate: LocalDate,
        @StringRes emptyRangeRes: Int
    ) {
        viewModelScope.launch {
            _uiState.value = SettingsUiState.Exporting

            try {
                val settings = reminderSettingsManager.settings.first()
                if (settings.pdfEmployeeName.isNullOrBlank()) {
                    _uiState.value = SettingsUiState.ExportError(
                        UiText.StringResource(R.string.settings_error_name_missing)
                    )
                    return@launch
                }
                val employeeName = settings.pdfEmployeeName
                val entries = workEntryDao.getByDateRangeWithTravel(startDate, endDate)
                if (entries.isEmpty()) {
                    _uiState.value = SettingsUiState.ExportError(
                        UiText.StringResource(emptyRangeRes)
                    )
                    return@launch
                }

                _uiState.value = when (format) {
                    ExportFormat.PDF -> exportPdf(
                        entries = entries,
                        employeeName = employeeName,
                        company = settings.pdfCompany,
                        project = settings.pdfProject,
                        personnelNumber = settings.pdfPersonnelNumber,
                        startDate = startDate,
                        endDate = endDate
                    )
                    ExportFormat.CSV -> exportCsv(entries)
                }
            } catch (e: Exception) {
                _uiState.value = buildExportError(
                    message = e.message,
                    fallbackRes = when (format) {
                        ExportFormat.PDF -> R.string.settings_error_export_failed
                        ExportFormat.CSV -> R.string.settings_error_csv_export_failed
                    }
                )
            }
        }
    }

    private fun exportPdf(
        entries: List<WorkEntryWithTravelLegs>,
        employeeName: String,
        company: String?,
        project: String?,
        personnelNumber: String?,
        startDate: LocalDate,
        endDate: LocalDate
    ): SettingsUiState {
        return when (
            val exportResult = pdfExporter.exportToPdf(
                entries = entries,
                employeeName = employeeName,
                company = company,
                project = project,
                personnelNumber = personnelNumber,
                startDate = startDate,
                endDate = endDate
            )
        ) {
            is PdfExporter.PdfExportResult.Success -> {
                SettingsUiState.ExportSuccess(
                    fileUri = exportResult.fileUri,
                    format = ExportFormat.PDF
                )
            }
            else -> mapPdfExportError(exportResult)
        }
    }

    private fun exportCsv(entries: List<WorkEntryWithTravelLegs>): SettingsUiState {
        val fileUri = csvExporter.exportToCsv(entries)
        return if (fileUri != null) {
            SettingsUiState.ExportSuccess(
                fileUri = fileUri,
                format = ExportFormat.CSV
            )
        } else {
            buildExportError(
                message = null,
                fallbackRes = R.string.settings_error_csv_export_failed
            )
        }
    }

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

    /**
     * Aktualisiert das tägliche Überstunden-Ziel
     */
    fun updateDailyTargetHours(hours: Double) {
        viewModelScope.launch {
            reminderSettingsManager.updateSettings(
                dailyTargetHours = hours.coerceIn(0.5, 24.0)
            )
        }
    }

    /**
     * Aktualisiert das wöchentliche Überstunden-Ziel
     */
    fun updateWeeklyTargetHours(hours: Double) {
        viewModelScope.launch {
            reminderSettingsManager.updateSettings(
                weeklyTargetHours = hours.coerceIn(1.0, 168.0)
            )
        }
    }

    /**
     * Aktualisiert das monatliche Überstunden-Ziel
     */
    fun updateMonthlyTargetHours(hours: Double) {
        viewModelScope.launch {
            reminderSettingsManager.updateSettings(
                monthlyTargetHours = hours.coerceIn(1.0, 744.0)
            )
        }
    }

    private fun isValidWindow(start: LocalTime, end: LocalTime): Boolean = end.isAfter(start)

    private fun mapPdfExportError(result: PdfExporter.PdfExportResult): SettingsUiState.ExportError {
        return when (result) {
            is PdfExporter.PdfExportResult.Success -> error("Success must be handled separately")
            is PdfExporter.PdfExportResult.ValidationError -> buildExportError(
                message = result.message,
                fallbackRes = R.string.settings_error_pdf_export_failed
            )
            is PdfExporter.PdfExportResult.StorageError -> buildExportError(
                message = result.message,
                fallbackRes = R.string.export_preview_error_pdf_create_failed
            )
            is PdfExporter.PdfExportResult.FileWriteError -> buildExportError(
                message = result.message,
                fallbackRes = R.string.settings_error_pdf_export_failed
            )
            is PdfExporter.PdfExportResult.UnknownError -> buildExportError(
                message = result.message,
                fallbackRes = R.string.settings_error_export_failed
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
    PDF,
    CSV
}

sealed class SettingsUiState {
    object Initial : SettingsUiState()
    object Exporting : SettingsUiState()
    data class ExportSuccess(val fileUri: Uri, val format: ExportFormat) : SettingsUiState()
    data class ExportError(val message: UiText) : SettingsUiState()
    data class ReminderError(val message: UiText) : SettingsUiState()
}
