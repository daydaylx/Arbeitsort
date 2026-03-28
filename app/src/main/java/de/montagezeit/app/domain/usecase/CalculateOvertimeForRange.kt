package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntryWithTravelLegs
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

    operator fun invoke(entries: List<WorkEntryWithTravelLegs>, dailyTargetHours: Double): OvertimeResult {
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
        
        val classifyDay = ClassifyDay()

        entries.forEach { entry ->
            // Tage berücksichtigen, wenn sie entweder bestätigt sind oder Reisezeit enthalten
            // (Damit werden reine Reisetage nicht mehr stillschweigend ignoriert)
            val isConfirmedOrHasTravel = entry.workEntry.confirmedWorkDay || entry.orderedTravelLegs.isNotEmpty()
            if (!isConfirmedOrHasTravel) {
                return@forEach
            }
            
            val classification = classifyDay(entry)

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
                    val travelHours = TimeCalculator.calculateTravelMinutes(entry.orderedTravelLegs) / 60.0
                    if (travelHours > 0.0) {
                        // Fahrzeit an freien Tagen wird nun ebenfalls als Ist-Zeit erfasst
                        totalActualHours += travelHours
                        offDayTravelHours += travelHours
                        offDayTravelDays += 1
                    }
                }
                DayClassification.FREI -> {
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
        )
    }
}
