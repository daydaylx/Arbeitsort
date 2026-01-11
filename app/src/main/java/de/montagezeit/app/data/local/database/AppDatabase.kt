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
                // No schema changes between v1 and v2.
            }
        }

        val MIGRATIONS = arrayOf(MIGRATION_1_2)
        
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(*MIGRATIONS)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
