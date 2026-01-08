package de.montagezeit.app.work;

import android.content.Context;
import androidx.work.WorkerParameters;
import dagger.internal.DaggerGenerated;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import de.montagezeit.app.data.local.dao.WorkEntryDao;
import de.montagezeit.app.data.preferences.ReminderSettingsManager;
import de.montagezeit.app.notification.ReminderNotificationManager;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava"
})
public final class WindowCheckWorker_Factory {
  private final Provider<WorkEntryDao> workEntryDaoProvider;

  private final Provider<ReminderSettingsManager> reminderSettingsManagerProvider;

  private final Provider<ReminderNotificationManager> notificationManagerProvider;

  public WindowCheckWorker_Factory(Provider<WorkEntryDao> workEntryDaoProvider,
      Provider<ReminderSettingsManager> reminderSettingsManagerProvider,
      Provider<ReminderNotificationManager> notificationManagerProvider) {
    this.workEntryDaoProvider = workEntryDaoProvider;
    this.reminderSettingsManagerProvider = reminderSettingsManagerProvider;
    this.notificationManagerProvider = notificationManagerProvider;
  }

  public WindowCheckWorker get(Context context, WorkerParameters workerParams) {
    return newInstance(context, workerParams, workEntryDaoProvider.get(), reminderSettingsManagerProvider.get(), notificationManagerProvider.get());
  }

  public static WindowCheckWorker_Factory create(Provider<WorkEntryDao> workEntryDaoProvider,
      Provider<ReminderSettingsManager> reminderSettingsManagerProvider,
      Provider<ReminderNotificationManager> notificationManagerProvider) {
    return new WindowCheckWorker_Factory(workEntryDaoProvider, reminderSettingsManagerProvider, notificationManagerProvider);
  }

  public static WindowCheckWorker newInstance(Context context, WorkerParameters workerParams,
      WorkEntryDao workEntryDao, ReminderSettingsManager reminderSettingsManager,
      ReminderNotificationManager notificationManager) {
    return new WindowCheckWorker(context, workerParams, workEntryDao, reminderSettingsManager, notificationManager);
  }
}
