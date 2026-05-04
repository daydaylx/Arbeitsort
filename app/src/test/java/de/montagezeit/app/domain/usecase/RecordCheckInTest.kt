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

class RecordCheckInTest {

    private val workEntryDao = mockk<WorkEntryDao>(relaxed = true)
    private val useCase = RecordCheckIn(testRepository(workEntryDao))

    @Test
    fun `morning invoke creates new WORK entry with morning timestamp`() = runTest {
        val date = LocalDate.now()
        coEvery { workEntryDao.upsert(any()) } returns Unit
        stubReadModifyWrite(workEntryDao, existingEntry = null)

        val result = useCase(date, CheckInEntryBuilder.Snapshot.MORNING)

        assertEquals(date, result.date)
        assertEquals(DayType.WORK, result.dayType)
        assertNotNull(result.morningCapturedAt)
        assertEquals("", result.dayLocationLabel)

        coVerify(exactly = 1) { workEntryDao.upsert(result) }
    }

    @Test
    fun `evening invoke creates new WORK entry with evening timestamp`() = runTest {
        val date = LocalDate.now()
        coEvery { workEntryDao.upsert(any()) } returns Unit
        stubReadModifyWrite(workEntryDao, existingEntry = null)

        val result = useCase(date, CheckInEntryBuilder.Snapshot.EVENING)

        assertEquals(date, result.date)
        assertEquals(DayType.WORK, result.dayType)
        assertNotNull(result.eveningCapturedAt)
        assertEquals("", result.dayLocationLabel)

        coVerify(exactly = 1) { workEntryDao.upsert(result) }
    }

    @Test
    fun `morning invoke preserves location and evening data on existing WORK entry`() = runTest {
        val date = LocalDate.now()
        val existing = WorkEntry(
            date = date,
            dayType = DayType.WORK,
            dayLocationLabel = "Berlin",
            eveningCapturedAt = 7777L
        )

        coEvery { workEntryDao.upsert(any()) } returns Unit
        stubReadModifyWrite(workEntryDao, existing)

        val result = useCase(date, CheckInEntryBuilder.Snapshot.MORNING)

        assertEquals(DayType.WORK, result.dayType)
        assertEquals("Berlin", result.dayLocationLabel)
        assertNotNull(result.morningCapturedAt)
        assertEquals(7777L, result.eveningCapturedAt)
    }

    @Test
    fun `evening invoke preserves location and morning data on existing WORK entry`() = runTest {
        val date = LocalDate.now()
        val existing = WorkEntry(
            date = date,
            dayType = DayType.WORK,
            dayLocationLabel = "Berlin",
            morningCapturedAt = 1111L
        )

        coEvery { workEntryDao.upsert(any()) } returns Unit
        stubReadModifyWrite(workEntryDao, existing)

        val result = useCase(date, CheckInEntryBuilder.Snapshot.EVENING)

        assertEquals(DayType.WORK, result.dayType)
        assertEquals("Berlin", result.dayLocationLabel)
        assertEquals(1111L, result.morningCapturedAt)
        assertNotNull(result.eveningCapturedAt)
    }

    @Test
    fun `morning invoke blocks check-in for OFF day`() = runTest {
        val date = LocalDate.now()
        stubReadModifyWrite(workEntryDao, WorkEntry(date = date, dayType = DayType.OFF))

        try {
            useCase(date, CheckInEntryBuilder.Snapshot.MORNING)
            fail("Expected IllegalStateException")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("OFF") == true)
        }

        coVerify(exactly = 0) { workEntryDao.upsert(any()) }
    }

    @Test
    fun `evening invoke blocks check-in for OFF day`() = runTest {
        val date = LocalDate.now()
        stubReadModifyWrite(workEntryDao, WorkEntry(date = date, dayType = DayType.OFF))

        try {
            useCase(date, CheckInEntryBuilder.Snapshot.EVENING)
            fail("Expected IllegalStateException")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("OFF") == true)
        }

        coVerify(exactly = 0) { workEntryDao.upsert(any()) }
    }

    @Test
    fun `morning invoke blocks check-in for COMP_TIME day`() = runTest {
        val date = LocalDate.now()
        stubReadModifyWrite(workEntryDao, WorkEntry(date = date, dayType = DayType.COMP_TIME, confirmedWorkDay = true))

        try {
            useCase(date, CheckInEntryBuilder.Snapshot.MORNING)
            fail("Expected IllegalStateException")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("COMP_TIME") == true)
        }

        coVerify(exactly = 0) { workEntryDao.upsert(any()) }
    }

    @Test
    fun `morning invoke uses empty label when existing label is blank`() = runTest {
        val date = LocalDate.now()
        coEvery { workEntryDao.upsert(any()) } returns Unit
        stubReadModifyWrite(workEntryDao, WorkEntry(date = date, dayType = DayType.WORK, dayLocationLabel = "   "))

        val result = useCase(date, CheckInEntryBuilder.Snapshot.MORNING)

        assertEquals("", result.dayLocationLabel)
        assertNotNull(result.morningCapturedAt)
    }

    @Test
    fun `evening invoke uses empty label when existing label is blank`() = runTest {
        val date = LocalDate.now()
        coEvery { workEntryDao.upsert(any()) } returns Unit
        stubReadModifyWrite(workEntryDao, WorkEntry(date = date, dayType = DayType.WORK, dayLocationLabel = ""))

        val result = useCase(date, CheckInEntryBuilder.Snapshot.EVENING)

        assertEquals("", result.dayLocationLabel)
        assertNotNull(result.eveningCapturedAt)
    }
}
