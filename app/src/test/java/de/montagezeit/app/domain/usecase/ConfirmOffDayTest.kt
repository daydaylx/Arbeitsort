package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.preferences.ReminderSettings
import de.montagezeit.app.data.preferences.ReminderSettingsManager
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class ConfirmOffDayTest {

    private lateinit var confirmOffDay: ConfirmOffDay
    private lateinit var workEntryDao: WorkEntryDao
    private lateinit var reminderSettingsManager: ReminderSettingsManager

    @Before
    fun setup() {
        workEntryDao = mockk(relaxed = true)
        reminderSettingsManager = mockk(relaxed = true)
        coEvery { workEntryDao.getByDate(any()) } returns null
        every { reminderSettingsManager.settings } returns flowOf(ReminderSettings())
        confirmOffDay = ConfirmOffDay(workEntryDao, reminderSettingsManager)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `NEIN erzeugt OFF-Entry und confirmedWorkDay true`() = runTest {
        // Arrange
        val date = LocalDate.now()

        // Act
        val result = confirmOffDay(date, source = "TEST")

        // Assert
        assertNotNull(result)
        assertEquals(DayType.OFF, result.dayType)
        assertTrue(result.confirmedWorkDay)
        assertNotNull(result.confirmationAt)
        assertEquals("TEST", result.confirmationSource)
        
        coVerify { workEntryDao.upsert(match { entry ->
            entry.date == date &&
            entry.confirmedWorkDay == true &&
            entry.dayType == DayType.OFF
        }) }
    }

    @Test
    fun `Existing-WORK-Entry wird zu OFF geändert`() = runTest {
        // Arrange
        val date = LocalDate.now()
        val existingEntry = WorkEntry(
            date = date,
            dayType = DayType.WORK,
            workStart = java.time.LocalTime.of(9, 0),
            workEnd = java.time.LocalTime.of(17, 0),
            breakMinutes = 30
        )
        
        coEvery { workEntryDao.getByDate(date) } returns existingEntry

        // Act
        val result = confirmOffDay(date, source = "TEST")

        // Assert
        assertNotNull(result)
        assertEquals(DayType.OFF, result.dayType)
        assertTrue(result.confirmedWorkDay)
        
        coVerify { workEntryDao.upsert(match { entry ->
            entry.dayType == DayType.OFF &&
            entry.confirmedWorkDay == true
        }) }
    }

    @Test
    fun `Existing-OFF-Entry wird nur bestätigt`() = runTest {
        // Arrange
        val date = LocalDate.now()
        val existingEntry = WorkEntry(
            date = date,
            dayType = DayType.OFF
        )
        
        coEvery { workEntryDao.getByDate(date) } returns existingEntry

        // Act
        val result = confirmOffDay(date, source = "TEST")

        // Assert
        assertNotNull(result)
        assertEquals(DayType.OFF, result.dayType)
        assertTrue(result.confirmedWorkDay)
        assertNotNull(result.confirmationAt)
    }
}
