package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.entity.WorkEntryWithTravelLegs

/**
 * Eintrag geht nur dann in Statistik- und Überstundenlogik ein,
 * wenn der Tag fachlich bestätigt wurde.
 */
fun isStatisticsEligible(entry: WorkEntryWithTravelLegs): Boolean {
    return entry.workEntry.confirmedWorkDay
}
