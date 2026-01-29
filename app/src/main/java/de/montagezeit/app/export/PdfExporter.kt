package de.montagezeit.app.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.domain.util.TimeCalculator
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import android.os.StatFs

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
    
    // PDF-Konstanten (A4 in Punkten: 595 x 842)
    companion object {
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
        private const val COL_DATE = 55
        private const val COL_START = 45
        private const val COL_END = 45
        private const val COL_BREAK = 40
        private const val COL_WORK_TIME = 55
        private const val COL_TRAVEL_WINDOW = 70
        private const val COL_TRAVEL_TIME = 50
        private const val COL_TOTAL_TIME = 55
        private const val COL_LOCATION = 50
        private const val COL_NOTE = 50 // Rest
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
     * @return Uri der erstellten Datei oder null bei Fehler
     */
    fun exportToPdf(
        entries: List<WorkEntry>,
        employeeName: String,
        company: String? = null,
        project: String? = null,
        personnelNumber: String? = null,
        startDate: java.time.LocalDate,
        endDate: java.time.LocalDate
    ): Uri? {
        return try {
            // Pflichtfeld-Validierung
            if (employeeName.isBlank()) {
                throw IllegalArgumentException("Name fehlt")
            }
            
            val pdfDocument = PdfDocument()
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
            val fileUri = writePdfFile(pdfDocument, startDate, endDate)
            
            pdfDocument.close()
            fileUri
        } catch (e: Exception) {
            e.printStackTrace()
            null
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
        canvas.drawText("Arbeitsnachweis", MARGIN.toFloat(), y, paintTitle)
        y += SPACING * 2
        
        // Mitarbeiter-Info
        canvas.drawText("Mitarbeiter: $employeeName", MARGIN.toFloat(), y, paintHeader)
        y += 20
        
        company?.let {
            canvas.drawText("Firma: $it", MARGIN.toFloat(), y, paintHeader)
            y += 20
        }
        
        project?.let {
            canvas.drawText("Projekt: $it", MARGIN.toFloat(), y, paintHeader)
            y += 20
        }
        
        personnelNumber?.let {
            canvas.drawText("Personalnummer: $it", MARGIN.toFloat(), y, paintHeader)
            y += 20
        }
        
        // Zeitraum
        val dateRange = if (startDate == endDate) {
            dateFormat.format(java.sql.Date.valueOf(startDate.toString()))
        } else {
            "${dateFormat.format(java.sql.Date.valueOf(startDate.toString()))} - ${dateFormat.format(java.sql.Date.valueOf(endDate.toString()))}"
        }
        canvas.drawText("Zeitraum: $dateRange", MARGIN.toFloat(), y, paintHeader)
        y += 20
        
        // Erstelldatum
        canvas.drawText("Erstellt am: ${timestampFormat.format(Date())}", MARGIN.toFloat(), y, paintHeader)
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
        canvas.drawText("Datum", xPos, yPos + 20, paintTableHeader)
        xPos += COL_DATE
        
        canvas.drawText("Start", xPos, yPos + 20, paintTableHeader)
        xPos += COL_START
        
        canvas.drawText("Ende", xPos, yPos + 20, paintTableHeader)
        xPos += COL_END
        
        canvas.drawText("Pause", xPos, yPos + 20, paintTableHeader)
        xPos += COL_BREAK
        
        canvas.drawText("Arbeit", xPos, yPos + 20, paintTableHeader)
        xPos += COL_WORK_TIME

        canvas.drawText("Reise (von–bis)", xPos, yPos + 20, paintTableHeader)
        xPos += COL_TRAVEL_WINDOW

        canvas.drawText("Reisezeit", xPos, yPos + 20, paintTableHeader)
        xPos += COL_TRAVEL_TIME

        canvas.drawText("Gesamt", xPos, yPos + 20, paintTableHeader)
        xPos += COL_TOTAL_TIME

        canvas.drawText("Ort", xPos, yPos + 20, paintTableHeader)
        xPos += COL_LOCATION

        canvas.drawText("Notiz", xPos, yPos + 20, paintTableHeader)
        
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
        entries: List<WorkEntry>,
        startY: Float
    ): TableDrawResult {
        var y = startY
        var pageNum = 1
        var activePage = currentPage
        var activeCanvas = canvas
        
        entries.forEach { entry ->
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
            
            // Start
            activeCanvas.drawText(PdfUtilities.formatTime(entry.workStart), xPos, y + 15, paintTableText)
            xPos += COL_START
            
            // Ende
            activeCanvas.drawText(PdfUtilities.formatTime(entry.workEnd), xPos, y + 15, paintTableText)
            xPos += COL_END
            
            // Pause
            activeCanvas.drawText("${entry.breakMinutes} min", xPos, y + 15, paintTableText)
            xPos += COL_BREAK
            
            // Arbeitszeit
            val workHours = TimeCalculator.calculateWorkHours(entry)
            val travelMinutes = TimeCalculator.calculateTravelMinutes(entry)
            val travelHours = travelMinutes / 60.0
            val totalHours = workHours + travelHours
            val workText = "${PdfUtilities.formatWorkHours(workHours)} h"
            activeCanvas.drawText(workText, xPos, y + 15, paintTableText)
            xPos += COL_WORK_TIME

            // Reise (von–bis)
            val travelWindow = PdfUtilities.formatTravelWindow(entry.travelStartAt, entry.travelArriveAt)
            val travelWindowText = if (travelWindow.isNotBlank()) travelWindow else "–"
            activeCanvas.drawText(travelWindowText, xPos, y + 15, paintTableText)
            xPos += COL_TRAVEL_WINDOW

            // Reisezeit
            val travelTime = PdfUtilities.formatTravelTime(travelMinutes)
            val travelTimeText = if (travelMinutes > 0) "$travelTime h" else "–"
            activeCanvas.drawText(travelTimeText, xPos, y + 15, paintTableText)
            xPos += COL_TRAVEL_TIME

            // Gesamtzeit
            val totalText = "${PdfUtilities.formatWorkHours(totalHours)} h"
            activeCanvas.drawText(totalText, xPos, y + 15, paintTableText)
            xPos += COL_TOTAL_TIME

            // Ort
            val location = PdfUtilities.getLocation(entry)
            activeCanvas.drawText(location.take(8), xPos, y + 15, paintTableText) // Truncate zu 8 Zeichen
            xPos += COL_LOCATION

            // Notiz
            val note = PdfUtilities.getNote(entry)
            activeCanvas.drawText(note.take(10), xPos, y + 15, paintTableText) // Truncate zu 10 Zeichen
            
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
    private fun drawSummary(canvas: Canvas, entries: List<WorkEntry>, y: Float): Float {
        var yPos = y + 20
        
        val workDays = PdfUtilities.filterWorkDays(entries).size
        val totalWorkHours = PdfUtilities.sumWorkHours(entries)
        val totalTravelMinutes = PdfUtilities.sumTravelMinutes(entries)
        val totalTravelHours = totalTravelMinutes / 60.0
        val totalPaidHours = totalWorkHours + totalTravelHours
        
        canvas.drawText("Arbeitstage: $workDays", MARGIN.toFloat(), yPos, paintSummary)
        yPos += 25
        
        canvas.drawText(
            "Summe Arbeitszeit: ${PdfUtilities.formatWorkHours(totalWorkHours)} Stunden",
            MARGIN.toFloat(),
            yPos,
            paintSummary
        )
        yPos += 25
        
        canvas.drawText(
            "Summe Reisezeit: ${PdfUtilities.formatWorkHours(totalTravelHours)} Stunden",
            MARGIN.toFloat(),
            yPos,
            paintSummary
        )
        yPos += 25

        canvas.drawText(
            "Summe Gesamtzeit (bezahlt): ${PdfUtilities.formatWorkHours(totalPaidHours)} Stunden",
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
        canvas.drawText("Mitarbeiter", MARGIN.toFloat(), yPos, paintSignature)
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
        canvas.drawText("Vorgesetzter", MARGIN.toFloat(), yPos, paintSignature)
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
    ): Uri? {
        return try {
            // Cache-Verzeichnis verwenden
            val cacheDir = File(context.cacheDir, "exports")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }

            // Speicherplatz-Check BEVOR PDF geschrieben wird
            val stat = StatFs(cacheDir.absolutePath)
            val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
            val minRequiredBytes = 5 * 1024 * 1024L  // 5MB Reserve
            if (availableBytes < minRequiredBytes) {
                throw IOException("Nicht genug Speicherplatz verfügbar (benötigt: 5 MB)")
            }

            // Dateiname mit Zeitraum
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.GERMAN).format(Date())
            val dateRange = if (startDate == endDate) {
                startDate.toString()
            } else {
                "${startDate}_${endDate}"
            }
            val filename = "montagezeit_pdf_${dateRange}_$timestamp.pdf"
            val file = File(cacheDir, filename)

            // PDF schreiben mit Fehlerbehandlung
            try {
                FileOutputStream(file).use { fos ->
                    pdfDocument.writeTo(fos)
                }
            } catch (e: IOException) {
                file.delete()  // Partielle Datei löschen
                throw e  // Exception weiterwerfen
            }
            
            // Uri via FileProvider erstellen
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
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
            putExtra(Intent.EXTRA_SUBJECT, "MontageZeit PDF Export")
        }
        return Intent.createChooser(intent, "MontageZeit PDF Export teilen")
    }
}
