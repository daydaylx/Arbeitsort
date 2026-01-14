package de.montagezeit.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import de.montagezeit.app.data.local.entity.WorkEntry
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkEntryDao {
    
    @Query("SELECT * FROM work_entries WHERE date = :date")
    suspend fun getByDate(date: LocalDate): WorkEntry?
    
    @Query("SELECT * FROM work_entries WHERE date = :date")
    fun getByDateFlow(date: LocalDate): Flow<WorkEntry?>
    
    @Query("SELECT * FROM work_entries WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    suspend fun getByDateRange(startDate: LocalDate, endDate: LocalDate): List<WorkEntry>

    @Query("SELECT * FROM work_entries ORDER BY date DESC")
    suspend fun getAll(): List<WorkEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: WorkEntry): Long
    
    @Update
    suspend fun update(entry: WorkEntry)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: WorkEntry)
    
    @Query("DELETE FROM work_entries WHERE date = :date")
    suspend fun deleteByDate(date: LocalDate)
}
