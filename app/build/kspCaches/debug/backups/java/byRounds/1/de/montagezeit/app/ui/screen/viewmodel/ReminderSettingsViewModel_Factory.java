package de.montagezeit.app.ui.screen.viewmodel;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
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
public final class ReminderSettingsViewModel_Factory implements Factory<ReminderSettingsViewModel> {
  private final Provider<ReminderSettingsManager> settingsManagerProvider;

  public ReminderSettingsViewModel_Factory(
      Provider<ReminderSettingsManager> settingsManagerProvider) {
    this.settingsManagerProvider = settingsManagerProvider;
  }

  @Override
  public ReminderSettingsViewModel get() {
    return newInstance(settingsManagerProvider.get());
  }

  public static ReminderSettingsViewModel_Factory create(
      Provider<ReminderSettingsManager> settingsManagerProvider) {
    return new ReminderSettingsViewModel_Factory(settingsManagerProvider);
  }

  public static ReminderSettingsViewModel newInstance(ReminderSettingsManager settingsManager) {
    return new ReminderSettingsViewModel(settingsManager);
  }
}
