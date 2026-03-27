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
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class UpdateEntryTest {

    private lateinit var workEntryDao: WorkEntryDao
    private lateinit var updateEntry: UpdateEntry

    @Before
    fun setup() {
        workEntryDao = mockk(relaxed = true)
        updateEntry = UpdateEntry(workEntryDao)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `invoke aktualisiert entry und setzt updatedAt`() = runTest {
        val entry = validEntry(createdAt = 1_000_000L, updatedAt = 1_000_000L)
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
        val entry = validEntry(note = "Test Notiz", createdAt = 1_000_000L, updatedAt = 1_000_000L)
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
    fun `invoke speichert OFF-Tag ohne dayLocationLabel`() = runTest {
        val entry = WorkEntry(
            date = LocalDate.now(),
            dayType = DayType.OFF,
            dayLocationLabel = "",
            workStart = LocalTime.of(8, 0),
            workEnd = LocalTime.of(17, 0),
            breakMinutes = 60,
            createdAt = 1_000_000L,
            updatedAt = 1_000_000L
        )
        coEvery { workEntryDao.upsert(any()) } just runs

        val result = updateEntry.invoke(entry)

        assertEquals(DayType.OFF, result.dayType)
        assertEquals("", result.dayLocationLabel)
        coVerify { workEntryDao.upsert(result) }
    }

    @Test
    fun `invoke speichert COMP_TIME-Tag ohne dayLocationLabel`() = runTest {
        val entry = WorkEntry(
            date = LocalDate.now(),
            dayType = DayType.COMP_TIME,
            dayLocationLabel = "",
            workStart = LocalTime.of(8, 0),
            workEnd = LocalTime.of(17, 0),
            breakMinutes = 60,
            createdAt = 1_000_000L,
            updatedAt = 1_000_000L
        )
        coEvery { workEntryDao.upsert(any()) } just runs

        val result = updateEntry.invoke(entry)

        assertEquals(DayType.COMP_TIME, result.dayType)
        coVerify { workEntryDao.upsert(result) }
    }

    @Test
    fun `OFF Eintrag mit leerem dayLocationLabel wird gespeichert`() = runTest {
        val entry = WorkEntry(
            date = LocalDate.now(),
            dayType = DayType.OFF,
            dayLocationLabel = "",
            workStart = LocalTime.of(8, 0),
            workEnd = LocalTime.of(17, 0),
            breakMinutes = 60
        )
        coEvery { workEntryDao.upsert(any()) } just runs

        val result = updateEntry.invoke(entry)
        assertEquals(DayType.OFF, result.dayType)
        coVerify { workEntryDao.upsert(result) }
    }

    @Test
    fun `COMP_TIME Eintrag wird ohne Zeitvalidierung gespeichert`() = runTest {
        val entry = WorkEntry(
            date = LocalDate.now(),
            dayType = DayType.COMP_TIME,
            dayLocationLabel = "",
            workStart = LocalTime.of(8, 0),
            workEnd = LocalTime.of(8, 0), // Ende = Start – wäre bei WORK ungültig
            breakMinutes = 0
        )
        coEvery { workEntryDao.upsert(any()) } just runs

        val result = updateEntry.invoke(entry)
        assertEquals(DayType.COMP_TIME, result.dayType)
        coVerify { workEntryDao.upsert(result) }
    }

    @Test
    fun `WORK Eintrag ohne Arbeitszeiten wird gespeichert`() = runTest {
        val entry = WorkEntry(
            date = LocalDate.now(),
            dayType = DayType.WORK,
            dayLocationLabel = "Baustelle",
            workStart = null,
            workEnd = null,
            breakMinutes = 0
        )
        coEvery { workEntryDao.upsert(any()) } just runs

        val result = updateEntry.invoke(entry)
        assertEquals(DayType.WORK, result.dayType)
        assertEquals(0, result.breakMinutes)
        coVerify { workEntryDao.upsert(result) }
    }

    @Test
    fun `invoke trimmt dayLocationLabel vor dem Speichern`() = runTest {
        val entry = validEntry().copy(dayLocationLabel = "  Baustelle A  ")
        coEvery { workEntryDao.upsert(any()) } just runs

        val result = updateEntry.invoke(entry)

        assertEquals("Baustelle A", result.dayLocationLabel)
        coVerify { workEntryDao.upsert(match { it.dayLocationLabel == "Baustelle A" }) }
    }

    @Test
    fun `invoke lehnt WORK Eintrag ohne Arbeitsort ab`() = runTest {
        val entry = validEntry().copy(dayLocationLabel = "   ")

        val error = try {
            updateEntry.invoke(entry)
            throw AssertionError("Expected IllegalArgumentException")
        } catch (exception: IllegalArgumentException) {
            exception
        }

        assertEquals("dayLocationLabel darf bei WORK nicht leer sein", error.message)
        coVerify(exactly = 0) { workEntryDao.upsert(any()) }
    }

    @Test
    fun `invoke lehnt teilweisen Arbeitsblock ab`() = runTest {
        val entry = validEntry().copy(workEnd = null)

        val error = try {
            updateEntry.invoke(entry)
            throw AssertionError("Expected IllegalArgumentException")
        } catch (exception: IllegalArgumentException) {
            exception
        }

        assertEquals("workStart und workEnd muessen zusammen gesetzt werden", error.message)
        coVerify(exactly = 0) { workEntryDao.upsert(any()) }
    }

    private fun validEntry(
        date: LocalDate = LocalDate.now(),
        note: String? = null,
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
            note = note,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}
