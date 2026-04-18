package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.entity.WorkEntryWithTravelLegs
import de.montagezeit.app.diagnostics.DiagnosticTrace
import de.montagezeit.app.diagnostics.DiagnosticWarningCodes
import de.montagezeit.app.diagnostics.toSanitizedDiagnosticPayload
import de.montagezeit.app.domain.model.DayClassification
import de.montagezeit.app.domain.util.MealAllowanceCalculator

/**
 * Ergebnis der Arbeitsstatistik-Aggregation.
 * 
 * Enthält sowohl die klassischen Metriken (workDays, offDays) als auch
 * die neue differenzierte Tageszählung basierend auf DayClassification.
 */
data class WorkStatsResult(
    // Sichtbare Metriken für UI/Export
    val workDays: Int,
    val targetCountedDays: Int,
    val offDays: Int,
    val totalWorkMinutes: Int,
    val totalTravelMinutes: Int,
    val totalPaidMinutes: Int,
    val mealAllowanceCents: Int,
    
    // Neue differenzierte Metriken
    val workDaysWithWork: Int,           // ARBEITSTAG_MIT_ARBEIT
    val workDaysTravelOnly: Int,         // ARBEITSTAG_NUR_REISE
    val workDaysEmpty: Int,              // ARBEITSTAG_LEER
    val compTimeDays: Int,               // UEBERSTUNDEN_ABBAU
    val freeDaysWithTravel: Int,         // FREI_MIT_REISE
    val freeDaysWithoutTravel: Int       // FREI
) {
    val averageWorkHoursPerDay: Double
        get() = if (workDays > 0) totalWorkMinutes / 60.0 / workDays else 0.0

    /**
     * Durchschnittliche bezahlte Stunden pro Arbeitstag (inkl. Reisezeit).
     */
    val averagePaidHoursPerWorkDay: Double
        get() = if (workDays > 0) totalPaidMinutes / 60.0 / workDays else 0.0
    
    /**
     * Durchschnittliche Arbeitsstunden pro Arbeitstag mit Arbeit.
     * (Exkludiert ARBEITSTAG_NUR_REISE und ARBEITSTAG_LEER)
     */
    val averageWorkHoursPerWorkDayWithWork: Double
        get() = if (workDaysWithWork > 0) totalWorkMinutes / 60.0 / workDaysWithWork else 0.0
}

