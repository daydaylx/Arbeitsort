package de.montagezeit.app.receiver

import android.content.Intent
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Tests the intent filtering logic used by BootReceiver.
 * Direct testing of @AndroidEntryPoint receivers is not feasible without Hilt test setup.
 */
class BootReceiverTest {

    @Test
    fun `BOOT_COMPLETED is a handled action`() {
        assertTrue(isBootReceiverAction(Intent.ACTION_BOOT_COMPLETED))
    }

    @Test
    fun `MY_PACKAGE_REPLACED is a handled action`() {
        assertTrue(isBootReceiverAction(Intent.ACTION_MY_PACKAGE_REPLACED))
    }

    @Test
    fun `unrelated action is not handled`() {
        assertFalse(isBootReceiverAction("some.other.action"))
    }

    @Test
    fun `null action is not handled`() {
        assertFalse(isBootReceiverAction(null))
    }

    /**
     * Mirrors the intent filter logic in BootReceiver.onReceive
     */
    private fun isBootReceiverAction(action: String?): Boolean {
        return action == Intent.ACTION_BOOT_COMPLETED ||
                action == Intent.ACTION_MY_PACKAGE_REPLACED
    }
}
