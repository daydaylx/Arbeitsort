package de.montagezeit.app.diagnostics.debug

import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.diagnostics.DiagnosticCategory
import de.montagezeit.app.diagnostics.DiagnosticDateRange
import de.montagezeit.app.diagnostics.DiagnosticTraceRequest
import de.montagezeit.app.diagnostics.DiagnosticWarningCodes
import de.montagezeit.app.diagnostics.toSanitizedDiagnosticPayload
import de.montagezeit.app.domain.util.MealAllowanceCalculator
import de.montagezeit.app.domain.util.TimeCalculator
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class IntegrityScanResult(
    val traceId: String,
    val scannedEntries: Int,
    val anomalyCount: Int
)

@Singleton
class DiagnosticsIntegrityScanner @Inject constructor(
    private val workEntryDao: WorkEntryDao,
    private val diagnosticsController: DebugDiagnosticsController
) {

    suspend fun scanRecentDays(daysBack: Long = 90): IntegrityScanResult = withContext(Dispatchers.IO) {
        val endDate = LocalDate.now()
        val startDate = endDate.minusDays(daysBack.coerceAtLeast(1) - 1)
        val trace = diagnosticsController.startTrace(
            DiagnosticTraceRequest(
                category = DiagnosticCategory.INTEGRITY_SCAN,
                name = "integrity_scan_recent_days",
                sourceClass = "DiagnosticsIntegrityScanner",
                screenOrWorker = "DeveloperDiagnostics",
                dateRange = DiagnosticDateRange(startDate = startDate, endDate = endDate),
                payload = mapOf("daysBack" to daysBack)
            )
        )

        val entries = workEntryDao.getByDateRangeWithTravel(startDate, endDate)
        trace.event(
            name = "entries_loaded",
            payload = mapOf("entryCount" to entries.size)
        )

        var anomalyCount = 0
        entries.forEach { record ->
            val entry = record.workEntry
            val workMinutes = TimeCalculator.calculateWorkMinutes(entry)
            val travelMinutes = TimeCalculator.calculateTravelMinutes(record.orderedTravelLegs)
            val snapshotPayload = mapOf(
                "entry" to entry.toSanitizedDiagnosticPayload(),
                "travelLegCount" to record.orderedTravelLegs.size,
                "workMinutes" to workMinutes,
                "travelMinutes" to travelMinutes
            )

            if (entry.confirmedWorkDay && entry.dayType == DayType.WORK && workMinutes == 0 && travelMinutes == 0) {
                anomalyCount += 1
                trace.warning(DiagnosticWarningCodes.EMPTY_CONFIRMED_WORK_DAY, snapshotPayload)
            }
            if (entry.dayType == DayType.OFF && (entry.workStart != null || entry.workEnd != null)) {
                anomalyCount += 1
                trace.warning(DiagnosticWarningCodes.OFF_DAY_HAS_WORK_BLOCK, snapshotPayload)
            }
            if (entry.dayType == DayType.COMP_TIME && record.orderedTravelLegs.isNotEmpty()) {
                anomalyCount += 1
                trace.warning(DiagnosticWarningCodes.COMP_TIME_HAS_TRAVEL, snapshotPayload)
            }

            val storedMealSnapshot = MealAllowanceCalculator.resolveEffectiveStoredSnapshot(record)
            val eligible = MealAllowanceCalculator.isEligible(
                dayType = entry.dayType,
                workMinutes = workMinutes,
                travelMinutes = travelMinutes
            )
            if (!eligible && storedMealSnapshot.amountCents > 0) {
                anomalyCount += 1
                trace.warning(
                    DiagnosticWarningCodes.MEAL_ALLOWANCE_INELIGIBLE_VALUE_PRESENT,
                    snapshotPayload + mapOf(
                        "mealAllowanceAmountCents" to storedMealSnapshot.amountCents,
                        "mealAllowanceBaseCents" to storedMealSnapshot.baseCents
                    )
                )
            }
        }

        trace.finish(
            payload = mapOf(
                "scannedEntries" to entries.size,
                "anomalyCount" to anomalyCount,
                "dateRange" to mapOf(
                    "startDate" to startDate.toString(),
                    "endDate" to endDate.toString()
                )
            )
        )

        IntegrityScanResult(
            traceId = trace.traceId,
            scannedEntries = entries.size,
            anomalyCount = anomalyCount
        )
    }
}
