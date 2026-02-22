package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.DayLocationSource
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntry
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ConfirmOffDayTest {

    private val workEntryDao = mockk<WorkEntryDao>()
    private val useCase = ConfirmOffDay(workEntryDao)

    @Test
    fun `invoke creates OFF entry with confirmation data when no entry exists`() = runTest {
        val date = LocalDate.now()
        coEvery { workEntryDao.getByDate(date) } returns null
        coEvery { workEntryDao.upsert(any()) } returns Unit

        val result = useCase(date, source = "TEST")

        assertEquals(DayType.OFF, result.dayType)
        assertEquals("", result.dayLocationLabel)
        assertEquals(DayLocationSource.FALLBACK, result.dayLocationSource)
        assertEquals(0, result.travelPaidMinutes)
        assertEquals(true, result.confirmedWorkDay)
        assertNotNull(result.confirmationAt)
        assertEquals("TEST", result.confirmationSource)

        coVerify(exactly = 1) { workEntryDao.upsert(result) }
    }

    @Test
    fun `invoke converts existing work entry to OFF and keeps manual day location`() = runTest {
        val date = LocalDate.now()
        val existing = WorkEntry(
            date = date,
            dayType = DayType.WORK,
            dayLocationLabel = "Berlin",
            dayLocationSource = DayLocationSource.MANUAL,
            travelStartAt = 1000L,
            travelArriveAt = 2000L,
            travelFromLabel = "A",
            travelToLabel = "B",
            travelDistanceKm = 12.3,
            travelPaidMinutes = 45
        )

        coEvery { workEntryDao.getByDate(date) } returns existing
        coEvery { workEntryDao.upsert(any()) } returns Unit

        val result = useCase(date, source = "UI")

        assertEquals(DayType.OFF, result.dayType)
        assertEquals("Berlin", result.dayLocationLabel)
        assertEquals(DayLocationSource.MANUAL, result.dayLocationSource)
        assertNull(result.travelStartAt)
        assertNull(result.travelArriveAt)
        assertNull(result.travelFromLabel)
        assertNull(result.travelToLabel)
        assertNull(result.travelDistanceKm)
        assertEquals(0, result.travelPaidMinutes)
        assertTrue(result.confirmedWorkDay)
        assertEquals("UI", result.confirmationSource)
    }

    @Test
    fun `invoke keeps day location blank when existing day location label is blank`() = runTest {
        val date = LocalDate.now()
        val existing = WorkEntry(
            date = date,
            dayType = DayType.WORK,
            dayLocationLabel = "",
            dayLocationSource = DayLocationSource.GPS
        )

        coEvery { workEntryDao.getByDate(date) } returns existing
        coEvery { workEntryDao.upsert(any()) } returns Unit

        val result = useCase(date)

        assertEquals(DayType.OFF, result.dayType)
        assertEquals("", result.dayLocationLabel)
        assertEquals(DayLocationSource.GPS, result.dayLocationSource)
    }
}
