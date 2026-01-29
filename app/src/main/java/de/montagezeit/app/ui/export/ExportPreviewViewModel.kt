package de.montagezeit.app.ui.export

import androidx.lifecycle.ViewModel
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.export.PdfUtilities

/**
 * ViewModel für die PDF-Vorschau-Summen (Arbeits-/Reise-/Gesamtzeit).
 */
class ExportPreviewViewModel : ViewModel() {

    fun calculateSummary(entries: List<WorkEntry>): ExportPreviewSummary {
        val workHours = PdfUtilities.sumWorkHours(entries)
        val travelMinutes = PdfUtilities.sumTravelMinutes(entries)
        val travelHours = travelMinutes / 60.0
        val paidHours = workHours + travelHours
        return ExportPreviewSummary(
            workHours = workHours,
            travelMinutes = travelMinutes,
            travelHours = travelHours,
            paidHours = paidHours
        )
    }
}

data class ExportPreviewSummary(
    val workHours: Double,
    val travelMinutes: Int,
    val travelHours: Double,
    val paidHours: Double
)
