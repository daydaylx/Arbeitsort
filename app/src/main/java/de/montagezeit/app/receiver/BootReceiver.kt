package de.montagezeit.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import de.montagezeit.app.work.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * BroadcastReceiver für Boot-Completed und App-Update Events
 * 
 * Plant alle Reminder-Worker neu nach:
 * - System Boot (BOOT_COMPLETED)
 * - App Update (MY_PACKAGE_REPLACED)
 * 
 * Dies stellt sicher, dass Reminder auch nach Reboot/App-Update funktionieren.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    
    @Inject
    lateinit var reminderScheduler: ReminderScheduler
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {

            // Keep receiver alive until async work completes
            val pendingResult = goAsync()
            val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
            scope.launch {
                try {
                    withTimeoutOrNull(10.seconds) {
                        reminderScheduler.scheduleAll()
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
