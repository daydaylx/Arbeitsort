package de.montagezeit.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import de.montagezeit.app.data.local.entity.RouteCacheEntry

@Dao
interface RouteCacheDao {
    @Query("SELECT * FROM route_cache WHERE fromLabel = :fromLabel AND toLabel = :toLabel LIMIT 1")
    suspend fun getEntry(fromLabel: String, toLabel: String): RouteCacheEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: RouteCacheEntry)
}
