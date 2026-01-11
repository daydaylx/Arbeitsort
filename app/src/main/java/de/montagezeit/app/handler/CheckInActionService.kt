package de.montagezeit.app.handler

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import de.montagezeit.app.MainActivity
import de.montagezeit.app.R
import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.domain.usecase.RecordEveningCheckIn
import de.montagezeit.app.domain.usecase.RecordMorningCheckIn
import de.montagezeit.app.notification.ReminderActions
import de.montagezeit.app.notification.ReminderNotificationManager
import de.montagezeit.app.work.ReminderLaterWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Data
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * ForegroundService für Check-In Actions aus Notifications
 * 
 * Verwendet ForegroundService statt BroadcastReceiver, weil:
 * - Location-Requests mit Timeout (15s+) länger dauern können als BroadcastReceiver lebt
 * - ForegroundService hat höhere Priorität und wird vom System nicht abgebrochen
 */
@AndroidEntryPoint
class CheckInActionService : Service() {
    
    @Inject
    lateinit var workEntryDao: WorkEntryDao
    
    @Inject
    lateinit var recordMorningCheckIn: RecordMorningCheckIn
    
    @Inject
    lateinit var recordEveningCheckIn: RecordEveningCheckIn
    
    @Inject
    lateinit var notificationManager: ReminderNotificationManager
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    
    companion object {
        private const val NOTIFICATION_ID = 3000
        private const val CHANNEL_ID = "check_in_action_service"
        private const val CHANNEL_NAME = "Check-In Aktionen"
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ReminderActions.ACTION_MORNING_CHECK_IN_WITH_LOCATION,
            ReminderActions.ACTION_MORNING_CHECK_IN_WITHOUT_LOCATION -> {
                val dateStr = intent.getStringExtra(ReminderActions.EXTRA_DATE)
                val date = dateStr?.let { LocalDate.parse(it) } ?: LocalDate.now()
                val forceWithoutLocation = intent.action == ReminderActions.ACTION_MORNING_CHECK_IN_WITHOUT_LOCATION
                
                startForeground(NOTIFICATION_ID, createProcessingNotification("Morgendlicher Check-in..."))
                
                serviceScope.launch {
                    try {
                        recordMorningCheckIn(date, forceWithoutLocation)
                        showToast(R.string.toast_check_in_success)
                        notificationManager.cancelMorningReminder()
                    } catch (e: Exception) {
                        showToast(R.string.toast_check_in_error)
                    } finally {
                        stopSelf()
                    }
                }
            }
            
            ReminderActions.ACTION_EVENING_CHECK_IN_WITH_LOCATION,
            ReminderActions.ACTION_EVENING_CHECK_IN_WITHOUT_LOCATION -> {
                val dateStr = intent.getStringExtra(ReminderActions.EXTRA_DATE)
                val date = dateStr?.let { LocalDate.parse(it) } ?: LocalDate.now()
                val forceWithoutLocation = intent.action == ReminderActions.ACTION_EVENING_CHECK_IN_WITHOUT_LOCATION
                
                startForeground(NOTIFICATION_ID, createProcessingNotification("Abendlicher Check-in..."))
                
                serviceScope.launch {
                    try {
                        recordEveningCheckIn(date, forceWithoutLocation)
                        showToast(R.string.toast_check_in_success)
                        notificationManager.cancelEveningReminder()
                    } catch (e: Exception) {
                        showToast(R.string.toast_check_in_error)
                    } finally {
                        stopSelf()
                    }
                }
            }
            
            ReminderActions.ACTION_EDIT_ENTRY -> {
                val dateStr = intent.getStringExtra(ReminderActions.EXTRA_DATE)
                val date = dateStr?.let { LocalDate.parse(it) } ?: LocalDate.now()
                val editIntent = Intent(this, MainActivity::class.java).apply {
                    action = ReminderActions.ACTION_EDIT_ENTRY
                    putExtra(ReminderActions.EXTRA_DATE, date.toString())
                    putExtra(ReminderActions.EXTRA_ACTION_TYPE, ReminderActions.ACTION_EDIT_ENTRY)
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                    )
                }
                startActivity(editIntent)
                notificationManager.cancelFallbackReminder()
                stopSelf()
            }
            
            ReminderActions.ACTION_REMIND_LATER -> {
                val dateStr = intent.getStringExtra(ReminderActions.EXTRA_DATE)
                val date = dateStr?.let { LocalDate.parse(it) } ?: LocalDate.now()
                val hoursLater = intent.getIntExtra(ReminderActions.EXTRA_HOURS_LATER, 1)
                
                // Entferne aktuelle Notification
                notificationManager.cancelMorningReminder()
                notificationManager.cancelEveningReminder()
                notificationManager.cancelFallbackReminder()
                
                // Plane neue Notification für später
                serviceScope.launch {
                    scheduleReminderLater(date, hoursLater)
                }
                
                stopSelf()
            }
        }
        
        return START_NOT_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
    }
    
    /**
     * Erstellt den Notification Channel für den ForegroundService
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notification für Check-In Aktionen"
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Erstellt eine Processing-Notification für den ForegroundService
     */
    private fun createProcessingNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MontageZeit")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    /**
     * Zeigt einen Toast an
     */
    private fun showToast(message: String) {
        serviceScope.launch(Dispatchers.Main) {
            Toast.makeText(this@CheckInActionService, message, Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showToast(resId: Int) {
        serviceScope.launch(Dispatchers.Main) {
            Toast.makeText(this@CheckInActionService, resId, Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Plant eine Reminder-Notification für später
     */
    private suspend fun scheduleReminderLater(date: LocalDate, hoursLater: Int) {
        val inputData = Data.Builder()
            .putString("date", date.toString())
            .putInt("hours_later", hoursLater)
            .build()
        
        val workRequest = OneTimeWorkRequestBuilder<ReminderLaterWorker>()
            .setInitialDelay(hoursLater.toLong(), TimeUnit.HOURS)
            .setInputData(inputData)
            .build()
        
        WorkManager.getInstance(this).enqueue(workRequest)
    }
}
