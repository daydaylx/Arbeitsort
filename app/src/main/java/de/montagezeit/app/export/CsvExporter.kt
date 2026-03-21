package de.montagezeit.app.export

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.domain.util.MealAllowanceCalculator
import de.montagezeit.app.domain.util.TimeCalculator
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

internal object CsvCellEncoder {
    private val formulaPrefixes = setOf('=', '+', '-', '@')

    fun encode(value: String): String {
        val hardened = if (value.trimStart().firstOrNull() in formulaPrefixes) {
            "'$value"
        } else {
            value
        }

        return if (hardened.contains(';') || hardened.contains('"') || hardened.contains('\n') || hardened.contains('\r')) {
            "\"${hardened.replace("\"", "\"\"")}\""
        } else {
            hardened
        }
    }
}

@Singleton
class CsvExporter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    /**
     * Exportiert Einträge als CSV-Datei.
     *
     * @param entries Liste der zu exportierenden Einträge
     * @return Uri zur CSV-Datei bei Erfolg, null bei Fehler
     */
    fun exportToCsv(entries: List<WorkEntry>): Uri? {
        // Validate input
        if (entries.isEmpty()) {
            android.util.Log.w("CsvExporter", "No entries to export")
            return null
        }

        return try {
            val cacheDir = File(context.cacheDir, "exports")

            // Ensure directory exists and is writable
            if (!cacheDir.exists()) {
                val created = cacheDir.mkdirs()
                if (!created) {
                    android.util.Log.e("CsvExporter", "Failed to create export directory")
                    return null
                }
            }

            if (!cacheDir.canWrite()) {
                android.util.Log.e("CsvExporter", "Export directory is not writable")
                return null
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.GERMAN).format(Date())
            val filename = "montagezeit_export_$timestamp.csv"
            val file = File(cacheDir, filename)

            // Check available disk space (require at least 1MB)
            val usableSpace = cacheDir.usableSpace
            if (usableSpace < 1024 * 1024) {
                android.util.Log.e("CsvExporter", "Insufficient disk space: $usableSpace bytes")
                return null
            }

            FileOutputStream(file).use {
                it.write(0xEF)
                it.write(0xBB)
                it.write(0xBF) // UTF-8 BOM

                // Header: workStart/workEnd/breakMinutes nur für WORK-Tage befüllt
                // confirmedWorkDay: 1 wenn Tag fachlich bestätigt, 0 sonst
                val header = "date;dayType;confirmedWorkDay;dayLocation;workStart;workEnd;breakMinutes;workMinutes;travelMinutes;paidTotalMinutes;mealIsArrivalDeparture;mealBreakfastIncluded;mealAllowanceBaseCents;mealAllowanceAmountCents;mealAllowanceEuro;note\n"
                it.write(header.toByteArray(Charsets.UTF_8))

                entries.forEach { entry ->
                    val workMinutes = TimeCalculator.calculateWorkMinutes(entry)
                    val travelMinutes = TimeCalculator.calculateTravelMinutes(entry)
                    val paidTotalMinutes = TimeCalculator.calculatePaidTotalMinutes(entry)

                    val dayTypeLabel = when (entry.dayType) {
                        DayType.COMP_TIME -> "Ü-Abbau"
                        else -> entry.dayType.name
                    }
                    // Zeitfelder nur für WORK-Tage befüllen – bei OFF/COMP_TIME sind sie fachlich irrelevant
                    val isWorkDay = entry.dayType == DayType.WORK
                    val line = buildString {
                        append(entry.date.format(dateFormatter))
                        append(";")
                        append(dayTypeLabel)
                        append(";")
                        append(if (entry.confirmedWorkDay) 1 else 0)
                        append(";")
                        append(CsvCellEncoder.encode(entry.dayLocationLabel))
                        append(";")
                        append(if (isWorkDay) entry.workStart.format(timeFormatter) else "")
                        append(";")
                        append(if (isWorkDay) entry.workEnd.format(timeFormatter) else "")
                        append(";")
                        append(if (isWorkDay) entry.breakMinutes.toString() else "")
                        append(";")
                        append(workMinutes)
                        append(";")
                        append(travelMinutes)
                        append(";")
                        append(paidTotalMinutes)
                        append(";")
                        append(if (entry.mealIsArrivalDeparture) 1 else 0)
                        append(";")
                        append(if (entry.mealBreakfastIncluded) 1 else 0)
                        append(";")
                        append(entry.mealAllowanceBaseCents)
                        append(";")
                        append(entry.mealAllowanceAmountCents)
                        append(";")
                        append(MealAllowanceCalculator.formatEuro(entry.mealAllowanceAmountCents))
                        append(";")
                        append(CsvCellEncoder.encode(entry.note ?: ""))
                        append("\n")
                    }
                    it.write(line.toByteArray(Charsets.UTF_8))
                }
            }

            android.util.Log.i("CsvExporter", "Successfully exported ${entries.size} entries to $filename")

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: java.io.IOException) {
            android.util.Log.e("CsvExporter", "IO error during export", e)
            null
        } catch (e: SecurityException) {
            android.util.Log.e("CsvExporter", "Permission error during export", e)
            null
        } catch (e: Exception) {
            android.util.Log.e("CsvExporter", "Unexpected error during export", e)
            null
        }
    }
}
