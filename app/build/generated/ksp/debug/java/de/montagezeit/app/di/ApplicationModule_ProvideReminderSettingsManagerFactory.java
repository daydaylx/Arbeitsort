package de.montagezeit.app.di;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import de.montagezeit.app.data.preferences.ReminderSettingsManager;
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
public final class ApplicationModule_ProvideReminderSettingsManagerFactory implements Factory<ReminderSettingsManager> {
  private final Provider<Context> contextProvider;

  public ApplicationModule_ProvideReminderSettingsManagerFactory(
      Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public ReminderSettingsManager get() {
    return provideReminderSettingsManager(contextProvider.get());
  }

  public static ApplicationModule_ProvideReminderSettingsManagerFactory create(
      Provider<Context> contextProvider) {
    return new ApplicationModule_ProvideReminderSettingsManagerFactory(contextProvider);
  }

  public static ReminderSettingsManager provideReminderSettingsManager(Context context) {
    return Preconditions.checkNotNullFromProvides(ApplicationModule.INSTANCE.provideReminderSettingsManager(context));
  }
}
