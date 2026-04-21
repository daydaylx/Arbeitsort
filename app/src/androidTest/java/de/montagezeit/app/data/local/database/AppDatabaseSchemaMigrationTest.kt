package de.montagezeit.app.data.local.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseSchemaMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    private val context: Context = ApplicationProvider.getApplicationContext()

    @After
    fun tearDown() {
        listOf(
            "schema_migration_13_16.db",
            "schema_migration_15_16.db"
        ).forEach(context::deleteDatabase)
    }

    @Test
    fun `migration helper validates 13 to 16 against exported schema`() {
        val dbName = "schema_migration_13_16.db"
        createVersion13Database(dbName)

        helper.runMigrationsAndValidate(
            dbName,
            16,
            true,
            AppDatabase.MIGRATION_13_14,
            AppDatabase.MIGRATION_14_15,
            AppDatabase.MIGRATION_15_16
        ).close()

        SQLiteDatabase.openDatabase(
            context.getDatabasePath(dbName).path,
            null,
            SQLiteDatabase.OPEN_READONLY
        ).use { db ->
            db.rawQuery("SELECT COUNT(*) FROM travel_legs WHERE workEntryDate = '2026-01-13'", null).use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(2, c.getInt(0))
            }
            db.rawQuery("SELECT confirmedWorkDay FROM work_entries WHERE date = '2026-01-13'", null).use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(1, c.getInt(0))
            }
        }
    }

    @Test
    fun `migration helper validates 15 to 16 against exported schema`() {
        val dbName = "schema_migration_15_16.db"
        createVersion15Database(dbName)

        helper.runMigrationsAndValidate(
            dbName,
            16,
            true,
            AppDatabase.MIGRATION_15_16
        ).close()

        SQLiteDatabase.openDatabase(
            context.getDatabasePath(dbName).path,
            null,
            SQLiteDatabase.OPEN_READONLY
        ).use { db ->
            db.rawQuery(
                """
                SELECT date, confirmedWorkDay
                FROM work_entries
                WHERE date IN ('2026-02-01', '2026-02-02', '2026-02-03')
                ORDER BY date
                """.trimIndent(),
                null
            ).use { c ->
                assertTrue(c.moveToFirst())
                assertEquals("2026-02-01", c.getString(0))
                assertEquals(1, c.getInt(1))

                assertTrue(c.moveToNext())
                assertEquals("2026-02-02", c.getString(0))
                assertEquals(1, c.getInt(1))

                assertTrue(c.moveToNext())
                assertEquals("2026-02-03", c.getString(0))
                assertEquals(0, c.getInt(1))
            }
        }
    }

    private fun createVersion13Database(dbName: String) {
        val dbFile = context.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()

        SQLiteDatabase.openOrCreateDatabase(dbFile, null).use {
            it.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `work_entries` (
                    `date` TEXT NOT NULL,
                    `workStart` TEXT NOT NULL,
                    `workEnd` TEXT NOT NULL,
                    `breakMinutes` INTEGER NOT NULL,
                    `dayType` TEXT NOT NULL,
                    `dayLocationLabel` TEXT NOT NULL,
                    `morningCapturedAt` INTEGER,
                    `eveningCapturedAt` INTEGER,
                    `confirmedWorkDay` INTEGER NOT NULL,
                    `confirmationAt` INTEGER,
                    `confirmationSource` TEXT,
                    `mealIsArrivalDeparture` INTEGER NOT NULL,
                    `mealBreakfastIncluded` INTEGER NOT NULL,
                    `mealAllowanceBaseCents` INTEGER NOT NULL,
                    `mealAllowanceAmountCents` INTEGER NOT NULL,
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
                    `returnStartAt` INTEGER,
                    `returnArriveAt` INTEGER,
                    `note` TEXT,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`date`)
                )
                """.trimIndent()
            )
            it.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_work_entries_date ON work_entries(date)")
            it.execSQL("CREATE INDEX IF NOT EXISTS index_work_entries_createdAt ON work_entries(createdAt)")
            it.execSQL("CREATE INDEX IF NOT EXISTS index_work_entries_dayType_date ON work_entries(dayType, date)")
            it.execSQL(
                """
                INSERT INTO work_entries (
                    date, workStart, workEnd, breakMinutes, dayType,
                    dayLocationLabel, morningCapturedAt, eveningCapturedAt,
                    confirmedWorkDay, confirmationAt, confirmationSource,
                    mealIsArrivalDeparture, mealBreakfastIncluded,
                    mealAllowanceBaseCents, mealAllowanceAmountCents,
                    travelStartAt, travelArriveAt, travelLabelStart, travelLabelEnd,
                    travelSource, travelUpdatedAt,
                    returnStartAt, returnArriveAt,
                    note, createdAt, updatedAt
                ) VALUES (
                    '2026-01-13', '07:00', '16:00', 45, 'WORK',
                    'Baustelle Süd', 1700000000, 1700013600,
                    1, 1700013700, 'UI',
                    1, 0, 1400, 820,
                    1700000000, 1700003600, 'Home', 'Site',
                    'MANUAL', 1700004000,
                    1700010000, 1700013600,
                    'Mit Rückfahrt', 1699990000, 1700013700
                )
                """.trimIndent()
            )
            it.version = 13
        }
    }

    private fun createVersion15Database(dbName: String) {
        val dbFile = context.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()

        SQLiteDatabase.openOrCreateDatabase(dbFile, null).use {
            it.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `work_entries` (
                    `date` TEXT NOT NULL,
                    `dayType` TEXT NOT NULL,
                    `workStart` TEXT,
                    `workEnd` TEXT,
                    `breakMinutes` INTEGER NOT NULL DEFAULT 0,
                    `confirmedWorkDay` INTEGER NOT NULL DEFAULT 0,
                    `confirmationAt` INTEGER,
                    `confirmationSource` TEXT,
                    PRIMARY KEY(`date`)
                )
                """.trimIndent()
            )
            it.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `travel_legs` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `workEntryDate` TEXT NOT NULL,
                    `sortOrder` INTEGER NOT NULL,
                    `category` TEXT NOT NULL,
                    `startAt` INTEGER,
                    `arriveAt` INTEGER,
                    `paidMinutesOverride` INTEGER
                )
                """.trimIndent()
            )
            it.execSQL(
                """
                INSERT INTO work_entries (
                    date, dayType, workStart, workEnd, breakMinutes,
                    confirmedWorkDay, confirmationAt, confirmationSource
                ) VALUES
                    ('2026-02-01', 'WORK', '08:00', '17:00', 60, 0, NULL, NULL),
                    ('2026-02-02', 'SCHULUNG', '08:00', '16:00', 30, 0, NULL, NULL),
                    ('2026-02-03', 'LEHRGANG', NULL, NULL, 0, 1, 3333, 'UI')
                """.trimIndent()
            )
            it.version = 15
        }
    }
}
