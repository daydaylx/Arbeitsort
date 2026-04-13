package de.montagezeit.app.data.repository

import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.TravelLeg
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.local.entity.WorkEntryWithTravelLegs
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface WorkEntryRepository {
    suspend fun getByDate(date: LocalDate): WorkEntry?
    fun getByDateFlow(date: LocalDate): Flow<WorkEntry?>

    suspend fun getByDateWithTravel(date: LocalDate): WorkEntryWithTravelLegs?
    fun getByDateWithTravelFlow(date: LocalDate): Flow<WorkEntryWithTravelLegs?>

    suspend fun getByDateRange(startDate: LocalDate, endDate: LocalDate): List<WorkEntry>
    suspend fun getByDateRangeWithTravel(startDate: LocalDate, endDate: LocalDate): List<WorkEntryWithTravelLegs>
    fun getAllWithTravelFlow(): Flow<List<WorkEntryWithTravelLegs>>
    fun getByDateRangeWithTravelFlow(
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<WorkEntryWithTravelLegs>>

    suspend fun getLatestDayLocationLabelByDayType(dayType: DayType): String?

    suspend fun upsert(entry: WorkEntry)
    suspend fun upsertAll(entries: List<WorkEntry>)
    suspend fun upsertAllAndDeleteTravelLegs(entries: List<WorkEntry>, travelLegDatesToDelete: List<LocalDate>)

    suspend fun getTravelLegsByDate(date: LocalDate): List<TravelLeg>
    suspend fun deleteTravelLegsByDate(date: LocalDate)
    suspend fun deleteByDate(date: LocalDate)

    suspend fun replaceEntryWithTravelLegs(entry: WorkEntry, legs: List<TravelLeg>)

    suspend fun readModifyWrite(date: LocalDate, modify: (WorkEntry?) -> WorkEntry)
}
