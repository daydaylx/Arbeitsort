package de.montagezeit.app.receiver;

import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
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
public final class BootReceiver_MembersInjector implements MembersInjector<BootReceiver> {
  private final Provider<ReminderScheduler> reminderSchedulerProvider;

  public BootReceiver_MembersInjector(Provider<ReminderScheduler> reminderSchedulerProvider) {
    this.reminderSchedulerProvider = reminderSchedulerProvider;
  }

  public static MembersInjector<BootReceiver> create(
      Provider<ReminderScheduler> reminderSchedulerProvider) {
    return new BootReceiver_MembersInjector(reminderSchedulerProvider);
  }

  @Override
  public void injectMembers(BootReceiver instance) {
    injectReminderScheduler(instance, reminderSchedulerProvider.get());
  }

  @InjectedFieldSignature("de.montagezeit.app.receiver.BootReceiver.reminderScheduler")
  public static void injectReminderScheduler(BootReceiver instance,
      ReminderScheduler reminderScheduler) {
    instance.reminderScheduler = reminderScheduler;
  }
}
