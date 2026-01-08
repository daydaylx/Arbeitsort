package de.montagezeit.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import de.montagezeit.app.logging.RingBufferLogger
import de.montagezeit.app.logging.i
import de.montagezeit.app.logging.e
import de.montagezeit.app.work.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * BroadcastReceiver für Zeitänderungen
 * 
 * Plant alle Reminder-Worker neu nach:
 * - Systemzeitänderung (TIME_SET, TIME_CHANGED)
 * - Zeitzone-Wechsel (TIMEZONE_CHANGED)
 * 
 * Dies stellt sicher, dass Reminder auch nach Zeitänderungen korrekt funktionieren.
 */
@AndroidEntryPoint
class TimeChangeReceiver : BroadcastReceiver() {
    
    @Inject
    lateinit var reminderScheduler: ReminderScheduler
    
    @Inject
    lateinit var logger: RingBufferLogger
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            "android.intent.action.TIME_SET" -> {
                
                // Reschedule alle Reminder-Worker
                val scope = CoroutineScope(Dispatchers.IO)
                scope.launch {
                    try {
                        logger.i("TimeChangeReceiver", "Zeitänderung erkannt: ${intent.action}, Reschedule Reminder")
                        reminderScheduler.scheduleAll()
                    } catch (e: Exception) {
                        logger.e("TimeChangeReceiver", "Fehler beim Reschedule nach Zeitänderung", e)
                    }
                }
            }
        }
    }
}
