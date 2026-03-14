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

class SetDayTypeTest {
    
    private lateinit var workEntryDao: WorkEntryDao
    private lateinit var setDayType: SetDayType
    
    @Before
    fun setup() {
        workEntryDao = mockk()
        setDayType = SetDayType(workEntryDao)
    }
    
    @After
    fun tearDown() {
        unmockkAll()
    }
    
    @Test
    fun `invoke - Neuer Eintrag - Erstellt WorkEntry mit DayType`() = runTest {
        // Arrange
        val date = LocalDate.now()
        val dayType = DayType.WORK
        
        coEvery { workEntryDao.getByDate(date) } returns null
        coEvery { workEntryDao.upsert(any()) } just Runs
        
        // Act
        val result = setDayType.invoke(date, dayType)
        
        // Assert
        assertEquals(date, result.date)
        assertEquals(dayType, result.dayType)
        assertNotNull(result.createdAt)
        assertNotNull(result.updatedAt)
        assertEquals(result.createdAt, result.updatedAt)
        
        coVerify { workEntryDao.upsert(result) }
    }
    
    @Test
    fun `invoke - Existierender Eintrag - Aktualisiert DayType`() = runTest {
        // Arrange
        val date = LocalDate.now()
        val existingEntry = WorkEntry(
            date = date,
            dayType = DayType.WORK,
            createdAt = 1000000L,
            updatedAt = 1000000L
        )
        val newDayType = DayType.OFF
        
        coEvery { workEntryDao.getByDate(date) } returns existingEntry
        coEvery { workEntryDao.upsert(any()) } just Runs
        
        // Act
        val result = setDayType.invoke(date, newDayType)
        
        // Assert
        assertEquals(date, result.date)
        assertEquals(newDayType, result.dayType)
        assertEquals(existingEntry.createdAt, result.createdAt)
        assertTrue(result.updatedAt > existingEntry.updatedAt)
        
        coVerify { workEntryDao.upsert(result) }
    }
    
    @Test
    fun `invoke - WORK zu OFF - Aktualisiert korrekt`() = runTest {
        // Arrange
        val date = LocalDate.now()
        val existingEntry = WorkEntry(
            date = date,
            dayType = DayType.WORK,
            mealIsArrivalDeparture = true,
            mealBreakfastIncluded = true,
            mealAllowanceBaseCents = 1400,
            mealAllowanceAmountCents = 820,
            createdAt = 1000000L,
            updatedAt = 1000000L
        )
        
        coEvery { workEntryDao.getByDate(date) } returns existingEntry
        coEvery { workEntryDao.upsert(any()) } just Runs
        
        // Act
        val result = setDayType.invoke(date, DayType.OFF)
        
        // Assert
        assertEquals(DayType.OFF, result.dayType)
        assertFalse(result.mealIsArrivalDeparture)
        assertFalse(result.mealBreakfastIncluded)
        assertEquals(0, result.mealAllowanceBaseCents)
        assertEquals(0, result.mealAllowanceAmountCents)
        
        coVerify { workEntryDao.upsert(result) }
    }
    
    @Test
    fun `invoke - OFF zu WORK - Aktualisiert korrekt`() = runTest {
        // Arrange
        val date = LocalDate.now()
        val existingEntry = WorkEntry(
            date = date,
            dayType = DayType.OFF,
            mealIsArrivalDeparture = true,
            mealBreakfastIncluded = true,
            mealAllowanceBaseCents = 1400,
            mealAllowanceAmountCents = 820,
            createdAt = 1000000L,
            updatedAt = 1000000L
        )

        coEvery { workEntryDao.getByDate(date) } returns existingEntry
        coEvery { workEntryDao.upsert(any()) } just Runs

        // Act
        val result = setDayType.invoke(date, DayType.WORK)

        // Assert
        assertEquals(DayType.WORK, result.dayType)
        assertFalse(result.mealIsArrivalDeparture)
        assertFalse(result.mealBreakfastIncluded)
        assertEquals(0, result.mealAllowanceBaseCents)
        assertEquals(0, result.mealAllowanceAmountCents)

        coVerify { workEntryDao.upsert(result) }
    }

