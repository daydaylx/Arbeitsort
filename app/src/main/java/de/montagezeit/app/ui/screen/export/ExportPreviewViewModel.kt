package de.montagezeit.app.ui.screen.export

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.montagezeit.app.R
import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.preferences.ReminderSettingsManager
import de.montagezeit.app.domain.util.TimeCalculator
import de.montagezeit.app.export.PdfExporter
import de.montagezeit.app.export.PdfUtilities
import de.montagezeit.app.ui.util.UiText
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

data class PreviewSummary(
    val workMinutes: Int,
    val travelMinutes: Int,
    val paidMinutes: Int
) {
    val workHours: Double = workMinutes / 60.0
    val travelHours: Double = travelMinutes / 60.0
    val paidHours: Double = paidMinutes / 60.0
}

internal fun calculatePreviewSummary(entries: List<WorkEntry>): PreviewSummary {
    val workMinutes = entries.sumOf { TimeCalculator.calculateWorkMinutes(it) }
    val travelMinutes = entries.sumOf { TimeCalculator.calculateTravelMinutes(it) }
    val paidMinutes = entries.sumOf { TimeCalculator.calculatePaidTotalMinutes(it) }
    return PreviewSummary(
        workMinutes = workMinutes,
        travelMinutes = travelMinutes,
        paidMinutes = paidMinutes
    )
}

sealed class PreviewState {
    data class List(
        val header: String,
        val totals: ExportPreviewTotals,
        val rows: kotlin.collections.List<ExportPreviewRow>
    ) : PreviewState()

    data class Empty(
        val header: String,
        val message: UiText
    ) : PreviewState()

    object CreatingPdf : PreviewState()

    data class PdfReady(
        val fileUri: Uri,
        val fileName: String
    ) : PreviewState()

    data class Error(
        val message: UiText,
        val canReturn: Boolean
    ) : PreviewState()
}

@HiltViewModel
class ExportPreviewViewModel @Inject constructor(
    private val workEntryDao: WorkEntryDao,
    private val reminderSettingsManager: ReminderSettingsManager,
    private val pdfExporter: PdfExporter
) : ViewModel() {

    private val _uiState = MutableStateFlow<PreviewState>(
        PreviewState.Empty(
            header = "",
            message = UiText.StringResource(R.string.export_preview_loading)
        )
    )
    val uiState: StateFlow<PreviewState> = _uiState.asStateFlow()

    private var currentRange: DateRange? = null
    private var lastPreviewState: PreviewState? = null

    fun loadRange(startDate: LocalDate, endDate: LocalDate) {
        currentRange = DateRange(startDate, endDate)
        refresh()
    }

    fun refresh() {
        // Lokale Kopie erstellen, um Race Condition zu vermeiden
        val range = currentRange
        if (range == null) {
            updateState(
                PreviewState.Error(
                    message = UiText.StringResource(R.string.export_preview_error_range_unavailable),
                    canReturn = false
                )
            )
            return
        }
        val header = buildHeader(range)
        updateState(
            PreviewState.Empty(
                header = header,
                message = UiText.StringResource(R.string.export_preview_loading)
            )
        )
        viewModelScope.launch {
            try {
                val entries = workEntryDao.getByDateRange(range.start, range.end)
                if (entries.isEmpty()) {
                    updateState(
                        PreviewState.Empty(
                            header = header,
                            message = UiText.StringResource(R.string.export_preview_empty_range)
                        )
                    )
                } else {
                    updateState(
                        PreviewState.List(
                            header = header,
                            totals = buildTotals(entries),
                            rows = entries.map { buildRow(it) }
                        )
                    )
                }
            } catch (e: Exception) {
                updateState(
                    PreviewState.Error(
                        message = toUiText(e.message, R.string.export_preview_error_export_failed),
                        canReturn = false
                    )
                )
            }
        }
    }

    fun createPdf() {
        // Lokale Kopie erstellen, um Race Condition zu vermeiden
        val range = currentRange
        if (range == null) {
            updateState(
                PreviewState.Error(
                    message = UiText.StringResource(R.string.export_preview_error_range_unavailable),
                    canReturn = true
                )
            )
            return
        }
        viewModelScope.launch {
            val settings = reminderSettingsManager.settings.first()
            if (settings.pdfEmployeeName.isNullOrBlank()) {
                updateState(
                    PreviewState.Error(
                        message = UiText.StringResource(R.string.export_preview_error_name_missing_profile),
                        canReturn = true
                    )
                )
                return@launch
            }
            updateState(PreviewState.CreatingPdf)
            try {
                val entries = workEntryDao.getByDateRange(range.start, range.end)
                if (entries.isEmpty()) {
                    updateState(
                        PreviewState.Error(
                            message = UiText.StringResource(R.string.export_preview_empty_range),
                            canReturn = true
                        )
                    )
                    return@launch
                }
                val fileUri = pdfExporter.exportToPdf(
                    entries = entries,
                    employeeName = settings.pdfEmployeeName,
                    company = settings.pdfCompany,
                    project = settings.pdfProject,
                    personnelNumber = settings.pdfPersonnelNumber,
                    startDate = range.start,
                    endDate = range.end
                )
                if (fileUri != null) {
                    val fileName = fileUri.lastPathSegment ?: "montagezeit_export.pdf"
                    updateState(PreviewState.PdfReady(fileUri = fileUri, fileName = fileName))
                } else {
                    updateState(
                        PreviewState.Error(
                            message = UiText.StringResource(R.string.export_preview_error_pdf_create_failed),
                            canReturn = true
                        )
                    )
                }
            } catch (e: Exception) {
                updateState(
                    PreviewState.Error(
                        message = toUiText(e.message, R.string.export_preview_error_export_failed),
                        canReturn = true
                    )
                )
            }
        }
    }

    fun returnToPreview() {
        val cached = lastPreviewState
        if (cached != null) {
            _uiState.value = cached
        } else {
            refresh()
        }
    }

    private fun updateState(state: PreviewState) {
        _uiState.value = state
        if (state is PreviewState.List || state is PreviewState.Empty) {
            lastPreviewState = state
        }
    }

    private fun buildHeader(range: DateRange): String {
        return "${PdfUtilities.formatDate(range.start)} – ${PdfUtilities.formatDate(range.end)}"
    }

    private fun buildTotals(entries: List<WorkEntry>): ExportPreviewTotals {
        val summary = calculatePreviewSummary(entries)
        return ExportPreviewTotals(
            workHours = formatMinutes(summary.workMinutes),
            travelHours = formatMinutes(summary.travelMinutes),
            paidHours = formatMinutes(summary.paidMinutes)
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

    private data class DateRange(val start: LocalDate, val end: LocalDate)
}

private fun toUiText(message: String?, fallbackRes: Int): UiText {
    return message
        ?.takeIf { it.isNotBlank() }
        ?.let(UiText::DynamicString)
        ?: UiText.StringResource(fallbackRes)
}
