package de.montagezeit.app.work

import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WindowCheckWorkerDecisionTest {

    @Test
    fun `manual daily check-in entry suppresses reminder decisions`() {
        val entry = WorkEntry(
            date = LocalDate.now(),
            dayType = DayType.WORK,
            morningCapturedAt = 1_000L,
            eveningCapturedAt = 2_000L,
            confirmedWorkDay = true
        )

        assertFalse(WindowCheckWorker.shouldShowMorningReminder(entry))
        assertFalse(WindowCheckWorker.shouldShowEveningReminder(entry))
        assertFalse(WindowCheckWorker.shouldShowFallbackReminder(entry))
        assertFalse(WindowCheckWorker.shouldShowDailyReminder(entry))
    }

    @Test
    fun `incomplete workday keeps reminder decisions active`() {
        val entry = WorkEntry(
            date = LocalDate.now(),
            dayType = DayType.WORK,
            morningCapturedAt = null,
            eveningCapturedAt = null,
            confirmedWorkDay = false
        )

        assertTrue(WindowCheckWorker.shouldShowMorningReminder(entry))
        assertTrue(WindowCheckWorker.shouldShowEveningReminder(entry))
        assertTrue(WindowCheckWorker.shouldShowFallbackReminder(entry))
        assertTrue(WindowCheckWorker.shouldShowDailyReminder(entry))
    }
}
