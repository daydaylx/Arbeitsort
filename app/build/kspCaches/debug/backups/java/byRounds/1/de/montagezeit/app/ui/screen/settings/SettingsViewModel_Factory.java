package de.montagezeit.app.ui.screen.settings;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import de.montagezeit.app.data.local.dao.WorkEntryDao;
import de.montagezeit.app.data.preferences.ReminderSettingsManager;
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
public final class SettingsViewModel_Factory implements Factory<SettingsViewModel> {
  private final Provider<ReminderSettingsManager> reminderSettingsManagerProvider;

  private final Provider<WorkEntryDao> workEntryDaoProvider;

  public SettingsViewModel_Factory(
      Provider<ReminderSettingsManager> reminderSettingsManagerProvider,
      Provider<WorkEntryDao> workEntryDaoProvider) {
    this.reminderSettingsManagerProvider = reminderSettingsManagerProvider;
    this.workEntryDaoProvider = workEntryDaoProvider;
  }

  @Override
  public SettingsViewModel get() {
    return newInstance(reminderSettingsManagerProvider.get(), workEntryDaoProvider.get());
  }

  public static SettingsViewModel_Factory create(
      Provider<ReminderSettingsManager> reminderSettingsManagerProvider,
      Provider<WorkEntryDao> workEntryDaoProvider) {
    return new SettingsViewModel_Factory(reminderSettingsManagerProvider, workEntryDaoProvider);
  }

  public static SettingsViewModel newInstance(ReminderSettingsManager reminderSettingsManager,
      WorkEntryDao workEntryDao) {
    return new SettingsViewModel(reminderSettingsManager, workEntryDao);
  }
}
