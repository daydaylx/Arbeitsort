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
            "migration_3_4_test.db",
            "migration_4_5_test.db",
            "migration_5_6_test.db",
            "migration_6_7_test.db",
            "migration_7_8_test.db",
            "migration_8_9_test.db",
            "migration_9_10_test.db",
            "migration_10_11_test.db",
            "migration_11_12_test.db"
        ).forEach { name ->
            context.deleteDatabase(name)
        }
    }

    @Test
    fun `migration chain from 5 updates day location and removes legacy location columns`() {
        val dbName = "migration_5_6_test.db"
        createVersion5Database(dbName)

        val roomDb = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(
                AppDatabase.MIGRATION_5_6,
                AppDatabase.MIGRATION_6_7,
                AppDatabase.MIGRATION_7_8,
                AppDatabase.MIGRATION_8_9,
                AppDatabase.MIGRATION_9_10,
                AppDatabase.MIGRATION_10_11,
                AppDatabase.MIGRATION_11_12
            )
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
            assertFalse(hasColumn(db, "work_entries", "dayLocationSource"))
            assertFalse(hasColumn(db, "work_entries", "dayLocationLat"))
            assertFalse(hasColumn(db, "work_entries", "dayLocationLon"))
            assertFalse(hasColumn(db, "work_entries", "dayLocationAccuracyMeters"))
            assertFalse(hasColumn(db, "work_entries", "morningLocationLabel"))
            assertFalse(hasColumn(db, "work_entries", "morningLat"))
            assertFalse(hasColumn(db, "work_entries", "morningLon"))
            assertFalse(hasColumn(db, "work_entries", "morningAccuracyMeters"))
            assertFalse(hasColumn(db, "work_entries", "morningLocationStatus"))
            assertFalse(hasColumn(db, "work_entries", "eveningLocationLabel"))
            assertFalse(hasColumn(db, "work_entries", "eveningLat"))
            assertFalse(hasColumn(db, "work_entries", "eveningLon"))
            assertFalse(hasColumn(db, "work_entries", "eveningAccuracyMeters"))
            assertFalse(hasColumn(db, "work_entries", "eveningLocationStatus"))
            assertFalse(hasColumn(db, "work_entries", "needsReview"))
            assertFalse(hasColumn(db, "work_entries", "outsideLeipzigMorning"))
            assertFalse(hasColumn(db, "work_entries", "outsideLeipzigEvening"))

            db.rawQuery(
                "SELECT dayLocationLabel, morningCapturedAt, eveningCapturedAt FROM work_entries WHERE date = ?",
                arrayOf("2026-01-05")
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("", cursor.getString(0))
                assertTrue(cursor.isNull(1))
                assertTrue(cursor.isNull(2))
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
    fun `migration 3 to 4 adds confirmation columns`() {
        val dbName = "migration_3_4_test.db"
        val (helper, db) = createSupportDatabase(dbName, version = 3)

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

            AppDatabase.MIGRATION_3_4.migrate(db)

            val sqlite = SQLiteDatabase.openDatabase(
                context.getDatabasePath(dbName).path,
                null,
                SQLiteDatabase.OPEN_READONLY
            )
            sqlite.use { rawDb ->
                assertTrue(hasColumn(rawDb, "work_entries", "confirmedWorkDay"))
                assertTrue(hasColumn(rawDb, "work_entries", "confirmationAt"))
                assertTrue(hasColumn(rawDb, "work_entries", "confirmationSource"))
            }
        } finally {
            helper.close()
        }
    }

    @Test
    fun `migration 4 to 5 creates dayType date index`() {
        val dbName = "migration_4_5_test.db"
        val (helper, db) = createSupportDatabase(dbName, version = 4)

        try {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `work_entries` (
                    `date` TEXT NOT NULL,
                    `dayType` TEXT NOT NULL,
                    `confirmedWorkDay` INTEGER NOT NULL DEFAULT 0,
                    `confirmationAt` INTEGER,
                    `confirmationSource` TEXT,
                    `needsReview` INTEGER NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    PRIMARY KEY(`date`)
                )
                """.trimIndent()
            )

            AppDatabase.MIGRATION_4_5.migrate(db)

            assertTrue(indexExists(db, "index_work_entries_dayType_date"))
        } finally {
            helper.close()
        }
    }

    @Test
    fun `migration 6 to 7 drops route_cache table`() {
        val dbName = "migration_6_7_test.db"
        val (helper, db) = createSupportDatabase(dbName, version = 6)

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
            db.execSQL(
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

            AppDatabase.MIGRATION_6_7.migrate(db)

            assertFalse(tableExists(db, "route_cache"))
            assertTrue(tableExists(db, "work_entries"))
        } finally {
            helper.close()
        }
    }

    @Test
    fun `migration 7 to 8 clears dayLocationLabel for FALLBACK entries`() {
        val dbName = "migration_7_8_test.db"
        val (helper, db) = createSupportDatabase(dbName, version = 7)

        try {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `work_entries` (
                    `date` TEXT NOT NULL,
                    `dayType` TEXT NOT NULL,
                    `dayLocationLabel` TEXT NOT NULL DEFAULT '',
                    `dayLocationSource` TEXT NOT NULL DEFAULT 'FALLBACK',
                    `needsReview` INTEGER NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    PRIMARY KEY(`date`)
                )
                """.trimIndent()
            )
            db.execSQL("INSERT INTO work_entries (date, dayType, dayLocationLabel, dayLocationSource, needsReview, createdAt) VALUES ('2026-01-01', 'WORK', 'Leipzig', 'FALLBACK', 0, 1000)")
            db.execSQL("INSERT INTO work_entries (date, dayType, dayLocationLabel, dayLocationSource, needsReview, createdAt) VALUES ('2026-01-02', 'WORK', 'München', 'MANUAL', 0, 1001)")

            AppDatabase.MIGRATION_7_8.migrate(db)

            val sqlite = SQLiteDatabase.openDatabase(
                context.getDatabasePath(dbName).path,
                null,
                SQLiteDatabase.OPEN_READONLY
            )
            sqlite.use { rawDb ->
                rawDb.rawQuery("SELECT dayLocationLabel FROM work_entries WHERE date = '2026-01-01'", null).use { c ->
                    assertTrue(c.moveToFirst())
                    assertEquals("", c.getString(0))
                }
                rawDb.rawQuery("SELECT dayLocationLabel FROM work_entries WHERE date = '2026-01-02'", null).use { c ->
                    assertTrue(c.moveToFirst())
                    assertEquals("München", c.getString(0))
                }
            }
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

    @Test
    fun `migration 10 to 11 adds meal allowance columns with default zero`() {
        val dbName = "migration_10_11_test.db"
        createVersion10Database(dbName)

        val (helper, db) = openSupportDatabase(dbName, version = 10)
        try {
            AppDatabase.MIGRATION_10_11.migrate(db)

            assertTrue(hasColumnSupport(db, "work_entries", "mealIsArrivalDeparture"))
            assertTrue(hasColumnSupport(db, "work_entries", "mealBreakfastIncluded"))
            assertTrue(hasColumnSupport(db, "work_entries", "mealAllowanceBaseCents"))
            assertTrue(hasColumnSupport(db, "work_entries", "mealAllowanceAmountCents"))

            db.query(
                "SELECT mealIsArrivalDeparture, mealBreakfastIncluded, mealAllowanceBaseCents, mealAllowanceAmountCents FROM work_entries WHERE date = ?",
                arrayOf("2026-01-10")
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
                assertEquals(0, cursor.getInt(1))
                assertEquals(0, cursor.getInt(2))
                assertEquals(0, cursor.getInt(3))
            }
        } finally {
            helper.close()
        }
    }

    @Test
    fun `migration 11 to 12 removes GPS review and snapshot label columns while preserving business data`() {
        val dbName = "migration_11_12_test.db"
        createVersion11Database(dbName)

        val (helper, db) = openSupportDatabase(dbName, version = 11)
        try {
            AppDatabase.MIGRATION_11_12.migrate(db)

            assertFalse(hasColumnSupport(db, "work_entries", "dayLocationSource"))
            assertFalse(hasColumnSupport(db, "work_entries", "dayLocationLat"))
            assertFalse(hasColumnSupport(db, "work_entries", "dayLocationLon"))
            assertFalse(hasColumnSupport(db, "work_entries", "dayLocationAccuracyMeters"))
            assertFalse(hasColumnSupport(db, "work_entries", "morningLocationLabel"))
            assertFalse(hasColumnSupport(db, "work_entries", "morningLat"))
            assertFalse(hasColumnSupport(db, "work_entries", "morningLon"))
            assertFalse(hasColumnSupport(db, "work_entries", "morningAccuracyMeters"))
            assertFalse(hasColumnSupport(db, "work_entries", "morningLocationStatus"))
            assertFalse(hasColumnSupport(db, "work_entries", "eveningLocationLabel"))
            assertFalse(hasColumnSupport(db, "work_entries", "eveningLat"))
            assertFalse(hasColumnSupport(db, "work_entries", "eveningLon"))
            assertFalse(hasColumnSupport(db, "work_entries", "eveningAccuracyMeters"))
            assertFalse(hasColumnSupport(db, "work_entries", "eveningLocationStatus"))
            assertFalse(hasColumnSupport(db, "work_entries", "needsReview"))
            assertFalse(indexExists(db, "index_work_entries_needsReview"))

            db.query(
                """
                SELECT
                    dayLocationLabel,
                    morningCapturedAt,
                    eveningCapturedAt,
                    travelPaidMinutes,
                    mealAllowanceAmountCents,
                    confirmationSource,
                    note
                FROM work_entries
                WHERE date = ?
                """.trimIndent(),
                arrayOf("2026-01-11")
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Baustelle Nord", cursor.getString(0))
                assertEquals(1_111L, cursor.getLong(1))
                assertEquals(2_222L, cursor.getLong(2))
                assertEquals(35, cursor.getInt(3))
                assertEquals(820, cursor.getInt(4))
                assertEquals("UI", cursor.getString(5))
                assertEquals("Altbestand", cursor.getString(6))
            }
        } finally {
            helper.close()
        }
    }

    private fun createVersion10Database(dbName: String) {
        val dbFile = context.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()

        val db = android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        db.use {
            it.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `work_entries` (
                    `date` TEXT NOT NULL,
                    `workStart` TEXT NOT NULL,
                    `workEnd` TEXT NOT NULL,
                    `breakMinutes` INTEGER NOT NULL,
                    `dayType` TEXT NOT NULL,
                    `dayLocationLabel` TEXT NOT NULL,
                    `dayLocationSource` TEXT NOT NULL,
                    `dayLocationLat` REAL,
                    `dayLocationLon` REAL,
                    `dayLocationAccuracyMeters` REAL,
                    `morningCapturedAt` INTEGER,
                    `morningLocationLabel` TEXT,
                    `morningLat` REAL,
                    `morningLon` REAL,
                    `morningAccuracyMeters` REAL,
                    `morningLocationStatus` TEXT NOT NULL,
                    `eveningCapturedAt` INTEGER,
                    `eveningLocationLabel` TEXT,
                    `eveningLat` REAL,
                    `eveningLon` REAL,
                    `eveningAccuracyMeters` REAL,
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
                    `confirmedWorkDay` INTEGER NOT NULL,
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
            it.execSQL("CREATE INDEX IF NOT EXISTS index_work_entries_needsReview ON work_entries(needsReview)")
            it.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_work_entries_date ON work_entries(date)")
            it.execSQL("CREATE INDEX IF NOT EXISTS index_work_entries_createdAt ON work_entries(createdAt)")
            it.execSQL("CREATE INDEX IF NOT EXISTS index_work_entries_dayType_date ON work_entries(dayType, date)")
            it.execSQL(
                """
                INSERT INTO work_entries (
                    date, workStart, workEnd, breakMinutes, dayType,
                    dayLocationLabel, dayLocationSource,
                    morningLocationStatus, eveningLocationStatus,
                    confirmedWorkDay, needsReview, createdAt, updatedAt
                ) VALUES (
                    '2026-01-10', '08:00', '19:00', 60, 'WORK',
                    'Baustelle', 'MANUAL',
                    'UNAVAILABLE', 'UNAVAILABLE',
                    1, 0, 1000, 1000
                )
                """.trimIndent()
            )
            it.version = 10
        }
    }

    private fun createVersion11Database(dbName: String) {
        val dbFile = context.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()

        val db = android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        db.use {
            it.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `work_entries` (
                    `date` TEXT NOT NULL,
                    `workStart` TEXT NOT NULL,
                    `workEnd` TEXT NOT NULL,
                    `breakMinutes` INTEGER NOT NULL,
                    `dayType` TEXT NOT NULL,
                    `dayLocationLabel` TEXT NOT NULL,
                    `dayLocationSource` TEXT NOT NULL,
                    `dayLocationLat` REAL,
                    `dayLocationLon` REAL,
                    `dayLocationAccuracyMeters` REAL,
                    `morningCapturedAt` INTEGER,
                    `morningLocationLabel` TEXT,
                    `morningLat` REAL,
                    `morningLon` REAL,
                    `morningAccuracyMeters` REAL,
                    `morningLocationStatus` TEXT NOT NULL,
                    `eveningCapturedAt` INTEGER,
                    `eveningLocationLabel` TEXT,
                    `eveningLat` REAL,
                    `eveningLon` REAL,
                    `eveningAccuracyMeters` REAL,
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
                    `confirmedWorkDay` INTEGER NOT NULL,
                    `confirmationAt` INTEGER,
                    `confirmationSource` TEXT,
                    `mealIsArrivalDeparture` INTEGER NOT NULL,
                    `mealBreakfastIncluded` INTEGER NOT NULL,
                    `mealAllowanceBaseCents` INTEGER NOT NULL,
                    `mealAllowanceAmountCents` INTEGER NOT NULL,
                    `needsReview` INTEGER NOT NULL,
                    `note` TEXT,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`date`)
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
                    dayLocationLabel, dayLocationSource,
                    morningCapturedAt, morningLocationLabel, morningLocationStatus,
                    eveningCapturedAt, eveningLocationLabel, eveningLocationStatus,
                    travelPaidMinutes, travelSource, confirmedWorkDay,
                    confirmationAt, confirmationSource,
                    mealIsArrivalDeparture, mealBreakfastIncluded,
                    mealAllowanceBaseCents, mealAllowanceAmountCents,
                    needsReview, note, createdAt, updatedAt
                ) VALUES (
                    '2026-01-11', '07:00', '16:00', 45, 'WORK',
                    'Baustelle Nord', 'MANUAL',
                    1111, 'Morgenort', 'OK',
                    2222, 'Abendort', 'UNAVAILABLE',
                    35, 'MANUAL', 1,
                    3333, 'UI',
                    1, 0,
                    1400, 820,
                    1, 'Altbestand', 1000, 2000
                )
                """.trimIndent()
            )
            it.version = 11
        }
    }

    private fun openSupportDatabase(
        dbName: String,
        version: Int
    ): Pair<SupportSQLiteOpenHelper, SupportSQLiteDatabase> {
        return createSupportDatabase(dbName, version)
    }

    private fun hasColumnSupport(db: SupportSQLiteDatabase, table: String, column: String): Boolean {
        db.query("PRAGMA table_info($table)").use { cursor ->
            while (cursor.moveToNext()) {
                val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                if (name == column) return true
            }
        }
        return false
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
