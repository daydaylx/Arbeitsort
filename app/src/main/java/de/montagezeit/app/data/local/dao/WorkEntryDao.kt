package de.montagezeit.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.TravelLeg
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.local.entity.WorkEntryWithTravelLegs
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow

@Dao
abstract class WorkEntryDao {
    
    @Query("SELECT * FROM work_entries WHERE date = :date")
    abstract suspend fun getByDate(date: LocalDate): WorkEntry?

    @Query("SELECT * FROM work_entries WHERE date = :date")
    abstract fun getByDateFlow(date: LocalDate): Flow<WorkEntry?>

    @Transaction
    @Query("SELECT * FROM work_entries WHERE date = :date")
    abstract suspend fun getByDateWithTravel(date: LocalDate): WorkEntryWithTravelLegs?

    @Transaction
    @Query("SELECT * FROM work_entries WHERE date = :date")
    abstract fun getByDateWithTravelFlow(date: LocalDate): Flow<WorkEntryWithTravelLegs?>

    @Query("SELECT * FROM work_entries WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    abstract suspend fun getByDateRange(startDate: LocalDate, endDate: LocalDate): List<WorkEntry>

    @Transaction
    @Query("SELECT * FROM work_entries WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    abstract suspend fun getByDateRangeWithTravel(
        startDate: LocalDate,
        endDate: LocalDate
    ): List<WorkEntryWithTravelLegs>

    @Transaction
    @Query("SELECT * FROM work_entries WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    abstract fun getByDateRangeWithTravelFlow(
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<WorkEntryWithTravelLegs>>

    @Query("SELECT * FROM work_entries ORDER BY date DESC")
    abstract suspend fun getAll(): List<WorkEntry>

    @Query(
        "SELECT dayLocationLabel FROM work_entries " +
            "WHERE dayType = :dayType AND LENGTH(TRIM(dayLocationLabel)) > 0 " +
            "ORDER BY date DESC LIMIT 1"
    )
    abstract suspend fun getLatestDayLocationLabelByDayType(dayType: DayType): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(entry: WorkEntry): Long

    @Update
    abstract suspend fun update(entry: WorkEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsert(entry: WorkEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertAll(entries: List<WorkEntry>)

    @Query("SELECT * FROM travel_legs WHERE workEntryDate = :date ORDER BY sortOrder ASC")
    abstract suspend fun getTravelLegsByDate(date: LocalDate): List<TravelLeg>

    @Query("SELECT * FROM travel_legs WHERE workEntryDate = :date ORDER BY sortOrder ASC")
    abstract fun getTravelLegsByDateFlow(date: LocalDate): Flow<List<TravelLeg>>

    @Query(
        """
        SELECT * FROM travel_legs
        WHERE workEntryDate >= :startDate AND workEntryDate <= :endDate
        ORDER BY workEntryDate ASC, sortOrder ASC
        """
    )
    abstract suspend fun getTravelLegsByDateRange(startDate: LocalDate, endDate: LocalDate): List<TravelLeg>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertTravelLegs(legs: List<TravelLeg>)

    @Query("DELETE FROM travel_legs WHERE workEntryDate = :date")
    abstract suspend fun deleteTravelLegsByDate(date: LocalDate)

    @Query("DELETE FROM work_entries WHERE date = :date")
    abstract suspend fun deleteByDate(date: LocalDate)

    @Transaction
    open suspend fun readModifyWrite(date: LocalDate, modify: (WorkEntry?) -> WorkEntry) {
        val existing = getByDate(date)
        upsert(modify(existing))
    }

    @Transaction
    open suspend fun upsertAllAndDeleteTravelLegs(
        entries: List<WorkEntry>,
        travelLegDatesToDelete: List<LocalDate>
    ) {
        upsertAll(entries)
        for (date in travelLegDatesToDelete) {
            deleteTravelLegsByDate(date)
        }
    }

    @Transaction
    open suspend fun replaceEntryWithTravelLegs(entry: WorkEntry, legs: List<TravelLeg>) {
        upsert(entry)
        deleteTravelLegsByDate(entry.date)
        if (legs.isNotEmpty()) {
            upsertTravelLegs(
                legs.mapIndexed { index, leg ->
                    leg.copy(
                        workEntryDate = entry.date,
                        sortOrder = index
                    )
                }
            )
        }
    }
}

