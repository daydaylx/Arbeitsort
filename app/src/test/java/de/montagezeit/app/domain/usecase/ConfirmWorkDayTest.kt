package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.DayLocationSource
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.LocationStatus
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.location.LocationProvider
import de.montagezeit.app.data.preferences.ReminderSettingsManager
import de.montagezeit.app.domain.location.LocationCheckResult
import de.montagezeit.app.domain.location.LocationCalculator
import de.montagezeit.app.domain.model.LocationResult
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class ConfirmWorkDayTest {

    private lateinit var confirmWorkDay: ConfirmWorkDay
    private lateinit var workEntryDao: WorkEntryDao
    private lateinit var locationProvider: LocationProvider
    private lateinit var locationCalculator: LocationCalculator
    private lateinit var reminderSettingsManager: ReminderSettingsManager

    @Before
    fun setup() {
        workEntryDao = mockk(relaxed = true)
        locationProvider = mockk(relaxed = true)
        locationCalculator = mockk(relaxed = true)
        reminderSettingsManager = mockk(relaxed = true)

        coEvery { workEntryDao.getByDate(any()) } returns null

        confirmWorkDay = ConfirmWorkDay(
            workEntryDao = workEntryDao,
            locationProvider = locationProvider,
            locationCalculator = locationCalculator,
            reminderSettingsManager = reminderSettingsManager
        )

        // Default Settings Mock
        every { reminderSettingsManager.settings } returns flowOf(
            de.montagezeit.app.data.preferences.ReminderSettings(
                workStart = LocalTime.of(8, 0),
                workEnd = LocalTime.of(19, 0),
                breakMinutes = 60,
                locationRadiusKm = 30
            )
        )
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `JA erzeugt WORK-Entry mit Defaults und confirmedWorkDay true`() = runTest {
        // Arrange
        val date = LocalDate.now()
        val locationResult = LocationResult.Success(
            lat = 51.340632,
            lon = 12.374729,
            accuracyMeters = 10.0f
        )
        
        coEvery { locationProvider.getCurrentLocation(any()) } returns locationResult
        coEvery { locationCalculator.checkLeipzigLocation(any(), any(), any()) } returns
            LocationCheckResult(
                isInside = true,
                distanceKm = 0.0,
                confirmRequired = false
            )

        // Act
        val result = confirmWorkDay(date, source = "TEST")

        // Assert
        assertNotNull(result)
        assertEquals(DayType.WORK, result.dayType)
        assertEquals(LocalTime.of(8, 0), result.workStart)
        assertEquals(LocalTime.of(19, 0), result.workEnd)
        assertEquals(60, result.breakMinutes)
        assertTrue(result.confirmedWorkDay)
        assertNotNull(result.confirmationAt)
        assertEquals("TEST", result.confirmationSource)
        
        coVerify { workEntryDao.upsert(match { entry ->
            entry.date == date &&
            entry.confirmedWorkDay == true &&
            entry.workStart == LocalTime.of(8, 0) &&
            entry.workEnd == LocalTime.of(19, 0) &&
            entry.breakMinutes == 60 &&
            entry.dayType == DayType.WORK
        }) }
    }

    @Test
    fun `Location-Failure setzt needsReview true`() = runTest {
        // Arrange
        val date = LocalDate.now()
        val locationResult = LocationResult.Unavailable
        
        coEvery { locationProvider.getCurrentLocation(any()) } returns locationResult

        // Act
        val result = confirmWorkDay(date, source = "TEST")

        // Assert
        assertNotNull(result)
        assertEquals(DayType.WORK, result.dayType)
        assertEquals(LocalTime.of(8, 0), result.workStart)
        assertEquals(LocalTime.of(19, 0), result.workEnd)
        assertEquals(60, result.breakMinutes)
        assertTrue(result.confirmedWorkDay)
        assertTrue(result.needsReview)
        assertEquals(LocationStatus.UNAVAILABLE, result.morningLocationStatus)
        
        coVerify { workEntryDao.upsert(match { entry ->
            entry.needsReview == true &&
            entry.morningLocationStatus == LocationStatus.UNAVAILABLE
        }) }
    }

    @Test
    fun `Location-Inside-Leipzig setzt Label`() = runTest {
        // Arrange
        val date = LocalDate.now()
        val locationResult = LocationResult.Success(
            lat = 51.340632,
            lon = 12.374729,
            accuracyMeters = 10.0f
        )
        
        coEvery { locationProvider.getCurrentLocation(any()) } returns locationResult
        coEvery { locationCalculator.checkLeipzigLocation(any(), any(), any()) } returns
            LocationCheckResult(
                isInside = true,
                distanceKm = 0.0,
                confirmRequired = false
            )

        // Act
        val result = confirmWorkDay(date, source = "TEST")

        // Assert
        assertEquals("Leipzig", result.morningLocationLabel)
        assertEquals(false, result.outsideLeipzigMorning)
    }

    @Test
    fun `Location-Outside-Leipzig setzt Label null`() = runTest {
        // Arrange
        val date = LocalDate.now()
        val locationResult = LocationResult.Success(
            lat = 52.520008,
            lon = 13.405,
            accuracyMeters = 10.0f // Berlin
        )
        
        coEvery { locationProvider.getCurrentLocation(any()) } returns locationResult
        coEvery { locationCalculator.checkLeipzigLocation(any(), any(), any()) } returns
            LocationCheckResult(
                isInside = false,
                distanceKm = 150.0,
                confirmRequired = false
            )

        // Act
        val result = confirmWorkDay(date, source = "TEST")

        // Assert
        assertEquals(null, result.morningLocationLabel)
        assertEquals(true, result.outsideLeipzigMorning)
        assertTrue(result.needsReview)
    }

    @Test
    fun `manual day location bleibt bei erfolgreichem Standort erhalten`() = runTest {
        val date = LocalDate.now()
        val existingEntry = WorkEntry(
            date = date,
            dayType = DayType.WORK,
            dayLocationLabel = "Berlin",
            dayLocationSource = DayLocationSource.MANUAL,
            dayLocationLat = 52.52,
            dayLocationLon = 13.40
        )
        coEvery { workEntryDao.getByDate(date) } returns existingEntry

        val locationResult = LocationResult.Success(
            lat = 51.340632,
            lon = 12.374729,
            accuracyMeters = 10.0f
        )
        coEvery { locationProvider.getCurrentLocation(any()) } returns locationResult
        coEvery { locationCalculator.checkLeipzigLocation(any(), any(), any()) } returns
            LocationCheckResult(
                isInside = true,
                distanceKm = 0.0,
                confirmRequired = false
            )

        val result = confirmWorkDay(date, source = "TEST")

        assertEquals("Berlin", result.dayLocationLabel)
        assertEquals(DayLocationSource.MANUAL, result.dayLocationSource)
        assertEquals(52.52, result.dayLocationLat)
        assertEquals(13.40, result.dayLocationLon)
    }

    @Test
    fun `fallback label nutzt Leipzig wenn Setting leer ist`() = runTest {
        val date = LocalDate.now()
        every { reminderSettingsManager.settings } returns flowOf(
            de.montagezeit.app.data.preferences.ReminderSettings(
                workStart = LocalTime.of(8, 0),
                workEnd = LocalTime.of(19, 0),
                breakMinutes = 60,
                locationRadiusKm = 30,
                defaultDayLocationLabel = ""
            )
        )
        coEvery { locationProvider.getCurrentLocation(any()) } returns LocationResult.Unavailable

        val result = confirmWorkDay(date, source = "TEST")

        assertEquals("Leipzig", result.dayLocationLabel)
        assertEquals(DayLocationSource.FALLBACK, result.dayLocationSource)
        assertTrue(result.needsReview)
    }

    @Test
    fun `force without location umgeht LocationProvider`() = runTest {
        val date = LocalDate.now()
        coEvery { locationProvider.getCurrentLocation(any()) } returns LocationResult.Success(
            lat = 51.3,
            lon = 12.3,
            accuracyMeters = 5f
        )

        val result = confirmWorkDay(date, forceWithoutLocation = true, source = "TEST")

        coVerify(exactly = 0) { locationProvider.getCurrentLocation(any()) }
        assertEquals(LocationStatus.UNAVAILABLE, result.morningLocationStatus)
        assertFalse("Bewusst ohne Standort sollte kein Review erzwingen", result.needsReview)
    }

    @Test
    fun `Existing-Entry wird aktualisiert`() = runTest {
        // Arrange
        val date = LocalDate.now()
        val existingEntry = WorkEntry(
            date = date,
            dayType = DayType.WORK,
            workStart = LocalTime.of(9, 0),
            workEnd = LocalTime.of(17, 0),
            breakMinutes = 30
        )
        
        coEvery { workEntryDao.getByDate(date) } returns existingEntry
        
        val locationResult = LocationResult.Success(
            lat = 51.340632,
            lon = 12.374729,
            accuracyMeters = 10.0f
        )
        
        coEvery { locationProvider.getCurrentLocation(any()) } returns locationResult
        coEvery { locationCalculator.checkLeipzigLocation(any(), any(), any()) } returns
            LocationCheckResult(
                isInside = true,
                distanceKm = 0.0,
                confirmRequired = false
            )

        // Act
        val result = confirmWorkDay(date, source = "TEST")

        // Assert
        assertNotNull(result)
        assertEquals(DayType.WORK, result.dayType)
        assertEquals(LocalTime.of(8, 0), result.workStart) // Aus Settings!
        assertEquals(LocalTime.of(19, 0), result.workEnd) // Aus Settings!
        assertEquals(60, result.breakMinutes) // Aus Settings!
        assertTrue(result.confirmedWorkDay)
    }
}
