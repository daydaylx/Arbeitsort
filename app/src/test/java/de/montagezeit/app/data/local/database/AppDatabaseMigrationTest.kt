package de.montagezeit.app.data.local.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppDatabaseMigrationTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        listOf(
            "migration_1_2_test.db",
            "migration_2_3_test.db",
            "migration_5_6_test.db"
        ).forEach { name ->
            context.deleteDatabase(name)
        }
    }

    @Test
    fun `migration 5 to 6 adds day location columns with defaults`() {
        val dbName = "migration_5_6_test.db"
        createVersion5Database(dbName)

        val roomDb = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(AppDatabase.MIGRATION_5_6)
            .build()

        roomDb.openHelper.writableDatabase
        roomDb.close()

        val sqlite = SQLiteDatabase.openDatabase(
            context.getDatabasePath(dbName).path,
            null,
            SQLiteDatabase.OPEN_READONLY
        )

        sqlite.use { db ->
            assertTrue(hasColumn(db, "work_entries", "dayLocationLabel"))
            assertTrue(hasColumn(db, "work_entries", "dayLocationSource"))
            assertTrue(hasColumn(db, "work_entries", "dayLocationLat"))
            assertTrue(hasColumn(db, "work_entries", "dayLocationLon"))
            assertTrue(hasColumn(db, "work_entries", "dayLocationAccuracyMeters"))

            db.rawQuery(
                "SELECT dayLocationLabel, dayLocationSource, dayLocationLat, dayLocationLon, dayLocationAccuracyMeters FROM work_entries WHERE date = ?",
                arrayOf("2026-01-05")
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Leipzig", cursor.getString(0))
                assertEquals("FALLBACK", cursor.getString(1))
                assertTrue(cursor.isNull(2))
                assertTrue(cursor.isNull(3))
                assertTrue(cursor.isNull(4))
            }
        }
    }

    @Test
    fun `migration 1 to 2 creates route_cache table when missing`() {
        val dbName = "migration_1_2_test.db"
        val (helper, db) = createSupportDatabase(dbName, version = 1)

        try {
            AppDatabase.MIGRATION_1_2.migrate(db)
            assertTrue(tableExists(db, "route_cache"))
        } finally {
            helper.close()
        }
    }

    @Test
    fun `migration 2 to 3 creates route_cache and performance indices`() {
        val dbName = "migration_2_3_test.db"
        val (helper, db) = createSupportDatabase(dbName, version = 2)

        try {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `work_entries` (
                    `date` TEXT NOT NULL,
                    `dayType` TEXT NOT NULL,
                    `needsReview` INTEGER NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    PRIMARY KEY(`date`)
                )
                """.trimIndent()
            )

            AppDatabase.MIGRATION_2_3.migrate(db)

            assertTrue(tableExists(db, "route_cache"))
            assertTrue(indexExists(db, "index_work_entries_needsReview"))
            assertTrue(indexExists(db, "index_work_entries_createdAt"))
            assertTrue(indexExists(db, "index_work_entries_date"))
            assertTrue(indexExists(db, "index_work_entries_dayType_date"))
        } finally {
            helper.close()
        }
    }

    private fun createVersion5Database(dbName: String) {
        val dbFile = context.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()

        val db = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        db.use {
            it.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `work_entries` (
                    `date` TEXT NOT NULL,
                    `workStart` TEXT NOT NULL,
                    `workEnd` TEXT NOT NULL,
                    `breakMinutes` INTEGER NOT NULL,
                    `dayType` TEXT NOT NULL,
                    `morningCapturedAt` INTEGER,
                    `morningLocationLabel` TEXT,
                    `morningLat` REAL,
                    `morningLon` REAL,
                    `morningAccuracyMeters` REAL,
                    `outsideLeipzigMorning` INTEGER,
                    `morningLocationStatus` TEXT NOT NULL,
                    `eveningCapturedAt` INTEGER,
                    `eveningLocationLabel` TEXT,
                    `eveningLat` REAL,
                    `eveningLon` REAL,
                    `eveningAccuracyMeters` REAL,
                    `outsideLeipzigEvening` INTEGER,
                    `eveningLocationStatus` TEXT NOT NULL,
                    `travelStartAt` INTEGER,
                    `travelArriveAt` INTEGER,
                    `travelLabelStart` TEXT,
                    `travelLabelEnd` TEXT,
                    `travelFromLabel` TEXT,
                    `travelToLabel` TEXT,
                    `travelDistanceKm` REAL,
                    `travelPaidMinutes` INTEGER,
                    `travelSource` TEXT,
                    `travelUpdatedAt` INTEGER,
                    `confirmedWorkDay` INTEGER NOT NULL DEFAULT 0,
                    `confirmationAt` INTEGER,
                    `confirmationSource` TEXT,
                    `needsReview` INTEGER NOT NULL,
                    `note` TEXT,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`date`)
                )
                """.trimIndent()
            )
            it.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `route_cache` (
                    `fromLabel` TEXT NOT NULL,
                    `toLabel` TEXT NOT NULL,
                    `distanceKm` REAL NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`fromLabel`, `toLabel`)
                )
                """.trimIndent()
            )
            it.execSQL("CREATE INDEX IF NOT EXISTS index_work_entries_needsReview ON work_entries(needsReview)")
            it.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_work_entries_date ON work_entries(date)")
            it.execSQL("CREATE INDEX IF NOT EXISTS index_work_entries_createdAt ON work_entries(createdAt)")
            it.execSQL("CREATE INDEX IF NOT EXISTS index_work_entries_dayType_date ON work_entries(dayType, date)")

            it.execSQL(
                """
                INSERT INTO work_entries (
                    date, workStart, workEnd, breakMinutes, dayType,
                    morningLocationStatus, eveningLocationStatus,
                    needsReview, createdAt, updatedAt
                ) VALUES (
                    '2026-01-05', '08:00', '19:00', 60, 'WORK',
                    'UNAVAILABLE', 'UNAVAILABLE',
                    0, 1000, 1000
                )
                """.trimIndent()
            )

            it.version = 5
        }
    }

    private fun createSupportDatabase(
        dbName: String,
        version: Int
    ): Pair<SupportSQLiteOpenHelper, SupportSQLiteDatabase> {
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(version) {
                override fun onCreate(db: SupportSQLiteDatabase) = Unit
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(configuration)
        val db = helper.writableDatabase
        return helper to db
    }

    private fun hasColumn(db: SQLiteDatabase, table: String, column: String): Boolean {
        db.rawQuery("PRAGMA table_info($table)", null).use { cursor ->
            while (cursor.moveToNext()) {
                val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                if (name == column) return true
            }
        }
        return false
    }

    private fun tableExists(db: SupportSQLiteDatabase, tableName: String): Boolean {
        return db.query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
            arrayOf(tableName)
        ).use { cursor ->
            cursor.moveToFirst()
        }
    }

    private fun indexExists(db: SupportSQLiteDatabase, indexName: String): Boolean {
        return db.query(
            "SELECT name FROM sqlite_master WHERE type='index' AND name=?",
            arrayOf(indexName)
        ).use { cursor ->
            cursor.moveToFirst()
        }
    }
}
