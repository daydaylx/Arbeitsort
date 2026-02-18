package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.DayLocationSource
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.LocationStatus
import de.montagezeit.app.data.local.entity.WorkEntry
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RecordMorningCheckInTest {

    private val workEntryDao = mockk<WorkEntryDao>()
    private val useCase = RecordMorningCheckIn(workEntryDao)

    @Test
    fun `invoke creates new morning snapshot entry`() = runTest {
        val date = LocalDate.now()
        coEvery { workEntryDao.getByDate(date) } returns null
        coEvery { workEntryDao.upsert(any()) } returns Unit

        val result = useCase(date)

        assertEquals(date, result.date)
        assertEquals(DayType.WORK, result.dayType)
        assertNotNull(result.morningCapturedAt)
        assertEquals(LocationStatus.UNAVAILABLE, result.morningLocationStatus)
        assertNull(result.morningLat)
        assertNull(result.morningLon)
        assertNull(result.morningLocationLabel)
        assertNull(result.outsideLeipzigMorning)
        assertEquals("Leipzig", result.dayLocationLabel)
        assertEquals(DayLocationSource.FALLBACK, result.dayLocationSource)
        assertFalse(result.needsReview)

        coVerify(exactly = 1) { workEntryDao.upsert(result) }
    }

    @Test
    fun `invoke preserves manual day location and evening data on existing entry`() = runTest {
        val date = LocalDate.now()
        val existing = WorkEntry(
            date = date,
            dayType = DayType.OFF,
            dayLocationLabel = "Berlin",
            dayLocationSource = DayLocationSource.MANUAL,
            dayLocationLat = 52.52,
            dayLocationLon = 13.40,
            eveningCapturedAt = 7777L,
            eveningLocationStatus = LocationStatus.UNAVAILABLE,
            needsReview = true
        )

        coEvery { workEntryDao.getByDate(date) } returns existing
        coEvery { workEntryDao.upsert(any()) } returns Unit

        val result = useCase(date)

        assertEquals(DayType.WORK, result.dayType)
        assertEquals("Berlin", result.dayLocationLabel)
        assertEquals(DayLocationSource.MANUAL, result.dayLocationSource)
        assertNull(result.dayLocationLat)
        assertNull(result.dayLocationLon)
        assertNotNull(result.morningCapturedAt)
        assertEquals(7777L, result.eveningCapturedAt)
        assertEquals(LocationStatus.UNAVAILABLE, result.eveningLocationStatus)
        assertFalse(result.needsReview)
    }

    @Test
    fun `invoke uses fallback city when existing label is blank and not manual`() = runTest {
        val date = LocalDate.now()
        val existing = WorkEntry(
            date = date,
            dayType = DayType.WORK,
            dayLocationLabel = "   ",
            dayLocationSource = DayLocationSource.FALLBACK
        )

        coEvery { workEntryDao.getByDate(date) } returns existing
        coEvery { workEntryDao.upsert(any()) } returns Unit

        val result = useCase(date)

        assertEquals("Leipzig", result.dayLocationLabel)
        assertEquals(DayLocationSource.FALLBACK, result.dayLocationSource)
        assertTrue(result.morningCapturedAt != null)
    }
}
