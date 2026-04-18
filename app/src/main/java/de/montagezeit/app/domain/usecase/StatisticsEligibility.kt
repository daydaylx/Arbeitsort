package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.entity.WorkEntryWithTravelLegs

/**
 * Statistik-Eligibility folgt denselben Regeln wie UI-Status und Reminder-Terminalzustand.
 */
fun isStatisticsEligible(entry: WorkEntryWithTravelLegs): Boolean {
    return EntryStatusResolver.resolve(entry).isStatisticsEligible
}
