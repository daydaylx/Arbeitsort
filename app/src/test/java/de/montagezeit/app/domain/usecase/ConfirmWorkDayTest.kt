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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class ConfirmWorkDayTest {

    private val workEntryDao = mockk<WorkEntryDao>()
    private val reminderSettingsManager = mockk<ReminderSettingsManager>()

    private val useCase = ConfirmWorkDay(
        workEntryDao = workEntryDao,
        reminderSettingsManager = reminderSettingsManager
    )

    @Test
    fun `invoke creates work entry with settings and confirmation metadata`() = runTest {
        val date = LocalDate.now()
        coEvery { workEntryDao.getByDate(date) } returns null
        coEvery { workEntryDao.upsert(any()) } returns Unit
        every { reminderSettingsManager.settings } returns flowOf(
            ReminderSettings(
                workStart = LocalTime.of(9, 0),
                workEnd = LocalTime.of(18, 0),
                breakMinutes = 45
            )
        )

        val result = useCase(date, source = "TEST")

        assertEquals(DayType.WORK, result.dayType)
        assertEquals(LocalTime.of(9, 0), result.workStart)
        assertEquals(LocalTime.of(18, 0), result.workEnd)
        assertEquals(45, result.breakMinutes)
        assertEquals("", result.dayLocationLabel)
        assertEquals(DayLocationSource.FALLBACK, result.dayLocationSource)
        assertNull(result.dayLocationLat)
        assertNull(result.dayLocationLon)
        assertEquals(LocationStatus.UNAVAILABLE, result.morningLocationStatus)
        assertNotNull(result.morningCapturedAt)
        assertEquals(true, result.confirmedWorkDay)
        assertNotNull(result.confirmationAt)
        assertEquals("TEST", result.confirmationSource)
        assertFalse(result.needsReview)

        coVerify(exactly = 1) { workEntryDao.upsert(result) }
    }

    @Test
    fun `invoke updates existing entry and preserves manual day location`() = runTest {
        val date = LocalDate.now()
        val existing = WorkEntry(
            date = date,
            dayType = DayType.OFF,
            dayLocationLabel = "Berlin",
            dayLocationSource = DayLocationSource.MANUAL,
            dayLocationLat = 52.52,
            dayLocationLon = 13.40,
            morningLocationStatus = LocationStatus.OK
        )

        coEvery { workEntryDao.getByDate(date) } returns existing
        coEvery { workEntryDao.upsert(any()) } returns Unit
        every { reminderSettingsManager.settings } returns flowOf(ReminderSettings())

        val result = useCase(date, source = "UI_TEST")

        assertEquals(DayType.WORK, result.dayType)
        assertEquals("Berlin", result.dayLocationLabel)
        assertEquals(DayLocationSource.MANUAL, result.dayLocationSource)
        assertNull(result.dayLocationLat)
        assertNull(result.dayLocationLon)
        assertEquals(LocationStatus.OK, result.morningLocationStatus)
        assertEquals(true, result.confirmedWorkDay)
        assertEquals("UI_TEST", result.confirmationSource)
        assertFalse(result.needsReview)
    }

    @Test
    fun `invoke keeps unavailable morning status and existing capture timestamp`() = runTest {
        val date = LocalDate.now()
        val existing = WorkEntry(
            date = date,
            dayType = DayType.OFF,
            morningCapturedAt = 1234L,
            morningLocationStatus = LocationStatus.UNAVAILABLE
        )

        coEvery { workEntryDao.getByDate(date) } returns existing
        coEvery { workEntryDao.upsert(any()) } returns Unit
        every { reminderSettingsManager.settings } returns flowOf(ReminderSettings())

        val result = useCase(date)

        assertEquals(1234L, result.morningCapturedAt)
        assertEquals(LocationStatus.UNAVAILABLE, result.morningLocationStatus)
        assertEquals(true, result.confirmedWorkDay)
    }
}
