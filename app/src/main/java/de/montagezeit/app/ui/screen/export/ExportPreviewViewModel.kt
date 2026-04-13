package de.montagezeit.app.ui.screen.export

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.montagezeit.app.R
import de.montagezeit.app.data.local.entity.TravelLeg
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.local.entity.WorkEntryWithTravelLegs
import de.montagezeit.app.data.preferences.ReminderSettingsManager
import de.montagezeit.app.data.repository.WorkEntryRepository
import de.montagezeit.app.domain.usecase.isStatisticsEligible
import de.montagezeit.app.domain.util.MealAllowanceCalculator
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
    val paidHours: String,
    val mealAllowanceTotal: String
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
    val locationNote: String?,
    val mealAllowanceLabel: String?
)

data class PreviewSummary(
    val workMinutes: Int,
    val travelMinutes: Int,
    val paidMinutes: Int,
    val mealAllowanceCents: Int
) {
    val workHours: Double = workMinutes / 60.0
    val travelHours: Double = travelMinutes / 60.0
    val paidHours: Double = paidMinutes / 60.0
}

internal fun calculatePreviewSummary(entries: List<WorkEntryWithTravelLegs>): PreviewSummary {
    val eligibleEntries = entries.filter(::isStatisticsEligible)
    val workMinutes = eligibleEntries.sumOf { TimeCalculator.calculateWorkMinutes(it.workEntry) }
    val travelMinutes = eligibleEntries.sumOf { TimeCalculator.calculateTravelMinutes(it.orderedTravelLegs) }
    val paidMinutes = eligibleEntries.sumOf { TimeCalculator.calculatePaidTotalMinutes(it.workEntry, it.orderedTravelLegs) }
    val mealAllowanceCents = eligibleEntries.sumOf {
        MealAllowanceCalculator.resolveEffectiveStoredSnapshot(it).amountCents
    }
    return PreviewSummary(
        workMinutes = workMinutes,
        travelMinutes = travelMinutes,
        paidMinutes = paidMinutes,
        mealAllowanceCents = mealAllowanceCents
    )
}

internal fun buildExportPreviewTotals(summary: PreviewSummary): ExportPreviewTotals {
    fun formatMinutes(minutes: Int): String = "${TimeCalculator.formatMinutesAsHours(minutes)} h"

    return ExportPreviewTotals(
        workHours = formatMinutes(summary.workMinutes),
        travelHours = formatMinutes(summary.travelMinutes),
        paidHours = formatMinutes(summary.paidMinutes),
        mealAllowanceTotal = MealAllowanceCalculator.formatEuro(summary.mealAllowanceCents)
    )
}

internal fun buildExportPreviewRow(record: WorkEntryWithTravelLegs): ExportPreviewRow {
    val entry = record.workEntry
    val workMinutes = TimeCalculator.calculateWorkMinutes(entry)
    val travelMinutes = TimeCalculator.calculateTravelMinutes(record.orderedTravelLegs)
    val paidMinutes = TimeCalculator.calculatePaidTotalMinutes(entry, record.orderedTravelLegs)
    val mealSnapshot = MealAllowanceCalculator.resolveEffectiveStoredSnapshot(record)
    val showWorkSchedule = entry.dayType == de.montagezeit.app.data.local.entity.DayType.WORK && entry.workStart != null && entry.workEnd != null
    val mealLabel = if (mealSnapshot.amountCents > 0) {
        MealAllowanceCalculator.formatEuro(mealSnapshot.amountCents)
    } else {
        null
    }
    return ExportPreviewRow(
        date = entry.date,
        dateLabel = PdfUtilities.formatDate(entry.date),
        startLabel = if (showWorkSchedule) PdfUtilities.formatTime(entry.workStart).ifBlank { PREVIEW_DASH } else PREVIEW_DASH,
        endLabel = if (showWorkSchedule) PdfUtilities.formatTime(entry.workEnd).ifBlank { PREVIEW_DASH } else PREVIEW_DASH,
        breakLabel = if (showWorkSchedule) "${entry.breakMinutes} min" else PREVIEW_DASH,
        workLabel = buildPreviewMinutesLabel(workMinutes),
        travelLabel = buildPreviewMinutesLabel(travelMinutes),
        totalLabel = buildPreviewMinutesLabel(paidMinutes),
        locationNote = buildPreviewLocationNote(entry, record.orderedTravelLegs),
        mealAllowanceLabel = mealLabel
    )
}

private fun buildPreviewMinutesLabel(minutes: Int): String {
    return "${TimeCalculator.formatMinutesAsHours(minutes)} h"
}

private const val PREVIEW_DASH = "–"

