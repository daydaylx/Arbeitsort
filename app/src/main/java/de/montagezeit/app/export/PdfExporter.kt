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
import de.montagezeit.app.data.local.entity.WorkEntryWithTravelLegs
import de.montagezeit.app.domain.usecase.AggregateWorkStats
import de.montagezeit.app.domain.usecase.WorkStatsResult
import de.montagezeit.app.domain.usecase.isStatisticsEligible
import de.montagezeit.app.domain.util.MealAllowanceCalculator
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
 * Exportiert WorkEntries als kompakten, einseitigen A4-Querformat-Arbeitsnachweis
 * (Büro/Lohnabrechnung). Nutzt android.graphics.pdf.PdfDocument (keine externen PDF-Libs).
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

    // PDF-Konstanten (A4 Querformat in Punkten: 842 x 595)
    companion object {
        // Safety limit: PdfDocument keeps all page canvases in memory until writeTo().
        // On low-end devices (< 2 GB RAM), rendering 180+ entries can cause an OOM.
        const val MAX_ENTRIES_PER_PDF = 180

        const val PAGE_WIDTH = 842
        const val PAGE_HEIGHT = 595
        const val MARGIN = 20 // ~7 mm
        const val CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN

        const val TABLE_CELL_HORIZONTAL_PADDING = 6f
        const val TABLE_CELL_VERTICAL_PADDING = 2.5f

        const val HEADER_TITLE_GAP = 5f
        const val HEADER_LINE_GAP = 2f
        const val HEADER_DIVIDER_GAP_BEFORE = 5f
        const val HEADER_DIVIDER_GAP_AFTER = 6f
        const val TABLE_HEADER_BOTTOM_GAP = 2.5f
        const val AFTER_TABLE_GAP = 3f
        const val LEGEND_TOP_GAP = 4f
        const val SUMMARY_TOP_GAP = 6f
        const val SUMMARY_VERTICAL_PADDING = 4f
        const val SUMMARY_HORIZONTAL_PADDING = 8f

        // Maximaler Faktor, um den die Zeilenhöhe über die Mindesthöhe hinaus wachsen darf, damit
        // Monate mit wenigen Einträgen die Seite ausfüllen statt unnötigen Leerraum zu hinterlassen.
        const val ROW_HEIGHT_EXPANSION_FACTOR = 1.6f

        const val LEGEND_MIN_FONT_SIZE = 7f
        const val LINE_STROKE_WIDTH = 0.75f

        // Tabellen-Spaltenbreiten (insgesamt CONTENT_WIDTH = 802)
        // 9 Spalten: 56+131+47+47+47+56+298+65+55 = 802
        const val COL_DATE = 56
        const val COL_LOCATION = 131
        const val COL_START = 47
        const val COL_END = 47
        const val COL_BREAK = 47
        const val COL_WORK = 56
        const val COL_TRAVEL = 298
        const val COL_TRAVEL_TIME = 65
        const val COL_VP = 55

        // Schriftgrößen-Stufen, von großzügig zu kompakt (siehe Aufgabenstellung: Titel 14-16pt,
        // Metadaten 9-10pt, Tabellenkopf/-inhalt 8-9pt, Summenzeile 9-10pt).
        private val DENSITY_LEVELS = listOf(
            Density(titleSize = 16f, metaSize = 10f, tableSize = 9f, summarySize = 10f),
            Density(titleSize = 15f, metaSize = 9.5f, tableSize = 8.5f, summarySize = 9.5f),
            Density(titleSize = 14f, metaSize = 9f, tableSize = 8f, summarySize = 9f)
        )
    }

    private data class Density(
        val titleSize: Float,
        val metaSize: Float,
        val tableSize: Float,
        val summarySize: Float
    )

    /**
     * Alle Paints für eine gewählte Schriftgrößen-Stufe. Wird pro Export neu erzeugt
     * (kein geteilter mutabler Zustand zwischen parallelen Exports).
     */
    private class PdfStyle(density: Density) {
        val title = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
            isFakeBoldText = true
            textSize = density.titleSize
        }
        val meta = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
            textSize = density.metaSize
        }
        val tableHeader = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
            isFakeBoldText = true
            textSize = density.tableSize
        }
        val tableHeaderBackground = Paint().apply {
            color = Color.parseColor("#EDEDED")
            style = Paint.Style.FILL
        }
        val tableText = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
            textSize = density.tableSize
        }
        val summary = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
            isFakeBoldText = true
            textSize = density.summarySize
        }
        val summaryBackground = Paint().apply {
            color = Color.parseColor("#E3E3E3")
            style = Paint.Style.FILL
        }
        val legend = Paint().apply {
            color = Color.parseColor("#555555")
            isAntiAlias = true
            textSize = (density.tableSize - 1f).coerceAtLeast(LEGEND_MIN_FONT_SIZE)
        }
        val line = Paint().apply {
            color = Color.BLACK
            strokeWidth = LINE_STROKE_WIDTH
            style = Paint.Style.STROKE
        }

        val rowHeight = tableText.fontSpacing + 2 * TABLE_CELL_VERTICAL_PADDING
        val tableHeaderHeight = tableHeader.fontSpacing + 2 * TABLE_CELL_VERTICAL_PADDING + TABLE_HEADER_BOTTOM_GAP
        val headerBlockHeight = title.fontSpacing + HEADER_TITLE_GAP +
            meta.fontSpacing + HEADER_LINE_GAP +
            meta.fontSpacing + HEADER_DIVIDER_GAP_BEFORE + HEADER_DIVIDER_GAP_AFTER

        fun legendHeight(present: Boolean): Float = if (present) legend.fontSpacing + LEGEND_TOP_GAP else 0f

        val summaryHeight = summary.fontSpacing + 2 * SUMMARY_VERTICAL_PADDING + SUMMARY_TOP_GAP

        fun footerBlockHeight(legendPresent: Boolean): Float =
            AFTER_TABLE_GAP + legendHeight(legendPresent) + summaryHeight

        /**
         * Zeilenhöhe, die den nach Kopf/Legende/Summenzeile verbleibenden Platz ausfüllt, statt
         * bei wenigen Einträgen unnötigen Leerraum am Seitenende zu hinterlassen. Wächst bis
         * maximal [ROW_HEIGHT_EXPANSION_FACTOR] der Mindestzeilenhöhe; überschüssiger Platz
         * darüber hinaus bleibt als normaler, unauffälliger Fußrand bestehen.
         */
        fun effectiveRowHeight(entryCount: Int, legendPresent: Boolean): Float {
            if (entryCount <= 0) return rowHeight
            val fixedHeight = headerBlockHeight + tableHeaderHeight + footerBlockHeight(legendPresent)
            val availableForRows = PAGE_HEIGHT - 2 * MARGIN - fixedHeight
            val idealRowHeight = availableForRows / entryCount
            return idealRowHeight.coerceIn(rowHeight, rowHeight * ROW_HEIGHT_EXPANSION_FACTOR)
        }
    }

    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN)

    private fun string(resId: Int, vararg args: Any): String {
        return context.getString(resId, *args)
    }

    /**
     * Liefert den rohen String-Ressourcentext ohne Formatierung (z.B. mit "%1$s"-Platzhalter
     * für spätere `String.format`-Aufrufe). [string] mit leeren varargs würde hier crashen, da
     * `Context.getString(id, *emptyArray())` trotzdem den Formatter mit fehlendem Argument aufruft.
     */
    private fun rawString(resId: Int): String {
        return context.getString(resId)
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
        startDate: LocalDate,
        endDate: LocalDate
    ): PdfExportResult = withContext(Dispatchers.IO) {
        try {
            // Innerhalb der Anwendung bleibt die Sortierung unverändert – für den PDF-Export
            // werden die Einträge chronologisch aufsteigend dargestellt.
            val eligibleEntries = entries.filter(::isStatisticsEligible).sortedBy { it.workEntry.date }
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

            val pdfDocument = renderPdfDocument(
                eligibleEntries, employeeName, company, project, personnelNumber, startDate, endDate
            )
            try {
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
     * Baut das vollständige [PdfDocument] (Kopfbereich, Tabelle, Legende, Summenzeile), inklusive
     * kontrolliertem Seitenumbruch für Ausnahmefälle. Getrennt von [exportToPdf], damit die reine
     * Layout-/Seitenumbruch-Logik unabhängig vom Datei-I/O getestet werden kann.
     */
    internal fun renderPdfDocument(
        eligibleEntries: List<WorkEntryWithTravelLegs>,
        employeeName: String,
        company: String?,
        project: String?,
        personnelNumber: String?,
        startDate: LocalDate,
        endDate: LocalDate
    ): PdfDocument {
        val stats = AggregateWorkStats()(eligibleEntries)
        val legendText = PdfUtilities.buildTravelLegend(
            entries = eligibleEntries,
            arrivalLabel = string(R.string.pdf_export_travel_type_arrival),
            departureLabel = string(R.string.pdf_export_travel_type_departure),
            continuationLabel = string(R.string.pdf_export_travel_type_continuation),
            travelLabel = string(R.string.pdf_export_travel_type_travel)
        )
        val style = chooseStyle(eligibleEntries.size, legendText.isNotBlank())

        val pdfDocument = PdfDocument()
        var pageNum = 1
        var page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create())
        var canvas = page.canvas

        val headerInfo = ExportHeaderInfo(employeeName, company, project, personnelNumber, startDate, endDate)
        var y = drawHeader(canvas, style, headerInfo)
        y = drawTableHeader(canvas, style, y)

        val dash = string(R.string.pdf_export_placeholder_dash)
        val footerBlockHeight = style.footerBlockHeight(legendText.isNotBlank())
        val rowHeight = style.effectiveRowHeight(eligibleEntries.size, legendText.isNotBlank())

        eligibleEntries.forEachIndexed { index, record ->
            val isLastEntry = index == eligibleEntries.lastIndex
            val reserve = if (isLastEntry) footerBlockHeight else 0f
            if (needsNewPage(y, rowHeight, reserve)) {
                pdfDocument.finishPage(page)
                pageNum++
                page = pdfDocument.startPage(
                    PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
                )
                canvas = page.canvas
                y = drawTableHeader(canvas, style, MARGIN.toFloat())
            }
            drawTableRow(canvas, style, record, dash, y, rowHeight)
            y += rowHeight
        }

        canvas.drawLine(MARGIN.toFloat(), y, (PAGE_WIDTH - MARGIN).toFloat(), y, style.line)
        y += AFTER_TABLE_GAP

        if (legendText.isNotBlank()) {
            y = drawLegend(canvas, style, legendText, y)
        }
        drawSummary(canvas, style, stats, y)

        pdfDocument.finishPage(page)
        return pdfDocument
    }

    /**
     * Entscheidet, ob eine Zeile mit Höhe [rowHeight] (plus [reserve] für Legende/Summe) noch
     * auf die aktuelle Seite passt.
     */
    private fun needsNewPage(y: Float, rowHeight: Float, reserve: Float): Boolean =
        y + rowHeight + reserve > PAGE_HEIGHT - MARGIN

    /**
     * Berechnet die für [entryCount] Einträge benötigte Seitenzahl mit derselben
     * Seitenumbruch-Arithmetik wie [renderPdfDocument] – jedoch ohne echtes Zeichnen, also ohne
     * [PdfDocument]/[Canvas]. Für Layout-Tests, die kein PDF-Rendering benötigen.
     */
    internal fun countPagesNeeded(entryCount: Int, legendPresent: Boolean): Int {
        if (entryCount <= 0) return 1
        val style = chooseStyle(entryCount, legendPresent)
        val rowHeight = style.effectiveRowHeight(entryCount, legendPresent)
        val footerBlockHeight = style.footerBlockHeight(legendPresent)

        var pageCount = 1
        var y = MARGIN.toFloat() + style.headerBlockHeight + style.tableHeaderHeight
        for (index in 0 until entryCount) {
            val isLastEntry = index == entryCount - 1
            val reserve = if (isLastEntry) footerBlockHeight else 0f
            if (needsNewPage(y, rowHeight, reserve)) {
                pageCount++
                y = MARGIN.toFloat() + style.tableHeaderHeight
            }
            y += rowHeight
        }
        return pageCount
    }

    /**
     * Wählt die großzügigste Schriftgrößen-Stufe, bei der alle Einträge auf eine Seite passen.
     * Passt kein normaler Datensatz mehr (Ausnahmefall), wird die kompakteste Stufe verwendet –
     * die Tabellen-Zeichenlogik bricht dann kontrolliert auf weitere Seiten um.
     */
    private fun chooseStyle(entryCount: Int, legendPresent: Boolean): PdfStyle {
        var fallback: PdfStyle? = null
        for (density in DENSITY_LEVELS) {
            val style = PdfStyle(density)
            fallback = style
            val fixedHeight = style.headerBlockHeight + style.tableHeaderHeight + style.footerBlockHeight(legendPresent)
            val availableForRows = PAGE_HEIGHT - 2 * MARGIN - fixedHeight
            val maxRows = (availableForRows / style.rowHeight).toInt()
            if (entryCount <= maxRows) {
                return style
            }
        }
        return fallback ?: PdfStyle(DENSITY_LEVELS.last())
    }

    /** Bündelt die Kopfbereich-Metadaten, um die Parameterliste von [drawHeader] kurz zu halten. */
    private data class ExportHeaderInfo(
        val employeeName: String,
        val company: String?,
        val project: String?,
        val personnelNumber: String?,
        val startDate: LocalDate,
        val endDate: LocalDate
    )

    /**
     * Zeichnet den kompakten Kopfbereich (Titel + max. zwei Metadatenzeilen) auf Seite 1.
     */
    private fun drawHeader(canvas: Canvas, style: PdfStyle, info: ExportHeaderInfo): Float {
        var lineTop = MARGIN.toFloat()

        val title = string(R.string.pdf_export_title, PdfUtilities.formatPeriodLabel(info.startDate, info.endDate))
        drawSingleLine(
            canvas, title, MARGIN.toFloat(),
            lineTop - style.title.fontMetrics.ascent,
            style.title, CONTENT_WIDTH.toFloat(), Align.LEFT
        )
        lineTop += style.title.fontSpacing + HEADER_TITLE_GAP

        val metaLine1 = buildMetaLine1(info)
        drawSingleLine(
            canvas, metaLine1, MARGIN.toFloat(),
            lineTop - style.meta.fontMetrics.ascent,
            style.meta, CONTENT_WIDTH.toFloat(), Align.LEFT
        )
        lineTop += style.meta.fontSpacing + HEADER_LINE_GAP

        val metaLine2 = buildMetaLine2(info.startDate, info.endDate)
        drawSingleLine(
            canvas, metaLine2, MARGIN.toFloat(),
            lineTop - style.meta.fontMetrics.ascent,
            style.meta, CONTENT_WIDTH.toFloat(), Align.LEFT
        )
        lineTop += style.meta.fontSpacing + HEADER_DIVIDER_GAP_BEFORE

        canvas.drawLine(MARGIN.toFloat(), lineTop, (PAGE_WIDTH - MARGIN).toFloat(), lineTop, style.line)
        lineTop += HEADER_DIVIDER_GAP_AFTER

        return lineTop
    }

    private fun buildMetaLine1(info: ExportHeaderInfo): String {
        return PdfUtilities.buildHeaderMetaLine1(
            employeeName = info.employeeName,
            employeeTemplate = rawString(R.string.pdf_export_header_employee),
            optionalFields = listOf(
                PdfUtilities.MetaField(info.personnelNumber, rawString(R.string.pdf_export_header_personnel_number)),
                PdfUtilities.MetaField(info.company, rawString(R.string.pdf_export_header_company)),
                PdfUtilities.MetaField(info.project, rawString(R.string.pdf_export_header_project))
            )
        )
    }

    private fun buildMetaLine2(startDate: LocalDate, endDate: LocalDate): String {
        val dateRange = if (startDate == endDate) {
            startDate.format(dateFormatter)
        } else {
            "${startDate.format(dateFormatter)}–${endDate.format(dateFormatter)}"
        }
        val range = string(R.string.pdf_export_header_range, dateRange)
        val created = string(R.string.pdf_export_header_created_at, LocalDate.now().format(dateFormatter))
        return "$range · $created"
    }

    private data class TableColumn(
        val width: Int,
        val headerText: String,
        val align: Align
    )

    /** Position und Größe einer Tabellenzelle, zur Bündelung von Zeichen-Parametern. */
    private data class CellBox(
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float
    )

    private fun tableColumns(): List<TableColumn> = listOf(
        TableColumn(COL_DATE, string(R.string.pdf_export_column_date), Align.CENTER),
        TableColumn(COL_LOCATION, string(R.string.pdf_export_column_location), Align.LEFT),
        TableColumn(COL_START, string(R.string.pdf_export_column_start), Align.CENTER),
        TableColumn(COL_END, string(R.string.pdf_export_column_end), Align.CENTER),
        TableColumn(COL_BREAK, string(R.string.pdf_export_column_break), Align.CENTER),
        TableColumn(COL_WORK, string(R.string.pdf_export_column_work), Align.RIGHT),
        TableColumn(COL_TRAVEL, string(R.string.pdf_export_column_travel_route), Align.LEFT),
        TableColumn(COL_TRAVEL_TIME, string(R.string.pdf_export_column_travel_time), Align.RIGHT),
        TableColumn(COL_VP, string(R.string.pdf_export_column_meal_allowance), Align.RIGHT)
    )

    /**
     * Zeichnet den Tabellenkopf (einzeilig) und gibt die y-Position der ersten Datenzeile zurück.
     */
    private fun drawTableHeader(canvas: Canvas, style: PdfStyle, y: Float): Float {
        val columns = tableColumns()
        val height = style.tableHeaderHeight - TABLE_HEADER_BOTTOM_GAP

        canvas.drawRect(
            MARGIN.toFloat(),
            y,
            (PAGE_WIDTH - MARGIN).toFloat(),
            y + height,
            style.tableHeaderBackground
        )

        var xPos = MARGIN.toFloat()
        columns.forEach { column ->
            drawCellText(
                canvas, column.headerText,
                CellBox(xPos, y, column.width.toFloat(), height),
                style.tableHeader, column.align
            )
            xPos += column.width
        }

        val bottomY = y + height
        canvas.drawLine(MARGIN.toFloat(), bottomY, (PAGE_WIDTH - MARGIN).toFloat(), bottomY, style.line)
        return y + style.tableHeaderHeight
    }

    /**
     * Zeichnet eine einzeilige Tabellenzeile. Zu lange Inhalte werden – nur als letzte Maßnahme –
     * mit Ellipsis abgekürzt statt mitten im Wort umgebrochen zu werden.
     */
    private fun drawTableRow(
        canvas: Canvas,
        style: PdfStyle,
        record: WorkEntryWithTravelLegs,
        dash: String,
        y: Float,
        rowHeight: Float
    ) {
        val columns = tableColumns()
        val texts = PdfUtilities.buildTableRowTexts(record, dash).toColumnList()
        var xPos = MARGIN.toFloat()
        columns.zip(texts).forEach { (column, text) ->
            val box = CellBox(xPos, y, column.width.toFloat(), rowHeight)
            drawCellText(canvas, text, box, style.tableText, column.align)
            xPos += column.width
        }
    }

    /**
     * Zeichnet einen einzeiligen Zellentext, vertikal zentriert innerhalb der Zeilenhöhe.
     */
    private fun drawCellText(
        canvas: Canvas,
        text: String,
        box: CellBox,
        paint: Paint,
        align: Align
    ) {
        val maxWidth = box.width - 2 * TABLE_CELL_HORIZONTAL_PADDING
        val fitted = fitTextToWidth(text, maxWidth, paint)
        val baseline = box.y + box.height / 2f - (paint.fontMetrics.ascent + paint.fontMetrics.descent) / 2f

        val previousAlign = paint.textAlign
        paint.textAlign = align
        val drawX = when (align) {
            Align.LEFT -> box.x + TABLE_CELL_HORIZONTAL_PADDING
            Align.RIGHT -> box.x + box.width - TABLE_CELL_HORIZONTAL_PADDING
            Align.CENTER -> box.x + box.width / 2f
        }
        canvas.drawText(fitted, drawX, baseline, paint)
        paint.textAlign = previousAlign
    }

    private fun drawSingleLine(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        paint: Paint,
        maxWidth: Float,
        align: Align
    ) {
        val fitted = fitTextToWidth(text, maxWidth, paint)
        val previousAlign = paint.textAlign
        paint.textAlign = align
        canvas.drawText(fitted, x, y, paint)
        paint.textAlign = previousAlign
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

    /**
     * Zeichnet die Legende der verwendeten Reiseart-Kurzcodes, z.B. "A = Anreise · AB = Abreise".
     */
    private fun drawLegend(canvas: Canvas, style: PdfStyle, legendText: String, y: Float): Float {
        val lineTop = y + LEGEND_TOP_GAP
        drawSingleLine(
            canvas, legendText, MARGIN.toFloat(),
            lineTop - style.legend.fontMetrics.ascent,
            style.legend, CONTENT_WIDTH.toFloat(), Align.LEFT
        )
        return y + style.legendHeight(true)
    }

    /**
     * Zeichnet die kompakte, hervorgehobene Summenzeile unterhalb der Tabelle.
     */
    private fun drawSummary(canvas: Canvas, style: PdfStyle, stats: WorkStatsResult, y: Float): Float {
        val barTop = y + SUMMARY_TOP_GAP
        val barHeight = style.summary.fontSpacing + 2 * SUMMARY_VERTICAL_PADDING
        canvas.drawRect(
            MARGIN.toFloat(),
            barTop,
            (PAGE_WIDTH - MARGIN).toFloat(),
            barTop + barHeight,
            style.summaryBackground
        )

        val totalWorkHours = stats.totalWorkMinutes / 60.0
        val totalTravelHours = stats.totalTravelMinutes / 60.0
        val totalPaidHours = stats.totalPaidMinutes / 60.0

        val totalMealAllowance = MealAllowanceCalculator.formatEuro(stats.mealAllowanceCents)
        val summaryText = listOf(
            string(R.string.pdf_export_summary_work_days, stats.workDays),
            string(R.string.pdf_export_summary_work_time, PdfUtilities.formatWorkHours(totalWorkHours)),
            string(R.string.pdf_export_summary_travel_time, PdfUtilities.formatWorkHours(totalTravelHours)),
            string(R.string.pdf_export_summary_paid_time, PdfUtilities.formatWorkHours(totalPaidHours)),
            string(R.string.pdf_export_summary_meal_allowance, totalMealAllowance)
        ).joinToString(" · ")

        val summaryMetrics = style.summary.fontMetrics
        val baseline = barTop + barHeight / 2f - (summaryMetrics.ascent + summaryMetrics.descent) / 2f
        val previousAlign = style.summary.textAlign
        style.summary.textAlign = Align.LEFT
        canvas.drawText(
            fitTextToWidth(summaryText, CONTENT_WIDTH - 2 * SUMMARY_HORIZONTAL_PADDING, style.summary),
            MARGIN.toFloat() + SUMMARY_HORIZONTAL_PADDING,
            baseline,
            style.summary
        )
        style.summary.textAlign = previousAlign

        return barTop + barHeight
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

        val timestampPattern = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss", Locale.GERMAN)
        val timestamp = LocalDateTime.now().format(timestampPattern)
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
