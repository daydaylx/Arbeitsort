package de.montagezeit.app.handler;

import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import de.montagezeit.app.data.local.dao.WorkEntryDao;
import de.montagezeit.app.domain.usecase.RecordEveningCheckIn;
import de.montagezeit.app.domain.usecase.RecordMorningCheckIn;
import de.montagezeit.app.notification.ReminderNotificationManager;
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
public final class CheckInActionService_MembersInjector implements MembersInjector<CheckInActionService> {
  private final Provider<WorkEntryDao> workEntryDaoProvider;

  private final Provider<RecordMorningCheckIn> recordMorningCheckInProvider;

  private final Provider<RecordEveningCheckIn> recordEveningCheckInProvider;

  private final Provider<ReminderNotificationManager> notificationManagerProvider;

  public CheckInActionService_MembersInjector(Provider<WorkEntryDao> workEntryDaoProvider,
      Provider<RecordMorningCheckIn> recordMorningCheckInProvider,
      Provider<RecordEveningCheckIn> recordEveningCheckInProvider,
      Provider<ReminderNotificationManager> notificationManagerProvider) {
    this.workEntryDaoProvider = workEntryDaoProvider;
    this.recordMorningCheckInProvider = recordMorningCheckInProvider;
    this.recordEveningCheckInProvider = recordEveningCheckInProvider;
    this.notificationManagerProvider = notificationManagerProvider;
  }

  public static MembersInjector<CheckInActionService> create(
      Provider<WorkEntryDao> workEntryDaoProvider,
      Provider<RecordMorningCheckIn> recordMorningCheckInProvider,
      Provider<RecordEveningCheckIn> recordEveningCheckInProvider,
      Provider<ReminderNotificationManager> notificationManagerProvider) {
    return new CheckInActionService_MembersInjector(workEntryDaoProvider, recordMorningCheckInProvider, recordEveningCheckInProvider, notificationManagerProvider);
  }

  @Override
  public void injectMembers(CheckInActionService instance) {
    injectWorkEntryDao(instance, workEntryDaoProvider.get());
    injectRecordMorningCheckIn(instance, recordMorningCheckInProvider.get());
    injectRecordEveningCheckIn(instance, recordEveningCheckInProvider.get());
    injectNotificationManager(instance, notificationManagerProvider.get());
  }

  @InjectedFieldSignature("de.montagezeit.app.handler.CheckInActionService.workEntryDao")
  public static void injectWorkEntryDao(CheckInActionService instance, WorkEntryDao workEntryDao) {
    instance.workEntryDao = workEntryDao;
  }

  @InjectedFieldSignature("de.montagezeit.app.handler.CheckInActionService.recordMorningCheckIn")
  public static void injectRecordMorningCheckIn(CheckInActionService instance,
      RecordMorningCheckIn recordMorningCheckIn) {
    instance.recordMorningCheckIn = recordMorningCheckIn;
  }

  @InjectedFieldSignature("de.montagezeit.app.handler.CheckInActionService.recordEveningCheckIn")
  public static void injectRecordEveningCheckIn(CheckInActionService instance,
      RecordEveningCheckIn recordEveningCheckIn) {
    instance.recordEveningCheckIn = recordEveningCheckIn;
  }

  @InjectedFieldSignature("de.montagezeit.app.handler.CheckInActionService.notificationManager")
  public static void injectNotificationManager(CheckInActionService instance,
      ReminderNotificationManager notificationManager) {
    instance.notificationManager = notificationManager;
  }
}
