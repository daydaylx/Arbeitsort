package de.montagezeit.app.receiver;

import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import de.montagezeit.app.logging.RingBufferLogger;
import de.montagezeit.app.work.ReminderScheduler;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class TimeChangeReceiver_MembersInjector implements MembersInjector<TimeChangeReceiver> {
  private final Provider<ReminderScheduler> reminderSchedulerProvider;

  private final Provider<RingBufferLogger> loggerProvider;

  public TimeChangeReceiver_MembersInjector(Provider<ReminderScheduler> reminderSchedulerProvider,
      Provider<RingBufferLogger> loggerProvider) {
    this.reminderSchedulerProvider = reminderSchedulerProvider;
    this.loggerProvider = loggerProvider;
  }

  public static MembersInjector<TimeChangeReceiver> create(
      Provider<ReminderScheduler> reminderSchedulerProvider,
      Provider<RingBufferLogger> loggerProvider) {
    return new TimeChangeReceiver_MembersInjector(reminderSchedulerProvider, loggerProvider);
  }

  @Override
  public void injectMembers(TimeChangeReceiver instance) {
    injectReminderScheduler(instance, reminderSchedulerProvider.get());
    injectLogger(instance, loggerProvider.get());
  }

  @InjectedFieldSignature("de.montagezeit.app.receiver.TimeChangeReceiver.reminderScheduler")
  public static void injectReminderScheduler(TimeChangeReceiver instance,
      ReminderScheduler reminderScheduler) {
    instance.reminderScheduler = reminderScheduler;
  }

  @InjectedFieldSignature("de.montagezeit.app.receiver.TimeChangeReceiver.logger")
  public static void injectLogger(TimeChangeReceiver instance, RingBufferLogger logger) {
    instance.logger = logger;
  }
}
