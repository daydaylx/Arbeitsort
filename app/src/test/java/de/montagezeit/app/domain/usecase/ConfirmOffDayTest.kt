package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.DayLocationSource
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.preferences.ReminderSettings
import de.montagezeit.app.data.preferences.ReminderSettingsManager
import de.montagezeit.app.domain.util.TimeCalculator
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
        assertEquals(0, TimeCalculator.calculateWorkMinutes(result))
        assertEquals(0, TimeCalculator.calculateTravelMinutes(result))
        assertEquals(0, result.travelPaidMinutes)
        
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
        val morningCapturedAt = System.currentTimeMillis() - 2_000_000
        val existingEntry = WorkEntry(
            date = date,
            dayType = DayType.WORK,
            workStart = java.time.LocalTime.of(9, 0),
            workEnd = java.time.LocalTime.of(17, 0),
            breakMinutes = 30,
            morningCapturedAt = morningCapturedAt,
            morningLocationLabel = "Leipzig",
            morningLat = 51.34,
            morningLon = 12.37,
            travelStartAt = System.currentTimeMillis() - 3_600_000,
            travelArriveAt = System.currentTimeMillis(),
            travelPaidMinutes = 45,
            travelDistanceKm = 12.3
        )
        
        coEvery { workEntryDao.getByDate(date) } returns existingEntry

        // Act
        val result = confirmOffDay(date, source = "TEST")

        // Assert
        assertNotNull(result)
        assertEquals(DayType.OFF, result.dayType)
        assertTrue(result.confirmedWorkDay)
        assertEquals(0, TimeCalculator.calculateWorkMinutes(result))
        assertEquals(0, TimeCalculator.calculateTravelMinutes(result))
        assertEquals(0, result.travelPaidMinutes)
        assertEquals(morningCapturedAt, result.morningCapturedAt)
        assertEquals("Leipzig", result.morningLocationLabel)
        assertEquals(51.34, result.morningLat)
        assertEquals(12.37, result.morningLon)
        
        coVerify { workEntryDao.upsert(match { entry ->
            entry.dayType == DayType.OFF &&
            entry.confirmedWorkDay == true &&
            entry.travelPaidMinutes == 0
        }) }
    }

    @Test
    fun `Existing-OFF-Entry wird nur bestätigt`() = runTest {
        // Arrange
        val date = LocalDate.now()
        val existingEntry = WorkEntry(
            date = date,
            dayType = DayType.OFF,
            travelPaidMinutes = 25
        )
        
        coEvery { workEntryDao.getByDate(date) } returns existingEntry

        // Act
        val result = confirmOffDay(date, source = "TEST")

        // Assert
        assertNotNull(result)
        assertEquals(DayType.OFF, result.dayType)
        assertTrue(result.confirmedWorkDay)
        assertNotNull(result.confirmationAt)
        assertEquals(0, TimeCalculator.calculateWorkMinutes(result))
        assertEquals(0, TimeCalculator.calculateTravelMinutes(result))
        assertEquals(0, result.travelPaidMinutes)
    }

    @Test
    fun `manual day location bleibt bei OFF confirmation erhalten`() = runTest {
        val date = LocalDate.now()
        val existingEntry = WorkEntry(
            date = date,
            dayType = DayType.WORK,
            dayLocationLabel = "Berlin",
            dayLocationSource = DayLocationSource.MANUAL
        )
        coEvery { workEntryDao.getByDate(date) } returns existingEntry

        val result = confirmOffDay(date, source = "MANUAL_TEST")

        assertEquals(DayType.OFF, result.dayType)
        assertEquals("Berlin", result.dayLocationLabel)
        assertEquals(DayLocationSource.MANUAL, result.dayLocationSource)
        assertEquals("MANUAL_TEST", result.confirmationSource)
    }

    @Test
    fun `leeres fallback location setting verwendet Leipzig`() = runTest {
        val date = LocalDate.now()
        every { reminderSettingsManager.settings } returns flowOf(ReminderSettings(defaultDayLocationLabel = ""))

        val result = confirmOffDay(date, source = "TEST")

        assertEquals(DayType.OFF, result.dayType)
        assertEquals("Leipzig", result.dayLocationLabel)
        assertEquals(DayLocationSource.FALLBACK, result.dayLocationSource)
    }
}
