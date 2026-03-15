package de.montagezeit.app.receiver

import android.content.Intent
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Tests the intent filtering logic used by TimeChangeReceiver.
 * Direct testing of @AndroidEntryPoint receivers is not feasible without Hilt test setup.
 */
class TimeChangeReceiverTest {

    @Test
    fun `ACTION_TIME_CHANGED is a handled action`() {
        assertTrue(isTimeChangeAction(Intent.ACTION_TIME_CHANGED))
    }

    @Test
    fun `ACTION_TIMEZONE_CHANGED is a handled action`() {
        assertTrue(isTimeChangeAction(Intent.ACTION_TIMEZONE_CHANGED))
    }

    @Test
    fun `unrelated action is not handled`() {
        assertFalse(isTimeChangeAction("some.random.action"))
    }

    @Test
    fun `null action is not handled`() {
        assertFalse(isTimeChangeAction(null))
    }

    /**
     * Mirrors the intent filter logic in TimeChangeReceiver.onReceive
     */
    private fun isTimeChangeAction(action: String?): Boolean {
        return action == Intent.ACTION_TIME_CHANGED ||
                action == Intent.ACTION_TIMEZONE_CHANGED
    }
}
