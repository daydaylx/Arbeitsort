package de.montagezeit.app

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import de.montagezeit.app.data.local.database.AppDatabase
import de.montagezeit.app.work.ReminderScheduler
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class MontageZeitApp : Application(), Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var appDatabase: AppDatabase

    @Inject
    lateinit var reminderScheduler: ReminderScheduler

    private val applicationScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO +
            CoroutineExceptionHandler { _, e ->
                Log.e("MontageZeitApp", "Unerwarteter Fehler in applicationScope", e)
            }
    )

    override val workManagerConfiguration: Configuration
        get() = if (::workerFactory.isInitialized) {
            Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .build()
        } else {
            Configuration.Builder().build()
        }

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            try {
                appDatabase.openHelper.writableDatabase
                reminderScheduler.scheduleAll()
            } catch (e: Exception) {
                Log.e("MontageZeitApp", "Fehler beim App-Start (DB/Reminder)", e)
            }
        }
    }

    override fun onTerminate() {
        applicationScope.cancel()
        super.onTerminate()
    }
}
