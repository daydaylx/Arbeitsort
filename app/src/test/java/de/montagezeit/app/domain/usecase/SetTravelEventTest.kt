package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.DayType
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
        workEntryDao = mockk()
        setTravelEvent = SetTravelEvent(workEntryDao)
    }
    
    @After
    fun tearDown() {
        unmockkAll()
    }
    
    @Test
    fun `invoke - START - Neuer Eintrag - Erstellt Entry mit travelStartAt`() = runTest {
        // Arrange
        val date = LocalDate.now()
        val timestamp = 1000000L
        val label = "Dresden"
        
        coEvery { workEntryDao.getByDate(date) } returns null
        coEvery { workEntryDao.upsert(any()) } just Runs
        
        // Act
        val result = setTravelEvent.invoke(date, SetTravelEvent.TravelType.START, timestamp, label)
        
        // Assert
        assertEquals(date, result.date)
        assertEquals(timestamp, result.travelStartAt)
        assertEquals(label, result.travelLabelStart)
        assertNull(result.travelArriveAt)
        assertNull(result.travelLabelEnd)
        assertNotNull(result.createdAt)
        
        coVerify { workEntryDao.upsert(result) }
    }
    
    @Test
    fun `invoke - START - Existierender Eintrag - Aktualisiert travelStartAt`() = runTest {
        // Arrange
        val date = LocalDate.now()
        val existingEntry = WorkEntry(
            date = date,
            dayType = DayType.WORK,
            createdAt = 1000000L,
            updatedAt = 1000000L
        )
        val timestamp = 2000000L
        val label = "Dresden"
        
        coEvery { workEntryDao.getByDate(date) } returns existingEntry
        coEvery { workEntryDao.upsert(any()) } just Runs
        
        // Act
        val result = setTravelEvent.invoke(date, SetTravelEvent.TravelType.START, timestamp, label)
        
        // Assert
        assertEquals(timestamp, result.travelStartAt)
        assertEquals(label, result.travelLabelStart)
        assertEquals(existingEntry.createdAt, result.createdAt)
        assertTrue(result.updatedAt > existingEntry.updatedAt)
        
        coVerify { workEntryDao.upsert(result) }
    }
    
    @Test
    fun `invoke - ARRIVE - Neuer Eintrag - Erstellt Entry mit travelArriveAt`() = runTest {
        // Arrange
        val date = LocalDate.now()
        val timestamp = 2000000L
        val label = "Dresden"
        
        coEvery { workEntryDao.getByDate(date) } returns null
        coEvery { workEntryDao.upsert(any()) } just Runs
        
        // Act
        val result = setTravelEvent.invoke(date, SetTravelEvent.TravelType.ARRIVE, timestamp, label)
        
        // Assert
        assertEquals(date, result.date)
        assertEquals(timestamp, result.travelArriveAt)
        assertEquals(label, result.travelLabelEnd)
        assertNull(result.travelStartAt)
        assertNull(result.travelLabelStart)
        
        coVerify { workEntryDao.upsert(result) }
    }
    
    @Test
    fun `invoke - ARRIVE - Existierender Eintrag - Aktualisiert travelArriveAt`() = runTest {
        // Arrange
        val date = LocalDate.now()
        val existingEntry = WorkEntry(
            date = date,
            dayType = DayType.WORK,
            travelStartAt = 1000000L,
            travelLabelStart = "Dresden",
            createdAt = 1000000L,
            updatedAt = 1000000L
        )
        val timestamp = 2000000L
        val label = "Dresden"
        
        coEvery { workEntryDao.getByDate(date) } returns existingEntry
        coEvery { workEntryDao.upsert(any()) } just Runs
        
        // Act
        val result = setTravelEvent.invoke(date, SetTravelEvent.TravelType.ARRIVE, timestamp, label)
        
        // Assert
        assertEquals(timestamp, result.travelArriveAt)
        assertEquals(label, result.travelLabelEnd)
        assertEquals(existingEntry.travelStartAt, result.travelStartAt) // Behält Start
        assertEquals(existingEntry.travelLabelStart, result.travelLabelStart) // Behält Start Label
        
        coVerify { workEntryDao.upsert(result) }
    }
    
    @Test
    fun `invoke - DEPARTURE - Neuer Eintrag - Erstellt Entry mit travelArriveAt`() = runTest {
        // Arrange
        val date = LocalDate.now()
        val timestamp = 3000000L
        val label = "Dresden"

        coEvery { workEntryDao.getByDate(date) } returns null
        coEvery { workEntryDao.upsert(any()) } just Runs

        // Act
        val result = setTravelEvent.invoke(date, SetTravelEvent.TravelType.DEPARTURE, timestamp, label)

        // Assert
        assertEquals(timestamp, result.travelArriveAt)
        assertEquals(label, result.travelLabelEnd)

        coVerify { workEntryDao.upsert(result) }
    }
    
    @Test
    fun `invoke - Ohne Label - Setzt null für Label`() = runTest {
        // Arrange
        val date = LocalDate.now()
        val timestamp = 1000000L
        
        coEvery { workEntryDao.getByDate(date) } returns null
        coEvery { workEntryDao.upsert(any()) } just Runs
        
        // Act
        val result = setTravelEvent.invoke(date, SetTravelEvent.TravelType.START, timestamp, null)
        
        // Assert
        assertEquals(timestamp, result.travelStartAt)
        assertNull(result.travelLabelStart)
        
        coVerify { workEntryDao.upsert(result) }
    }
    
    @Test
    fun `clearTravelEvents - Löscht alle Travel-Informationen`() = runTest {
        // Arrange
        val date = LocalDate.now()
        val existingEntry = WorkEntry(
            date = date,
            dayType = DayType.WORK,
            travelStartAt = 1000000L,
            travelArriveAt = 2000000L,
            travelLabelStart = "Dresden",
            travelLabelEnd = "Berlin",
            createdAt = 1000000L,
            updatedAt = 1000000L
        )
        
        coEvery { workEntryDao.getByDate(date) } returns existingEntry
        coEvery { workEntryDao.upsert(any()) } just Runs
        
        // Act
        val result = setTravelEvent.clearTravelEvents(date)
        
        // Assert
        assertNull(result.travelStartAt)
        assertNull(result.travelArriveAt)
        assertNull(result.travelLabelStart)
        assertNull(result.travelLabelEnd)
        assertEquals(existingEntry.dayType, result.dayType)
        assertTrue(result.updatedAt > existingEntry.updatedAt)
        
        coVerify { workEntryDao.upsert(result) }
    }
    
    @Test(expected = IllegalArgumentException::class)
    fun `clearTravelEvents - Nicht existierender Eintrag - Wirft Exception`() = runTest {
        // Arrange
        val date = LocalDate.now()
        
        coEvery { workEntryDao.getByDate(date) } returns null
        
        // Act
        setTravelEvent.clearTravelEvents(date)
    }
}
