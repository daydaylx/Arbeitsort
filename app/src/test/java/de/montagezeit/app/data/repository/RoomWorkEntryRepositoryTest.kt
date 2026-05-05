package de.montagezeit.app.data.repository

import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.TravelLeg
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.domain.util.ConfirmationSources
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.Ordering
import io.mockk.slot
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomWorkEntryRepositoryTest {

    private val workEntryDao = mockk<WorkEntryDao>(relaxed = true)
    private val repository = RoomWorkEntryRepository(workEntryDao)

    @Test
    fun `upsert normalizes entry with stored travel legs`() = runTest {
        val date = LocalDate.of(2026, 2, 17)
        val entry = unconfirmedWorkEntry(date)
        val savedEntry = slot<WorkEntry>()

        coEvery { workEntryDao.getTravelLegsByDate(date) } returns listOf(travelLeg(date))
        coEvery { workEntryDao.upsert(capture(savedEntry)) } returns Unit

        repository.upsert(entry)

        assertTrue(savedEntry.captured.confirmedWorkDay)
        assertEquals(ConfirmationSources.DERIVED_STATE, savedEntry.captured.confirmationSource)
    }

    @Test
    fun `upsertAllAndDeleteTravelLegs normalizes deleted travel dates as empty travel`() = runTest {
        val date = LocalDate.of(2026, 2, 19)
        val entry = unconfirmedWorkEntry(date)
        val savedEntries = slot<List<WorkEntry>>()

        coEvery {
            workEntryDao.upsertAllAndDeleteTravelLegs(capture(savedEntries), listOf(date))
        } returns Unit

        repository.upsertAllAndDeleteTravelLegs(
            entries = listOf(entry),
            travelLegDatesToDelete = listOf(date)
        )

        assertFalse(savedEntries.captured.single().confirmedWorkDay)
    }

    @Test
    fun `replaceEntryWithTravelLegs normalizes entry with provided legs`() = runTest {
        val date = LocalDate.of(2026, 2, 20)
        val entry = unconfirmedWorkEntry(date)
        val savedEntry = slot<WorkEntry>()
        val legs = listOf(travelLeg(date))

        coEvery { workEntryDao.replaceEntryWithTravelLegs(capture(savedEntry), legs) } returns Unit

        repository.replaceEntryWithTravelLegs(entry, legs)

        assertTrue(savedEntry.captured.confirmedWorkDay)
        assertEquals(ConfirmationSources.DERIVED_STATE, savedEntry.captured.confirmationSource)
    }

    @Test
    fun `readModifyWrite normalizes modified entry before delegating to dao`() = runTest {
        val date = LocalDate.of(2026, 2, 21)
        var savedEntry: WorkEntry? = null

        coEvery { workEntryDao.getTravelLegsByDate(date) } returns listOf(travelLeg(date))
        coEvery { workEntryDao.readModifyWrite(date, any()) } coAnswers {
            val modify = secondArg<(WorkEntry?) -> WorkEntry>()
            savedEntry = modify(null)
        }

        repository.readModifyWrite(date) { unconfirmedWorkEntry(date) }

        assertTrue(requireNotNull(savedEntry).confirmedWorkDay)
        assertEquals(ConfirmationSources.DERIVED_STATE, requireNotNull(savedEntry).confirmationSource)
    }

    @Test
    fun `deleteTravelLegsByDate renormalizes persisted work entry`() = runTest {
        val date = LocalDate.of(2026, 2, 18)
        val existingEntry = WorkEntry(
            date = date,
            dayType = DayType.WORK,
            dayLocationLabel = "Baustelle Nord",
            confirmedWorkDay = true,
            confirmationAt = 1234L,
            confirmationSource = ConfirmationSources.UI,
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

    private fun unconfirmedWorkEntry(date: LocalDate) = WorkEntry(
        date = date,
        dayType = DayType.WORK,
        dayLocationLabel = "Baustelle Nord",
        workStart = LocalTime.of(8, 0),
        workEnd = LocalTime.of(8, 0),
        breakMinutes = 0,
        confirmedWorkDay = false
    )

    // ── Mutex / concurrency ─────────────────────────────────────────────────

    @Test
    fun `concurrent writes to same date are serialized and produce consistent state`() = runTest {
        val date = LocalDate.of(2026, 5, 1)
        val executionOrder = java.util.concurrent.CopyOnWriteArrayList<Int>()

        coEvery { workEntryDao.getTravelLegsByDate(date) } returns emptyList()
        coEvery { workEntryDao.readModifyWrite(date, any()) } coAnswers {
            val modify = secondArg<(WorkEntry?) -> WorkEntry>()
            modify(null)
        }
        coEvery { workEntryDao.upsert(any()) } returns Unit

        // Two operations on the same date dispatched on IO to enable true parallelism
        val jobs = (1..2).map { i ->
            async(Dispatchers.IO) {
                repository.readModifyWrite(date) {
                    executionOrder += i
                    WorkEntry(date = date, dayType = DayType.WORK, dayLocationLabel = "Ort$i")
                }
            }
        }
        jobs.awaitAll()

        // Both operations completed and execution order recorded without data corruption
        assertEquals(2, executionOrder.size)
        assertTrue(executionOrder.containsAll(listOf(1, 2)))
    }

    @Test
    fun `concurrent writes to different dates proceed independently`() = runTest {
        val dates = (1..5).map { LocalDate.of(2026, 5, it) }

        dates.forEach { d ->
            coEvery { workEntryDao.getTravelLegsByDate(d) } returns emptyList()
            coEvery { workEntryDao.upsert(any()) } returns Unit
        }

        val jobs = dates.map { d ->
            async(Dispatchers.IO) {
                repository.upsert(
                    WorkEntry(date = d, dayType = DayType.WORK, dayLocationLabel = "Ort")
                )
            }
        }
        jobs.awaitAll()

        dates.forEach { d -> coVerify { workEntryDao.upsert(match { it.date == d }) } }
    }

    @Test
    fun `LHM eviction over 128 dates does not break repository`() = runTest {
        val dates = (1..140).map { LocalDate.of(2026, 1, 1).plusDays(it.toLong()) }

        dates.forEach { d ->
            coEvery { workEntryDao.getTravelLegsByDate(d) } returns emptyList()
            coEvery { workEntryDao.upsert(any()) } returns Unit
        }

        // Sequential upserts across 140 dates — forces LHM trim after 128
        dates.forEach { d ->
            repository.upsert(WorkEntry(date = d, dayType = DayType.OFF))
        }

        coVerify(exactly = 140) { workEntryDao.upsert(any()) }
    }

    private fun travelLeg(date: LocalDate) = TravelLeg(
        workEntryDate = date,
        sortOrder = 0,
        startAt = 1_700_000_000_000L,
        arriveAt = 1_700_003_600_000L
    )
}
