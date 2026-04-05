package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntryWithTravelLegs
import de.montagezeit.app.domain.model.DayClassification
import de.montagezeit.app.domain.util.TimeCalculator

/**
 * Ergebnis der Arbeitsstatistik-Aggregation.
 * 
 * Enthält sowohl die klassischen Metriken (workDays, offDays) als auch
 * die neue differenzierte Tageszählung basierend auf DayClassification.
 */
data class WorkStatsResult(
    // Klassische Metriken (für Backwards Compatibility)
    val workDays: Int,
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
        get() {
            val effectiveDays = workDays - compTimeDays
            return if (effectiveDays > 0) totalWorkMinutes / 60.0 / effectiveDays else 0.0
        }

    /**
     * Durchschnittliche bezahlte Stunden pro Arbeitstag (inkl. Reisezeit).
     */
    val averagePaidHoursPerWorkDay: Double
        get() {
            val effectiveDays = workDays - compTimeDays
            return if (effectiveDays > 0) totalPaidMinutes / 60.0 / effectiveDays else 0.0
        }
    
    /**
     * Durchschnittliche Arbeitsstunden pro Arbeitstag mit Arbeit.
     * (Exkludiert ARBEITSTAG_NUR_REISE und ARBEITSTAG_LEER)
     */
    val averageWorkHoursPerWorkDayWithWork: Double
        get() = if (workDaysWithWork > 0) totalWorkMinutes / 60.0 / workDaysWithWork else 0.0
}

class AggregateWorkStats {

    private val classifier = ClassifyDay()

    operator fun invoke(entries: List<WorkEntryWithTravelLegs>): WorkStatsResult {
        val eligibleEntries = entries.filter(::isStatisticsEligible)
        
        // Neue Klassifikation-basierte Zählung
        val classifiedDays = eligibleEntries.map { entry ->
            ClassifiedDayWithEntry(
                classification = classifier(entry),
                entry = entry
            )
        }
        
        // Klassische Metriken (synchronisiert mit Overtime-Logik)
        val workDays = classifiedDays.count { it.classification.isCountedWorkDay }
        val offDays = eligibleEntries.size - workDays
        val totalWorkMinutes = eligibleEntries.sumOf { TimeCalculator.calculateWorkMinutes(it.workEntry) }
        val totalTravelMinutes = eligibleEntries.sumOf { TimeCalculator.calculateTravelMinutes(it.orderedTravelLegs) }
        
        // Verpflegungspauschale: Nur für ARBEITSTAG_MIT_ARBEIT und ARBEITSTAG_NUR_REISE
        val mealAllowanceCents = classifiedDays
            .filter { it.classification.isMealAllowanceEligible }
            .sumOf { it.entry.workEntry.mealAllowanceAmountCents }
        
        // Neue differenzierte Metriken
        val workDaysWithWork = classifiedDays.count { it.classification == DayClassification.ARBEITSTAG_MIT_ARBEIT }
        val workDaysTravelOnly = classifiedDays.count { it.classification == DayClassification.ARBEITSTAG_NUR_REISE }
        val workDaysEmpty = classifiedDays.count { it.classification == DayClassification.ARBEITSTAG_LEER }
        val compTimeDays = classifiedDays.count { it.classification == DayClassification.UEBERSTUNDEN_ABBAU }
        val freeDaysWithTravel = classifiedDays.count { it.classification == DayClassification.FREI_MIT_REISE }
        val freeDaysWithoutTravel = classifiedDays.count { it.classification == DayClassification.FREI }
        
        return WorkStatsResult(
            workDays = workDays,
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
        )
    }
    
    /**
     * Hilfsklasse für die interne Verarbeitung.
     */
    private data class ClassifiedDayWithEntry(
        val classification: DayClassification,
        val entry: WorkEntryWithTravelLegs
    )
}
