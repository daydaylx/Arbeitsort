package de.montagezeit.app.ui.screen.export

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.preferences.ReminderSettingsManager
import de.montagezeit.app.domain.util.TimeCalculator
import de.montagezeit.app.export.PdfExporter
import de.montagezeit.app.export.PdfUtilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class ExportPreviewTotals(
    val workHours: String,
    val travelHours: String,
    val paidHours: String
)

data class ExportPreviewRow(
    val date: LocalDate,
    val dateLabel: String,
    val startLabel: String,
    val endLabel: String,
    val breakLabel: String,
    val workLabel: String,
    val travelLabel: String,
    val totalLabel: String,
    val locationNote: String?
)

sealed class ExportPreviewExportStatus {
    object Idle : ExportPreviewExportStatus()
    object Exporting : ExportPreviewExportStatus()
    data class Success(val fileUri: Uri) : ExportPreviewExportStatus()
    data class Error(val message: String) : ExportPreviewExportStatus()
}

sealed class ExportPreviewUiState {
    object Loading : ExportPreviewUiState()
    data class Error(val message: String) : ExportPreviewUiState()
    data class Empty(
        val header: String,
        val isNameMissing: Boolean,
        val exportStatus: ExportPreviewExportStatus = ExportPreviewExportStatus.Idle
    ) : ExportPreviewUiState()
    data class Content(
        val header: String,
        val totals: ExportPreviewTotals,
        val rows: List<ExportPreviewRow>,
        val isNameMissing: Boolean,
        val exportStatus: ExportPreviewExportStatus = ExportPreviewExportStatus.Idle
    ) : ExportPreviewUiState()
}

@HiltViewModel
class ExportPreviewViewModel @Inject constructor(
    private val workEntryDao: WorkEntryDao,
    private val reminderSettingsManager: ReminderSettingsManager,
    private val pdfExporter: PdfExporter,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val startDate = savedStateHandle.get<String>("startDate")?.let(LocalDate::parse)
    private val endDate = savedStateHandle.get<String>("endDate")?.let(LocalDate::parse)

    private val _uiState = MutableStateFlow<ExportPreviewUiState>(ExportPreviewUiState.Loading)
    val uiState: StateFlow<ExportPreviewUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = ExportPreviewUiState.Loading
            try {
                val start = startDate
                val end = endDate
                if (start == null || end == null) {
                    _uiState.value = ExportPreviewUiState.Error("Zeitraum konnte nicht geladen werden.")
                    return@launch
                }
                val entries = workEntryDao.getByDateRange(start, end)
                val settings = reminderSettingsManager.settings.first()
                val isNameMissing = settings.pdfEmployeeName.isNullOrBlank()
                val header = "${PdfUtilities.formatDate(start)} – ${PdfUtilities.formatDate(end)}"
                if (entries.isEmpty()) {
                    _uiState.value = ExportPreviewUiState.Empty(header = header, isNameMissing = isNameMissing)
                    return@launch
                }
                _uiState.value = ExportPreviewUiState.Content(
                    header = header,
                    totals = buildTotals(entries),
                    rows = entries.map { buildRow(it) },
                    isNameMissing = isNameMissing
                )
            } catch (e: Exception) {
                _uiState.value = ExportPreviewUiState.Error(e.message ?: "Export fehlgeschlagen")
            }
        }
    }

    fun createPdf() {
        viewModelScope.launch {
            val currentStart = startDate
            val currentEnd = endDate
            if (currentStart == null || currentEnd == null) {
                updateExportStatus(ExportPreviewExportStatus.Error("Zeitraum konnte nicht geladen werden."))
                return@launch
            }
            val settings = reminderSettingsManager.settings.first()
            if (settings.pdfEmployeeName.isNullOrBlank()) {
                updateExportStatus(ExportPreviewExportStatus.Error("Name fehlt. Bitte zuerst in den Settings eingeben."))
                return@launch
            }
            updateExportStatus(ExportPreviewExportStatus.Exporting)
            try {
                val entries = workEntryDao.getByDateRange(currentStart, currentEnd)
                if (entries.isEmpty()) {
                    updateExportStatus(ExportPreviewExportStatus.Error("Keine Einträge im Zeitraum"))
                    return@launch
                }
                val fileUri = pdfExporter.exportToPdf(
                    entries = entries,
                    employeeName = settings.pdfEmployeeName,
                    company = settings.pdfCompany,
                    project = settings.pdfProject,
                    personnelNumber = settings.pdfPersonnelNumber,
                    startDate = currentStart,
                    endDate = currentEnd
                )
                if (fileUri != null) {
                    updateExportStatus(ExportPreviewExportStatus.Success(fileUri))
                } else {
                    updateExportStatus(ExportPreviewExportStatus.Error("Export fehlgeschlagen"))
                }
            } catch (e: Exception) {
                updateExportStatus(ExportPreviewExportStatus.Error(e.message ?: "Export fehlgeschlagen"))
            }
        }
    }

    fun clearExportStatus() {
        updateExportStatus(ExportPreviewExportStatus.Idle)
    }

    private fun updateExportStatus(status: ExportPreviewExportStatus) {
        val state = _uiState.value
        _uiState.value = when (state) {
            is ExportPreviewUiState.Content -> state.copy(exportStatus = status)
            is ExportPreviewUiState.Empty -> state.copy(exportStatus = status)
            else -> state
        }
    }

    private fun buildTotals(entries: List<WorkEntry>): ExportPreviewTotals {
        val workMinutes = entries.sumOf { TimeCalculator.calculateWorkMinutes(it) }
        val travelMinutes = entries.sumOf { TimeCalculator.calculateTravelMinutes(it) }
        val paidMinutes = entries.sumOf { TimeCalculator.calculatePaidTotalMinutes(it) }
        return ExportPreviewTotals(
            workHours = formatMinutes(workMinutes),
            travelHours = formatMinutes(travelMinutes),
            paidHours = formatMinutes(paidMinutes)
        )
    }

    private fun buildRow(entry: WorkEntry): ExportPreviewRow {
        val workMinutes = TimeCalculator.calculateWorkMinutes(entry)
        val travelMinutes = TimeCalculator.calculateTravelMinutes(entry)
        val paidMinutes = TimeCalculator.calculatePaidTotalMinutes(entry)
        return ExportPreviewRow(
            date = entry.date,
            dateLabel = PdfUtilities.formatDate(entry.date),
            startLabel = PdfUtilities.formatTime(entry.workStart),
            endLabel = PdfUtilities.formatTime(entry.workEnd),
            breakLabel = "${entry.breakMinutes} min",
            workLabel = formatMinutes(workMinutes),
            travelLabel = formatMinutes(travelMinutes),
            totalLabel = formatMinutes(paidMinutes),
            locationNote = formatLocationNote(entry)
        )
    }

    private fun formatMinutes(minutes: Int): String {
        return "${TimeCalculator.formatMinutesAsHours(minutes)} h"
    }

    private fun formatLocationNote(entry: WorkEntry): String? {
        val location = PdfUtilities.getLocation(entry).trim().takeIf { it.isNotBlank() }
        val note = PdfUtilities.getNote(entry).trim().takeIf { it.isNotBlank() }
        val combined = listOfNotNull(location, note).joinToString(" • ")
        if (combined.isBlank()) return null
        val maxLength = 48
        return if (combined.length > maxLength) {
            combined.take(maxLength - 1) + "…"
        } else {
            combined
        }
    }
}
