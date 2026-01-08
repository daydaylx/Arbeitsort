package de.montagezeit.app.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import de.montagezeit.app.data.local.entity.WorkEntry
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CSV Exporter für MontageZeit
 * 
 * Exportiert WorkEntries im CSV-Format (Semikolon-separiert, UTF-8)
 * für die Verwendung in Excel, Google Sheets, etc.
 */
@Singleton
class CsvExporter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.GERMAN)
    private val timestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.GERMAN)
    
    /**
     * Exportiert alle WorkEntries in eine CSV-Datei
     * 
     * @param entries Die zu exportierenden Einträge
     * @return Uri der erstellten Datei oder null bei Fehler
     */
    fun exportToCsv(entries: List<WorkEntry>): Uri? {
        val csvContent = generateCsvContent(entries)
        return writeCsvFile(csvContent)
    }
    
    /**
     * Erstellt eine Share Intent für die CSV-Datei
     * 
     * @param fileUri Die Uri der CSV-Datei
     * @return Intent für das Teilen der Datei
     */
    fun createShareIntent(fileUri: Uri): Intent {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(Intent.EXTRA_SUBJECT, "MontageZeit Export")
        }
        return Intent.createChooser(intent, "MontageZeit Export teilen")
    }
    
    private fun generateCsvContent(entries: List<WorkEntry>): String {
        val sb = StringBuilder()
        
        // Header
        sb.appendLine(buildString {
            append("date;dayType;workStart;workEnd;breakMinutes;")
            append("morningCapturedAt;morningLocationLabel;morningOutside;")
            append("eveningCapturedAt;eveningLocationLabel;eveningOutside;")
            append("travelStartAt;travelArriveAt;travelLabelStart;travelLabelEnd;")
            append("note;needsReview;createdAt;updatedAt")
        })
        
        // Datenzeilen
        entries.forEach { entry ->
            sb.appendLine(buildString {
                // Grunddaten
                append(formatDate(entry.date)).append(";")
                append(entry.dayType.name).append(";")
                append(entry.workStart).append(";")
                append(entry.workEnd).append(";")
                append(entry.breakMinutes).append(";")
                
                // Morning
                append(formatTimestamp(entry.morningCapturedAt)).append(";")
                append(escapeCsv(entry.morningLocationLabel)).append(";")
                append(entry.outsideLeipzigMorning?.toString() ?: "").append(";")
                
                // Evening
                append(formatTimestamp(entry.eveningCapturedAt)).append(";")
                append(escapeCsv(entry.eveningLocationLabel)).append(";")
                append(entry.outsideLeipzigEvening?.toString() ?: "").append(";")
                
                // Travel
                append(formatTimestamp(entry.travelStartAt)).append(";")
                append(formatTimestamp(entry.travelArriveAt)).append(";")
                append(escapeCsv(entry.travelLabelStart)).append(";")
                append(escapeCsv(entry.travelLabelEnd)).append(";")
                
                // Meta
                append(escapeCsv(entry.note)).append(";")
                append(entry.needsReview).append(";")
                append(formatTimestamp(entry.createdAt)).append(";")
                append(formatTimestamp(entry.updatedAt))
            })
        }
        
        return sb.toString()
    }
    
    private fun writeCsvFile(content: String): Uri? {
        return try {
            // Cache-Verzeichnis verwenden
            val cacheDir = File(context.cacheDir, "exports")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            
            // Dateiname mit Timestamp
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.GERMAN).format(Date())
            val file = File(cacheDir, "montagezeit_export_$timestamp.csv")
            
            FileOutputStream(file).use { fos ->
                fos.write(content.toByteArray(Charsets.UTF_8))
            }
            
            // Uri via FileProvider erstellen
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
    
    private fun formatDate(date: java.time.LocalDate): String {
        return date.toString()
    }
    
    private fun formatTimestamp(timestamp: Long?): String {
        return if (timestamp != null && timestamp > 0) {
            timestampFormat.format(Date(timestamp))
        } else {
            ""
        }
    }
    
    private fun escapeCsv(value: String?): String {
        if (value == null) return ""
        
        // Semikolon escapen
        val escaped = value.replace(";", "\\;")
        
        // Anführungszeichen escapen
        return escaped.replace("\"", "\\\"")
    }
}
