package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.LocationStatus
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.location.LocationProvider
import de.montagezeit.app.data.preferences.ReminderSettings
import de.montagezeit.app.data.preferences.ReminderSettingsManager
import de.montagezeit.app.domain.location.LocationCalculator
import de.montagezeit.app.domain.location.LocationCheckResult
import de.montagezeit.app.domain.model.LocationResult
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse

class RecordMorningCheckInTest {
    
    private lateinit var workEntryDao: WorkEntryDao
    private lateinit var locationProvider: LocationProvider
    private lateinit var locationCalculator: LocationCalculator
    private lateinit var reminderSettingsManager: ReminderSettingsManager
    private lateinit var recordMorningCheckIn: RecordMorningCheckIn
    
    @Before
    fun setup() {
        workEntryDao = mockk()
        locationProvider = mockk()
        locationCalculator = mockk()
        reminderSettingsManager = mockk()
        
        // Default settings mock
        every { reminderSettingsManager.settings } returns flowOf(ReminderSettings())
        
        recordMorningCheckIn = RecordMorningCheckIn(
            workEntryDao,
            locationProvider,
            locationCalculator,
            reminderSettingsManager
        )
    }
    
    @After
    fun tearDown() {
        unmockkAll()
    }
    
    @Test
    fun `invoke - Innerhalb Leipzig - Setzt korrekte Werte`() = runTest {
        // Arrange
        val date = LocalDate.now()
        val locationResult = LocationResult.Success(
            lat = 51.400,
            lon = 12.450,
            accuracyMeters = 100.0f
        )
        val locationCheck = LocationCheckResult(
            isInside = true,
            distanceKm = 10.0,
            confirmRequired = false
        )
        
        coEvery { workEntryDao.getByDate(date) } returns null
        coEvery { locationProvider.getCurrentLocation(any()) } returns locationResult
        every { locationCalculator.checkLeipzigLocation(51.400, 12.450, any()) } returns locationCheck
        coEvery { workEntryDao.upsert(any()) } just Runs
        
        // Act
        val result = recordMorningCheckIn.invoke(date)
        
        // Assert
        assertEquals(date, result.date)
        assertEquals(DayType.WORK, result.dayType)
        assertEquals(51.400, result.morningLat!!, 0.0)
        assertEquals(12.450, result.morningLon!!, 0.0)
        assertEquals(100.0f, result.morningAccuracyMeters)
        assertEquals("Leipzig", result.morningLocationLabel)
        assertTrue(result.outsideLeipzigMorning == false)
        assertEquals(LocationStatus.OK, result.morningLocationStatus)
        assertTrue(result.needsReview == false)
        
        coVerify { workEntryDao.upsert(result) }
    }
    
    @Test
    fun `invoke - Außerhalb Leipzig - Setzt outsideLeipzig true`() = runTest {
        // Arrange
        val date = LocalDate.now()
        val locationResult = LocationResult.Success(
            lat = 51.050,
            lon = 13.737, // Dresden (ca. 100km)
            accuracyMeters = 100.0f
        )
        val locationCheck = LocationCheckResult(
            isInside = false,
            distanceKm = 100.0,
            confirmRequired = false
        )
        
        coEvery { workEntryDao.getByDate(date) } returns null
        coEvery { locationProvider.getCurrentLocation(any()) } returns locationResult
        every { locationCalculator.checkLeipzigLocation(51.050, 13.737, any()) } returns locationCheck
        coEvery { workEntryDao.upsert(any()) } just Runs
        
        // Act
        val result = recordMorningCheckIn.invoke(date)
        
        // Assert
        assertTrue(result.outsideLeipzigMorning == true)
        assertNull("Label sollte null sein für außerhalb", result.morningLocationLabel)
        assertEquals(LocationStatus.OK, result.morningLocationStatus)
        
        coVerify { workEntryDao.upsert(result) }
    }
    
    @Test
    fun `invoke - Grenzzone - Setzt confirmRequired und needsReview`() = runTest {
        // Arrange
        val date = LocalDate.now()
        val locationResult = LocationResult.Success(
            lat = 51.580,
            lon = 12.200,
            accuracyMeters = 100.0f
        )
        val locationCheck = LocationCheckResult(
            isInside = null, // Unklar
            distanceKm = 29.0,
            confirmRequired = true
        )
        
        coEvery { workEntryDao.getByDate(date) } returns null
        coEvery { locationProvider.getCurrentLocation(any()) } returns locationResult
        every { locationCalculator.checkLeipzigLocation(51.580, 12.200, any()) } returns locationCheck
        coEvery { workEntryDao.upsert(any()) } just Runs
        
        // Act
        val result = recordMorningCheckIn.invoke(date)
        
        // Assert
        assertNull("Grenzzone sollte null sein", result.outsideLeipzigMorning)
        assertTrue("Grenzzone sollte needsReview=true setzen", result.needsReview)
        
        coVerify { workEntryDao.upsert(result) }
    }
    
    @Test
    fun `invoke - Low Accuracy - Setzt LOW_ACCURACY und needsReview`() = runTest {
        // Arrange
        val date = LocalDate.now()
        val locationResult = LocationResult.LowAccuracy(accuracyMeters = 5000.0f)
        
        coEvery { workEntryDao.getByDate(date) } returns null
        coEvery { locationProvider.getCurrentLocation(any()) } returns locationResult
        coEvery { workEntryDao.upsert(any()) } just Runs
        
        // Act
        val result = recordMorningCheckIn.invoke(date)
        
        // Assert
        assertEquals(LocationStatus.LOW_ACCURACY, result.morningLocationStatus)
        assertEquals(5000.0f, result.morningAccuracyMeters)
        assertNull("Low accuracy sollte outside=null setzen", result.outsideLeipzigMorning)
        assertTrue("Low accuracy sollte needsReview=true setzen", result.needsReview)
        
        coVerify { workEntryDao.upsert(result) }
    }
    