class AggregateWorkStats {
    operator fun invoke(
        entries: List<WorkEntryWithTravelLegs>,
        trace: DiagnosticTrace? = null
    ): WorkStatsResult {
        val resolvedEntries = entries.map { entry ->
            ResolvedEntry(entry = entry, status = EntryStatusResolver.resolve(entry))
        }
        resolvedEntries
            .filter {
                it.entry.workEntry.confirmedWorkDay &&
                    it.entry.workEntry.dayType == de.montagezeit.app.data.local.entity.DayType.WORK &&
                    !it.status.hasActivity
            }
            .forEach { resolved ->
                trace?.warning(
                    DiagnosticWarningCodes.EMPTY_CONFIRMED_WORK_DAY,
                    payload = mapOf(
                        "classification" to resolved.status.classification.name,
                        "entry" to resolved.entry.toSanitizedDiagnosticPayload()
                    )
                )
            }
        val eligibleEntries = resolvedEntries.filter { it.status.isStatisticsEligible }
        trace?.event(
            name = "aggregate_work_stats_start",
            payload = mapOf(
                "entryCount" to entries.size,
                "eligibleEntryCount" to eligibleEntries.size
            )
        )
        
        // Neue Klassifikation-basierte Zählung
        val classifiedDays = eligibleEntries.map { resolved ->
            val entry = resolved.entry
            val status = resolved.status
            trace?.event(
                name = "aggregate_entry",
                payload = mapOf(
                    "classification" to status.classification.name,
                    "workMinutes" to status.workMinutes,
                    "travelMinutes" to status.travelMinutes,
                    "entry" to entry.toSanitizedDiagnosticPayload()
                )
            )
            if (!MealAllowanceCalculator.isEligible(
                    dayType = entry.workEntry.dayType,
                    workMinutes = status.workMinutes,
                    travelMinutes = status.travelMinutes
                ) &&
                entry.workEntry.mealAllowanceAmountCents > 0
            ) {
                trace?.warning(
                    DiagnosticWarningCodes.MEAL_ALLOWANCE_INELIGIBLE_VALUE_PRESENT,
                    payload = mapOf(
                        "classification" to status.classification.name,
                        "mealAllowanceAmountCents" to entry.workEntry.mealAllowanceAmountCents,
                        "entry" to entry.toSanitizedDiagnosticPayload()
                    )
                )
            }
            ClassifiedDayWithEntry(
                classification = status.classification,
                entry = entry
            )
        }
        
        // Sichtbare Arbeitstage ohne UEBERSTUNDEN_ABBAU; Soll-/Overtime-Zähler separat
        val visibleWorkDays = classifiedDays.count {
            it.classification == DayClassification.ARBEITSTAG_MIT_ARBEIT ||
                it.classification == DayClassification.ARBEITSTAG_NUR_REISE
        }
        val targetCountedDays = classifiedDays.count { it.classification.isCountedWorkDay }
        val offDays = eligibleEntries.size - visibleWorkDays
        val totalWorkMinutes = eligibleEntries.sumOf { it.status.workMinutes }
        val totalTravelMinutes = eligibleEntries.sumOf { it.status.travelMinutes }

        val mealAllowanceCents = classifiedDays
            .sumOf { MealAllowanceCalculator.resolveEffectiveStoredSnapshot(it.entry).amountCents }
        
        // Neue differenzierte Metriken
        val workDaysWithWork = classifiedDays.count { it.classification == DayClassification.ARBEITSTAG_MIT_ARBEIT }
        val workDaysTravelOnly = classifiedDays.count { it.classification == DayClassification.ARBEITSTAG_NUR_REISE }
        val workDaysEmpty = classifiedDays.count { it.classification == DayClassification.ARBEITSTAG_LEER }
        val compTimeDays = classifiedDays.count { it.classification == DayClassification.UEBERSTUNDEN_ABBAU }
        val freeDaysWithTravel = classifiedDays.count { it.classification == DayClassification.FREI_MIT_REISE }
        val freeDaysWithoutTravel = classifiedDays.count { it.classification == DayClassification.FREI }
        
        return WorkStatsResult(
            workDays = visibleWorkDays,
            targetCountedDays = targetCountedDays,
            offDays = offDays,
            totalWorkMinutes = totalWorkMinutes,
            totalTravelMinutes = totalTravelMinutes,
            totalPaidMinutes = totalWorkMinutes + totalTravelMinutes,
            mealAllowanceCents = mealAllowanceCents,
            workDaysWithWork = workDaysWithWork,
            workDaysTravelOnly = workDaysTravelOnly,
            workDaysEmpty = workDaysEmpty,
            compTimeDays = compTimeDays,
            freeDaysWithTravel = freeDaysWithTravel,
            freeDaysWithoutTravel = freeDaysWithoutTravel
        ).also { result ->
            trace?.event(
                name = "aggregate_work_stats_result",
                phase = de.montagezeit.app.diagnostics.DiagnosticPhase.RESULT,
                payload = mapOf(
                    "workDays" to result.workDays,
                    "targetCountedDays" to result.targetCountedDays,
                    "offDays" to result.offDays,
                    "totalWorkMinutes" to result.totalWorkMinutes,
                    "totalTravelMinutes" to result.totalTravelMinutes,
                    "totalPaidMinutes" to result.totalPaidMinutes,
                    "mealAllowanceCents" to result.mealAllowanceCents,
                    "workDaysWithWork" to result.workDaysWithWork,
                    "workDaysTravelOnly" to result.workDaysTravelOnly,
                    "workDaysEmpty" to result.workDaysEmpty,
                    "compTimeDays" to result.compTimeDays,
                    "freeDaysWithTravel" to result.freeDaysWithTravel,
                    "freeDaysWithoutTravel" to result.freeDaysWithoutTravel
                )
            )
        }
    }
    
    /**
     * Hilfsklasse für die interne Verarbeitung.
     */
    private data class ClassifiedDayWithEntry(
        val classification: DayClassification,
        val entry: WorkEntryWithTravelLegs
    )

    private data class ResolvedEntry(
        val entry: WorkEntryWithTravelLegs,
        val status: EntryStatus
    )
}
