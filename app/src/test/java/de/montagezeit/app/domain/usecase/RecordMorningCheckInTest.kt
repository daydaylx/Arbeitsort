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

class RecordMorningCheckInTest {

    private val workEntryDao = mockk<WorkEntryDao>(relaxed = true)
    private val useCase = RecordMorningCheckIn(testRepository(workEntryDao))

    @Test
    fun `invoke creates new WORK entry with morning timestamp`() = runTest {
        val date = LocalDate.now()
        coEvery { workEntryDao.upsert(any()) } returns Unit
        stubReadModifyWrite(workEntryDao, existingEntry = null)

        val result = useCase(date)

        assertEquals(date, result.date)
        assertEquals(DayType.WORK, result.dayType)
        assertNotNull(result.morningCapturedAt)
        assertEquals("", result.dayLocationLabel)

        coVerify(exactly = 1) { workEntryDao.upsert(result) }
    }

    @Test
    fun `invoke preserves manual day location and evening data on existing WORK entry`() = runTest {
        val date = LocalDate.now()
        val existing = WorkEntry(
            date = date,
            dayType = DayType.WORK,
            dayLocationLabel = "Berlin",
            eveningCapturedAt = 7777L
        )

        coEvery { workEntryDao.upsert(any()) } returns Unit
        stubReadModifyWrite(workEntryDao, existing)

        val result = useCase(date)

        assertEquals(DayType.WORK, result.dayType)
        assertEquals("Berlin", result.dayLocationLabel)
        assertNotNull(result.morningCapturedAt)
        assertEquals(7777L, result.eveningCapturedAt)
    }

    @Test
    fun `invoke blocks morning check-in for OFF day`() = runTest {
        val date = LocalDate.now()
        val existing = WorkEntry(
            date = date,
            dayType = DayType.OFF
        )

        stubReadModifyWrite(workEntryDao, existing)

        try {
            useCase(date)
            fail("Expected IllegalStateException")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("OFF") == true)
        }

        coVerify(exactly = 0) { workEntryDao.upsert(any()) }
    }

    @Test
    fun `invoke blocks morning check-in for COMP_TIME day`() = runTest {
        val date = LocalDate.now()
        val existing = WorkEntry(
            date = date,
            dayType = DayType.COMP_TIME,
            confirmedWorkDay = true
        )

        stubReadModifyWrite(workEntryDao, existing)

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
            dayLocationLabel = "   "
        )

        coEvery { workEntryDao.upsert(any()) } returns Unit
        stubReadModifyWrite(workEntryDao, existing)

        val result = useCase(date)

        assertEquals("", result.dayLocationLabel)
        assertTrue(result.morningCapturedAt != null)
    }
}