    @Test
    fun `invoke - WORK zu COMP_TIME - Setzt confirmedWorkDay und confirmationSource`() = runTest {
        val date = LocalDate.now()
        val existingEntry = WorkEntry(
            date = date,
            dayType = DayType.WORK,
            confirmedWorkDay = false,
            travelStartAt = 1_000L,
            travelArriveAt = 2_000L,
            travelLabelStart = "Start",
            travelLabelEnd = "Ziel",
            travelFromLabel = "A",
            travelToLabel = "B",
            travelDistanceKm = 42.0,
            travelPaidMinutes = 30,
            mealIsArrivalDeparture = true,
            mealBreakfastIncluded = true,
            mealAllowanceBaseCents = 1400,
            mealAllowanceAmountCents = 820,
            createdAt = 1000000L,
            updatedAt = 1000000L
        )
        coEvery { workEntryDao.getByDate(date) } returns existingEntry
        coEvery { workEntryDao.upsert(any()) } just Runs

        val result = setDayType.invoke(date, DayType.COMP_TIME)

        assertEquals(DayType.COMP_TIME, result.dayType)
        assertTrue(result.confirmedWorkDay)
        assertEquals(DayType.COMP_TIME.name, result.confirmationSource)
        assertNotNull(result.confirmationAt)
        assertNull(result.travelStartAt)
        assertNull(result.travelArriveAt)
        assertNull(result.travelLabelStart)
        assertNull(result.travelLabelEnd)
        assertNull(result.travelFromLabel)
        assertNull(result.travelToLabel)
        assertNull(result.travelDistanceKm)
        assertNull(result.travelPaidMinutes)
        assertFalse(result.mealIsArrivalDeparture)
        assertFalse(result.mealBreakfastIncluded)
        assertEquals(0, result.mealAllowanceBaseCents)
        assertEquals(0, result.mealAllowanceAmountCents)
        coVerify { workEntryDao.upsert(result) }
    }

    @Test
    fun `invoke - OFF zu COMP_TIME - Setzt confirmedWorkDay und confirmationSource`() = runTest {
        val date = LocalDate.now()
        val existingEntry = WorkEntry(
            date = date,
            dayType = DayType.OFF,
            confirmedWorkDay = false,
            createdAt = 1000000L,
            updatedAt = 1000000L
        )
        coEvery { workEntryDao.getByDate(date) } returns existingEntry
        coEvery { workEntryDao.upsert(any()) } just Runs

        val result = setDayType.invoke(date, DayType.COMP_TIME)

        assertEquals(DayType.COMP_TIME, result.dayType)
        assertTrue(result.confirmedWorkDay)
        assertEquals(DayType.COMP_TIME.name, result.confirmationSource)
        assertNotNull(result.confirmationAt)
        coVerify { workEntryDao.upsert(result) }
    }

    @Test
    fun `invoke - COMP_TIME zu WORK - Loescht confirmedWorkDay und confirmationSource`() = runTest {
        val date = LocalDate.now()
        val existingEntry = WorkEntry(
            date = date,
            dayType = DayType.COMP_TIME,
            confirmedWorkDay = true,
            confirmationAt = 9000000L,
            confirmationSource = DayType.COMP_TIME.name,
            createdAt = 1000000L,
            updatedAt = 1000000L
        )
        coEvery { workEntryDao.getByDate(date) } returns existingEntry
        coEvery { workEntryDao.upsert(any()) } just Runs

        val result = setDayType.invoke(date, DayType.WORK)

        assertEquals(DayType.WORK, result.dayType)
        assertFalse(result.confirmedWorkDay)
        assertNull(result.confirmationAt)
        assertNull(result.confirmationSource)
        coVerify { workEntryDao.upsert(result) }
    }

    @Test
    fun `invoke - COMP_TIME zu OFF - Loescht confirmedWorkDay und confirmationSource`() = runTest {
        val date = LocalDate.now()
        val existingEntry = WorkEntry(
            date = date,
            dayType = DayType.COMP_TIME,
            confirmedWorkDay = true,
            confirmationAt = 9000000L,
            confirmationSource = DayType.COMP_TIME.name,
            createdAt = 1000000L,
            updatedAt = 1000000L
        )
        coEvery { workEntryDao.getByDate(date) } returns existingEntry
        coEvery { workEntryDao.upsert(any()) } just Runs

        val result = setDayType.invoke(date, DayType.OFF)

        assertEquals(DayType.OFF, result.dayType)
        assertFalse(result.confirmedWorkDay)
        assertNull(result.confirmationAt)
        assertNull(result.confirmationSource)
        coVerify { workEntryDao.upsert(result) }
    }
}
