package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntry
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.LocalDate

class RecordEveningCheckInTest {

    private val workEntryDao = mockk<WorkEntryDao>(relaxed = true)
    private val useCase = RecordEveningCheckIn(workEntryDao)

    @Test
    fun `invoke creates new WORK entry with evening timestamp`() = runTest {
        val date = LocalDate.now()
        coEvery { workEntryDao.getByDate(date) } returns null
        coEvery { workEntryDao.upsert(any()) } returns Unit

        val result = useCase(date)

        assertEquals(date, result.date)
        assertEquals(DayType.WORK, result.dayType)
        assertNotNull(result.eveningCapturedAt)
        assertEquals("", result.dayLocationLabel)

        coVerify(exactly = 1) { workEntryDao.upsert(result) }
    }

    @Test
    fun `invoke preserves manual day location and morning data on existing WORK entry`() = runTest {
        val date = LocalDate.now()
        val existing = WorkEntry(
            date = date,
            dayType = DayType.WORK,
            dayLocationLabel = "Berlin",
            morningCapturedAt = 1111L
        )

        coEvery { workEntryDao.getByDate(date) } returns existing
        coEvery { workEntryDao.upsert(any()) } returns Unit

        val result = useCase(date)

        assertEquals(DayType.WORK, result.dayType)
        assertEquals("Berlin", result.dayLocationLabel)
        assertEquals(1111L, result.morningCapturedAt)
        assertNotNull(result.eveningCapturedAt)
    }

    @Test
    fun `invoke blocks evening check-in for OFF day`() = runTest {
        val date = LocalDate.now()
        val existing = WorkEntry(
            date = date,
            dayType = DayType.OFF
        )

        coEvery { workEntryDao.getByDate(date) } returns existing

        try {
            useCase(date)
            fail("Expected IllegalStateException")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("OFF") == true)
        }

        coVerify(exactly = 0) { workEntryDao.upsert(any()) }
    }

    @Test
    fun `invoke blocks evening check-in for COMP_TIME day`() = runTest {
        val date = LocalDate.now()
        val existing = WorkEntry(
            date = date,
            dayType = DayType.COMP_TIME,
            confirmedWorkDay = true
        )

        coEvery { workEntryDao.getByDate(date) } returns existing

        try {
            useCase(date)
            fail("Expected IllegalStateException")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("COMP_TIME") == true)
        }

        coVerify(exactly = 0) { workEntryDao.upsert(any()) }
    }

    @Test
    fun `invoke uses empty label when existing label is blank`() = runTest {
        val date = LocalDate.now()
        val existing = WorkEntry(
            date = date,
            dayType = DayType.WORK,
            dayLocationLabel = ""
        )

        coEvery { workEntryDao.getByDate(date) } returns existing
        coEvery { workEntryDao.upsert(any()) } returns Unit

        val result = useCase(date)

        assertEquals("", result.dayLocationLabel)
        assertTrue(result.eveningCapturedAt != null)
    }
}
