package de.montagezeit.app.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint.Align
import android.graphics.Paint
import android.os.StatFs
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import de.montagezeit.app.R
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntryWithTravelLegs
import de.montagezeit.app.domain.usecase.AggregateWorkStats
import de.montagezeit.app.domain.usecase.isStatisticsEligible
import de.montagezeit.app.domain.util.MealAllowanceCalculator
import de.montagezeit.app.domain.util.TimeCalculator
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
        private const val MIN_TABLE_HEADER_HEIGHT = 30f
        private const val MIN_ROW_HEIGHT = 25f
        private const val FOOTER_HEIGHT = 24f
        private const val SPACING = 10
        private const val SUMMARY_HEIGHT = 125  // bis zu 5 Zeilen × 25 pt
        private const val SIGNATURE_HEIGHT = 120  // je Block: Label(15) + Linie(20) + Ort/Datum(25) + Abstand(20)
        private const val TABLE_CELL_HORIZONTAL_PADDING = 5f
        private const val TABLE_CELL_VERTICAL_PADDING = 4f

        // Tabellen-Spaltenbreiten (insgesamt CONTENT_WIDTH = 515)
        // 9 Spalten: 60+50+50+45+50+95+55+70+40 = 515
        private const val COL_DATE = 60
        private const val COL_START = 50
        private const val COL_END = 50
        private const val COL_BREAK = 45
        private const val COL_WORK_TIME = 50
        private const val COL_TRAVEL_ROUTE = 95   // war COL_TRAVEL_WINDOW=70; breiter für längere Routen
        private const val COL_TRAVEL_TYPE = 55    // war COL_TRAVEL_TIME=55; jetzt Reiseart (Anreise/Abreise/…)
        private const val COL_LOCATION = 70       // war 50; breiter durch Wegfall der Gesamt-Spalte
        private const val COL_BREAKFAST = 40      // war COL_MEAL_ALLOWANCE=30; Frühstück ✓/–
    }
    
    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN)
    private val timestampFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale.GERMAN)
    
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

    // Dedizierter Paint für den Tabellenkopf-Hintergrund – vermeidet Mutation von paintTableHeader
    private val paintTableHeaderBackground = Paint().apply {
        color = Color.parseColor("#E0E0E0")
        style = Paint.Style.FILL
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
    suspend fun exportToPdf(
        entries: List<WorkEntryWithTravelLegs>,
        employeeName: String,
        company: String? = null,
        project: String? = null,
        personnelNumber: String? = null,
        startDate: java.time.LocalDate,
        endDate: java.time.LocalDate
    ): PdfExportResult = withContext(Dispatchers.IO) {
        try {
            val eligibleEntries = entries.filter(::isStatisticsEligible)
            // Pflichtfeld-Validierung
            if (employeeName.isBlank()) {
                throw IllegalArgumentException(string(R.string.pdf_export_error_name_missing))
            }
            if (eligibleEntries.isEmpty()) {
                return@withContext PdfExportResult.ValidationError(string(R.string.export_preview_empty_range))
            }
            if (eligibleEntries.size > MAX_ENTRIES_PER_PDF) {
                return@withContext PdfExportResult.ValidationError(string(R.string.pdf_export_error_too_many_entries))
            }

            val pdfDocument = PdfDocument()
            try {
                var currentPage = pdfDocument.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create())
                var canvas = currentPage.canvas

                // Header zeichnen
                // Letzten erfassten Tag ermitteln – bei Teilmonat wird "Stand:" angezeigt
                val lastEntryDate = eligibleEntries.maxOfOrNull { it.workEntry.date }
                val actualEndDate = if (lastEntryDate != null && lastEntryDate < endDate) lastEntryDate else null
                var yPosition = drawHeader(canvas, employeeName, company, project, personnelNumber, startDate, endDate, actualEndDate)

                // Tabellenkopf zeichnen
                yPosition = drawTableHeader(canvas, yPosition)

                // Tabelle zeichnen (Multi-Pag)
                val tableResult = drawTable(canvas, pdfDocument, currentPage, eligibleEntries, yPosition)
                currentPage = tableResult.page
                canvas = currentPage.canvas
                yPosition = tableResult.yPosition

                // Neue Seite für Summen und Unterschriften
                if (yPosition + SUMMARY_HEIGHT + SIGNATURE_HEIGHT > PAGE_HEIGHT - MARGIN - FOOTER_HEIGHT) {
                    pdfDocument.finishPage(currentPage)
                    val nextPageNumber = tableResult.pageNumber + 1
                    currentPage = pdfDocument.startPage(
                        PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, nextPageNumber).create()
                    )
                    canvas = currentPage.canvas
                    yPosition = MARGIN.toFloat()
                }

                // Summenblock zeichnen
                yPosition = drawSummary(canvas, eligibleEntries, yPosition)

                // Unterschriften zeichnen
                drawSignatures(canvas, yPosition)

                // Seitennummer auf der letzten Seite
                drawFooter(canvas, tableResult.pageNumber)
                pdfDocument.finishPage(currentPage)

                // PDF schreiben
                writePdfFile(pdfDocument, startDate, endDate)
            } finally {
                closePdfDocumentQuietly(pdfDocument)
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
        startDate: LocalDate,
        endDate: LocalDate,
        actualEndDate: LocalDate? = null
    ): Float {
        var y = MARGIN.toFloat() + 20
        
        // Titel
        canvas.drawText(string(R.string.pdf_export_title), MARGIN.toFloat(), y, paintTitle)
        y += SPACING * 2
        
        // Mitarbeiter-Info
        y = drawWrappedTextLine(
            canvas = canvas,
            text = string(R.string.pdf_export_header_employee, employeeName),
            x = MARGIN.toFloat(),
            y = y,
            paint = paintHeader
        )
        
        company?.let {
            y = drawWrappedTextLine(
                canvas = canvas,
                text = string(R.string.pdf_export_header_company, it),
                x = MARGIN.toFloat(),
                y = y,
                paint = paintHeader
            )
        }
        
        project?.let {
            y = drawWrappedTextLine(
                canvas = canvas,
                text = string(R.string.pdf_export_header_project, it),
                x = MARGIN.toFloat(),
                y = y,
                paint = paintHeader
            )
        }
        
        personnelNumber?.let {
            y = drawWrappedTextLine(
                canvas = canvas,
                text = string(R.string.pdf_export_header_personnel_number, it),
                x = MARGIN.toFloat(),
                y = y,
                paint = paintHeader
            )
        }
        
        // Zeitraum
        val dateRange = if (startDate == endDate) {
            startDate.format(dateFormatter)
        } else {
            "${startDate.format(dateFormatter)} - ${endDate.format(dateFormatter)}"
        }
        y = drawWrappedTextLine(
            canvas = canvas,
            text = string(R.string.pdf_export_header_range, dateRange),
            x = MARGIN.toFloat(),
            y = y,
            paint = paintHeader
        )
        
        // Erstelldatum
        y = drawWrappedTextLine(
            canvas = canvas,
            text = string(R.string.pdf_export_header_created_at, LocalDateTime.now().format(timestampFormatter)),
            x = MARGIN.toFloat(),
            y = y,
            paint = paintHeader
        )

        // Stand-Hinweis: nur wenn Daten nicht bis zum Ende des gewählten Zeitraums reichen
        if (actualEndDate != null) {
            y = drawWrappedTextLine(
                canvas = canvas,
                text = string(R.string.pdf_export_header_as_of, actualEndDate.format(dateFormatter)),
                x = MARGIN.toFloat(),
                y = y,
                paint = paintHeader
            )
        }

        y += SPACING - 10  // Abstand vor Trennlinie beibehalten

        // Trennlinie
        y += 10
        canvas.drawLine(MARGIN.toFloat(), y, (PAGE_WIDTH - MARGIN).toFloat(), y, paintLine)
        y += SPACING
        
        return y
    }
    
    private data class TableColumn(
        val width: Int,
        val headerText: String
    )

    private data class CellLayout(
        val lines: List<String>
    )

    private fun tableColumns(): List<TableColumn> = listOf(
        TableColumn(COL_DATE, string(R.string.pdf_export_column_date)),
        TableColumn(COL_START, string(R.string.pdf_export_column_start)),
        TableColumn(COL_END, string(R.string.pdf_export_column_end)),
        TableColumn(COL_BREAK, string(R.string.pdf_export_column_break)),
        TableColumn(COL_WORK_TIME, string(R.string.pdf_export_column_work)),
        TableColumn(COL_TRAVEL_ROUTE, string(R.string.pdf_export_column_travel_route)),
        TableColumn(COL_TRAVEL_TYPE, string(R.string.pdf_export_column_travel_type)),
        TableColumn(COL_LOCATION, string(R.string.pdf_export_column_location)),
        TableColumn(COL_BREAKFAST, string(R.string.pdf_export_column_breakfast))
    )

    /**
     * Zeichnet den Tabellenkopf
     */
    private fun drawTableHeader(canvas: Canvas, y: Float): Float {
        val columns = tableColumns()
        val headerHeight = columns.maxOfOrNull { column ->
            cellHeight(
                layout = CellLayout(
                    lines = wrapText(
                        text = column.headerText,
                        maxWidth = column.width - TABLE_CELL_HORIZONTAL_PADDING * 2,
                        paint = paintTableHeader
                    )
                ),
                lineHeight = paintTableHeader.fontSpacing
            )
        }?.coerceAtLeast(MIN_TABLE_HEADER_HEIGHT) ?: MIN_TABLE_HEADER_HEIGHT

        canvas.drawRect(
            MARGIN.toFloat(),
            y,
            (PAGE_WIDTH - MARGIN).toFloat(),
            y + headerHeight,
            paintTableHeaderBackground
        )

        var xPos = MARGIN.toFloat()
        columns.forEach { column ->
            val layout = CellLayout(
                lines = wrapText(
                    text = column.headerText,
                    maxWidth = column.width - TABLE_CELL_HORIZONTAL_PADDING * 2,
                    paint = paintTableHeader
                )
            )
            drawCell(
                canvas = canvas,
                layout = layout,
                x = xPos,
                y = y,
                columnWidth = column.width.toFloat(),
                paint = paintTableHeader
            )
            xPos += column.width
        }

        val bottomY = y + headerHeight
        canvas.drawLine(MARGIN.toFloat(), bottomY, (PAGE_WIDTH - MARGIN).toFloat(), bottomY, paintLine)
        return bottomY + 5
    }
    
    /**
     * Zeichnet die Seitennummer am unteren rechten Rand ("Seite X")
     */
    private fun drawFooter(canvas: Canvas, pageNum: Int) {
        val text = string(R.string.pdf_export_footer_page, pageNum)
        val previousAlign = paintTableText.textAlign
        paintTableText.textAlign = Align.RIGHT
        canvas.drawText(
            text,
            (PAGE_WIDTH - MARGIN).toFloat(),
            (PAGE_HEIGHT - 15).toFloat(),
            paintTableText
        )
        paintTableText.textAlign = previousAlign
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
        val columns = tableColumns()
        val dash = string(R.string.pdf_export_placeholder_dash)
        var y = startY
        var pageNum = 1
        var activePage = currentPage
        var activeCanvas = canvas
        
        entries.forEach { record ->
            val cellLayouts = buildTableCellTexts(record, dash)
                .zip(columns)
                .map { (text, column) ->
                    CellLayout(
                        lines = wrapText(
                            text = text,
                            maxWidth = column.width - TABLE_CELL_HORIZONTAL_PADDING * 2,
                            paint = paintTableText
                        )
                    )
                }
            val rowHeight = cellLayouts.maxOfOrNull { cellHeight(it, paintTableText.fontSpacing) }
                ?.coerceAtLeast(MIN_ROW_HEIGHT)
                ?: MIN_ROW_HEIGHT

            if (y + rowHeight > PAGE_HEIGHT - MARGIN - FOOTER_HEIGHT) {
                drawFooter(activeCanvas, pageNum)
                pdfDocument.finishPage(activePage)
                pageNum++
                activePage = pdfDocument.startPage(
                    PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
                )
                activeCanvas = activePage.canvas
                y = drawTableHeader(activeCanvas, MARGIN.toFloat())
            }

            var xPos = MARGIN.toFloat()
            columns.zip(cellLayouts).forEach { (column, layout) ->
                drawCell(
                    canvas = activeCanvas,
                    layout = layout,
                    x = xPos,
                    y = y,
                    columnWidth = column.width.toFloat(),
                    paint = paintTableText
                )
                xPos += column.width
            }

            y += rowHeight
        }
        
        activeCanvas.drawLine(MARGIN.toFloat(), y, (PAGE_WIDTH - MARGIN).toFloat(), y, paintLine)
        y += SPACING * 2
        
        return TableDrawResult(activePage, y, pageNum)
    }

    private fun buildTableCellTexts(record: WorkEntryWithTravelLegs, dash: String): List<String> {
        val entry = record.workEntry
        val travelLegs = record.orderedTravelLegs
        val travelMinutes = TimeCalculator.calculateTravelMinutes(travelLegs)
        val workHours = TimeCalculator.calculateWorkHours(entry)
        val isWorkDay = entry.dayType.isWorkLike
        val mealSnapshot = MealAllowanceCalculator.resolveEffectiveStoredSnapshot(record)

        val startText = when (entry.dayType) {
            DayType.WORK, DayType.SCHULUNG, DayType.LEHRGANG -> when {
                entry.workStart != null -> PdfUtilities.formatTime(entry.workStart)
                travelMinutes > 0 -> string(R.string.pdf_export_row_label_travel_only)
                else -> dash
            }
            DayType.OFF -> string(R.string.day_type_off)
            DayType.COMP_TIME -> string(R.string.day_type_comp_time)
        }

        val travelTypeText = when (PdfUtilities.determineTravelTypeKey(travelLegs)) {
            "ARRIVAL" -> string(R.string.pdf_export_travel_type_arrival)
            "DEPARTURE" -> string(R.string.pdf_export_travel_type_departure)
            "ARRIVAL_DEPARTURE" -> string(R.string.pdf_export_travel_type_arrival_departure)
            "CONTINUATION" -> string(R.string.pdf_export_travel_type_continuation)
            "TRAVEL" -> string(R.string.pdf_export_travel_type_travel)
            else -> dash
        }

        return listOf(
            PdfUtilities.formatDate(entry.date),
            startText,
            if (isWorkDay && entry.workStart != null) PdfUtilities.formatTime(entry.workEnd).ifBlank { dash } else dash,
            if (isWorkDay && entry.workStart != null) string(R.string.format_minutes, entry.breakMinutes) else dash,
            if (workHours > 0) string(R.string.pdf_export_value_hours, PdfUtilities.formatWorkHours(workHours)) else dash,
            PdfUtilities.buildTravelRouteSummary(travelLegs).ifBlank { dash },
            travelTypeText,
            PdfUtilities.getLocation(entry, travelLegs).ifBlank { dash },
            if (mealSnapshot.breakfastIncluded && mealSnapshot.baseCents > 0) {
                string(R.string.pdf_export_breakfast_yes)
            } else {
                dash
            }
        )
    }

    private fun cellHeight(layout: CellLayout, lineHeight: Float): Float {
        return (layout.lines.size * lineHeight) + TABLE_CELL_VERTICAL_PADDING * 2
    }

    private fun drawCell(
        canvas: Canvas,
        layout: CellLayout,
        x: Float,
        y: Float,
        columnWidth: Float,
        paint: Paint
    ) {
        drawWrappedLines(
            canvas = canvas,
            lines = layout.lines,
            x = x + TABLE_CELL_HORIZONTAL_PADDING,
            y = y + TABLE_CELL_VERTICAL_PADDING,
            paint = paint,
            maxWidth = columnWidth - TABLE_CELL_HORIZONTAL_PADDING * 2
        )
    }

    private fun drawWrappedTextLine(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        paint: Paint,
        maxWidth: Float = CONTENT_WIDTH.toFloat()
    ): Float {
        val lines = wrapText(text, maxWidth, paint)
        drawWrappedLines(canvas, lines, x, y, paint, maxWidth)
        return y + lines.size * paint.fontSpacing + 4f
    }

    private fun drawWrappedLines(
        canvas: Canvas,
        lines: List<String>,
        x: Float,
        y: Float,
        paint: Paint,
        maxWidth: Float
    ) {
        val baselineStart = y - paint.fontMetrics.ascent
        lines.forEachIndexed { index, line ->
            val drawText = fitTextToWidth(line, maxWidth, paint)
            canvas.drawText(drawText, x, baselineStart + index * paint.fontSpacing, paint)
        }
    }

    private fun fitTextToWidth(text: String, maxWidth: Float, paint: Paint): String {
        if (paint.measureText(text) <= maxWidth) return text
        val ellipsis = string(R.string.common_ellipsis)
        val ellipsisWidth = paint.measureText(ellipsis)
        var endIndex = text.length
        while (endIndex > 1 && paint.measureText(text, 0, endIndex) + ellipsisWidth > maxWidth) {
            endIndex--
        }
        return text.take(endIndex.coerceAtLeast(1)).trimEnd() + ellipsis
    }

    private fun wrapText(text: String, maxWidth: Float, paint: Paint): List<String> {
        if (text.isBlank()) return listOf("")

        val lines = mutableListOf<String>()
        var remaining = text.trim()
        while (remaining.isNotEmpty()) {
            var breakIndex = paint.breakText(remaining, true, maxWidth, null)
            if (breakIndex <= 0) {
                breakIndex = 1
            }
            if (breakIndex < remaining.length) {
                val whitespaceBreak = remaining.substring(0, breakIndex).lastIndexOf(' ')
                if (whitespaceBreak > 0) {
                    breakIndex = whitespaceBreak
                }
            }
            val nextLine = remaining.substring(0, breakIndex).trimEnd()
            lines += nextLine.ifBlank { remaining.substring(0, breakIndex) }
            remaining = remaining.substring(breakIndex).trimStart()
        }
        return lines
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
        val stats = AggregateWorkStats()(entries)
        val totalWorkHours = stats.totalWorkMinutes / 60.0
        val totalTravelMinutes = stats.totalTravelMinutes
        val totalMealAllowanceCents = stats.mealAllowanceCents

        canvas.drawText(
            string(R.string.pdf_export_summary_work_days, stats.workDays),
            MARGIN.toFloat(), yPos, paintSummary
        )
        yPos += 25

        canvas.drawText(
            string(R.string.pdf_export_summary_work_time, PdfUtilities.formatWorkHours(totalWorkHours)),
            MARGIN.toFloat(), yPos, paintSummary
        )
        yPos += 25

        if (totalTravelMinutes > 0) {
            canvas.drawText(
                string(R.string.pdf_export_summary_travel_time, PdfUtilities.formatWorkHours(totalTravelMinutes / 60.0)),
                MARGIN.toFloat(), yPos, paintSummary
            )
            yPos += 25

            val totalPaidHours = totalWorkHours + totalTravelMinutes / 60.0
            canvas.drawText(
                string(R.string.pdf_export_summary_paid_time, PdfUtilities.formatWorkHours(totalPaidHours)),
                MARGIN.toFloat(), yPos, paintSummary
            )
            yPos += 25
        }

        canvas.drawText(
            string(R.string.pdf_export_summary_meal_allowance, MealAllowanceCalculator.formatEuro(totalMealAllowanceCents)),
            MARGIN.toFloat(), yPos, paintSummary
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
        canvas.drawLine(MARGIN.toFloat(), yPos, (MARGIN + 200).toFloat(), yPos, paintLine)
        yPos += 20
        canvas.drawText(string(R.string.pdf_export_signature_date_location), MARGIN.toFloat(), yPos, paintSignature)
        yPos += 30

        // Vorgesetzter-Unterschrift
        canvas.drawText(string(R.string.pdf_export_signature_supervisor), MARGIN.toFloat(), yPos, paintSignature)
        yPos += 15
        canvas.drawLine(MARGIN.toFloat(), yPos, (MARGIN + 200).toFloat(), yPos, paintLine)
        yPos += 20
        canvas.drawText(string(R.string.pdf_export_signature_date_location), MARGIN.toFloat(), yPos, paintSignature)
        
        return yPos
    }
    
    /**
     * Schreibt das PDF in eine Datei
     */
    private fun writePdfFile(
        pdfDocument: PdfDocument,
        startDate: LocalDate,
        endDate: LocalDate
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

        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss", Locale.GERMAN))
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

    private fun closePdfDocumentQuietly(pdfDocument: PdfDocument) {
        try {
            pdfDocument.close()
        } catch (e: IllegalStateException) {
            if (e.message?.contains("closed", ignoreCase = true) != true) {
                throw e
            }
        }
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
