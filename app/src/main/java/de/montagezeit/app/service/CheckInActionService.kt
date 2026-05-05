package de.montagezeit.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import de.montagezeit.app.R
import de.montagezeit.app.domain.usecase.CheckInEntryBuilder
import de.montagezeit.app.domain.usecase.RecordCheckIn
import de.montagezeit.app.domain.usecase.ConfirmWorkDay
import de.montagezeit.app.data.preferences.ReminderFlagsStore
import de.montagezeit.app.domain.usecase.ConfirmOffDay
import de.montagezeit.app.domain.util.ConfirmationSources
import de.montagezeit.app.notification.ConfirmationReminderLimiter
import de.montagezeit.app.notification.ReminderActions
import de.montagezeit.app.notification.ReminderNotificationManager
import de.montagezeit.app.work.ReminderLaterWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import de.montagezeit.app.work.ReminderType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.abs

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
    lateinit var recordCheckIn: RecordCheckIn

    @Inject
    lateinit var notificationManager: ReminderNotificationManager

    @Inject
    lateinit var confirmWorkDay: ConfirmWorkDay

    @Inject
    lateinit var confirmOffDay: ConfirmOffDay

    @Inject
    lateinit var reminderFlagsStore: ReminderFlagsStore

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val operationMutex = Mutex()

    companion object {
        private const val NOTIFICATION_ID = 3000
        private const val CHANNEL_ID = "check_in_action_service"
        private const val CONFIRMATION_REMINDER_PREFS = "confirmation_reminder_count"
        private const val CONFIRMATION_REMINDER_MAX = 2
        private const val CONFIRMATION_REMIND_LATER_MINUTES = 60

        /**
         * Parst ein Datums-String mit Fehlerbehandlung.
         * Bei Parse-Fehler oder wenn das Datum >1 Tag von [now] abweicht, wird `null` geliefert.
         */
        internal fun parseDateFromExtra(
            dateStr: String?,
            now: LocalDate = LocalDate.now()
        ): LocalDate? {
            if (dateStr.isNullOrBlank()) return null
            return try {
                val parsed = LocalDate.parse(dateStr)
                if (abs(ChronoUnit.DAYS.between(parsed, now)) > 1) null else parsed
            } catch (_: Exception) {
                null
            }
        }

        internal fun processingNotificationTextRes(action: String?): Int? = when (action) {
            ReminderActions.ACTION_MORNING_CHECK_IN -> R.string.notification_processing_morning
            ReminderActions.ACTION_EVENING_CHECK_IN -> R.string.notification_processing_evening
            ReminderActions.ACTION_REMIND_LATER,
            ReminderActions.ACTION_REMIND_LATER_CONFIRMATION -> R.string.notification_processing_remind_later
            ReminderActions.ACTION_CONFIRM_WORK_DAY,
            ReminderActions.ACTION_CONFIRM_OFF_DAY -> R.string.notification_processing_confirm_day
            ReminderActions.ACTION_MARK_DAY_OFF -> R.string.notification_processing_mark_day_off
            else -> null
        }

        internal fun foregroundNotificationTextRes(action: String?): Int =
            processingNotificationTextRes(action) ?: R.string.notification_processing_generic
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ReminderActions.ACTION_MORNING_CHECK_IN -> {
                val date = parseActionDate(intent) ?: return START_NOT_STICKY

                startForegroundForAction(intent.action)

                serviceScope.launch {
                    operationMutex.withLock {
                        try {
                            recordCheckIn(date, CheckInEntryBuilder.Snapshot.MORNING)
                            showToast(R.string.toast_check_in_success)
                            notificationManager.cancelMorningReminder()
                        } catch (e: IllegalStateException) {
                            notificationManager.cancelMorningReminder()
                            showToast(R.string.toast_check_in_error)
                        } catch (e: Exception) {
                            showToast(R.string.toast_check_in_error)
                        } finally {
                            stopSelf()
                        }
                    }
                }
            }

            ReminderActions.ACTION_EVENING_CHECK_IN -> {
                val date = parseActionDate(intent) ?: return START_NOT_STICKY

                startForegroundForAction(intent.action)

                serviceScope.launch {
                    operationMutex.withLock {
                        try {
                            recordCheckIn(date, CheckInEntryBuilder.Snapshot.EVENING)
                            showToast(R.string.toast_check_in_success)
                            notificationManager.cancelEveningReminder()
                        } catch (e: IllegalStateException) {
                            notificationManager.cancelEveningReminder()
                            showToast(R.string.toast_check_in_error)
                        } catch (e: Exception) {
                            showToast(R.string.toast_check_in_error)
                        } finally {
                            stopSelf()
                        }
                    }
                }
            }

            ReminderActions.ACTION_REMIND_LATER -> {
                val date = parseActionDate(intent) ?: return START_NOT_STICKY
                val minutesLater = intent.getIntExtra(ReminderActions.EXTRA_MINUTES_LATER, -1)
                val hoursLater = intent.getIntExtra(ReminderActions.EXTRA_HOURS_LATER, 1)
                val delayMinutes = if (minutesLater > 0) minutesLater.toLong() else hoursLater * 60L
                val reminderTypeRaw = intent.getStringExtra(ReminderActions.EXTRA_REMINDER_TYPE)

                startForegroundForAction(intent.action)

                // Entferne nur den spezifischen Reminder-Typ der gesnoozed wird
                when (reminderTypeRaw) {
                    ReminderType.MORNING.name -> notificationManager.cancelMorningReminder()
                    ReminderType.EVENING.name -> notificationManager.cancelEveningReminder()
                    ReminderType.FALLBACK.name -> notificationManager.cancelFallbackReminder()
                    ReminderType.DAILY.name -> notificationManager.cancelDailyReminder()
                    else -> {
                        notificationManager.cancelMorningReminder()
                        notificationManager.cancelEveningReminder()
                        notificationManager.cancelFallbackReminder()
                        notificationManager.cancelDailyReminder()
                    }
                }

                serviceScope.launch {
                    operationMutex.withLock {
                        try {
                            scheduleReminderLater(
                                date = date,
                                delayMinutes = delayMinutes,
                                reminderType = reminderTypeRaw
                            )
                        } finally {
                            stopSelf()
                        }
                    }
                }
            }

            ReminderActions.ACTION_CONFIRM_WORK_DAY -> {
                val date = parseActionDate(intent) ?: return START_NOT_STICKY
                val source = intent.getStringExtra(ReminderActions.EXTRA_CONFIRMATION_SOURCE)
                    ?: ConfirmationSources.NOTIFICATION

                startForegroundForAction(intent.action)

                serviceScope.launch {
                    operationMutex.withLock {
                        try {
                            confirmWorkDay(date, source = source)
                            showToast(R.string.toast_work_day_confirmed)
                            confirmationLimiter().reset(date)
                            notificationManager.cancelDailyReminder()
                        } catch (e: Exception) {
                            showToast(R.string.toast_confirm_day_failed)
                        } finally {
                            stopSelf()
                        }
                    }
                }
            }

            ReminderActions.ACTION_CONFIRM_OFF_DAY -> {
                val date = parseActionDate(intent) ?: return START_NOT_STICKY
                val source = intent.getStringExtra(ReminderActions.EXTRA_CONFIRMATION_SOURCE)
                    ?: ConfirmationSources.NOTIFICATION

                startForegroundForAction(intent.action)

                serviceScope.launch {
                    operationMutex.withLock {
                        try {
                            confirmOffDay(date, source = source)
                            showToast(R.string.toast_off_day_confirmed)
                            confirmationLimiter().reset(date)
                            notificationManager.cancelDailyReminder()
                        } catch (e: Exception) {
                            showToast(R.string.toast_confirm_day_failed)
                        } finally {
                            stopSelf()
                        }
                    }
                }
            }

            ReminderActions.ACTION_REMIND_LATER_CONFIRMATION -> {
                val date = parseActionDate(intent) ?: return START_NOT_STICKY
                val limiter = confirmationLimiter()

                // Prüfe Reminder Counter (max 2x pro Tag)
                // Notification bewusst NICHT canceln: Nutzer soll sie weiter sehen können
                if (!limiter.canSchedule(date)) {
                    showToast(R.string.toast_reminder_limit_reached)
                    stopSelf()
                    return START_NOT_STICKY
                }

                // Entferne aktuelle Notification
                notificationManager.cancelDailyReminder()

                // Inkrementiere Counter
                limiter.increment(date)

                // Plane neue Notification in +60 Minuten
                startForegroundForAction(intent.action)
                serviceScope.launch {
                    operationMutex.withLock {
                        try {
                            scheduleConfirmationReminderLater(date, CONFIRMATION_REMIND_LATER_MINUTES)
                            showToast(R.string.toast_reminder_set_later)
                        } finally {
                            stopSelf()
                        }
                    }
                }
            }

            ReminderActions.ACTION_MARK_DAY_OFF -> {
                val date = parseActionDate(intent) ?: return START_NOT_STICKY

                startForegroundForAction(intent.action)

                serviceScope.launch {
                    operationMutex.withLock {
                        try {
                            confirmOffDay(date, source = ConfirmationSources.NOTIFICATION)
                            showToast(R.string.toast_day_marked_off)
                            markReminderFlags(date)
                            notificationManager.cancelMorningReminder()
                            notificationManager.cancelEveningReminder()
                            notificationManager.cancelFallbackReminder()
                            notificationManager.cancelDailyReminder()
                        } catch (e: Exception) {
                            showToast(R.string.toast_mark_day_off_failed)
                        } finally {
                            stopSelf()
                        }
                    }
                }
            }
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun parseActionDate(intent: Intent): LocalDate? {
        val parsedDate = parseDateFromExtra(intent.getStringExtra(ReminderActions.EXTRA_DATE))
        if (parsedDate != null) {
            return parsedDate
        }
        showToast(R.string.toast_reminder_action_expired)
        stopSelf()
        return null
    }

    /**
     * Erstellt den Notification Channel für den ForegroundService
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_check_in_actions),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_check_in_actions_description)
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
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun startForegroundForAction(action: String?) {
        val textRes = foregroundNotificationTextRes(action)
        startForeground(NOTIFICATION_ID, createProcessingNotification(getString(textRes)))
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
    private suspend fun scheduleReminderLater(
        date: LocalDate,
        delayMinutes: Long,
        reminderType: String?
    ) {
        val inputData = Data.Builder()
            .putString("date", date.toString())
            .putString(ReminderActions.EXTRA_REMINDER_TYPE, reminderType)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<ReminderLaterWorker>()
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .setInputData(inputData)
            .build()

        val uniqueName = "reminder_later_${date}_${reminderType ?: "UNKNOWN"}"
        WorkManager.getInstance(this).enqueueUniqueWork(
            uniqueName,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    private suspend fun markReminderFlags(date: LocalDate) {
        reminderFlagsStore.setAllReminded(date)
    }

    private fun confirmationLimiter(): ConfirmationReminderLimiter {
        val prefs = getSharedPreferences(CONFIRMATION_REMINDER_PREFS, Context.MODE_PRIVATE)
        val limiter = ConfirmationReminderLimiter(prefs, CONFIRMATION_REMINDER_MAX)
        limiter.cleanup(LocalDate.now())
        return limiter
    }

    private fun scheduleConfirmationReminderLater(date: LocalDate, minutesLater: Int) {
        val inputData = Data.Builder()
            .putString("date", date.toString())
            .putInt("minutes_later", minutesLater)
            .putString(ReminderActions.EXTRA_REMINDER_TYPE, ReminderType.DAILY.name)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<ReminderLaterWorker>()
            .setInitialDelay(minutesLater.toLong(), TimeUnit.MINUTES)
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(this).enqueueUniqueWork(
            "reminder_later_confirmation_$date",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }
}
