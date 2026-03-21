package de.montagezeit.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntry
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow

@Dao
abstract class WorkEntryDao {
    
    @Query("SELECT * FROM work_entries WHERE date = :date")
    abstract suspend fun getByDate(date: LocalDate): WorkEntry?

    @Query("SELECT * FROM work_entries WHERE date = :date")
    abstract fun getByDateFlow(date: LocalDate): Flow<WorkEntry?>

    @Query("SELECT * FROM work_entries WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    abstract suspend fun getByDateRange(startDate: LocalDate, endDate: LocalDate): List<WorkEntry>

    @Query(
        """
        SELECT
            date,
            workStart,
            workEnd,
            breakMinutes,
            dayType,
            confirmedWorkDay,
            travelStartAt,
            travelArriveAt,
            travelPaidMinutes,
            returnStartAt,
            returnArriveAt
        FROM work_entries
        WHERE date >= :startDate AND date <= :endDate
        ORDER BY date ASC
        """
    )
    abstract suspend fun getEntriesBetween(startDate: LocalDate, endDate: LocalDate): List<OvertimeEntryRow>

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

    @Query("DELETE FROM work_entries WHERE date = :date")
    abstract suspend fun deleteByDate(date: LocalDate)

    @Transaction
    open suspend fun readModifyWrite(date: LocalDate, modify: (WorkEntry?) -> WorkEntry) {
        val existing = getByDate(date)
        upsert(modify(existing))
    }
}

data class OvertimeEntryRow(
    val date: LocalDate,
    val workStart: LocalTime,
    val workEnd: LocalTime,
    val breakMinutes: Int,
    val dayType: DayType,
    val confirmedWorkDay: Boolean,
    val travelStartAt: Long?,
    val travelArriveAt: Long?,
    val travelPaidMinutes: Int?,
    val returnStartAt: Long?,
    val returnArriveAt: Long?
)
