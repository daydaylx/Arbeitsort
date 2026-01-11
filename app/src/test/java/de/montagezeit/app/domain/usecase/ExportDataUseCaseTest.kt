package de.montagezeit.app.domain.usecase

import android.net.Uri
import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.export.CsvExporter
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.*

class ExportDataUseCaseTest {
    
    private lateinit var workEntryDao: WorkEntryDao
    private lateinit var csvExporter: CsvExporter
    private lateinit var exportDataUseCase: ExportDataUseCase
    
    @Before
    fun setup() {
        workEntryDao = mockk()
        csvExporter = mockk()
        exportDataUseCase = ExportDataUseCase(workEntryDao, csvExporter)
    }
    
    @After
    fun tearDown() {
        unmockkAll()
    }
    
    @Test
    fun `invoke - Mit Datum-Range - Exportiert Einträge im Zeitraum`() = runTest {
        // Arrange
        val startDate = LocalDate.of(2024, 1, 1)
        val endDate = LocalDate.of(2024, 1, 31)
        val entries = listOf(
            createTestEntry(LocalDate.of(2024, 1, 15)),
            createTestEntry(LocalDate.of(2024, 1, 20))
        )
        val expectedUri = mockk<Uri>()
        
        coEvery { workEntryDao.getByDateRange(startDate, endDate) } returns entries
        every { csvExporter.exportToCsv(entries) } returns expectedUri
        
        // Act
        val result = exportDataUseCase.invoke(startDate, endDate)
        
        // Assert
        assertNotNull(result)
        assertEquals(expectedUri, result)
        
        coVerify { workEntryDao.getByDateRange(startDate, endDate) }
        verify { csvExporter.exportToCsv(entries) }
    }
    
    @Test
    fun `invoke - Ohne Parameter - Exportiert alle Einträge bis heute`() = runTest {
        // Arrange
        val today = LocalDate.now()
        val startDate = LocalDate.of(2020, 1, 1)
        val entries = listOf(
            createTestEntry(LocalDate.of(2024, 1, 15)),
            createTestEntry(today)
        )
        val expectedUri = mockk<Uri>()
        
        coEvery { workEntryDao.getByDateRange(startDate, today) } returns entries
        every { csvExporter.exportToCsv(entries) } returns expectedUri
        
        // Act
        val result = exportDataUseCase.invoke()
        
        // Assert
        assertNotNull(result)
        assertEquals(expectedUri, result)
        
        coVerify { workEntryDao.getByDateRange(startDate, today) }
        verify { csvExporter.exportToCsv(entries) }
    }
    
    @Test
    fun `invoke - Leere Liste - Exportiert trotzdem`() = runTest {
        // Arrange
        val startDate = LocalDate.of(2024, 1, 1)
        val endDate = LocalDate.of(2024, 1, 31)
        val entries = emptyList<WorkEntry>()
        val expectedUri = mockk<Uri>()
        
        coEvery { workEntryDao.getByDateRange(startDate, endDate) } returns entries
        every { csvExporter.exportToCsv(entries) } returns expectedUri
        
        // Act
        val result = exportDataUseCase.invoke(startDate, endDate)
        
        // Assert
        assertNotNull(result)
        assertEquals(expectedUri, result)
        
        coVerify { workEntryDao.getByDateRange(startDate, endDate) }
        verify { csvExporter.exportToCsv(entries) }
    }
    
    @Test
    fun `invoke - Export fehlgeschlagen - Gibt null zurück`() = runTest {
        // Arrange
        val startDate = LocalDate.of(2024, 1, 1)
        val endDate = LocalDate.of(2024, 1, 31)
        val entries = listOf(createTestEntry(LocalDate.of(2024, 1, 15)))
        
        coEvery { workEntryDao.getByDateRange(startDate, endDate) } returns entries
        every { csvExporter.exportToCsv(entries) } returns null
        
        // Act
        val result = exportDataUseCase.invoke(startDate, endDate)
        
        // Assert
        assertNull(result)
        
        coVerify { workEntryDao.getByDateRange(startDate, endDate) }
        verify { csvExporter.exportToCsv(entries) }
    }
    
    @Test
    fun `invoke - Einzelner Tag - Exportiert Eintrag für diesen Tag`() = runTest {
        // Arrange
        val date = LocalDate.of(2024, 1, 15)
        val entries = listOf(createTestEntry(date))
        val expectedUri = mockk<Uri>()
        
        coEvery { workEntryDao.getByDateRange(date, date) } returns entries
        every { csvExporter.exportToCsv(entries) } returns expectedUri
        
        // Act
        val result = exportDataUseCase.invoke(date, date)
        
        // Assert
        assertNotNull(result)
        assertEquals(expectedUri, result)
        
        coVerify { workEntryDao.getByDateRange(date, date) }
        verify { csvExporter.exportToCsv(entries) }
    }
    
    private fun createTestEntry(date: LocalDate): WorkEntry {
        return WorkEntry(
            date = date,
            dayType = DayType.WORK,
            workStart = LocalTime.of(8, 0),
            workEnd = LocalTime.of(17, 0),
            breakMinutes = 60,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }
}

