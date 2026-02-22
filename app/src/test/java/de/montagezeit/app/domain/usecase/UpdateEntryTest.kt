package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntry
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class UpdateEntryTest {

    private lateinit var workEntryDao: WorkEntryDao
    private lateinit var updateEntry: UpdateEntry

    @Before
    fun setup() {
        workEntryDao = mockk()
        updateEntry = UpdateEntry(workEntryDao)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `invoke aktualisiert entry und setzt updatedAt`() = runTest {
        val entry = validEntry(
            createdAt = 1_000_000L,
            updatedAt = 1_000_000L
        )
        coEvery { workEntryDao.upsert(any()) } just runs

        val result = updateEntry.invoke(entry)

        assertEquals(entry.date, result.date)
        assertEquals(entry.dayType, result.dayType)
        assertEquals(entry.createdAt, result.createdAt)
        assertTrue(result.updatedAt > entry.updatedAt)
        coVerify { workEntryDao.upsert(result) }
    }

    @Test
    fun `invoke behaelt alle felder ausser updatedAt`() = runTest {
        val entry = validEntry(
            note = "Test Notiz",
            createdAt = 1_000_000L,
            updatedAt = 1_000_000L
        )
        coEvery { workEntryDao.upsert(any()) } just runs

        val result = updateEntry.invoke(entry)

        assertEquals(entry.workStart, result.workStart)
        assertEquals(entry.workEnd, result.workEnd)
        assertEquals(entry.breakMinutes, result.breakMinutes)
        assertEquals(entry.note, result.note)
        assertEquals(entry.dayLocationLabel, result.dayLocationLabel)
        assertTrue(result.updatedAt > entry.updatedAt)
        coVerify { workEntryDao.upsert(result) }
    }

    @Test
    fun `invoke erlaubt uebernachtfahrt`() = runTest {
        val date = LocalDate.of(2026, 2, 1)
        val entry = validEntry(
            date = date,
            travelStartAt = epochOf(date, LocalTime.of(23, 0)),
            travelArriveAt = epochOf(date, LocalTime.of(1, 0))
        )
        coEvery { workEntryDao.upsert(any()) } just runs

        val result = updateEntry.invoke(entry)

        assertEquals(entry.travelStartAt, result.travelStartAt)
        assertEquals(entry.travelArriveAt, result.travelArriveAt)
        coVerify { workEntryDao.upsert(result) }
    }

    @Test
    fun `invoke lehnt zu lange fahrzeit ab`() = runTest {
        val date = LocalDate.of(2026, 2, 1)
        val entry = validEntry(
            date = date,
            travelStartAt = epochOf(date, LocalTime.of(2, 0)),
            travelArriveAt = epochOf(date, LocalTime.of(19, 0))
        )

        try {
            updateEntry.invoke(entry)
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Reisezeit darf maximal 16 Stunden betragen", e.message)
        }
    }

    private fun validEntry(
        date: LocalDate = LocalDate.now(),
        note: String? = null,
        travelStartAt: Long? = null,
        travelArriveAt: Long? = null,
        createdAt: Long = System.currentTimeMillis(),
        updatedAt: Long = System.currentTimeMillis()
    ): WorkEntry {
        return WorkEntry(
            date = date,
            dayType = DayType.WORK,
            dayLocationLabel = "Baustelle A",
            workStart = LocalTime.of(8, 0),
            workEnd = LocalTime.of(17, 0),
            breakMinutes = 60,
            travelStartAt = travelStartAt,
            travelArriveAt = travelArriveAt,
            note = note,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun epochOf(date: LocalDate, time: LocalTime): Long {
        return date.atTime(time)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }
}
