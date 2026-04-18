package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntryWithTravelLegs
import de.montagezeit.app.diagnostics.DiagnosticTrace
import de.montagezeit.app.diagnostics.toSanitizedDiagnosticPayload
import de.montagezeit.app.domain.model.DayClassification

data class OvertimeResult(
    val totalOvertimeHours: Double,
    val totalActualHours: Double,
    val totalTargetHours: Double,
    val countedDays: Int,
    val offDayTravelHours: Double,
    val offDayTravelDays: Int
)

class CalculateOvertimeForRange {
    operator fun invoke(
        entries: List<WorkEntryWithTravelLegs>,
        dailyTargetHours: Double,
        trace: DiagnosticTrace? = null
    ): OvertimeResult {
        require(dailyTargetHours > 0) { "dailyTargetHours must be > 0" }

        if (entries.isEmpty()) {
            return OvertimeResult(
                totalOvertimeHours = 0.0,
                totalActualHours = 0.0,
                totalTargetHours = 0.0,
                countedDays = 0,
                offDayTravelHours = 0.0,
                offDayTravelDays = 0
            )
        }

        var totalActualHours = 0.0
        var totalTargetHours = 0.0
        var countedDays = 0
        var offDayTravelHours = 0.0
        var offDayTravelDays = 0

        entries.forEach { entry ->
            val status = EntryStatusResolver.resolve(entry)
            if (!status.isStatisticsEligible) {
                return@forEach
            }
            trace?.event(
                name = "overtime_entry",
                payload = mapOf(
                    "classification" to status.classification.name,
                    "dailyTargetHours" to dailyTargetHours,
                    "workMinutes" to status.workMinutes,
                    "travelMinutes" to status.travelMinutes,
                    "entry" to entry.toSanitizedDiagnosticPayload()
                )
            )

            when (status.classification) {
                DayClassification.ARBEITSTAG_MIT_ARBEIT,
                DayClassification.ARBEITSTAG_NUR_REISE -> {
                    countedDays += 1
                    totalTargetHours += dailyTargetHours
                    totalActualHours += (status.workMinutes + status.travelMinutes) / 60.0
                }
                DayClassification.FREI_MIT_REISE -> {
                    val travelHours = status.travelMinutes / 60.0
                    if (travelHours > 0.0) {
                        // Fahrzeit an freien Tagen wird nun ebenfalls als Ist-Zeit erfasst
                        totalActualHours += travelHours
                        offDayTravelHours += travelHours
                        offDayTravelDays += 1
                    }
                }
                DayClassification.FREI -> {
                    // Keine Stunden, kein Ziel
                }
                DayClassification.ARBEITSTAG_LEER -> Unit
                DayClassification.UEBERSTUNDEN_ABBAU -> {
                    if (entry.workEntry.dayType == DayType.COMP_TIME) {
                        countedDays += 1
                        totalTargetHours += dailyTargetHours
                    }
                }
            }
        }

        return OvertimeResult(
            totalOvertimeHours = totalActualHours - totalTargetHours,
            totalActualHours = totalActualHours,
            totalTargetHours = totalTargetHours,
            countedDays = countedDays,
            offDayTravelHours = offDayTravelHours,
            offDayTravelDays = offDayTravelDays
        ).also { result ->
            trace?.event(
                name = "overtime_result",
                phase = de.montagezeit.app.diagnostics.DiagnosticPhase.RESULT,
                payload = mapOf(
                    "totalOvertimeHours" to result.totalOvertimeHours,
                    "totalActualHours" to result.totalActualHours,
                    "totalTargetHours" to result.totalTargetHours,
                    "countedDays" to result.countedDays,
                    "offDayTravelHours" to result.offDayTravelHours,
                    "offDayTravelDays" to result.offDayTravelDays
                )
            )
        }
    }
}
