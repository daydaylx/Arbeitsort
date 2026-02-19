package de.montagezeit.app.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.montagezeit.app.data.local.database.AppDatabase
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class WorkEntryDaoIntegrationTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: WorkEntryDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.workEntryDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun upsert_creates_new_entry() = runTest {
        val date = LocalDate.of(2024, 6, 10)
        val entry = WorkEntry(date = date, dayType = DayType.WORK)

        dao.upsert(entry)

        val retrieved = dao.getByDate(date)
        assertNotNull(retrieved)
        assertEquals(DayType.WORK, retrieved!!.dayType)
    }

    @Test
    fun upsert_updates_existing_entry() = runTest {
        val date = LocalDate.of(2024, 6, 11)
        val original = WorkEntry(date = date, dayType = DayType.WORK)
        dao.upsert(original)

        val updated = original.copy(dayType = DayType.OFF)
        dao.upsert(updated)

        val retrieved = dao.getByDate(date)
        assertEquals(DayType.OFF, retrieved!!.dayType)
    }

    @Test
    fun getByDate_returns_null_for_missing_entry() = runTest {
        val result = dao.getByDate(LocalDate.of(2099, 1, 1))
        assertNull(result)
    }

    @Test
    fun getByDateFlow_emits_updated_entry() = runTest {
        val date = LocalDate.of(2024, 7, 1)
        val entry = WorkEntry(date = date, dayType = DayType.WORK)

        dao.upsert(entry)
        val emitted = dao.getByDateFlow(date).first()

        assertNotNull(emitted)
        assertEquals(DayType.WORK, emitted!!.dayType)
    }

    @Test
    fun getByDateRange_returns_entries_in_range() = runTest {
        val inRange1 = WorkEntry(date = LocalDate.of(2024, 8, 5))
        val inRange2 = WorkEntry(date = LocalDate.of(2024, 8, 10))
        val outOfRange = WorkEntry(date = LocalDate.of(2024, 8, 20))
        dao.upsert(inRange1)
        dao.upsert(inRange2)
        dao.upsert(outOfRange)

        val results = dao.getByDateRange(LocalDate.of(2024, 8, 1), LocalDate.of(2024, 8, 15))

        assertEquals(2, results.size)
        assertEquals(LocalDate.of(2024, 8, 5), results[0].date)
        assertEquals(LocalDate.of(2024, 8, 10), results[1].date)
    }

    @Test
    fun getEntriesBetween_returns_projection_in_ascending_order() = runTest {
        val unconfirmed = WorkEntry(
            date = LocalDate.of(2024, 9, 1),
            confirmedWorkDay = false,
            breakMinutes = 30
        )
        val confirmedWork = WorkEntry(
            date = LocalDate.of(2024, 9, 2),
            dayType = DayType.WORK,
            confirmedWorkDay = true,
            breakMinutes = 45
        )
        val confirmedOff = WorkEntry(
            date = LocalDate.of(2024, 9, 3),
            dayType = DayType.OFF,
            confirmedWorkDay = true,
            travelPaidMinutes = 90
        )
        dao.upsert(confirmedOff)
        dao.upsert(confirmedWork)
        dao.upsert(unconfirmed)

        val result = dao.getEntriesBetween(
            LocalDate.of(2024, 9, 1),
            LocalDate.of(2024, 9, 3)
        )

        assertEquals(3, result.size)
        assertEquals(LocalDate.of(2024, 9, 1), result[0].date)
        assertEquals(LocalDate.of(2024, 9, 2), result[1].date)
        assertEquals(LocalDate.of(2024, 9, 3), result[2].date)
        assertTrue(result[1].confirmedWorkDay)
        assertEquals(90, result[2].travelPaidMinutes)
    }
}
