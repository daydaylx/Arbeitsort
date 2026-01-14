package de.montagezeit.app.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import de.montagezeit.app.data.local.converters.LocalDateConverter
import de.montagezeit.app.data.local.converters.LocalTimeConverter
import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.dao.RouteCacheDao
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.local.entity.RouteCacheEntry

@Database(
    entities = [WorkEntry::class, RouteCacheEntry::class],
    version = 4,
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
        const val DATABASE_NAME = "montagezeit_database"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // No schema changes between v1 and v2.
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add indices for performance
                db.execSQL("CREATE INDEX IF NOT EXISTS index_work_entries_needsReview ON work_entries(needsReview)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_work_entries_createdAt ON work_entries(createdAt)")
                // Note: date already has unique index as PRIMARY KEY
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add Daily Confirmation fields
                db.execSQL("ALTER TABLE work_entries ADD COLUMN confirmedWorkDay INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE work_entries ADD COLUMN confirmationAt INTEGER")
                db.execSQL("ALTER TABLE work_entries ADD COLUMN confirmationSource TEXT")
            }
        }

        val MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
    }
}