private fun buildPreviewLocationNote(entry: WorkEntry, travelLegs: List<TravelLeg>): String? {
    val location = PdfUtilities.getLocation(entry, travelLegs).trim().takeIf { it.isNotBlank() }
    val route = PdfUtilities.buildTravelRouteSummary(travelLegs).trim().takeIf { it.isNotBlank() }
    val note = PdfUtilities.getNote(entry).trim().takeIf { it.isNotBlank() }
    val combined = listOfNotNull(location, route, note).joinToString(" • ")
    if (combined.isBlank()) return null
    val maxLength = 48
    return if (combined.length > maxLength) {
        combined.take(maxLength - 1) + "…"
    } else {
        combined
    }
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
    private val workEntryRepository: WorkEntryRepository,
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
    private var cachedPreviewEntries: CachedPreviewEntries? = null

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
        cachedPreviewEntries = null
        updateState(
            PreviewState.Empty(
                header = header,
                message = UiText.StringResource(R.string.export_preview_loading)
            )
        )
        viewModelScope.launch {
            try {
                val entries = loadEntries(range)
                updateState(buildPreviewState(header, entries))
            } catch (e: Exception) {
                cachedPreviewEntries = null
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
                val entries = getEntriesForPdf(range)
                if (entries.isEmpty()) {
                    updateState(
                        PreviewState.Error(
                            message = UiText.StringResource(R.string.export_preview_empty_range),
                            canReturn = true
                        )
                    )
                    return@launch
                }
                when (val exportResult = pdfExporter.exportToPdf(
                    entries = entries,
                    employeeName = settings.pdfEmployeeName,
                    company = settings.pdfCompany,
                    project = settings.pdfProject,
                    personnelNumber = settings.pdfPersonnelNumber,
                    startDate = range.start,
                    endDate = range.end
                )) {
                    is PdfExporter.PdfExportResult.Success -> {
                        val fileName = exportResult.fileUri.lastPathSegment ?: "montagezeit_export.pdf"
                        updateState(PreviewState.PdfReady(fileUri = exportResult.fileUri, fileName = fileName))
                    }
                    is PdfExporter.PdfExportResult.ValidationError -> {
                        updateState(
                            PreviewState.Error(
                                message = toUiText(exportResult.message, R.string.export_preview_error_export_failed),
                                canReturn = true
                            )
                        )
                    }
                    is PdfExporter.PdfExportResult.StorageError -> {
                        updateState(
                            PreviewState.Error(
                                message = toUiText(exportResult.message, R.string.export_preview_error_pdf_create_failed),
                                canReturn = true
                            )
                        )
                    }
                    is PdfExporter.PdfExportResult.FileWriteError -> {
                        updateState(
                            PreviewState.Error(
                                message = toUiText(exportResult.message, R.string.export_preview_error_pdf_create_failed),
                                canReturn = true
                            )
                        )
                    }
                    is PdfExporter.PdfExportResult.UnknownError -> {
                        updateState(
                            PreviewState.Error(
                                message = toUiText(exportResult.message, R.string.export_preview_error_export_failed),
                                canReturn = true
                            )
                        )
                    }
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

    private suspend fun loadEntries(range: DateRange): List<WorkEntryWithTravelLegs> {
        val entries = workEntryRepository.getByDateRangeWithTravel(range.start, range.end)
            .filter(::isStatisticsEligible)
        cachedPreviewEntries = CachedPreviewEntries(range = range, entries = entries)
        return entries
    }

    private suspend fun getEntriesForPdf(range: DateRange): List<WorkEntryWithTravelLegs> {
        return cachedPreviewEntries
            ?.takeIf { it.range == range }
            ?.entries
            ?: loadEntries(range)
    }

    private fun buildPreviewState(
        header: String,
        entries: List<WorkEntryWithTravelLegs>
    ): PreviewState {
        return if (entries.isEmpty()) {
            PreviewState.Empty(
                header = header,
                message = UiText.StringResource(R.string.export_preview_empty_range)
            )
        } else {
            PreviewState.List(
                header = header,
                totals = buildTotals(entries),
                rows = entries.map(::buildRow)
            )
        }
    }

    private fun buildTotals(entries: List<WorkEntryWithTravelLegs>): ExportPreviewTotals {
        val summary = calculatePreviewSummary(entries)
        return buildExportPreviewTotals(summary)
    }

    private fun buildRow(entry: WorkEntryWithTravelLegs): ExportPreviewRow {
        return buildExportPreviewRow(entry)
    }

    private data class DateRange(val start: LocalDate, val end: LocalDate)

    private data class CachedPreviewEntries(
        val range: DateRange,
        val entries: List<WorkEntryWithTravelLegs>
    )
}

private fun toUiText(message: String?, fallbackRes: Int): UiText {
    return message
        ?.takeIf { it.isNotBlank() }
        ?.let(UiText::DynamicString)
        ?: UiText.StringResource(fallbackRes)
}
