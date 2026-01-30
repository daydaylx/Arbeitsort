package de.montagezeit.app.export

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.domain.util.TimeCalculator
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CsvExporter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun exportToCsv(entries: List<WorkEntry>): Uri? {
        return try {
            val cacheDir = File(context.cacheDir, "exports")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.GERMAN).format(Date())
            val filename = "montagezeit_export_$timestamp.csv"
            val file = File(cacheDir, filename)

            FileOutputStream(file).use {
                it.write(0xEF)
                it.write(0xBB)
                it.write(0xBF) // UTF-8 BOM
                
                // Header
                val header = "date;dayType;dayLocation;dayLocationSource;workStart;workEnd;breakMinutes;workMinutes;travelMinutes;paidTotalMinutes;note\n"
                it.write(header.toByteArray(Charsets.UTF_8))

                entries.forEach { entry ->
                    val workMinutes = TimeCalculator.calculateWorkMinutes(entry)
                    val travelMinutes = TimeCalculator.calculateTravelMinutes(entry)
                    val paidTotalMinutes = TimeCalculator.calculatePaidTotalMinutes(entry)
                    
                    val line = buildString {
                        append(entry.date.format(dateFormatter))
                        append(";")
                        append(entry.dayType.name)
                        append(";")
                        append(entry.dayLocationLabel.replace(";", ","))
                        append(";")
                        append(entry.dayLocationSource.name)
                        append(";")
                        append(entry.workStart.format(timeFormatter))
                        append(";")
                        append(entry.workEnd.format(timeFormatter))
                        append(";")
                        append(entry.breakMinutes)
                        append(";")
                        append(workMinutes)
                        append(";")
                        append(travelMinutes)
                        append(";")
                        append(paidTotalMinutes) // NEW FIELD
                        append(";")
                        append(entry.note?.replace(";", ",")?.replace("\n", " ")?.replace("\r", "") ?: "") // Escape special chars
                        append("\n")
                    }
                    it.write(line.toByteArray(Charsets.UTF_8))
                }
            }

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
}
