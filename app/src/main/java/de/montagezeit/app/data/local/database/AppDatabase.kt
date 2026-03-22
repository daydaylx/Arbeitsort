package de.montagezeit.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import de.montagezeit.app.data.local.converters.LocalDateConverter
import de.montagezeit.app.data.local.converters.LocalTimeConverter
import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.TravelLeg
import de.montagezeit.app.data.local.entity.WorkEntry

@Database(
    entities = [WorkEntry::class, TravelLeg::class],
    version = 14,
    exportSchema = false
)
@TypeConverters(
    LocalDateConverter::class,
    LocalTimeConverter::class
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun workEntryDao(): WorkEntryDao

    companion object {
        const val DATABASE_NAME = "montagezeit_database"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `route_cache` (
                        `fromLabel` TEXT NOT NULL,
                        `toLabel` TEXT NOT NULL,
                        `distanceKm` REAL NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`fromLabel`, `toLabel`)
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `route_cache` (
                        `fromLabel` TEXT NOT NULL,
                        `toLabel` TEXT NOT NULL,
                        `distanceKm` REAL NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`fromLabel`, `toLabel`)
                    )
                """.trimIndent())

                db.execSQL("CREATE INDEX IF NOT EXISTS index_work_entries_needsReview ON work_entries(needsReview)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_work_entries_createdAt ON work_entries(createdAt)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_work_entries_date ON work_entries(date)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_work_entries_dayType_date ON work_entries(dayType, date)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE work_entries ADD COLUMN confirmedWorkDay INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE work_entries ADD COLUMN confirmationAt INTEGER")
                db.execSQL("ALTER TABLE work_entries ADD COLUMN confirmationSource TEXT")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_work_entries_dayType_date ON work_entries(dayType, date)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE work_entries ADD COLUMN dayLocationLabel TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE work_entries ADD COLUMN dayLocationSource TEXT NOT NULL DEFAULT 'FALLBACK'")
                db.execSQL("ALTER TABLE work_entries ADD COLUMN dayLocationLat REAL")
                db.execSQL("ALTER TABLE work_entries ADD COLUMN dayLocationLon REAL")
                db.execSQL("ALTER TABLE work_entries ADD COLUMN dayLocationAccuracyMeters REAL")
            }
        }

        // Migration 6→7: Route-Cache-Feature entfernt.
        // Die route_cache-Tabelle wird gedropt – sie war nie produktiv genutzt.
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `route_cache`")
            }
        }

        // Migration 7→8: Legacy-Fallback-Werte aus dayLocationLabel entfernen.
        // Fallback-Einträge werden grundsätzlich auf leeren Tagesort normalisiert.
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "UPDATE work_entries SET dayLocationLabel = '' " +
                    "WHERE dayLocationSource = 'FALLBACK'"
                )
            }
        }

        // Migration 8→9:
        // 1) Entfernt legacy Standort-Spalten aus älteren Versionen
        // 2) Deaktiviert bestehende Review-Flags, da der Review-Flow entfällt
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `work_entries_new` (
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

                db.execSQL(
                    """
                    INSERT INTO `work_entries_new` (
                        date,
                        workStart,
                        workEnd,
                        breakMinutes,
                        dayType,
                        dayLocationLabel,
                        dayLocationSource,
                        dayLocationLat,
                        dayLocationLon,
                        dayLocationAccuracyMeters,
                        morningCapturedAt,
                        morningLocationLabel,
                        morningLat,
                        morningLon,
                        morningAccuracyMeters,
                        morningLocationStatus,
                        eveningCapturedAt,
                        eveningLocationLabel,
                        eveningLat,
                        eveningLon,
                        eveningAccuracyMeters,
                        eveningLocationStatus,
                        travelStartAt,
                        travelArriveAt,
                        travelLabelStart,
                        travelLabelEnd,
                        travelFromLabel,
                        travelToLabel,
                        travelDistanceKm,
                        travelPaidMinutes,
                        travelSource,
                        travelUpdatedAt,
                        confirmedWorkDay,
                        confirmationAt,
                        confirmationSource,
                        needsReview,
                        note,
                        createdAt,
                        updatedAt
                    )
                    SELECT
                        date,
                        workStart,
                        workEnd,
                        breakMinutes,
                        dayType,
                        dayLocationLabel,
                        dayLocationSource,
                        dayLocationLat,
                        dayLocationLon,
                        dayLocationAccuracyMeters,
                        morningCapturedAt,
                        morningLocationLabel,
                        morningLat,
                        morningLon,
                        morningAccuracyMeters,
                        morningLocationStatus,
                        eveningCapturedAt,
                        eveningLocationLabel,
                        eveningLat,
                        eveningLon,
                        eveningAccuracyMeters,
                        eveningLocationStatus,
                        travelStartAt,
                        travelArriveAt,
                        travelLabelStart,
                        travelLabelEnd,
                        travelFromLabel,
                        travelToLabel,
                        travelDistanceKm,
                        travelPaidMinutes,
                        travelSource,
                        travelUpdatedAt,
                        confirmedWorkDay,
                        confirmationAt,
                        confirmationSource,
                        0,
                        note,
                        createdAt,
                        updatedAt
                    FROM `work_entries`
                    """.trimIndent()
                )

                db.execSQL("DROP TABLE `work_entries`")
                db.execSQL("ALTER TABLE `work_entries_new` RENAME TO `work_entries`")

                db.execSQL("CREATE INDEX IF NOT EXISTS index_work_entries_needsReview ON work_entries(needsReview)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_work_entries_date ON work_entries(date)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_work_entries_createdAt ON work_entries(createdAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_work_entries_dayType_date ON work_entries(dayType, date)")
            }
        }

        // Migration 9→10: COMP_TIME added to DayType enum.
        // dayType is persisted as TEXT in Room; no column changes required.
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // No schema changes needed – adding a new DayType enum value
                // does not alter any column definition.
            }
        }

        // Migration 10→11: Verpflegungspauschale-Felder hinzugefügt.
        // Altbestände erhalten Standardwerte false/false/0/0.
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE work_entries ADD COLUMN mealIsArrivalDeparture INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE work_entries ADD COLUMN mealBreakfastIncluded INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE work_entries ADD COLUMN mealAllowanceBaseCents INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE work_entries ADD COLUMN mealAllowanceAmountCents INTEGER NOT NULL DEFAULT 0")
            }
        }

        // Migration 11→12: GPS-/Review-/Snapshot-Label-Datenmodell entfernt.
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `work_entries_new` (
                        `date` TEXT NOT NULL,
                        `workStart` TEXT NOT NULL,
                        `workEnd` TEXT NOT NULL,
                        `breakMinutes` INTEGER NOT NULL,
                        `dayType` TEXT NOT NULL,
                        `dayLocationLabel` TEXT NOT NULL,
                        `morningCapturedAt` INTEGER,
                        `eveningCapturedAt` INTEGER,
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
                        `note` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`date`)
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    INSERT INTO `work_entries_new` (
                        date,
                        workStart,
                        workEnd,
                        breakMinutes,
                        dayType,
                        dayLocationLabel,
                        morningCapturedAt,
                        eveningCapturedAt,
                        travelStartAt,
                        travelArriveAt,
                        travelLabelStart,
                        travelLabelEnd,
                        travelFromLabel,
                        travelToLabel,
                        travelDistanceKm,
                        travelPaidMinutes,
                        travelSource,
                        travelUpdatedAt,
                        confirmedWorkDay,
                        confirmationAt,
                        confirmationSource,
                        mealIsArrivalDeparture,
                        mealBreakfastIncluded,
                        mealAllowanceBaseCents,
                        mealAllowanceAmountCents,
                        note,
                        createdAt,
                        updatedAt
                    )
                    SELECT
                        date,
                        workStart,
                        workEnd,
                        breakMinutes,
                        dayType,
                        dayLocationLabel,
                        morningCapturedAt,
                        eveningCapturedAt,
                        travelStartAt,
                        travelArriveAt,
                        travelLabelStart,
                        travelLabelEnd,
                        travelFromLabel,
                        travelToLabel,
                        travelDistanceKm,
                        travelPaidMinutes,
                        travelSource,
                        travelUpdatedAt,
                        confirmedWorkDay,
                        confirmationAt,
                        confirmationSource,
                        mealIsArrivalDeparture,
                        mealBreakfastIncluded,
                        mealAllowanceBaseCents,
                        mealAllowanceAmountCents,
                        note,
                        createdAt,
                        updatedAt
                    FROM `work_entries`
                    """.trimIndent()
                )

                db.execSQL("DROP TABLE `work_entries`")
                db.execSQL("ALTER TABLE `work_entries_new` RENAME TO `work_entries`")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_work_entries_date ON work_entries(date)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_work_entries_createdAt ON work_entries(createdAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_work_entries_dayType_date ON work_entries(dayType, date)")
            }
        }

        // Migration 12→13: Rückfahrt-Zeitstempel hinzugefügt.
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE work_entries ADD COLUMN returnStartAt INTEGER")
                db.execSQL("ALTER TABLE work_entries ADD COLUMN returnArriveAt INTEGER")
            }
        }

        // Migration 13→14: Travel normalisiert in Child-Tabelle `travel_legs`,
        // Work-Block wird nullable, Legacy-Travel-Spalten werden entfernt.
        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `travel_legs` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `workEntryDate` TEXT NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        `category` TEXT NOT NULL,
                        `startAt` INTEGER,
                        `arriveAt` INTEGER,
                        `startLabel` TEXT,
                        `endLabel` TEXT,
                        `paidMinutesOverride` INTEGER,
                        `source` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        FOREIGN KEY(`workEntryDate`) REFERENCES `work_entries`(`date`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_travel_legs_workEntryDate ON travel_legs(workEntryDate)")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_travel_legs_workEntryDate_sortOrder " +
                        "ON travel_legs(workEntryDate, sortOrder)"
                )

                db.execSQL(
                    """
                    INSERT INTO `travel_legs` (
                        workEntryDate,
                        sortOrder,
                        category,
                        startAt,
                        arriveAt,
                        startLabel,
                        endLabel,
                        paidMinutesOverride,
                        source,
                        createdAt,
                        updatedAt
                    )
                    SELECT
                        date,
                        0,
                        'OUTBOUND',
                        travelStartAt,
                        travelArriveAt,
                        travelLabelStart,
                        travelLabelEnd,
                        NULL,
                        travelSource,
                        COALESCE(travelUpdatedAt, updatedAt, createdAt),
                        COALESCE(travelUpdatedAt, updatedAt, createdAt)
                    FROM work_entries
                    WHERE travelStartAt IS NOT NULL
                        OR travelArriveAt IS NOT NULL
                        OR travelLabelStart IS NOT NULL
                        OR travelLabelEnd IS NOT NULL
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    INSERT INTO `travel_legs` (
                        workEntryDate,
                        sortOrder,
                        category,
                        startAt,
                        arriveAt,
                        startLabel,
                        endLabel,
                        paidMinutesOverride,
                        source,
                        createdAt,
                        updatedAt
                    )
                    SELECT
                        date,
                        1,
                        'RETURN',
                        returnStartAt,
                        returnArriveAt,
                        NULL,
                        NULL,
                        NULL,
                        travelSource,
                        COALESCE(travelUpdatedAt, updatedAt, createdAt),
                        COALESCE(travelUpdatedAt, updatedAt, createdAt)
                    FROM work_entries
                    WHERE returnStartAt IS NOT NULL
                        OR returnArriveAt IS NOT NULL
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    INSERT INTO `travel_legs` (
                        workEntryDate,
                        sortOrder,
                        category,
                        startAt,
                        arriveAt,
                        startLabel,
                        endLabel,
                        paidMinutesOverride,
                        source,
                        createdAt,
                        updatedAt
                    )
                    SELECT
                        date,
                        2,
                        'OTHER',
                        NULL,
                        NULL,
                        travelLabelStart,
                        travelLabelEnd,
                        travelPaidMinutes,
                        travelSource,
                        COALESCE(travelUpdatedAt, updatedAt, createdAt),
                        COALESCE(travelUpdatedAt, updatedAt, createdAt)
                    FROM work_entries
                    WHERE travelPaidMinutes IS NOT NULL
                        AND travelPaidMinutes > 0
                        AND NOT (travelStartAt IS NOT NULL AND travelArriveAt IS NOT NULL)
                        AND NOT (returnStartAt IS NOT NULL AND returnArriveAt IS NOT NULL)
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `work_entries_new` (
                        `date` TEXT NOT NULL,
                        `workStart` TEXT,
                        `workEnd` TEXT,
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
                        `note` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`date`)
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    INSERT INTO `work_entries_new` (
                        date,
                        workStart,
                        workEnd,
                        breakMinutes,
                        dayType,
                        dayLocationLabel,
                        morningCapturedAt,
                        eveningCapturedAt,
                        confirmedWorkDay,
                        confirmationAt,
                        confirmationSource,
                        mealIsArrivalDeparture,
                        mealBreakfastIncluded,
                        mealAllowanceBaseCents,
                        mealAllowanceAmountCents,
                        note,
                        createdAt,
                        updatedAt
                    )
                    SELECT
                        date,
                        workStart,
                        workEnd,
                        breakMinutes,
                        dayType,
                        dayLocationLabel,
                        morningCapturedAt,
                        eveningCapturedAt,
                        confirmedWorkDay,
                        confirmationAt,
                        confirmationSource,
                        mealIsArrivalDeparture,
                        mealBreakfastIncluded,
                        mealAllowanceBaseCents,
                        mealAllowanceAmountCents,
                        note,
                        createdAt,
                        updatedAt
                    FROM `work_entries`
                    """.trimIndent()
                )

                db.execSQL("DROP TABLE `work_entries`")
                db.execSQL("ALTER TABLE `work_entries_new` RENAME TO `work_entries`")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_work_entries_date ON work_entries(date)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_work_entries_createdAt ON work_entries(createdAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_work_entries_dayType_date ON work_entries(dayType, date)")
            }
        }

        val MIGRATIONS = arrayOf(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
            MIGRATION_6_7,
            MIGRATION_7_8,
            MIGRATION_8_9,
            MIGRATION_9_10,
            MIGRATION_10_11,
            MIGRATION_11_12,
            MIGRATION_12_13,
            MIGRATION_13_14
        )
    }
}
