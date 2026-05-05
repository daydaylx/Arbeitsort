package de.montagezeit.app.data.repository

import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.TravelLeg
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.local.entity.WorkEntryWithTravelLegs
import de.montagezeit.app.domain.util.WorkEntryDerivedStateNormalizer
import java.util.LinkedHashMap
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class RoomWorkEntryRepository @Inject constructor(
    private val workEntryDao: WorkEntryDao
) : WorkEntryRepository {
    private val writeMutexesGuard = Any()
    private val writeMutexes = LinkedHashMap<LocalDate, DateMutex>(
        MAX_CACHED_DATE_MUTEXES,
        LOAD_FACTOR,
        true
    )

    override suspend fun getByDate(date: LocalDate): WorkEntry? = workEntryDao.getByDate(date)

    override fun getByDateFlow(date: LocalDate): Flow<WorkEntry?> = workEntryDao.getByDateFlow(date)

    override suspend fun getByDateWithTravel(date: LocalDate): WorkEntryWithTravelLegs? =
        workEntryDao.getByDateWithTravel(date)

    override fun getByDateWithTravelFlow(date: LocalDate): Flow<WorkEntryWithTravelLegs?> =
        workEntryDao.getByDateWithTravelFlow(date)

    override suspend fun getByDateRange(startDate: LocalDate, endDate: LocalDate): List<WorkEntry> =
        workEntryDao.getByDateRange(startDate, endDate)

    override suspend fun getByDateRangeWithTravel(
        startDate: LocalDate,
        endDate: LocalDate
    ): List<WorkEntryWithTravelLegs> = workEntryDao.getByDateRangeWithTravel(startDate, endDate)

    override fun getAllWithTravelFlow(): Flow<List<WorkEntryWithTravelLegs>> =
        workEntryDao.getAllWithTravelFlow()

    override fun getByDateRangeWithTravelFlow(
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<WorkEntryWithTravelLegs>> = workEntryDao.getByDateRangeWithTravelFlow(startDate, endDate)

    override suspend fun getLatestDayLocationLabelByDayType(dayType: DayType): String? =
        workEntryDao.getLatestDayLocationLabelByDayType(dayType)

    override suspend fun upsert(entry: WorkEntry) = withDateLock(entry.date) {
        workEntryDao.upsert(normalizeWithStoredTravel(entry))
    }

    override suspend fun upsertAll(entries: List<WorkEntry>) = withDateLocks(entries.map(WorkEntry::date)) {
        val normalizedEntries = mutableListOf<WorkEntry>()
        for (entry in entries) {
            normalizedEntries += normalizeWithStoredTravel(entry)
        }
        workEntryDao.upsertAll(normalizedEntries)
    }

    override suspend fun upsertAllAndDeleteTravelLegs(
        entries: List<WorkEntry>,
        travelLegDatesToDelete: List<LocalDate>
    ) = withDateLocks(entries.map(WorkEntry::date) + travelLegDatesToDelete) {
        val deletedDates = travelLegDatesToDelete.toSet()
        val normalizedEntries = entries.map { entry ->
            val travelLegs = if (entry.date in deletedDates) {
                emptyList()
            } else {
                workEntryDao.getTravelLegsByDate(entry.date)
            }
            WorkEntryDerivedStateNormalizer.normalize(entry, travelLegs)
        }
        workEntryDao.upsertAllAndDeleteTravelLegs(normalizedEntries, travelLegDatesToDelete)
    }

    override suspend fun getTravelLegsByDate(date: LocalDate): List<TravelLeg> =
        workEntryDao.getTravelLegsByDate(date)

    override suspend fun deleteTravelLegsByDate(date: LocalDate) = withDateLock(date) {
        val existingEntry = workEntryDao.getByDate(date)
        workEntryDao.deleteTravelLegsByDate(date)
        if (existingEntry != null) {
            workEntryDao.upsert(
                WorkEntryDerivedStateNormalizer.normalize(
                    entry = existingEntry.copy(updatedAt = System.currentTimeMillis()),
                    travelLegs = emptyList()
                )
            )
        }
    }

    override suspend fun deleteByDate(date: LocalDate) = withDateLock(date) {
        workEntryDao.deleteByDate(date)
    }

    override suspend fun replaceEntryWithTravelLegs(
        entry: WorkEntry,
        legs: List<TravelLeg>,
    ) = withDateLock(entry.date) {
        workEntryDao.replaceEntryWithTravelLegs(
            WorkEntryDerivedStateNormalizer.normalize(entry, legs),
            legs
        )
    }

    override suspend fun readModifyWrite(date: LocalDate, modify: (WorkEntry?) -> WorkEntry) = withDateLock(date) {
        val travelLegs = workEntryDao.getTravelLegsByDate(date)
        workEntryDao.readModifyWrite(date) { existing ->
            WorkEntryDerivedStateNormalizer.normalize(modify(existing), travelLegs)
        }
    }

    private suspend fun normalizeWithStoredTravel(entry: WorkEntry): WorkEntry {
        return WorkEntryDerivedStateNormalizer.normalize(
            entry = entry,
            travelLegs = workEntryDao.getTravelLegsByDate(entry.date)
        )
    }

    private suspend fun <T> withDateLock(date: LocalDate, block: suspend () -> T): T {
        val dateMutex = synchronized(writeMutexesGuard) {
            writeMutexes.getOrPut(date) { DateMutex() }.also { it.references++ }
        }
        try {
            return dateMutex.mutex.withLock { block() }
        } finally {
            synchronized(writeMutexesGuard) {
                dateMutex.references--
                trimUnusedDateMutexes()
            }
        }
    }

    private suspend fun <T> withDateLocks(dates: List<LocalDate>, block: suspend () -> T): T {
        val sortedDates = dates.distinct().sorted()
        return withDateLocks(sortedDates, index = 0, block = block)
    }

    private suspend fun <T> withDateLocks(
        dates: List<LocalDate>,
        index: Int,
        block: suspend () -> T
    ): T {
        if (index >= dates.size) return block()
        return withDateLock(dates[index]) {
            withDateLocks(dates, index + 1, block)
        }
    }

    private fun trimUnusedDateMutexes() {
        if (writeMutexes.size <= MAX_CACHED_DATE_MUTEXES) return

        val iterator = writeMutexes.entries.iterator()
        while (writeMutexes.size > MAX_CACHED_DATE_MUTEXES && iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value.references == 0) {
                iterator.remove()
            }
        }
    }

    private class DateMutex(
        val mutex: Mutex = Mutex(),
        var references: Int = 0
    )

    private companion object {
        private const val MAX_CACHED_DATE_MUTEXES = 128
        private const val LOAD_FACTOR = 0.75f
    }
}
