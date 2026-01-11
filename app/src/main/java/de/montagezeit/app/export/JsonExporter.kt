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
 * JSON Exporter für MontageZeit
 *
 * Exportiert WorkEntries im JSON-Format (UTF-8)
 * für maschinenlesbare Weiterverarbeitung und API-Integration.
 */
@Singleton
class JsonExporter @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val timestampFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.GERMAN)

    /**
     * Exportiert alle WorkEntries in eine JSON-Datei
     *
     * @param entries Die zu exportierenden Einträge
     * @return Uri der erstellten Datei oder null bei Fehler
     */
    fun exportToJson(entries: List<WorkEntry>): Uri? {
        val jsonContent = generateJsonContent(entries)
        return writeJsonFile(jsonContent)
    }

    /**
     * Erstellt eine Share Intent für die JSON-Datei
     *
     * @param fileUri Die Uri der JSON-Datei
     * @return Intent für das Teilen der Datei
     */
    fun createShareIntent(fileUri: Uri): Intent {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(Intent.EXTRA_SUBJECT, "MontageZeit Export (JSON)")
        }
        return Intent.createChooser(intent, "MontageZeit Export teilen")
    }

    private fun generateJsonContent(entries: List<WorkEntry>): String {
        val sb = StringBuilder()

        sb.append("{\n")
        sb.append("  \"exportVersion\": \"1.0\",\n")
        sb.append("  \"exportedAt\": \"${timestampFormat.format(Date())}\",\n")
        sb.append("  \"entryCount\": ${entries.size},\n")
        sb.append("  \"entries\": [\n")

        entries.forEachIndexed { index, entry ->
            sb.append("    {\n")

            // Grunddaten
            sb.append("      \"date\": \"${entry.date}\",\n")
            sb.append("      \"dayType\": \"${entry.dayType.name}\",\n")
            sb.append("      \"workStart\": \"${entry.workStart}\",\n")
            sb.append("      \"workEnd\": \"${entry.workEnd}\",\n")
            sb.append("      \"breakMinutes\": ${entry.breakMinutes},\n")

            // Morning Snapshot
            sb.append("      \"morningSnapshot\": {\n")
            appendOptionalTimestamp(sb, "capturedAt", entry.morningCapturedAt, indent = "        ")
            appendOptionalString(sb, "locationLabel", entry.morningLocationLabel, indent = "        ")
            appendOptionalDouble(sb, "latitude", entry.morningLat, indent = "        ")
            appendOptionalDouble(sb, "longitude", entry.morningLon, indent = "        ")
            appendOptionalFloat(sb, "accuracyMeters", entry.morningAccuracyMeters, indent = "        ")
            appendOptionalBoolean(sb, "outsideLeipzig", entry.outsideLeipzigMorning, indent = "        ", isLast = true)
            sb.append("      },\n")

            // Evening Snapshot
            sb.append("      \"eveningSnapshot\": {\n")
            appendOptionalTimestamp(sb, "capturedAt", entry.eveningCapturedAt, indent = "        ")
            appendOptionalString(sb, "locationLabel", entry.eveningLocationLabel, indent = "        ")
            appendOptionalDouble(sb, "latitude", entry.eveningLat, indent = "        ")
            appendOptionalDouble(sb, "longitude", entry.eveningLon, indent = "        ")
            appendOptionalFloat(sb, "accuracyMeters", entry.eveningAccuracyMeters, indent = "        ")
            appendOptionalBoolean(sb, "outsideLeipzig", entry.outsideLeipzigEvening, indent = "        ", isLast = true)
            sb.append("      },\n")

            // Travel Events
            sb.append("      \"travelEvents\": {\n")
            appendOptionalTimestamp(sb, "startAt", entry.travelStartAt, indent = "        ")
            appendOptionalTimestamp(sb, "arriveAt", entry.travelArriveAt, indent = "        ")
            appendOptionalString(sb, "labelStart", entry.travelLabelStart, indent = "        ")
            appendOptionalString(sb, "labelEnd", entry.travelLabelEnd, indent = "        ", isLast = true)
            sb.append("      },\n")

            // Meta
            appendOptionalString(sb, "note", entry.note, indent = "      ")
            sb.append("      \"needsReview\": ${entry.needsReview},\n")
            sb.append("      \"createdAt\": ${entry.createdAt},\n")
            sb.append("      \"updatedAt\": ${entry.updatedAt}\n")

            if (index < entries.size - 1) {
                sb.append("    },\n")
            } else {
                sb.append("    }\n")
            }
        }

        sb.append("  ]\n")
        sb.append("}\n")

        return sb.toString()
    }

    private fun writeJsonFile(content: String): Uri? {
        return try {
            // Cache-Verzeichnis verwenden
            val cacheDir = File(context.cacheDir, "exports")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }

            // Dateiname mit Timestamp
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.GERMAN).format(Date())
            val file = File(cacheDir, "montagezeit_export_$timestamp.json")

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

    private fun appendOptionalTimestamp(
        sb: StringBuilder,
        key: String,
        value: Long?,
        indent: String,
        isLast: Boolean = false
    ) {
        if (value != null && value > 0) {
            sb.append("$indent\"$key\": \"${timestampFormat.format(Date(value))}")
            sb.append(if (isLast) "\"\n" else "\",\n")
        } else {
            sb.append("$indent\"$key\": null")
            sb.append(if (isLast) "\n" else ",\n")
        }
    }

    private fun appendOptionalString(
        sb: StringBuilder,
        key: String,
        value: String?,
        indent: String,
        isLast: Boolean = false
    ) {
        val escapedValue = value?.let { escapeJson(it) }
        sb.append("$indent\"$key\": ")
        sb.append(if (escapedValue != null) "\"$escapedValue\"" else "null")
        sb.append(if (isLast) "\n" else ",\n")
    }

    private fun appendOptionalDouble(
        sb: StringBuilder,
        key: String,
        value: Double?,
        indent: String,
        isLast: Boolean = false
    ) {
        sb.append("$indent\"$key\": ")
        sb.append(value?.toString() ?: "null")
        sb.append(if (isLast) "\n" else ",\n")
    }

    private fun appendOptionalFloat(
        sb: StringBuilder,
        key: String,
        value: Float?,
        indent: String,
        isLast: Boolean = false
    ) {
        sb.append("$indent\"$key\": ")
        sb.append(value?.toString() ?: "null")
        sb.append(if (isLast) "\n" else ",\n")
    }

    private fun appendOptionalBoolean(
        sb: StringBuilder,
        key: String,
        value: Boolean?,
        indent: String,
        isLast: Boolean = false
    ) {
        sb.append("$indent\"$key\": ")
        sb.append(value?.toString() ?: "null")
        sb.append(if (isLast) "\n" else ",\n")
    }

    private fun escapeJson(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}
