package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.DayType
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class ConfirmWorkDayTest {

    private val workEntryDao = mockk<WorkEntryDao>(relaxed = true)
    private val reminderSettingsManager = mockk<ReminderSettingsManager>()

    private val useCase = ConfirmWorkDay(
        workEntryDao = workEntryDao,
        reminderSettingsManager = reminderSettingsManager
    )

    @Test
    fun `invoke throws when no day location is available`() = runTest {
        val date = LocalDate.now()
        coEvery { workEntryDao.upsert(any()) } returns Unit
        stubReadModifyWrite(workEntryDao, existingEntry = null)
        every { reminderSettingsManager.settings } returns flowOf(
            ReminderSettings(
                workStart = LocalTime.of(9, 0),
                workEnd = LocalTime.of(18, 0),
                breakMinutes = 45
            )
        )

        try {
            useCase(date, source = "TEST")
            fail("Expected missing day location to fail")
        } catch (expected: IllegalArgumentException) {
            assertEquals("Arbeitsort fehlt für bestätigten Arbeitstag", expected.message)
        }

        coVerify(exactly = 0) { workEntryDao.upsert(any()) }
    }

    @Test
    fun `invoke updates existing entry and preserves manual day location`() = runTest {
        val date = LocalDate.now()
        val existing = WorkEntry(
            date = date,
            dayType = DayType.OFF,
            dayLocationLabel = "Berlin"
        )

        coEvery { workEntryDao.upsert(any()) } returns Unit
        stubReadModifyWrite(workEntryDao, existing)
        every { reminderSettingsManager.settings } returns flowOf(ReminderSettings())

        val result = useCase(date, source = "UI_TEST")

        assertEquals(DayType.WORK, result.dayType)
        assertEquals("Berlin", result.dayLocationLabel)
        assertEquals(LocalTime.of(8, 0), result.workStart)
        assertEquals(LocalTime.of(19, 0), result.workEnd)
        assertEquals(60, result.breakMinutes)
        assertEquals(true, result.confirmedWorkDay)
        assertEquals("UI_TEST", result.confirmationSource)
    }

    @Test
    fun `invoke keeps configured times and confirmation metadata for located work day`() = runTest {
        val date = LocalDate.now()
        val existing = WorkEntry(
            date = date,
            dayType = DayType.WORK,
            dayLocationLabel = "Werk 9"
        )

        coEvery { workEntryDao.upsert(any()) } returns Unit
        stubReadModifyWrite(workEntryDao, existing)
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
        assertEquals("Werk 9", result.dayLocationLabel)
        assertNotNull(result.morningCapturedAt)
        assertEquals(true, result.confirmedWorkDay)
        assertNotNull(result.confirmationAt)
        assertEquals("TEST", result.confirmationSource)

        coVerify(exactly = 1) { workEntryDao.upsert(result) }
    }

    @Test
    fun `invoke keeps existing morning capture timestamp`() = runTest {
        val date = LocalDate.now()
        val existing = WorkEntry(
            date = date,
            dayType = DayType.OFF,
            dayLocationLabel = "Werk 7",
            morningCapturedAt = 1234L
        )

        coEvery { workEntryDao.upsert(any()) } returns Unit
        stubReadModifyWrite(workEntryDao, existing)
        every { reminderSettingsManager.settings } returns flowOf(ReminderSettings())

        val result = useCase(date)

        assertEquals(1234L, result.morningCapturedAt)
        assertEquals(true, result.confirmedWorkDay)
    }

    @Test
    fun `bereits bestaetigter Eintrag mit manuellen Zeiten behaelt Zeiten bei`() = runTest {
        val date = LocalDate.now()
        // Eintrag der bereits manuell bestätigt und mit individuellen Zeiten ausgestattet ist
        val manualStart = LocalTime.of(6, 30)
        val manualEnd = LocalTime.of(15, 0)
        val existing = WorkEntry(
            date = date,
            dayType = DayType.WORK,
            dayLocationLabel = "Baustelle",
            workStart = manualStart,
            workEnd = manualEnd,
            breakMinutes = 30,
            confirmedWorkDay = true  // bereits bestätigt
        )
        coEvery { workEntryDao.upsert(any()) } returns Unit
        stubReadModifyWrite(workEntryDao, existing)
        every { reminderSettingsManager.settings } returns flowOf(
            ReminderSettings(
                workStart = LocalTime.of(8, 0),
                workEnd = LocalTime.of(17, 0),
                breakMinutes = 60
            )
        )

        val result = useCase(date, source = "NOTIFICATION")

        // Manuelle Zeiten müssen erhalten bleiben – Settings dürfen sie nicht überschreiben
        assertEquals("Manuelle workStart darf nicht überschrieben werden", manualStart, result.workStart)
        assertEquals("Manuelle workEnd darf nicht überschrieben werden", manualEnd, result.workEnd)
        assertEquals("Manuelle breakMinutes darf nicht überschrieben werden", 30, result.breakMinutes)
    }

    @Test
    fun `unbestaetigter WORK Eintrag behaelt bestehende Zeiten`() = runTest {
        val date = LocalDate.now()
        val existing = WorkEntry(
            date = date,
            dayType = DayType.WORK,
            dayLocationLabel = "Baustelle",
            workStart = LocalTime.of(6, 0),
            workEnd = LocalTime.of(14, 0),
            breakMinutes = 30,
            confirmedWorkDay = false  // noch nicht bestätigt
        )
        coEvery { workEntryDao.upsert(any()) } returns Unit
        stubReadModifyWrite(workEntryDao, existing)
        every { reminderSettingsManager.settings } returns flowOf(
            ReminderSettings(
                workStart = LocalTime.of(8, 0),
                workEnd = LocalTime.of(17, 0),
                breakMinutes = 60
            )
        )

        val result = useCase(date, source = "NOTIFICATION")

        assertEquals(LocalTime.of(6, 0), result.workStart)
        assertEquals(LocalTime.of(14, 0), result.workEnd)
        assertEquals(30, result.breakMinutes)
        assertEquals(true, result.confirmedWorkDay)
    }
}
