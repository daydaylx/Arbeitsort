package de.montagezeit.app.data.repository

import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntry
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.Ordering
import io.mockk.slot
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class RoomWorkEntryRepositoryTest {

    private val workEntryDao = mockk<WorkEntryDao>(relaxed = true)
    private val repository = RoomWorkEntryRepository(workEntryDao)

    @Test
    fun `deleteTravelLegsByDate renormalizes persisted work entry`() = runTest {
        val date = LocalDate.of(2026, 2, 18)
        val existingEntry = WorkEntry(
            date = date,
            dayType = DayType.WORK,
            dayLocationLabel = "Baustelle Nord",
            confirmedWorkDay = true,
            confirmationAt = 1234L,
            confirmationSource = "UI",
            mealIsArrivalDeparture = true,
            mealAllowanceBaseCents = 1400,
            mealAllowanceAmountCents = 820,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        val savedEntry = slot<WorkEntry>()

        coEvery { workEntryDao.getByDate(date) } returns existingEntry
        coEvery { workEntryDao.upsert(capture(savedEntry)) } returns Unit

        repository.deleteTravelLegsByDate(date)

        coVerify(ordering = Ordering.SEQUENCE) {
            workEntryDao.getByDate(date)
            workEntryDao.deleteTravelLegsByDate(date)
            workEntryDao.upsert(any())
        }
        assertFalse(savedEntry.captured.confirmedWorkDay)
        assertEquals(0, savedEntry.captured.mealAllowanceBaseCents)
        assertEquals(0, savedEntry.captured.mealAllowanceAmountCents)
    }
}
