package de.montagezeit.app.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import de.montagezeit.app.data.local.converters.LocalDateConverter
import de.montagezeit.app.data.local.converters.LocalTimeConverter
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import de.montagezeit.app.data.local.dao.RouteCacheDao
import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.RouteCacheEntry
import de.montagezeit.app.data.local.entity.WorkEntry

@Database(
    entities = [WorkEntry::class, RouteCacheEntry::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(
    LocalDateConverter::class,
    LocalTimeConverter::class
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun workEntryDao(): WorkEntryDao
    abstract fun routeCacheDao(): RouteCacheDao
    
    companion object {
        private const val DATABASE_NAME = "montagezeit.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(\"ALTER TABLE work_entries ADD COLUMN travelFromLabel TEXT\")
                database.execSQL(\"ALTER TABLE work_entries ADD COLUMN travelToLabel TEXT\")
                database.execSQL(\"ALTER TABLE work_entries ADD COLUMN travelDistanceKm REAL\")
                database.execSQL(\"ALTER TABLE work_entries ADD COLUMN travelPaidMinutes INTEGER\")
                database.execSQL(\"ALTER TABLE work_entries ADD COLUMN travelSource TEXT\")
                database.execSQL(\"ALTER TABLE work_entries ADD COLUMN travelUpdatedAt INTEGER\")
                database.execSQL(\n                    \"\"\"\n                    CREATE TABLE IF NOT EXISTS route_cache (\n                        fromLabel TEXT NOT NULL,\n                        toLabel TEXT NOT NULL,\n                        distanceKm REAL NOT NULL,\n                        updatedAt INTEGER NOT NULL,\n                        PRIMARY KEY(fromLabel, toLabel)\n                    )\n                    \"\"\".trimIndent()\n                )\n            }\n        }
        
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                // ⚠️ CRITICAL: fallbackToDestructiveMigration() muss VOR Release ersetzt werden!
                // 
                // PROBLEM: Diese Konfiguration löscht ALLE Nutzerdaten bei Datenbank-Updates.
                // Das ist für MVP-Entwicklung akzeptabel, aber KATASTROPHAL für Produktivnutzer.
                //
                // LÖSUNG VOR RELEASE:
                // 1. Entferne .fallbackToDestructiveMigration()
                // 2. Implementiere echte Migrationen mit .addMigrations():
                //    - Migration von Version 1 auf 2 (wenn Schema geändert wird)
                //    - Jede Migration benötigt Migration(startVersion, endVersion) {
                //          // Migrate hier mit ALTER TABLE, CREATE INDEX, etc.
                //      }
                // 3. Teste Migrationen gründlich mit:
                //    - DatabaseInspector in Android Studio
                //    - Instrumented Tests
                //    - Test-APK mit echten Daten
                // 4. Exportiere das Schema: .fallbackToDestructiveMigrationOnDowngrade() nur als letzte Option
                //
                // BEISPIEL für erste Migration:
                // .addMigrations(MIGRATION_1_2)
                //
                // Migration 1 -> 2 (Beispiel):
                // val MIGRATION_1_2 = object : Migration(1, 2) {
                //     override fun migrate(database: SupportSQLiteDatabase) {
                //         // Beispiel: Neue Spalte hinzufügen
                //         database.execSQL("ALTER TABLE work_entries ADD COLUMN newColumn TEXT")
                //     }
                // }
                //
                // REF: https://developer.android.com/training/data-storage/room/migrating-db-versions
                // STATUS: TODO - Implement before first release with real user data
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
