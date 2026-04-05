package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.TravelLeg
import de.montagezeit.app.data.local.entity.TravelLegCategory
import de.montagezeit.app.data.local.entity.TravelSource
import de.montagezeit.app.data.local.entity.WorkEntry
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import org.junit.Assert.*

class SetTravelEventTest {

    private lateinit var workEntryDao: WorkEntryDao
    private lateinit var setTravelEvent: SetTravelEvent

    @Before
    fun setup() {
        workEntryDao = mockk(relaxed = true)
        setTravelEvent = SetTravelEvent(testRepository(workEntryDao))
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `invoke - START - Neuer Eintrag - Erstellt TravelLeg mit startAt`() = runTest {
        val date = LocalDate.now()
        val timestamp = 1000000L
        val label = "Dresden"
        coEvery { workEntryDao.getByDate(date) } returns null
        coEvery { workEntryDao.getTravelLegsByDate(date) } returns emptyList()

        setTravelEvent.invoke(date, SetTravelEvent.TravelType.START, timestamp, label)

        coVerify {
            workEntryDao.replaceEntryWithTravelLegs(
                any(),
                match { legs ->
                    legs.size == 1 &&
                    legs[0].startAt == timestamp &&
                    legs[0].startLabel == label &&
                    legs[0].source == TravelSource.MANUAL
                }
            )
        }
    }

    @Test
    fun `invoke - START - Erstellt WorkEntry wenn keiner existiert`() = runTest {
        val date = LocalDate.now()
        val timestamp = 1000000L
        coEvery { workEntryDao.getByDate(date) } returns null
        coEvery { workEntryDao.getTravelLegsByDate(date) } returns emptyList()

        val result = setTravelEvent.invoke(date, SetTravelEvent.TravelType.START, timestamp, null)

        assertEquals(date, result.date)
        assertNotNull(result.createdAt)
    }

    @Test
    fun `invoke - START - Existierender Eintrag - Erhöht updatedAt`() = runTest {
        val date = LocalDate.now()
        val existingEntry = WorkEntry(
            date = date,
            dayType = DayType.WORK,
            createdAt = 1000000L,
            updatedAt = 1000000L
        )
        val timestamp = 2000000L
        coEvery { workEntryDao.getByDate(date) } returns existingEntry
        coEvery { workEntryDao.getTravelLegsByDate(date) } returns emptyList()

        val result = setTravelEvent.invoke(date, SetTravelEvent.TravelType.START, timestamp, null)

        assertEquals(existingEntry.createdAt, result.createdAt)
        assertTrue(result.updatedAt > existingEntry.updatedAt)
    }

    @Test
    fun `invoke - ARRIVE - Erstellt TravelLeg mit arriveAt`() = runTest {
        val date = LocalDate.now()
        val timestamp = 2000000L
        val label = "Berlin"
        coEvery { workEntryDao.getByDate(date) } returns null
        coEvery { workEntryDao.getTravelLegsByDate(date) } returns emptyList()

        setTravelEvent.invoke(date, SetTravelEvent.TravelType.ARRIVE, timestamp, label)

        coVerify {
            workEntryDao.replaceEntryWithTravelLegs(
                any(),
                match { legs ->
                    legs.size == 1 &&
                    legs[0].arriveAt == timestamp &&
                    legs[0].endLabel == label &&
                    legs[0].source == TravelSource.MANUAL
                }
            )
        }
    }

    @Test
    fun `invoke - ARRIVE - Behält vorhandene startAt eines existierenden Legs`() = runTest {
        val date = LocalDate.now()
        val existingLeg = TravelLeg(
            workEntryDate = date,
            sortOrder = 0,
            category = TravelLegCategory.OUTBOUND,
            startAt = 1000000L,
            startLabel = "Dresden",
            source = TravelSource.MANUAL
        )
        val timestamp = 2000000L
        coEvery { workEntryDao.getByDate(date) } returns WorkEntry(date = date)
        coEvery { workEntryDao.getTravelLegsByDate(date) } returns listOf(existingLeg)

        setTravelEvent.invoke(date, SetTravelEvent.TravelType.ARRIVE, timestamp, "Berlin")

        coVerify {
            workEntryDao.replaceEntryWithTravelLegs(
                any(),
                match { legs ->
                    legs.size == 1 &&
                    legs[0].startAt == existingLeg.startAt &&
                    legs[0].arriveAt == timestamp
                }
            )
        }
    }

    @Test
    fun `invoke - Neues Leg bekommt OUTBOUND als Kategorie`() = runTest {
        val date = LocalDate.now()
        coEvery { workEntryDao.getByDate(date) } returns null
        coEvery { workEntryDao.getTravelLegsByDate(date) } returns emptyList()

        setTravelEvent.invoke(date, SetTravelEvent.TravelType.START, 1000000L, null)

        coVerify {
            workEntryDao.replaceEntryWithTravelLegs(
                any(),
                match { legs -> legs[0].category == TravelLegCategory.OUTBOUND }
            )
        }
    }

    @Test
    fun `clearTravelEvents - Loescht alle TravelLegs`() = runTest {
        val date = LocalDate.now()
        val existingEntry = WorkEntry(date = date, dayType = DayType.WORK, createdAt = 1000000L, updatedAt = 1000000L)
        coEvery { workEntryDao.getByDate(date) } returns existingEntry

        val result = setTravelEvent.clearTravelEvents(date)

        assertEquals(existingEntry.dayType, result.dayType)
        assertTrue(result.updatedAt > existingEntry.updatedAt)
        coVerify { workEntryDao.replaceEntryWithTravelLegs(result, emptyList()) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `clearTravelEvents - Nicht existierender Eintrag - Wirft Exception`() = runTest {
        val date = LocalDate.now()
        coEvery { workEntryDao.getByDate(date) } returns null

        setTravelEvent.clearTravelEvents(date)
    }
}
