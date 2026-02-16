package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.DayLocationSource
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.LocationStatus
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.preferences.ReminderSettings
import de.montagezeit.app.data.preferences.ReminderSettingsManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class RecordDailyManualCheckInTest {

    private lateinit var workEntryDao: WorkEntryDao
    private lateinit var reminderSettingsManager: ReminderSettingsManager
    private lateinit var useCase: RecordDailyManualCheckIn

    @Before
    fun setup() {
        workEntryDao = mockk()
        reminderSettingsManager = mockk()

        every { reminderSettingsManager.settings } returns flowOf(
            ReminderSettings(
                workStart = LocalTime.of(8, 0),
                workEnd = LocalTime.of(17, 0),
                breakMinutes = 45,
                defaultDayLocationLabel = "Leipzig"
            )
        )

        useCase = RecordDailyManualCheckIn(
            workEntryDao = workEntryDao,
            reminderSettingsManager = reminderSettingsManager,
            resolveDayLocationPrefill = ResolveDayLocationPrefill(workEntryDao, reminderSettingsManager)
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `invoke setzt Tagesabschlussfelder bei manuellem Label`() = runTest {
        val date = LocalDate.now()
        coEvery { workEntryDao.getByDate(date) } returns null
        coEvery { workEntryDao.getLatestDayLocationLabelByDayType(DayType.WORK) } returns "Alt"
        coEvery { workEntryDao.getLatestDayLocationLabel() } returns "Fallback"
        coEvery { workEntryDao.upsert(any()) } returns Unit

        val result = useCase(date, "  Baustelle XY  ")

        assertEquals(DayType.WORK, result.dayType)
        assertEquals("Baustelle XY", result.dayLocationLabel)
        assertEquals(DayLocationSource.MANUAL, result.dayLocationSource)
        assertEquals(LocationStatus.UNAVAILABLE, result.morningLocationStatus)
        assertEquals(LocationStatus.UNAVAILABLE, result.eveningLocationStatus)
        assertNotNull(result.morningCapturedAt)
        assertNotNull(result.eveningCapturedAt)
        assertEquals(true, result.confirmedWorkDay)
        assertNotNull(result.confirmationAt)
        assertEquals("UI", result.confirmationSource)
        assertFalse(result.needsReview)

        coVerify(exactly = 1) { workEntryDao.upsert(result) }
    }

    @Test
    fun `invoke nutzt heutigen Label bei leerem Input`() = runTest {
        val date = LocalDate.now()
        val existingEntry = WorkEntry(
            date = date,
            dayType = DayType.OFF,
            dayLocationLabel = "Heute Ort",
            dayLocationSource = DayLocationSource.FALLBACK
        )

        coEvery { workEntryDao.getByDate(date) } returns existingEntry
        coEvery { workEntryDao.upsert(any()) } returns Unit

        val result = useCase(date, "   ")

        assertEquals("Heute Ort", result.dayLocationLabel)
        assertEquals(DayType.WORK, result.dayType)
        assertEquals(true, result.confirmedWorkDay)
        assertNotNull(result.morningCapturedAt)
        assertNotNull(result.eveningCapturedAt)

        coVerify(exactly = 0) { workEntryDao.getLatestDayLocationLabelByDayType(any()) }
        coVerify(exactly = 0) { workEntryDao.getLatestDayLocationLabel() }
    }

    @Test
    fun `invoke nutzt letzten WORK Label vor any Label`() = runTest {
        val date = LocalDate.now()
        coEvery { workEntryDao.getByDate(date) } returns null
        coEvery { workEntryDao.getLatestDayLocationLabelByDayType(DayType.WORK) } returns "Werk 7"
        coEvery { workEntryDao.getLatestDayLocationLabel() } returns "Sonstiger Ort"
        coEvery { workEntryDao.upsert(any()) } returns Unit

        val result = useCase(date, "")

        assertEquals("Werk 7", result.dayLocationLabel)
    }

    @Test
    fun `invoke nutzt Settings Label wenn keine Historie vorhanden`() = runTest {
        val date = LocalDate.now()
        coEvery { workEntryDao.getByDate(date) } returns null
        coEvery { workEntryDao.getLatestDayLocationLabelByDayType(DayType.WORK) } returns null
        coEvery { workEntryDao.getLatestDayLocationLabel() } returns null
        coEvery { workEntryDao.upsert(any()) } returns Unit

        every { reminderSettingsManager.settings } returns flowOf(
            ReminderSettings(
                workStart = LocalTime.of(8, 0),
                workEnd = LocalTime.of(17, 0),
                breakMinutes = 45,
                defaultDayLocationLabel = "Berlin"
            )
        )

        val result = useCase(date, " ")

        assertEquals("Berlin", result.dayLocationLabel)
    }

    @Test
    fun `invoke behaelt vorhandene status bei bereits erfassten snapshots`() = runTest {
        val date = LocalDate.now()
        val existing = WorkEntry(
            date = date,
            dayType = DayType.WORK,
            dayLocationLabel = "Alt",
            morningCapturedAt = 1_000L,
            morningLocationStatus = LocationStatus.OK,
            eveningCapturedAt = 2_000L,
            eveningLocationStatus = LocationStatus.LOW_ACCURACY
        )
        coEvery { workEntryDao.getByDate(date) } returns existing
        coEvery { workEntryDao.upsert(any()) } returns Unit

        val result = useCase(date, "Neuer Ort")

        assertEquals(LocationStatus.OK, result.morningLocationStatus)
        assertEquals(LocationStatus.LOW_ACCURACY, result.eveningLocationStatus)
        assertEquals(1_000L, result.morningCapturedAt)
        assertEquals(2_000L, result.eveningCapturedAt)
        assertEquals("Neuer Ort", result.dayLocationLabel)
    }
}
