package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.entity.TravelLeg
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.repository.WorkEntryRepository
import de.montagezeit.app.domain.util.WorkEntryDerivedStateNormalizer

internal suspend fun WorkEntryRepository.normalizeForPersistence(entry: WorkEntry): WorkEntry {
    return WorkEntryDerivedStateNormalizer.normalize(entry, getTravelLegsByDate(entry.date))
}

internal fun normalizeForPersistence(entry: WorkEntry, travelLegs: List<TravelLeg>): WorkEntry {
    return WorkEntryDerivedStateNormalizer.normalize(entry, travelLegs)
}
