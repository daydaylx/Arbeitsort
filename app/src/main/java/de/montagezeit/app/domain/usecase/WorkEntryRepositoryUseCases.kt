package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.entity.TravelLeg
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.local.entity.WorkEntryWithTravelLegs
import de.montagezeit.app.data.repository.WorkEntryRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

class ObserveWorkEntryByDate(private val repository: WorkEntryRepository) {
    operator fun invoke(date: LocalDate): Flow<WorkEntry?> = repository.getByDateFlow(date)
}

class ObserveWorkEntryWithTravelByDate(private val repository: WorkEntryRepository) {
    operator fun invoke(date: LocalDate): Flow<WorkEntryWithTravelLegs?> = repository.getByDateWithTravelFlow(date)
}

class ObserveWorkEntriesWithTravelByDateRange(private val repository: WorkEntryRepository) {
    operator fun invoke(startDate: LocalDate, endDate: LocalDate): Flow<List<WorkEntryWithTravelLegs>> =
        repository.getByDateRangeWithTravelFlow(startDate, endDate)
}

class GetWorkEntryByDate(private val repository: WorkEntryRepository) {
    suspend operator fun invoke(date: LocalDate): WorkEntry? = repository.getByDate(date)
}

class GetWorkEntryWithTravelByDate(private val repository: WorkEntryRepository) {
    suspend operator fun invoke(date: LocalDate): WorkEntryWithTravelLegs? = repository.getByDateWithTravel(date)
}

class GetWorkEntriesByDateRange(private val repository: WorkEntryRepository) {
    suspend operator fun invoke(startDate: LocalDate, endDate: LocalDate): List<WorkEntry> =
        repository.getByDateRange(startDate, endDate)
}

class GetWorkEntriesWithTravelByDateRange(private val repository: WorkEntryRepository) {
    suspend operator fun invoke(startDate: LocalDate, endDate: LocalDate): List<WorkEntryWithTravelLegs> =
        repository.getByDateRangeWithTravel(startDate, endDate)
}

class UpsertWorkEntries(private val repository: WorkEntryRepository) {
    suspend operator fun invoke(entries: List<WorkEntry>) = repository.upsertAll(entries)
}

class DeleteWorkEntryByDate(private val repository: WorkEntryRepository) {
    suspend operator fun invoke(date: LocalDate) = repository.deleteByDate(date)
}

class ReplaceWorkEntryWithTravelLegs(private val repository: WorkEntryRepository) {
    suspend operator fun invoke(entry: WorkEntry, legs: List<TravelLeg>) =
        repository.replaceEntryWithTravelLegs(entry, legs)
}
