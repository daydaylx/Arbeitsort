package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.TravelLeg
import de.montagezeit.app.data.local.entity.TravelLegCategory
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.local.entity.WorkEntryWithTravelLegs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DeleteDayEntryTest {

    private val workEntryDao = mockk<WorkEntryDao>(relaxed = true)
    private val useCase = DeleteDayEntry(workEntryDao)

    private val date = LocalDate.of(2024, 7, 15)

    @Test
    fun `invoke returns null when no entry exists for date`() = runTest {
        coEvery { workEntryDao.getByDateWithTravel(date) } returns null

        val result = useCase(date)

        assertNull(result)
        coVerify(exactly = 0) { workEntryDao.deleteByDate(any()) }
    }

    @Test
    fun `invoke calls deleteByDate and returns snapshot when entry exists`() = runTest {
        val entry = WorkEntry(date = date, dayType = DayType.WORK)
        coEvery { workEntryDao.getByDateWithTravel(date) } returns WorkEntryWithTravelLegs(
            workEntry = entry,
            travelLegs = emptyList()
        )

        val result = useCase(date)

        coVerify(exactly = 1) { workEntryDao.deleteByDate(date) }
        assertEquals(entry, result?.entry)
    }

    @Test
    fun `invoke snapshot contains correct WorkEntry from database`() = runTest {
        val entry = WorkEntry(date = date, dayType = DayType.WORK, dayLocationLabel = "Frankfurt")
        coEvery { workEntryDao.getByDateWithTravel(date) } returns WorkEntryWithTravelLegs(
            workEntry = entry,
            travelLegs = emptyList()
        )

        val result = useCase(date)

        assertEquals("Frankfurt", result?.entry?.dayLocationLabel)
    }

    @Test
    fun `invoke snapshot contains ordered travel legs from database`() = runTest {
        val entry = WorkEntry(date = date, dayType = DayType.WORK)
        val leg0 = TravelLeg(workEntryDate = date, sortOrder = 0, category = TravelLegCategory.OUTBOUND)
        val leg1 = TravelLeg(workEntryDate = date, sortOrder = 1, category = TravelLegCategory.RETURN)
        coEvery { workEntryDao.getByDateWithTravel(date) } returns WorkEntryWithTravelLegs(
            workEntry = entry,
            travelLegs = listOf(leg1, leg0)
        )

        val result = useCase(date)

        assertEquals(2, result?.travelLegs?.size)
        assertEquals(0, result?.travelLegs?.get(0)?.sortOrder)
        assertEquals(1, result?.travelLegs?.get(1)?.sortOrder)
    }

    @Test
    fun `invoke snapshot contains empty travel legs list when no travel exists`() = runTest {
        val entry = WorkEntry(date = date, dayType = DayType.WORK)
        coEvery { workEntryDao.getByDateWithTravel(date) } returns WorkEntryWithTravelLegs(
            workEntry = entry,
            travelLegs = emptyList()
        )

        val result = useCase(date)

        assertTrue(result?.travelLegs?.isEmpty() == true)
    }
}