    @Test
    fun `invoke - Unavailable - Setzt UNAVAILABLE und needsReview`() = runTest {
        // Arrange
        val date = LocalDate.now()
        val locationResult = LocationResult.Unavailable
        
        coEvery { workEntryDao.getByDate(date) } returns null
        coEvery { locationProvider.getCurrentLocation(any()) } returns locationResult
        coEvery { workEntryDao.upsert(any()) } just Runs
        
        // Act
        val result = recordMorningCheckIn.invoke(date)
        
        // Assert
        assertEquals(LocationStatus.UNAVAILABLE, result.morningLocationStatus)
        assertNull(result.outsideLeipzigMorning)
        assertTrue("Unavailable sollte needsReview=true setzen", result.needsReview)
        
        coVerify { workEntryDao.upsert(result) }
    }
    
    @Test
    fun `invoke - forceWithoutLocation - Verwendet Unavailable ohne LocationProvider Call`() = runTest {
        // Arrange
        val date = LocalDate.now()
        
        coEvery { workEntryDao.getByDate(date) } returns null
        coEvery { workEntryDao.upsert(any()) } just Runs
        
        // Act
        val result = recordMorningCheckIn.invoke(date, forceWithoutLocation = true)
        
        // Assert
        assertEquals(LocationStatus.UNAVAILABLE, result.morningLocationStatus)
        assertFalse("Bewusst ohne Standort sollte kein Review erzwingen", result.needsReview)
        coVerify(exactly = 0) { locationProvider.getCurrentLocation(any()) }
        coVerify { workEntryDao.upsert(result) }
    }

    @Test
    fun `invoke - Nicht-heutiges Datum - Setzt date korrekt`() = runTest {
        // Arrange
        val date = LocalDate.of(2024, 1, 15)

        coEvery { workEntryDao.getByDate(date) } returns null
        coEvery { workEntryDao.upsert(any()) } just Runs

        // Act
        val result = recordMorningCheckIn.invoke(date, forceWithoutLocation = true)

        // Assert
        assertEquals(date, result.date)
        coVerify { workEntryDao.upsert(result) }
    }
    
    @Test
    fun `invoke - Idempotent Upsert - Zweiter Morgen Check-in aktualisiert existierenden Eintrag`() = runTest {
        // Arrange
        val date = LocalDate.now()
        val existingEntry = WorkEntry(
            date = date,
            dayType = DayType.WORK,
            workStart = java.time.LocalTime.of(8, 0),
            workEnd = java.time.LocalTime.of(17, 0),
            breakMinutes = 60,
            morningCapturedAt = 1000000L,
            morningLocationStatus = LocationStatus.OK,
            morningLat = 51.400,
            morningLon = 12.450,
            morningAccuracyMeters = 100.0f,
            morningLocationLabel = "Leipzig",
            outsideLeipzigMorning = false,
            needsReview = false,
            createdAt = 1000000L,
            updatedAt = 1000000L
        )
        
        val locationResult = LocationResult.Success(
            lat = 51.410,
            lon = 12.460,
            accuracyMeters = 150.0f
        )
        val locationCheck = LocationCheckResult(
            isInside = true,
            distanceKm = 10.0,
            confirmRequired = false
        )
        
        coEvery { workEntryDao.getByDate(date) } returns existingEntry
        coEvery { locationProvider.getCurrentLocation(any()) } returns locationResult
        every { locationCalculator.checkLeipzigLocation(51.410, 12.460, any()) } returns locationCheck
        coEvery { workEntryDao.upsert(any()) } just Runs
        
        // Act
        val result = recordMorningCheckIn.invoke(date)
        
        // Assert
        assertEquals(date, result.date)
        assertEquals(DayType.WORK, result.dayType)
        assertEquals(java.time.LocalTime.of(8, 0), result.workStart) // Nicht verändert
        assertEquals(51.410, result.morningLat!!, 0.0) // Aktualisiert
        assertEquals(12.460, result.morningLon!!, 0.0) // Aktualisiert
        assertEquals(150.0f, result.morningAccuracyMeters) // Aktualisiert
        assertTrue(result.updatedAt > existingEntry.updatedAt) // updatedAt aktualisiert
        assertEquals(existingEntry.createdAt, result.createdAt) // createdAt gleich
        
        coVerify { workEntryDao.upsert(result) }
    }
    
    @Test
    fun `invoke - Existing mit needsReview true - Behält needsReview true`() = runTest {
        // Arrange
        val date = LocalDate.now()
        val existingEntry = WorkEntry(
            date = date,
            dayType = DayType.WORK,
            needsReview = true, // Bereits needsReview
            createdAt = 1000000L,
            updatedAt = 1000000L
        )
        
        val locationResult = LocationResult.Success(
            lat = 51.400,
            lon = 12.450,
            accuracyMeters = 100.0f
        )
        val locationCheck = LocationCheckResult(
            isInside = true,
            distanceKm = 10.0,
            confirmRequired = false
        )
        
        coEvery { workEntryDao.getByDate(date) } returns existingEntry
        coEvery { locationProvider.getCurrentLocation(any()) } returns locationResult
        every { locationCalculator.checkLeipzigLocation(51.400, 12.450, any()) } returns locationCheck
        coEvery { workEntryDao.upsert(any()) } just Runs
        
        // Act
        val result = recordMorningCheckIn.invoke(date)
        
        // Assert
        assertTrue("Bestehendes needsReview sollte beibehalten werden", result.needsReview)
        
        coVerify { workEntryDao.upsert(result) }
    }
}
