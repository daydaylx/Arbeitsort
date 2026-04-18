package de.montagezeit.app.export

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.storage.StorageManager
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntryWithTravelLegs
import de.montagezeit.app.domain.usecase.isStatisticsEligible
import de.montagezeit.app.domain.util.MealAllowanceCalculator
import de.montagezeit.app.domain.util.TimeCalculator
import de.montagezeit.app.export.PdfUtilities.buildTravelRouteSummary
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

internal fun filterCsvExportEntries(entries: List<WorkEntryWithTravelLegs>): List<WorkEntryWithTravelLegs> =
    entries.filter(::isStatisticsEligible)

@Singleton
class CsvExporter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    private companion object {
        const val MIN_DISK_SPACE_BYTES = 1024L * 1024L // 1 MB
    }

    /**
     * Exportiert Einträge als CSV-Datei.
     *
     * @param entries Liste der zu exportierenden Einträge
     * @return Uri zur CSV-Datei bei Erfolg, null bei Fehler
     */
    fun exportToCsv(entries: List<WorkEntryWithTravelLegs>): Uri? {
        val eligibleEntries = filterCsvExportEntries(entries)

        // Validate input
        if (eligibleEntries.isEmpty()) {
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
            val availableBytes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.getSystemService(StorageManager::class.java)
                    ?.getAllocatableBytes(StorageManager.UUID_DEFAULT) ?: cacheDir.usableSpace
            } else {
                cacheDir.usableSpace
            }
            if (availableBytes < MIN_DISK_SPACE_BYTES) {
                android.util.Log.e("CsvExporter", "Insufficient disk space: $availableBytes bytes")
                return null
            }

            FileOutputStream(file).use {
                it.write(0xEF)
                it.write(0xBB)
                it.write(0xBF) // UTF-8 BOM

                // Header: workStart/workEnd/breakMinutes nur für WORK-Tage befüllt
                // confirmedWorkDay: 1 wenn Tag fachlich bestätigt, 0 sonst
                val header = "date;dayType;confirmedWorkDay;dayLocation;workStart;workEnd;breakMinutes;workMinutes;travelMinutes;travelLegCount;travelRouteSummary;paidTotalMinutes;mealIsArrivalDeparture;mealBreakfastIncluded;mealAllowanceBaseCents;mealAllowanceAmountCents;mealAllowanceEuro;note\n"
                it.write(header.toByteArray(Charsets.UTF_8))

                eligibleEntries.forEach { record ->
                    val entry = record.workEntry
                    val travelLegs = record.orderedTravelLegs
                    val workMinutes = TimeCalculator.calculateWorkMinutes(entry)
                    val travelMinutes = TimeCalculator.calculateTravelMinutes(travelLegs)
                    val paidTotalMinutes = TimeCalculator.calculatePaidTotalMinutes(entry, travelLegs)
                    val travelRouteSummary = buildTravelRouteSummary(travelLegs)
                    val mealSnapshot = MealAllowanceCalculator.resolveEffectiveStoredSnapshot(record)

                    val dayTypeLabel = when (entry.dayType) {
                        DayType.COMP_TIME -> "Ausgleich"
                        else -> entry.dayType.name
                    }
                    val isWorkDay = entry.dayType.isWorkLike
                    val line = buildString {
                        append(entry.date.format(dateFormatter))
                        append(";")
                        append(dayTypeLabel)
                        append(";")
                        append(if (entry.confirmedWorkDay) 1 else 0)
                        append(";")
                        append(CsvCellEncoder.encode(entry.dayLocationLabel))
                        append(";")
                        append(if (isWorkDay) entry.workStart?.format(timeFormatter).orEmpty() else "")
                        append(";")
                        append(if (isWorkDay) entry.workEnd?.format(timeFormatter).orEmpty() else "")
                        append(";")
                        append(if (isWorkDay) entry.breakMinutes.toString() else "")
                        append(";")
                        append(workMinutes)
                        append(";")
                        append(travelMinutes)
                        append(";")
                        append(travelLegs.size)
                        append(";")
                        append(CsvCellEncoder.encode(travelRouteSummary))
                        append(";")
                        append(paidTotalMinutes)
                        append(";")
                        append(if (mealSnapshot.isArrivalDeparture) 1 else 0)
                        append(";")
                        append(if (mealSnapshot.breakfastIncluded) 1 else 0)
                        append(";")
                        append(mealSnapshot.baseCents)
                        append(";")
                        append(mealSnapshot.amountCents)
                        append(";")
                        append(MealAllowanceCalculator.formatEuro(mealSnapshot.amountCents))
                        append(";")
                        append(CsvCellEncoder.encode(entry.note ?: ""))
                        append("\n")
                    }
                    it.write(line.toByteArray(Charsets.UTF_8))
                }
            }

            android.util.Log.i(
                "CsvExporter",
                "Successfully exported ${eligibleEntries.size} entries to $filename"
            )

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
