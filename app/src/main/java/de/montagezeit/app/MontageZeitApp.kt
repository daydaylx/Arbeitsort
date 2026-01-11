package de.montagezeit.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import de.montagezeit.app.data.local.database.AppDatabase
import de.montagezeit.app.work.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class MontageZeitApp : Application(), Configuration.Provider {
    @Inject
    lateinit var appDatabase: AppDatabase

    @Inject
    lateinit var reminderScheduler: ReminderScheduler

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    
    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.IO).launch {
            appDatabase.openHelper.writableDatabase
            reminderScheduler.scheduleAll()
        }
    }
}
