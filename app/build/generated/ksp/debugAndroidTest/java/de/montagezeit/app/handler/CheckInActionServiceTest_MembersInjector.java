package de.montagezeit.app.handler;

import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import de.montagezeit.app.data.local.dao.WorkEntryDao;
import de.montagezeit.app.domain.usecase.RecordEveningCheckIn;
import de.montagezeit.app.domain.usecase.RecordMorningCheckIn;
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
public final class CheckInActionServiceTest_MembersInjector implements MembersInjector<CheckInActionServiceTest> {
  private final Provider<WorkEntryDao> workEntryDaoProvider;

  private final Provider<RecordMorningCheckIn> recordMorningCheckInProvider;

  private final Provider<RecordEveningCheckIn> recordEveningCheckInProvider;

  public CheckInActionServiceTest_MembersInjector(Provider<WorkEntryDao> workEntryDaoProvider,
      Provider<RecordMorningCheckIn> recordMorningCheckInProvider,
      Provider<RecordEveningCheckIn> recordEveningCheckInProvider) {
    this.workEntryDaoProvider = workEntryDaoProvider;
    this.recordMorningCheckInProvider = recordMorningCheckInProvider;
    this.recordEveningCheckInProvider = recordEveningCheckInProvider;
  }

  public static MembersInjector<CheckInActionServiceTest> create(
      Provider<WorkEntryDao> workEntryDaoProvider,
      Provider<RecordMorningCheckIn> recordMorningCheckInProvider,
      Provider<RecordEveningCheckIn> recordEveningCheckInProvider) {
    return new CheckInActionServiceTest_MembersInjector(workEntryDaoProvider, recordMorningCheckInProvider, recordEveningCheckInProvider);
  }

  @Override
  public void injectMembers(CheckInActionServiceTest instance) {
    injectWorkEntryDao(instance, workEntryDaoProvider.get());
    injectRecordMorningCheckIn(instance, recordMorningCheckInProvider.get());
    injectRecordEveningCheckIn(instance, recordEveningCheckInProvider.get());
  }

  @InjectedFieldSignature("de.montagezeit.app.handler.CheckInActionServiceTest.workEntryDao")
  public static void injectWorkEntryDao(CheckInActionServiceTest instance,
      WorkEntryDao workEntryDao) {
    instance.workEntryDao = workEntryDao;
  }

  @InjectedFieldSignature("de.montagezeit.app.handler.CheckInActionServiceTest.recordMorningCheckIn")
  public static void injectRecordMorningCheckIn(CheckInActionServiceTest instance,
      RecordMorningCheckIn recordMorningCheckIn) {
    instance.recordMorningCheckIn = recordMorningCheckIn;
  }

  @InjectedFieldSignature("de.montagezeit.app.handler.CheckInActionServiceTest.recordEveningCheckIn")
  public static void injectRecordEveningCheckIn(CheckInActionServiceTest instance,
      RecordEveningCheckIn recordEveningCheckIn) {
    instance.recordEveningCheckIn = recordEveningCheckIn;
  }
}
