package de.montagezeit.app.diagnostics.debug.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        DiagnosticSessionEntity::class,
        DiagnosticTraceEntity::class,
        DiagnosticEventEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class DebugDiagnosticsDatabase : RoomDatabase() {
    abstract fun diagnosticsDao(): DebugDiagnosticsDao

    companion object {
        const val DATABASE_NAME = "debug_diagnostics.db"
    }
}
