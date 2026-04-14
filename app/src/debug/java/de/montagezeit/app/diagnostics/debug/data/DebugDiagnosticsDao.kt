package de.montagezeit.app.diagnostics.debug.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface DebugDiagnosticsDao {
    @Upsert
    suspend fun upsertSession(session: DiagnosticSessionEntity)

    @Upsert
    suspend fun upsertTrace(trace: DiagnosticTraceEntity)

    @Insert
    suspend fun insertEvent(event: DiagnosticEventEntity)

    @Query("SELECT * FROM diagnostic_traces ORDER BY startedAtEpochMs DESC")
    fun observeTraces(): Flow<List<DiagnosticTraceEntity>>

    @Query("SELECT * FROM diagnostic_traces WHERE traceId = :traceId LIMIT 1")
    fun observeTrace(traceId: String): Flow<DiagnosticTraceEntity?>

    @Query("SELECT * FROM diagnostic_events WHERE traceId = :traceId ORDER BY seq ASC")
    fun observeEvents(traceId: String): Flow<List<DiagnosticEventEntity>>

    @Query("SELECT COUNT(*) FROM diagnostic_events")
    fun observeEventCount(): Flow<Int>

    @Query("SELECT * FROM diagnostic_traces ORDER BY startedAtEpochMs ASC")
    suspend fun getAllTracesOldestFirst(): List<DiagnosticTraceEntity>

    @Query("SELECT * FROM diagnostic_traces ORDER BY startedAtEpochMs DESC")
    suspend fun getAllTracesNewestFirst(): List<DiagnosticTraceEntity>

    @Query("SELECT * FROM diagnostic_events WHERE traceId IN (:traceIds) ORDER BY traceId ASC, seq ASC")
    suspend fun getEventsForTraces(traceIds: List<String>): List<DiagnosticEventEntity>

    @Query("DELETE FROM diagnostic_traces WHERE traceId IN (:traceIds)")
    suspend fun deleteTraces(traceIds: List<String>)

    @Query("DELETE FROM diagnostic_events")
    suspend fun clearEvents()

    @Query("DELETE FROM diagnostic_traces")
    suspend fun clearTraces()

    @Query("DELETE FROM diagnostic_sessions")
    suspend fun clearSessions()
}
