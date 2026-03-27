package de.montagezeit.app.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.StatFs
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import de.montagezeit.app.R
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntryWithTravelLegs
import de.montagezeit.app.domain.util.MealAllowanceCalculator
import de.montagezeit.app.domain.util.TimeCalculator
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PDF Exporter für MontageZeit
 * 
 * Exportiert WorkEntries im PDF-Format (A4) für die Verwendung als Arbeitsnachweis
 * Nutzt android.graphics.pdf.PdfDocument (keine externen PDF-Libs)
 */
@Singleton
class PdfExporter @Inject constructor(
    @ApplicationContext private val context: Context
) {

    sealed interface PdfExportResult {
        data class Success(val fileUri: Uri) : PdfExportResult
        data class ValidationError(val message: String) : PdfExportResult
        data class StorageError(val message: String) : PdfExportResult
        data class FileWriteError(val message: String) : PdfExportResult
        data class UnknownError(val message: String) : PdfExportResult
    }
    
    // PDF-Konstanten (A4 in Punkten: 595 x 842)
    companion object {
        // Safety limit: PdfDocument keeps all page canvases in memory until writeTo().
        // On low-end devices (< 2 GB RAM), rendering 180+ entries can cause an OOM.
        const val MAX_ENTRIES_PER_PDF = 180

        private const val PAGE_WIDTH = 595
        private const val PAGE_HEIGHT = 842
        private const val MARGIN = 40
        private const val CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN
        
        // Zeilenhöhen und Abstände
        private const val HEADER_HEIGHT = 100
        private const val TABLE_HEADER_HEIGHT = 30
        private const val ROW_HEIGHT = 25
        private const val SPACING = 10
        private const val SUMMARY_HEIGHT = 120
        private const val SIGNATURE_HEIGHT = 80
        
        // Tabellen-Spaltenbreiten (insgesamt CONTENT_WIDTH = 515)
        private const val COL_DATE = 60
        private const val COL_START = 50
        private const val COL_END = 50
        private const val COL_BREAK = 45
        private const val COL_WORK_TIME = 50
        private const val COL_TRAVEL_WINDOW = 70
        private const val COL_TRAVEL_TIME = 55
        private const val COL_TOTAL_TIME = 55
        private const val COL_LOCATION = 50
        private const val COL_MEAL_ALLOWANCE = 30
    }
    
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN)
    private val timestampFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMAN)
    
    // Paints für verschiedene Text-Stile
    private val paintTitle = Paint().apply {
        color = Color.BLACK
        textSize = 24f
        isAntiAlias = true
        isFakeBoldText = true
    }
    
    private val paintHeader = Paint().apply {
        color = Color.BLACK
        textSize = 12f
        isAntiAlias = true
    }
    
    private val paintTableHeader = Paint().apply {
        color = Color.BLACK
        textSize = 10f
        isAntiAlias = true
        isFakeBoldText = true
    }
    
    private val paintTableText = Paint().apply {
        color = Color.BLACK
        textSize = 10f
        isAntiAlias = true
    }
    
    private val paintSummary = Paint().apply {
        color = Color.BLACK
        textSize = 12f
        isAntiAlias = true
        isFakeBoldText = true
    }
    
    private val paintSignature = Paint().apply {
        color = Color.BLACK
        textSize = 11f
        isAntiAlias = true
    }
    
    private val paintLine = Paint().apply {
        color = Color.BLACK
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }

    private fun string(resId: Int, vararg args: Any): String {
        return context.getString(resId, *args)
    }
    
    /**
     * Exportiert WorkEntries in eine PDF-Datei
     * 
     * @param entries Die zu exportierenden Einträge
     * @param employeeName Name des Mitarbeiters (Pflichtfeld)
     * @param company Firma (optional)
     * @param project Projekt (optional)
     * @param personnelNumber Personalnummer (optional)
     * @param startDate Startdatum des Zeitraums
     * @param endDate Enddatum des Zeitraums
     * @return Ergebnis der PDF-Erstellung
     */
    fun exportToPdf(
        entries: List<WorkEntryWithTravelLegs>,
        employeeName: String,
        company: String? = null,
        project: String? = null,
        personnelNumber: String? = null,
        startDate: java.time.LocalDate,
        endDate: java.time.LocalDate
    ): PdfExportResult {
        return try {
            // Pflichtfeld-Validierung
            if (employeeName.isBlank()) {
                throw IllegalArgumentException(string(R.string.pdf_export_error_name_missing))
            }
            if (entries.size > MAX_ENTRIES_PER_PDF) {
                return PdfExportResult.ValidationError(string(R.string.pdf_export_error_too_many_entries))
            }

            val pdfDocument = PdfDocument()
            try {
                var currentPage = pdfDocument.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create())
                var canvas = currentPage.canvas

                // Header zeichnen
                var yPosition = drawHeader(canvas, employeeName, company, project, personnelNumber, startDate, endDate)

                // Tabellenkopf zeichnen
                yPosition = drawTableHeader(canvas, yPosition)

                // Tabelle zeichnen (Multi-Pag)
                val tableResult = drawTable(canvas, pdfDocument, currentPage, entries, yPosition)
                currentPage = tableResult.page
                canvas = currentPage.canvas
                yPosition = tableResult.yPosition

                // Neue Seite für Summen und Unterschriften
                if (yPosition > PAGE_HEIGHT - MARGIN) {
                    pdfDocument.finishPage(currentPage)
                    val nextPageNumber = tableResult.pageNumber + 1
                    currentPage = pdfDocument.startPage(
                        PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, nextPageNumber).create()
                    )
                    canvas = currentPage.canvas
                    yPosition = MARGIN.toFloat()
                }

                // Summenblock zeichnen
                yPosition = drawSummary(canvas, entries, yPosition)

                // Unterschriften zeichnen
                drawSignatures(canvas, yPosition)

                pdfDocument.finishPage(currentPage)

                // PDF schreiben
                writePdfFile(pdfDocument, startDate, endDate)
            } finally {
                pdfDocument.close()
            }
        } catch (e: IOException) {
            Log.e("PdfExporter", "PDF export IO error: ${e.javaClass.simpleName}", e)
            PdfExportResult.FileWriteError(e.message ?: string(R.string.settings_error_pdf_export_failed))
        } catch (e: IllegalArgumentException) {
            Log.e("PdfExporter", "PDF export validation error: ${e.message}", e)
            PdfExportResult.ValidationError(e.message ?: string(R.string.settings_error_pdf_export_failed))
        } catch (e: IllegalStateException) {
            Log.e("PdfExporter", "PDF export state error: ${e.javaClass.simpleName}", e)
            PdfExportResult.StorageError(e.message ?: string(R.string.export_preview_error_pdf_create_failed))
        } catch (e: Exception) {
            Log.e("PdfExporter", "PDF export failed (${e.javaClass.simpleName})", e)
            PdfExportResult.UnknownError(e.message ?: string(R.string.settings_error_export_failed))
        }
    }

    /**
     * Zeichnet den Header auf die erste Seite
     */
    private fun drawHeader(
        canvas: Canvas,
        employeeName: String,
        company: String?,
        project: String?,
        personnelNumber: String?,
        startDate: java.time.LocalDate,
        endDate: java.time.LocalDate
    ): Float {
        var y = MARGIN.toFloat() + 20
        
        // Titel
        canvas.drawText(string(R.string.pdf_export_title), MARGIN.toFloat(), y, paintTitle)
        y += SPACING * 2
        
        // Mitarbeiter-Info
        canvas.drawText(
            string(R.string.pdf_export_header_employee, employeeName),
            MARGIN.toFloat(),
            y,
            paintHeader
        )
        y += 20
        
        company?.let {
            canvas.drawText(string(R.string.pdf_export_header_company, it), MARGIN.toFloat(), y, paintHeader)
            y += 20
        }
        
        project?.let {
            canvas.drawText(string(R.string.pdf_export_header_project, it), MARGIN.toFloat(), y, paintHeader)
            y += 20
        }
        
        personnelNumber?.let {
            canvas.drawText(
                string(R.string.pdf_export_header_personnel_number, it),
                MARGIN.toFloat(),
                y,
                paintHeader
            )
            y += 20
        }
        
        // Zeitraum
        val dateRange = if (startDate == endDate) {
            dateFormat.format(java.sql.Date.valueOf(startDate.toString()))
        } else {
            "${dateFormat.format(java.sql.Date.valueOf(startDate.toString()))} - ${dateFormat.format(java.sql.Date.valueOf(endDate.toString()))}"
        }
        canvas.drawText(string(R.string.pdf_export_header_range, dateRange), MARGIN.toFloat(), y, paintHeader)
        y += 20
        
        // Erstelldatum
        canvas.drawText(
            string(R.string.pdf_export_header_created_at, timestampFormat.format(Date())),
            MARGIN.toFloat(),
            y,
            paintHeader
        )
        y += SPACING
        
        // Trennlinie
        y += 10
        canvas.drawLine(MARGIN.toFloat(), y, (PAGE_WIDTH - MARGIN).toFloat(), y, paintLine)
        y += SPACING
        
        return y
    }
    
    /**
     * Zeichnet den Tabellenkopf
     */
    private fun drawTableHeader(canvas: Canvas, y: Float): Float {
        var yPos = y
        
        // Hintergrund für Tabellenkopf
        paintTableHeader.color = Color.parseColor("#E0E0E0")
        canvas.drawRect(
            MARGIN.toFloat(),
            yPos,
            (PAGE_WIDTH - MARGIN).toFloat(),
            yPos + TABLE_HEADER_HEIGHT,
            paintTableHeader
        )
        paintTableHeader.color = Color.BLACK
        
        // Spaltenüberschriften
        var xPos = MARGIN.toFloat() + 5
        canvas.drawText(string(R.string.pdf_export_column_date), xPos, yPos + 20, paintTableHeader)
        xPos += COL_DATE
        
        canvas.drawText(string(R.string.pdf_export_column_start), xPos, yPos + 20, paintTableHeader)
        xPos += COL_START
        
        canvas.drawText(string(R.string.pdf_export_column_end), xPos, yPos + 20, paintTableHeader)
        xPos += COL_END
        
        canvas.drawText(string(R.string.pdf_export_column_break), xPos, yPos + 20, paintTableHeader)
        xPos += COL_BREAK
        
        canvas.drawText(string(R.string.pdf_export_column_work), xPos, yPos + 20, paintTableHeader)
        xPos += COL_WORK_TIME

        canvas.drawText(string(R.string.pdf_export_column_travel_window), xPos, yPos + 20, paintTableHeader)
        xPos += COL_TRAVEL_WINDOW

        canvas.drawText(string(R.string.pdf_export_column_travel_time), xPos, yPos + 20, paintTableHeader)
        xPos += COL_TRAVEL_TIME

        canvas.drawText(string(R.string.pdf_export_column_total), xPos, yPos + 20, paintTableHeader)
        xPos += COL_TOTAL_TIME

        canvas.drawText(string(R.string.pdf_export_column_location), xPos, yPos + 20, paintTableHeader)
        xPos += COL_LOCATION

        canvas.drawText(string(R.string.pdf_export_column_meal_allowance), xPos, yPos + 20, paintTableHeader)

        // Trennlinie unter Tabellenkopf
        yPos += TABLE_HEADER_HEIGHT
        canvas.drawLine(MARGIN.toFloat(), yPos, (PAGE_WIDTH - MARGIN).toFloat(), yPos, paintLine)
        yPos += 5
        
        return yPos
    }
    
    /**
     * Zeichnet die Tabelle mit allen Einträgen (Multi-Pag)
     */
    private fun drawTable(
        canvas: Canvas,
        pdfDocument: PdfDocument,
        currentPage: PdfDocument.Page,
        entries: List<WorkEntryWithTravelLegs>,
        startY: Float
    ): TableDrawResult {
        var y = startY
        var pageNum = 1
        var activePage = currentPage
        var activeCanvas = canvas
        
        entries.forEach { record ->
            val entry = record.workEntry
            val travelLegs = record.orderedTravelLegs
            // Prüfen, ob eine neue Seite benötigt wird
            if (y + ROW_HEIGHT > PAGE_HEIGHT - MARGIN) {
                pdfDocument.finishPage(activePage)
                pageNum++
                activePage = pdfDocument.startPage(
                    PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
                )
                activeCanvas = activePage.canvas
                y = MARGIN.toFloat()
                y = drawTableHeader(activeCanvas, y)
            }
            
            // Tabellenzeile zeichnen
            var xPos = MARGIN.toFloat() + 5
            
            // Datum
            activeCanvas.drawText(PdfUtilities.formatDate(entry.date), xPos, y + 15, paintTableText)
            xPos += COL_DATE
            
            // Start – nur für WORK-Tage fachlich relevant
            val isWorkDay = entry.dayType == DayType.WORK
            val dash = string(R.string.pdf_export_placeholder_dash)
            activeCanvas.drawText(
                if (isWorkDay) PdfUtilities.formatTime(entry.workStart).ifBlank { dash } else dash,
                xPos, y + 15, paintTableText
            )
            xPos += COL_START

            // Ende – nur für WORK-Tage
            activeCanvas.drawText(
                if (isWorkDay) PdfUtilities.formatTime(entry.workEnd).ifBlank { dash } else dash,
                xPos, y + 15, paintTableText
            )
            xPos += COL_END

            // Pause – nur für WORK-Tage
            activeCanvas.drawText(
                if (isWorkDay) string(R.string.format_minutes, entry.breakMinutes) else dash,
                xPos, y + 15, paintTableText
            )
            xPos += COL_BREAK
            
            // Arbeitszeit
            val workHours = TimeCalculator.calculateWorkHours(entry)
            val travelMinutes = TimeCalculator.calculateTravelMinutes(travelLegs)
            val travelHours = travelMinutes / 60.0
            val totalHours = workHours + travelHours
            val workText = string(R.string.pdf_export_value_hours, PdfUtilities.formatWorkHours(workHours))
            activeCanvas.drawText(workText, xPos, y + 15, paintTableText)
            xPos += COL_WORK_TIME

            // Reise (von–bis)
            val travelWindow = PdfUtilities.buildTravelRouteSummary(travelLegs)
            val travelWindowText = if (travelWindow.isNotBlank()) {
                if (travelWindow.length > 18) travelWindow.take(17) + "…" else travelWindow
            } else {
                string(R.string.pdf_export_placeholder_dash)
            }
            activeCanvas.drawText(travelWindowText, xPos, y + 15, paintTableText)
            xPos += COL_TRAVEL_WINDOW

            // Reisezeit
            val travelTime = PdfUtilities.formatTravelTime(travelMinutes)
            val travelTimeText = if (travelMinutes > 0) {
                string(R.string.pdf_export_value_hours, travelTime)
            } else {
                string(R.string.pdf_export_placeholder_dash)
            }
            activeCanvas.drawText(travelTimeText, xPos, y + 15, paintTableText)
            xPos += COL_TRAVEL_TIME

            // Gesamtzeit
            val totalText = string(R.string.pdf_export_value_hours, PdfUtilities.formatWorkHours(totalHours))
            activeCanvas.drawText(totalText, xPos, y + 15, paintTableText)
            xPos += COL_TOTAL_TIME

            // Ort
            val location = PdfUtilities.getLocation(entry, travelLegs)
            activeCanvas.drawText(if (location.length > 8) location.take(7) + "…" else location, xPos, y + 15, paintTableText)
            xPos += COL_LOCATION

            // Verpflegungspauschale
            val vpText = if (entry.mealAllowanceAmountCents > 0) {
                MealAllowanceCalculator.formatEuro(entry.mealAllowanceAmountCents)
            } else {
                string(R.string.pdf_export_placeholder_dash)
            }
            activeCanvas.drawText(if (vpText.length > 8) vpText.take(7) + "…" else vpText, xPos, y + 15, paintTableText)

            y += ROW_HEIGHT
        }
        
        // Trennlinie nach der Tabelle
        activeCanvas.drawLine(MARGIN.toFloat(), y, (PAGE_WIDTH - MARGIN).toFloat(), y, paintLine)
        y += SPACING * 2
        
        return TableDrawResult(activePage, y, pageNum)
    }

    private data class TableDrawResult(
        val page: PdfDocument.Page,
        val yPosition: Float,
        val pageNumber: Int
    )
    
    /**
     * Zeichnet den Summenblock
     */
    private fun drawSummary(canvas: Canvas, entries: List<WorkEntryWithTravelLegs>, y: Float): Float {
        var yPos = y + 20
        
        val workDays = PdfUtilities.filterWorkDays(entries).size
        val totalWorkHours = PdfUtilities.sumWorkHours(entries)
        val totalTravelMinutes = PdfUtilities.sumTravelMinutes(entries)
        val totalTravelHours = totalTravelMinutes / 60.0
        val totalPaidHours = totalWorkHours + totalTravelHours
        
        canvas.drawText(string(R.string.pdf_export_summary_work_days, workDays), MARGIN.toFloat(), yPos, paintSummary)
        yPos += 25
        
        canvas.drawText(
            string(R.string.pdf_export_summary_work_time, PdfUtilities.formatWorkHours(totalWorkHours)),
            MARGIN.toFloat(),
            yPos,
            paintSummary
        )
        yPos += 25

        canvas.drawText(
            string(R.string.pdf_export_summary_travel_time, PdfUtilities.formatWorkHours(totalTravelHours)),
            MARGIN.toFloat(),
            yPos,
            paintSummary
        )
        yPos += 25

        canvas.drawText(
            string(R.string.pdf_export_summary_total_paid_time, PdfUtilities.formatWorkHours(totalPaidHours)),
            MARGIN.toFloat(),
            yPos,
            paintSummary
        )
        yPos += 25

        val totalMealAllowanceCents = entries.sumOf { it.workEntry.mealAllowanceAmountCents }
        canvas.drawText(
            string(R.string.pdf_export_summary_meal_allowance, MealAllowanceCalculator.formatEuro(totalMealAllowanceCents)),
            MARGIN.toFloat(),
            yPos,
            paintSummary
        )
        yPos += 25

        // Trennlinie
        canvas.drawLine(MARGIN.toFloat(), yPos, (PAGE_WIDTH - MARGIN).toFloat(), yPos, paintLine)
        yPos += SPACING * 3
        
        return yPos
    }
    
    /**
     * Zeichnet die Unterschriftenzeilen
     */
    private fun drawSignatures(canvas: Canvas, y: Float): Float {
        var yPos = y
        
        // Mitarbeiter-Unterschrift
        canvas.drawText(string(R.string.pdf_export_signature_employee), MARGIN.toFloat(), yPos, paintSignature)
        yPos += 15
        canvas.drawLine(
            MARGIN.toFloat(),
            yPos,
            (MARGIN + 200).toFloat(),
            yPos,
            paintLine
        )
        yPos += 20
        
        // Vorgesetzter-Unterschrift
        canvas.drawText(string(R.string.pdf_export_signature_supervisor), MARGIN.toFloat(), yPos, paintSignature)
        yPos += 15
        canvas.drawLine(
            MARGIN.toFloat(),
            yPos,
            (MARGIN + 200).toFloat(),
            yPos,
            paintLine
        )
        
        return yPos
    }
    
    /**
     * Schreibt das PDF in eine Datei
     */
    private fun writePdfFile(
        pdfDocument: PdfDocument,
        startDate: java.time.LocalDate,
        endDate: java.time.LocalDate
    ): PdfExportResult {
        val cacheDir = File(context.cacheDir, "exports")
        if (!cacheDir.exists() && !cacheDir.mkdirs()) {
            throw IllegalStateException(string(R.string.pdf_export_error_cache_dir_failed))
        }

        val stat = StatFs(cacheDir.absolutePath)
        val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
        val minRequiredBytes = 5 * 1024 * 1024L
        if (availableBytes < minRequiredBytes) {
            throw IllegalStateException(string(R.string.pdf_export_error_not_enough_storage_mb, 5))
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.GERMAN).format(Date())
        val dateRange = if (startDate == endDate) {
            startDate.toString()
        } else {
            "${startDate}_${endDate}"
        }
        val filename = "montagezeit_pdf_${dateRange}_$timestamp.pdf"
        val file = File(cacheDir, filename)

        try {
            FileOutputStream(file).use { fos ->
                pdfDocument.writeTo(fos)
            }
        } catch (e: IOException) {
            file.delete()
            throw e
        }

        val fileUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return PdfExportResult.Success(fileUri)
    }
    
    /**
     * Erstellt eine Share Intent für die PDF-Datei
     * 
     * @param fileUri Die Uri der PDF-Datei
     * @return Intent für das Teilen der Datei
     */
    fun createShareIntent(fileUri: Uri): Intent {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(Intent.EXTRA_SUBJECT, string(R.string.export_share_subject))
        }
        return Intent.createChooser(intent, string(R.string.export_preview_share_chooser_title))
    }
}
