package de.montagezeit.app.export

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.storage.StorageManager
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import de.montagezeit.app.R
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntryWithTravelLegs
import de.montagezeit.app.domain.usecase.isStatisticsEligible
import de.montagezeit.app.domain.util.MealAllowanceCalculator
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

        val requiresQuoting = hardened.any { it == ';' || it == '"' || it == '\n' || it == '\r' }
        return if (requiresQuoting) {
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
    sealed interface CsvExportResult {
        data class Success(val fileUri: Uri) : CsvExportResult
        data class ValidationError(val message: String) : CsvExportResult
        data class StorageError(val message: String) : CsvExportResult
        data class FileWriteError(val message: String) : CsvExportResult
        data class SecurityError(val message: String) : CsvExportResult
        data class UnknownError(val message: String) : CsvExportResult
    }

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    private companion object {
        const val MIN_DISK_SPACE_BYTES = 1024L * 1024L // 1 MB
        const val CSV_HEADER =
            "date;dayType;confirmedWorkDay;dayLocation;workStart;workEnd;breakMinutes;workMinutes;" +
                "travelMinutes;travelLegCount;travelRouteSummary;paidTotalMinutes;mealIsArrivalDeparture;" +
                "mealBreakfastIncluded;mealAllowanceBaseCents;mealAllowanceAmountCents;mealAllowanceEuro;note\n"
    }

    private sealed interface CsvFilePreparation {
        data class Ready(val file: File) : CsvFilePreparation
        data class Error(val result: CsvExportResult) : CsvFilePreparation
    }

    /**
     * Exportiert Einträge als CSV-Datei.
     *
     * @param entries Liste der zu exportierenden Einträge
     * @return Strukturiertes Exportergebnis mit konkretem Fehlergrund
     */
    fun exportToCsv(
        entries: List<WorkEntryWithTravelLegs>,
        dailyTargetHours: Double = 8.0
    ): CsvExportResult {
        val eligibleEntries = filterCsvExportEntries(entries)

        // Validate input
        if (eligibleEntries.isEmpty()) {
            android.util.Log.w("CsvExporter", "No entries to export")
            return CsvExportResult.ValidationError(context.getString(R.string.export_preview_empty_range))
        }

        return when (val prepared = prepareCsvFile()) {
            is CsvFilePreparation.Error -> prepared.result
            is CsvFilePreparation.Ready -> writeCsvFile(
                file = prepared.file,
                entries = eligibleEntries,
                dailyTargetHours = dailyTargetHours
            )
        }
    }

    private fun prepareCsvFile(): CsvFilePreparation {
        val cacheDir = File(context.cacheDir, "exports")
        val storageError = validateCacheDir(cacheDir) ?: validateDiskSpace(cacheDir)
        if (storageError != null) {
            return CsvFilePreparation.Error(storageError)
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.GERMAN).format(Date())
        return CsvFilePreparation.Ready(File(cacheDir, "montagezeit_export_$timestamp.csv"))
    }

    private fun validateCacheDir(cacheDir: File): CsvExportResult.StorageError? {
        var error: CsvExportResult.StorageError? = null
        if (!cacheDir.exists() && !cacheDir.mkdirs()) {
            android.util.Log.e("CsvExporter", "Failed to create export directory")
            error = CsvExportResult.StorageError(context.getString(R.string.csv_export_error_export_dir_failed))
        } else if (!cacheDir.canWrite()) {
            android.util.Log.e("CsvExporter", "Export directory is not writable")
            error = CsvExportResult.StorageError(context.getString(R.string.csv_export_error_export_dir_not_writable))
        }

        return error
    }

    private fun validateDiskSpace(cacheDir: File): CsvExportResult.StorageError? {
        val availableBytes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.getSystemService(StorageManager::class.java)
                ?.getAllocatableBytes(StorageManager.UUID_DEFAULT) ?: cacheDir.usableSpace
        } else {
            cacheDir.usableSpace
        }
        if (availableBytes < MIN_DISK_SPACE_BYTES) {
            android.util.Log.e("CsvExporter", "Insufficient disk space: $availableBytes bytes")
            return CsvExportResult.StorageError(context.getString(R.string.csv_export_error_not_enough_storage))
        }

        return null
    }

    private fun writeCsvFile(
        file: File,
        entries: List<WorkEntryWithTravelLegs>,
        dailyTargetHours: Double
    ): CsvExportResult = try {
        FileOutputStream(file).use {
            it.write(0xEF)
            it.write(0xBB)
            it.write(0xBF)
            it.write(CSV_HEADER.toByteArray(Charsets.UTF_8))

            entries.forEach { record ->
                it.write(buildCsvLine(record, dailyTargetHours).toByteArray(Charsets.UTF_8))
            }
        }

        android.util.Log.i("CsvExporter", "Successfully exported ${entries.size} entries to ${file.name}")
        CsvExportResult.Success(
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        )
    } catch (e: java.io.IOException) {
        android.util.Log.e("CsvExporter", "IO error during export", e)
        CsvExportResult.FileWriteError(context.getString(R.string.csv_export_error_write_failed))
    } catch (e: SecurityException) {
        android.util.Log.e("CsvExporter", "Permission error during export", e)
        CsvExportResult.SecurityError(context.getString(R.string.csv_export_error_permission_denied))
    } catch (e: Exception) {
        android.util.Log.e("CsvExporter", "Unexpected error during export", e)
        CsvExportResult.UnknownError(context.getString(R.string.settings_error_csv_export_failed))
    }

    private fun buildCsvLine(record: WorkEntryWithTravelLegs, dailyTargetHours: Double): String {
        val entry = record.workEntry
        val metrics = buildExportDayMetrics(record, dailyTargetHours)
        val mealSnapshot = MealAllowanceCalculator.resolveEffectiveStoredSnapshot(record)
        val travelRouteSummary = if (metrics.isWorkDay) buildTravelRouteSummary(metrics.travelLegs) else ""

        return listOf(
            entry.date.format(dateFormatter),
            csvDayTypeLabel(entry.dayType),
            if (entry.confirmedWorkDay) "1" else "0",
            CsvCellEncoder.encode(workDayText(metrics.isWorkDay) { entry.dayLocationLabel }),
            workDayText(metrics.isWorkDay) { entry.workStart?.format(timeFormatter).orEmpty() },
            workDayText(metrics.isWorkDay) { entry.workEnd?.format(timeFormatter).orEmpty() },
            workDayText(metrics.isWorkDay) { entry.breakMinutes.toString() },
            metrics.workMinutes.toString(),
            metrics.travelMinutes.toString(),
            metrics.travelLegs.size.toString(),
            CsvCellEncoder.encode(travelRouteSummary),
            metrics.paidTotalMinutes.toString(),
            if (mealSnapshot.isArrivalDeparture) "1" else "0",
            if (mealSnapshot.breakfastIncluded) "1" else "0",
            mealSnapshot.baseCents.toString(),
            mealSnapshot.amountCents.toString(),
            MealAllowanceCalculator.formatEuro(mealSnapshot.amountCents),
            CsvCellEncoder.encode(entry.note ?: "")
        ).joinToString(separator = ";", postfix = "\n")
    }

    private fun workDayText(isWorkDay: Boolean, value: () -> String): String {
        return if (isWorkDay) value() else ""
    }

    private fun csvDayTypeLabel(dayType: DayType): String = when (dayType) {
        DayType.WORK, DayType.OFF -> dayType.name
        DayType.VACATION -> "Urlaub"
        DayType.COMP_TIME -> "Ausgleich"
    }

}
