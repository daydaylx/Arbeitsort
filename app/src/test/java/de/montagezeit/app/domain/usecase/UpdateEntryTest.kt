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
import java.time.LocalTime
import org.junit.Assert.*

class UpdateEntryTest {
    
    private lateinit var workEntryDao: WorkEntryDao
    private lateinit var updateEntry: UpdateEntry
    
    @Before
    fun setup() {
        workEntryDao = mockk()
        updateEntry = UpdateEntry(workEntryDao)
    }
    
    @After
    fun tearDown() {
        unmockkAll()
    }
    
    @Test
    fun `invoke - Aktualisiert Entry und setzt updatedAt`() = runTest {
        // Arrange
        val entry = WorkEntry(
            date = LocalDate.now(),
            dayType = DayType.WORK,
            workStart = LocalTime.of(8, 0),
            workEnd = LocalTime.of(17, 0),
            breakMinutes = 60,
            createdAt = 1000000L,
            updatedAt = 1000000L
        )
        
        coEvery { workEntryDao.upsert(any()) } just Runs
        
        // Act
        val result = updateEntry.invoke(entry)
        
        // Assert
        assertEquals(entry.date, result.date)
        assertEquals(entry.dayType, result.dayType)
        assertEquals(entry.createdAt, result.createdAt)
        assertTrue(result.updatedAt > entry.updatedAt)
        
        coVerify { workEntryDao.upsert(result) }
    }
    
    @Test
    fun `invoke - Behält alle Felder außer updatedAt`() = runTest {
        // Arrange
        val entry = WorkEntry(
            date = LocalDate.now(),
            dayType = DayType.WORK,
            workStart = LocalTime.of(8, 0),
            workEnd = LocalTime.of(17, 0),
            breakMinutes = 60,
            note = "Test Notiz",
            createdAt = 1000000L,
            updatedAt = 1000000L
        )
        
        coEvery { workEntryDao.upsert(any()) } just Runs
        
        // Act
        val result = updateEntry.invoke(entry)
        
        // Assert
        assertEquals(entry.workStart, result.workStart)
        assertEquals(entry.workEnd, result.workEnd)
        assertEquals(entry.breakMinutes, result.breakMinutes)
        assertEquals(entry.note, result.note)
        assertTrue(result.updatedAt > entry.updatedAt)
        
        coVerify { workEntryDao.upsert(result) }
    }
}

