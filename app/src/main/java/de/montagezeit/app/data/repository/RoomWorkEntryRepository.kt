package de.montagezeit.app.data.repository

import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.TravelLeg
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.local.entity.WorkEntryWithTravelLegs
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class RoomWorkEntryRepository @Inject constructor(
    private val workEntryDao: WorkEntryDao
) : WorkEntryRepository {
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

    override suspend fun upsert(entry: WorkEntry) = workEntryDao.upsert(entry)

    override suspend fun upsertAll(entries: List<WorkEntry>) = workEntryDao.upsertAll(entries)

    override suspend fun upsertAllAndDeleteTravelLegs(
        entries: List<WorkEntry>,
        travelLegDatesToDelete: List<LocalDate>
    ) = workEntryDao.upsertAllAndDeleteTravelLegs(entries, travelLegDatesToDelete)

    override suspend fun getTravelLegsByDate(date: LocalDate): List<TravelLeg> =
        workEntryDao.getTravelLegsByDate(date)

    override suspend fun deleteTravelLegsByDate(date: LocalDate) = workEntryDao.deleteTravelLegsByDate(date)

    override suspend fun deleteByDate(date: LocalDate) = workEntryDao.deleteByDate(date)

    override suspend fun replaceEntryWithTravelLegs(entry: WorkEntry, legs: List<TravelLeg>) =
        workEntryDao.replaceEntryWithTravelLegs(entry, legs)

    override suspend fun readModifyWrite(date: LocalDate, modify: (WorkEntry?) -> WorkEntry) =
        workEntryDao.readModifyWrite(date, modify)
}
