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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class UpsertPreservesTravelLegsTest {

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
    fun `parent upsert preserves travel legs`() = runTest {
        val date = LocalDate.of(2024, 10, 1)
        val entry = WorkEntry(date = date, dayType = DayType.WORK, createdAt = 100L, updatedAt = 100L)
        dao.upsert(entry)
        dao.upsertTravelLegs(
            listOf(
                TravelLeg(
                    workEntryDate = date,
                    sortOrder = 0,
                    category = TravelLegCategory.OUTBOUND,
                    startLabel = "Home",
                    createdAt = 200L,
                    updatedAt = 200L
                ),
                TravelLeg(
                    workEntryDate = date,
                    sortOrder = 1,
                    category = TravelLegCategory.RETURN,
                    endLabel = "Home",
                    createdAt = 300L,
                    updatedAt = 300L
                )
            )
        )
        val legsBefore = dao.getTravelLegsByDate(date)
        assertEquals(2, legsBefore.size)
        val leg0Id = legsBefore[0].id
        val leg1Id = legsBefore[1].id
        val leg0CreatedAt = legsBefore[0].createdAt
        val leg1CreatedAt = legsBefore[1].createdAt

        val updatedEntry = entry.copy(dayType = DayType.OFF, updatedAt = 999L)
        dao.upsert(updatedEntry)

        val legsAfter = dao.getTravelLegsByDate(date)
        assertEquals("Legs should survive parent upsert", 2, legsAfter.size)
        assertEquals(leg0Id, legsAfter[0].id)
        assertEquals(leg1Id, legsAfter[1].id)
        assertEquals(leg0CreatedAt, legsAfter[0].createdAt)
        assertEquals(leg1CreatedAt, legsAfter[1].createdAt)
        assertEquals("Home", legsAfter[0].startLabel)
        assertEquals("Home", legsAfter[1].endLabel)
    }

    @Test
    fun `replaceEntryWithTravelLegs preserves existing leg IDs and createdAt`() = runTest {
        val date = LocalDate.of(2024, 10, 2)
        val entry = WorkEntry(date = date, dayType = DayType.WORK, createdAt = 100L, updatedAt = 100L)
        dao.upsert(entry)
        dao.upsertTravelLegs(
            listOf(
                TravelLeg(
                    workEntryDate = date,
                    sortOrder = 0,
                    category = TravelLegCategory.OUTBOUND,
                    startLabel = "A",
                    createdAt = 200L,
                    updatedAt = 200L
                ),
                TravelLeg(
                    workEntryDate = date,
                    sortOrder = 1,
                    category = TravelLegCategory.RETURN,
                    endLabel = "B",
                    createdAt = 300L,
                    updatedAt = 300L
                )
            )
        )
        val originalLegs = dao.getTravelLegsByDate(date)
        assertEquals(2, originalLegs.size)
        val originalId0 = originalLegs[0].id
        val originalCreatedAt0 = originalLegs[0].createdAt
        val originalId1 = originalLegs[1].id
        val originalCreatedAt1 = originalLegs[1].createdAt
        assertTrue("Leg should have auto-generated ID > 0", originalId0 > 0)
        assertTrue("Leg should have auto-generated ID > 0", originalId1 > 0)

        val modifiedEntry = entry.copy(note = "updated", updatedAt = 999L)
        val modifiedLegs = listOf(
            TravelLeg(
                workEntryDate = date,
                sortOrder = 0,
                category = TravelLegCategory.OUTBOUND,
                startLabel = "A-modified",
                createdAt = 0L,
                updatedAt = 999L
            ),
            TravelLeg(
                workEntryDate = date,
                sortOrder = 1,
                category = TravelLegCategory.RETURN,
                endLabel = "B-modified",
                createdAt = 0L,
                updatedAt = 999L
            )
        )
        dao.replaceEntryWithTravelLegs(modifiedEntry, modifiedLegs)

        val resultLegs = dao.getTravelLegsByDate(date)
        assertEquals(2, resultLegs.size)
        assertEquals("Leg 0 ID should be preserved", originalId0, resultLegs[0].id)
        assertEquals("Leg 0 createdAt should be preserved", originalCreatedAt0, resultLegs[0].createdAt)
        assertEquals("A-modified", resultLegs[0].startLabel)
        assertEquals("Leg 1 ID should be preserved", originalId1, resultLegs[1].id)
        assertEquals("Leg 1 createdAt should be preserved", originalCreatedAt1, resultLegs[1].createdAt)
        assertEquals("B-modified", resultLegs[1].endLabel)
    }

    @Test
    fun `replaceEntryWithTravelLegs deletes removed legs only`() = runTest {
        val date = LocalDate.of(2024, 10, 3)
        val entry = WorkEntry(date = date, dayType = DayType.WORK, createdAt = 100L, updatedAt = 100L)
        dao.upsert(entry)
        dao.upsertTravelLegs(
            listOf(
                TravelLeg(workEntryDate = date, sortOrder = 0, category = TravelLegCategory.OUTBOUND, createdAt = 200L),
                TravelLeg(workEntryDate = date, sortOrder = 1, category = TravelLegCategory.INTERSITE, createdAt = 300L),
                TravelLeg(workEntryDate = date, sortOrder = 2, category = TravelLegCategory.RETURN, createdAt = 400L)
            )
        )
        val originalLegs = dao.getTravelLegsByDate(date)
        val keptId = originalLegs[0].id
        val keptCreatedAt = originalLegs[0].createdAt

        val modifiedEntry = entry.copy(updatedAt = 999L)
        val singleLeg = listOf(
            TravelLeg(workEntryDate = date, sortOrder = 0, category = TravelLegCategory.OUTBOUND, createdAt = 0L)
        )
        dao.replaceEntryWithTravelLegs(modifiedEntry, singleLeg)

        val resultLegs = dao.getTravelLegsByDate(date)
        assertEquals("Only 1 leg should remain", 1, resultLegs.size)
        assertEquals("Kept leg should preserve ID", keptId, resultLegs[0].id)
        assertEquals("Kept leg should preserve createdAt", keptCreatedAt, resultLegs[0].createdAt)
    }

    @Test
    fun `readModifyWrite preserves travel legs`() = runTest {
        val date = LocalDate.of(2024, 10, 4)
        val entry = WorkEntry(date = date, dayType = DayType.WORK, createdAt = 100L, updatedAt = 100L)
        dao.upsert(entry)
        dao.upsertTravelLegs(
            listOf(
                TravelLeg(workEntryDate = date, sortOrder = 0, category = TravelLegCategory.OUTBOUND, createdAt = 200L),
                TravelLeg(workEntryDate = date, sortOrder = 1, category = TravelLegCategory.RETURN, createdAt = 300L)
            )
        )
        val legsBefore = dao.getTravelLegsByDate(date)
        val id0 = legsBefore[0].id
        val id1 = legsBefore[1].id

        dao.readModifyWrite(date) { existing ->
            existing!!.copy(dayLocationLabel = "Office", updatedAt = 999L)
        }

        val legsAfter = dao.getTravelLegsByDate(date)
        assertEquals("readModifyWrite should not delete legs", 2, legsAfter.size)
        assertEquals(id0, legsAfter[0].id)
        assertEquals(id1, legsAfter[1].id)
        val updatedEntry = dao.getByDate(date)
        assertEquals("Office", updatedEntry!!.dayLocationLabel)
    }

    @Test
    fun `upsertAllAndDeleteTravelLegs only deletes targeted dates`() = runTest {
        val date1 = LocalDate.of(2024, 10, 5)
        val date2 = LocalDate.of(2024, 10, 6)
        val date3 = LocalDate.of(2024, 10, 7)
        val entry1 = WorkEntry(date = date1, dayType = DayType.WORK, createdAt = 100L, updatedAt = 100L)
        val entry2 = WorkEntry(date = date2, dayType = DayType.WORK, createdAt = 100L, updatedAt = 100L)
        val entry3 = WorkEntry(date = date3, dayType = DayType.WORK, createdAt = 100L, updatedAt = 100L)
        dao.upsert(entry1)
        dao.upsert(entry2)
        dao.upsert(entry3)
        dao.upsertTravelLegs(
            listOf(
                TravelLeg(workEntryDate = date1, sortOrder = 0, category = TravelLegCategory.OUTBOUND, createdAt = 200L),
                TravelLeg(workEntryDate = date2, sortOrder = 0, category = TravelLegCategory.OUTBOUND, createdAt = 200L),
                TravelLeg(workEntryDate = date3, sortOrder = 0, category = TravelLegCategory.OUTBOUND, createdAt = 200L)
            )
        )

        val updatedEntries = listOf(
            entry1.copy(note = "changed", updatedAt = 999L),
            entry2.copy(note = "changed", updatedAt = 999L),
            entry3.copy(note = "changed", updatedAt = 999L)
        )
        dao.upsertAllAndDeleteTravelLegs(updatedEntries, travelLegDatesToDelete = listOf(date1))

        val legs1 = dao.getTravelLegsByDate(date1)
        val legs2 = dao.getTravelLegsByDate(date2)
        val legs3 = dao.getTravelLegsByDate(date3)
        assertTrue("date1 legs should be explicitly deleted", legs1.isEmpty())
        assertEquals("date2 legs should survive upsertAll", 1, legs2.size)
        assertEquals("date3 legs should survive upsertAll", 1, legs3.size)
    }

    @Test
    fun `upsertAll does not cascade-delete travel legs`() = runTest {
        val date = LocalDate.of(2024, 10, 8)
        val entry = WorkEntry(date = date, dayType = DayType.WORK, createdAt = 100L, updatedAt = 100L)
        dao.upsert(entry)
        dao.upsertTravelLegs(
            listOf(
                TravelLeg(workEntryDate = date, sortOrder = 0, category = TravelLegCategory.OUTBOUND, createdAt = 200L)
            )
        )
        val legsBefore = dao.getTravelLegsByDate(date)
        val originalId = legsBefore[0].id

        dao.upsertAll(listOf(entry.copy(note = "batch update", updatedAt = 999L)))

        val legsAfter = dao.getTravelLegsByDate(date)
        assertEquals("Leg should survive upsertAll", 1, legsAfter.size)
        assertEquals(originalId, legsAfter[0].id)
    }

    @Test
    fun `replaceEntryWithTravelLegs assigns new IDs for genuinely new legs`() = runTest {
        val date = LocalDate.of(2024, 10, 9)
        val entry = WorkEntry(date = date, dayType = DayType.WORK, createdAt = 100L, updatedAt = 100L)
        dao.replaceEntryWithTravelLegs(
            entry,
            listOf(
                TravelLeg(workEntryDate = date, sortOrder = 0, category = TravelLegCategory.OUTBOUND, createdAt = 200L)
            )
        )

        val legs = dao.getTravelLegsByDate(date)
        assertEquals(1, legs.size)
        assertTrue("New leg should get auto-generated ID", legs[0].id > 0)
        assertEquals(200L, legs[0].createdAt)
    }

    @Test
    fun `replaceEntryWithTravelLegs with empty legs removes all existing`() = runTest {
        val date = LocalDate.of(2024, 10, 10)
        val entry = WorkEntry(date = date, dayType = DayType.WORK, createdAt = 100L, updatedAt = 100L)
        dao.upsert(entry)
        dao.upsertTravelLegs(
            listOf(
                TravelLeg(workEntryDate = date, sortOrder = 0, category = TravelLegCategory.OUTBOUND, createdAt = 200L),
                TravelLeg(workEntryDate = date, sortOrder = 1, category = TravelLegCategory.RETURN, createdAt = 300L)
            )
        )
        assertEquals(2, dao.getTravelLegsByDate(date).size)

        dao.replaceEntryWithTravelLegs(entry.copy(updatedAt = 999L), emptyList())

        val legs = dao.getTravelLegsByDate(date)
        assertTrue("All legs should be removed", legs.isEmpty())
    }
}
