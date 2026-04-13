package de.montagezeit.app.work

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.Operation
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderWorkEnqueuerTest {

    @Test
    fun `enqueue uses UPDATE policy and forwards work spec`() {
        val workManager = mockk<WorkManager>()
        val enqueuer = ReminderWorkEnqueuer(workManager)
        val requestSlot = slot<PeriodicWorkRequest>()
        val operation = mockk<Operation>(relaxed = true)
        val spec = ReminderPeriodicWorkSpec(
            uniqueWorkName = "daily_reminder_work",
            tag = "daily_reminder",
            reminderType = ReminderType.DAILY,
            initialDelayMillis = 42_000L,
            repeatInterval = 1,
            repeatIntervalTimeUnit = TimeUnit.DAYS
        )

        every {
            workManager.enqueueUniquePeriodicWork(
                spec.uniqueWorkName,
                ExistingPeriodicWorkPolicy.UPDATE,
                capture(requestSlot)
            )
        } returns operation

        enqueuer.enqueue(spec)

        verify(exactly = 1) {
            workManager.enqueueUniquePeriodicWork(
                spec.uniqueWorkName,
                ExistingPeriodicWorkPolicy.UPDATE,
                any()
            )
        }
        assertTrue(requestSlot.captured.tags.contains(spec.tag))
        assertEquals(spec.initialDelayMillis, requestSlot.captured.workSpec.initialDelay)
        assertEquals(spec.reminderType.name, requestSlot.captured.workSpec.input.getString(WindowCheckWorker.KEY_REMINDER_TYPE))
    }
}
