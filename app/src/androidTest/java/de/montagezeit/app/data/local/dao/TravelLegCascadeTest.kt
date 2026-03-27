package de.montagezeit.app.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.montagezeit.app.data.local.database.AppDatabase
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.TravelLeg
import de.montagezeit.app.data.local.entity.TravelLegCategory
import de.montagezeit.app.data.local.entity.WorkEntry
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Verifies that deleting a WorkEntry cascades to its TravelLeg rows.
 *
 * SQLite foreign key enforcement is disabled by default; Room must call
 * setForeignKeyConstraintsEnabled(true) on the builder or the CASCADE declaration
 * on TravelLeg.workEntryDate is silently ignored.
 */
@RunWith(AndroidJUnit4::class)
class TravelLegCascadeTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: WorkEntryDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        )
            .setForeignKeyConstraintsEnabled(true)
            .allowMainThreadQueries()
            .build()
        dao = db.workEntryDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `deleteByDate removes associated travel legs via cascade`() = runTest {
        val date = LocalDate.of(2024, 9, 1)
        dao.upsert(WorkEntry(date = date, dayType = DayType.WORK))
        dao.upsertTravelLegs(
            listOf(
                TravelLeg(workEntryDate = date, sortOrder = 0, category = TravelLegCategory.OUTBOUND),
                TravelLeg(workEntryDate = date, sortOrder = 1, category = TravelLegCategory.RETURN)
            )
        )

        dao.deleteByDate(date)

        val remainingLegs = dao.getTravelLegsByDate(date)
        assertTrue(
            "Expected no travel legs after WorkEntry deletion, but found: $remainingLegs",
            remainingLegs.isEmpty()
        )
    }
}
