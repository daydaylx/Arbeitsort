package de.montagezeit.app.notification;

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
public final class ReminderNotificationManager_Factory implements Factory<ReminderNotificationManager> {
  private final Provider<Context> contextProvider;

  public ReminderNotificationManager_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public ReminderNotificationManager get() {
    return newInstance(contextProvider.get());
  }

  public static ReminderNotificationManager_Factory create(Provider<Context> contextProvider) {
    return new ReminderNotificationManager_Factory(contextProvider);
  }

  public static ReminderNotificationManager newInstance(Context context) {
    return new ReminderNotificationManager(context);
  }
}
