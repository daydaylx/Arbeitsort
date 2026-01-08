package de.montagezeit.app.data.preferences;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class ReminderSettingsManager_Factory implements Factory<ReminderSettingsManager> {
  private final Provider<Context> contextProvider;

  public ReminderSettingsManager_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public ReminderSettingsManager get() {
    return newInstance(contextProvider.get());
  }

  public static ReminderSettingsManager_Factory create(Provider<Context> contextProvider) {
    return new ReminderSettingsManager_Factory(contextProvider);
  }

  public static ReminderSettingsManager newInstance(Context context) {
    return new ReminderSettingsManager(context);
  }
}
