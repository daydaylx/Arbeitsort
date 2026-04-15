package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntryWithTravelLegs
import de.montagezeit.app.diagnostics.DiagnosticTrace
import de.montagezeit.app.diagnostics.toSanitizedDiagnosticPayload
import de.montagezeit.app.domain.model.DayClassification
import de.montagezeit.app.domain.util.TimeCalculator

data class OvertimeResult(
    val totalOvertimeHours: Double,
    val totalActualHours: Double,
    val totalTargetHours: Double,
    val countedDays: Int,
    val offDayTravelHours: Double,
    val offDayTravelDays: Int
)

class CalculateOvertimeForRange {

    private val classifyDay = ClassifyDay()

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
            if (!isStatisticsEligible(entry)) {
                return@forEach
            }
            
            val classification = classifyDay(entry)
            val workMinutes = TimeCalculator.calculateWorkMinutes(entry.workEntry)
            val travelMinutes = TimeCalculator.calculateTravelMinutes(entry.orderedTravelLegs)
            trace?.event(
                name = "overtime_entry",
                payload = mapOf(
                    "classification" to classification.name,
                    "dailyTargetHours" to dailyTargetHours,
                    "workMinutes" to workMinutes,
                    "travelMinutes" to travelMinutes,
                    "entry" to entry.toSanitizedDiagnosticPayload()
                )
            )

            when (classification) {
                DayClassification.ARBEITSTAG_MIT_ARBEIT,
                DayClassification.ARBEITSTAG_NUR_REISE -> {
                    countedDays += 1
                    totalTargetHours += dailyTargetHours
                    totalActualHours += TimeCalculator.calculatePaidTotalHours(entry.workEntry, entry.orderedTravelLegs)
                }
                DayClassification.ARBEITSTAG_LEER -> {
                    // Leere Arbeitstage zählen nur, wenn sie explizit bestätigt wurden
                    if (entry.workEntry.confirmedWorkDay) {
                        countedDays += 1
                        totalTargetHours += dailyTargetHours
                    }
                }
                DayClassification.FREI_MIT_REISE -> {
                    val travelHours = travelMinutes / 60.0
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
                DayClassification.UEBERSTUNDEN_ABBAU -> {
                    // Wenn es ein COMP_TIME (Überstundenabbau) Tag ist, reduzieren wir das Saldo
                    if (entry.workEntry.dayType == DayType.COMP_TIME && entry.workEntry.confirmedWorkDay) {
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
