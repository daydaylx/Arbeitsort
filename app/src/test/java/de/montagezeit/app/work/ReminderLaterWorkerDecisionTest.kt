package de.montagezeit.app.work

import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.preferences.ReminderSettings
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.LocalDate

class ReminderLaterWorkerDecisionTest {

    private val workEntryDao = mockk<WorkEntryDao>(relaxed = true)

    @Test
    fun `MORNING snooze is suppressed for OFF entry`() = runBlocking {
        val date = LocalDate.of(2026, 3, 12)
        coEvery { workEntryDao.getByDate(date) } returns workEntry(dayType = DayType.OFF)

        val shouldShow = ReminderLaterWorker.shouldShowReminder(
            date = date,
            reminderType = ReminderType.MORNING,
            workEntryDao = workEntryDao,
            settings = ReminderSettings()
        )

        assertFalse(shouldShow)
    }

    @Test
    fun `EVENING snooze is suppressed when evening snapshot already exists`() = runBlocking {
        val date = LocalDate.of(2026, 3, 12)
        coEvery { workEntryDao.getByDate(date) } returns workEntry(eveningCapturedAt = 1_000L)

        val shouldShow = ReminderLaterWorker.shouldShowReminder(
            date = date,
            reminderType = ReminderType.EVENING,
            workEntryDao = workEntryDao,
            settings = ReminderSettings()
        )

        assertFalse(shouldShow)
    }

    @Test
    fun `FALLBACK snooze is suppressed when work day is already complete`() = runBlocking {
        val date = LocalDate.of(2026, 3, 12)
        coEvery { workEntryDao.getByDate(date) } returns workEntry(
            morningCapturedAt = 1_000L,
            eveningCapturedAt = 2_000L
        )

        val shouldShow = ReminderLaterWorker.shouldShowReminder(
            date = date,
            reminderType = ReminderType.FALLBACK,
            workEntryDao = workEntryDao,
            settings = ReminderSettings()
        )

        assertFalse(shouldShow)
    }

    @Test
    fun `DAILY snooze is suppressed for COMP_TIME entry`() = runBlocking {
        val date = LocalDate.of(2026, 3, 12)
        coEvery { workEntryDao.getByDate(date) } returns workEntry(
            dayType = DayType.COMP_TIME,
            confirmedWorkDay = false
        )

        val shouldShow = ReminderLaterWorker.shouldShowReminder(
            date = date,
            reminderType = ReminderType.DAILY,
            workEntryDao = workEntryDao,
            settings = ReminderSettings()
        )

        assertFalse(shouldShow)
    }

    @Test
    fun `snooze is suppressed for auto off day without entry`() = runBlocking {
        val saturday = LocalDate.of(2026, 3, 14)
        coEvery { workEntryDao.getByDate(saturday) } returns null

        val shouldShow = ReminderLaterWorker.shouldShowReminder(
            date = saturday,
            reminderType = ReminderType.MORNING,
            workEntryDao = workEntryDao,
            settings = ReminderSettings(autoOffWeekends = true)
        )

        assertFalse(shouldShow)
    }

    @Test
    fun `unknown reminder type is suppressed instead of falling back to morning`() = runBlocking {
        val date = LocalDate.of(2026, 3, 12)
        coEvery { workEntryDao.getByDate(date) } returns workEntry()

        val shouldShow = ReminderLaterWorker.shouldShowReminder(
            date = date,
            reminderType = null,
            workEntryDao = workEntryDao,
            settings = ReminderSettings()
        )

        assertFalse(shouldShow)
    }

    private fun workEntry(
        dayType: DayType = DayType.WORK,
        morningCapturedAt: Long? = null,
        eveningCapturedAt: Long? = null,
        confirmedWorkDay: Boolean = false
    ) = WorkEntry(
        date = LocalDate.of(2026, 3, 12),
        dayType = dayType,
        morningCapturedAt = morningCapturedAt,
        eveningCapturedAt = eveningCapturedAt,
        confirmedWorkDay = confirmedWorkDay
    )
}
