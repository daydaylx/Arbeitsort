package de.montagezeit.app.work

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class ReminderPeriodicWorkSpec(
    val uniqueWorkName: String,
    val tag: String,
    val reminderType: ReminderType,
    val initialDelayMillis: Long,
    val repeatInterval: Long,
    val repeatIntervalTimeUnit: TimeUnit,
    val flexInterval: Long? = null,
    val flexIntervalTimeUnit: TimeUnit = TimeUnit.MINUTES
)

@Singleton
class ReminderWorkEnqueuer @Inject constructor(
    private val workManager: WorkManager
) {
    fun enqueue(spec: ReminderPeriodicWorkSpec) {
        workManager.enqueueUniquePeriodicWork(
            spec.uniqueWorkName,
            ExistingPeriodicWorkPolicy.UPDATE,
            spec.toWorkRequest()
        )
    }

    fun cancel(uniqueWorkName: String) {
        workManager.cancelUniqueWork(uniqueWorkName)
    }

    private fun ReminderPeriodicWorkSpec.toWorkRequest(): PeriodicWorkRequest {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val builder = if (flexInterval != null) {
            PeriodicWorkRequestBuilder<WindowCheckWorker>(
                repeatInterval = repeatInterval,
                repeatIntervalTimeUnit = repeatIntervalTimeUnit,
                flexTimeInterval = flexInterval,
                flexTimeIntervalUnit = flexIntervalTimeUnit
            )
        } else {
            PeriodicWorkRequestBuilder<WindowCheckWorker>(
                repeatInterval = repeatInterval,
                repeatIntervalTimeUnit = repeatIntervalTimeUnit
            )
        }

        return builder
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .addTag(tag)
            .setInputData(workDataOf(WindowCheckWorker.KEY_REMINDER_TYPE to reminderType.name))
            .build()
    }
}
