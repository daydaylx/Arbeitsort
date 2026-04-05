package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntryWithTravelLegs
import de.montagezeit.app.domain.model.DayClassification
import de.montagezeit.app.domain.util.MealAllowanceCalculator
import de.montagezeit.app.domain.util.TimeCalculator

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
        
        // Sichtbare Arbeitstage ohne UEBERSTUNDEN_ABBAU; Soll-/Overtime-Zähler separat
        val visibleWorkDays = classifiedDays.count {
            it.classification == DayClassification.ARBEITSTAG_MIT_ARBEIT ||
                it.classification == DayClassification.ARBEITSTAG_NUR_REISE ||
                it.classification == DayClassification.ARBEITSTAG_LEER
        }
        val targetCountedDays = classifiedDays.count { it.classification.isCountedWorkDay }
        val offDays = eligibleEntries.size - visibleWorkDays
        val totalWorkMinutes = eligibleEntries.sumOf { TimeCalculator.calculateWorkMinutes(it.workEntry) }
        val totalTravelMinutes = eligibleEntries.sumOf { TimeCalculator.calculateTravelMinutes(it.orderedTravelLegs) }

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
