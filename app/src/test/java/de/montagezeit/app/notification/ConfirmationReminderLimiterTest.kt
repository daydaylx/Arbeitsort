package de.montagezeit.app.notification

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class ConfirmationReminderLimiterTest {

    private lateinit var limiter: ConfirmationReminderLimiter
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        val prefs = context.getSharedPreferences("confirmation_reminder_count_test", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        limiter = ConfirmationReminderLimiter(prefs)
    }

    @Test
    fun `spaeter stops after max 2 per day`() {
        val date = LocalDate.of(2026, 1, 5)

        assertTrue(limiter.canSchedule(date))
        limiter.increment(date)
        assertTrue(limiter.canSchedule(date))
        limiter.increment(date)
        assertFalse(limiter.canSchedule(date))
    }
}
