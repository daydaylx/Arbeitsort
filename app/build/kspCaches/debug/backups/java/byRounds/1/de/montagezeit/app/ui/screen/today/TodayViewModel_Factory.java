package de.montagezeit.app.ui.screen.today;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import de.montagezeit.app.data.local.dao.WorkEntryDao;
import de.montagezeit.app.domain.usecase.RecordEveningCheckIn;
import de.montagezeit.app.domain.usecase.RecordMorningCheckIn;
import de.montagezeit.app.domain.usecase.UpdateEntry;
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
public final class TodayViewModel_Factory implements Factory<TodayViewModel> {
  private final Provider<WorkEntryDao> workEntryDaoProvider;

  private final Provider<RecordMorningCheckIn> recordMorningCheckInProvider;

  private final Provider<RecordEveningCheckIn> recordEveningCheckInProvider;

  private final Provider<UpdateEntry> updateEntryProvider;

  public TodayViewModel_Factory(Provider<WorkEntryDao> workEntryDaoProvider,
      Provider<RecordMorningCheckIn> recordMorningCheckInProvider,
      Provider<RecordEveningCheckIn> recordEveningCheckInProvider,
      Provider<UpdateEntry> updateEntryProvider) {
    this.workEntryDaoProvider = workEntryDaoProvider;
    this.recordMorningCheckInProvider = recordMorningCheckInProvider;
    this.recordEveningCheckInProvider = recordEveningCheckInProvider;
    this.updateEntryProvider = updateEntryProvider;
  }

  @Override
  public TodayViewModel get() {
    return newInstance(workEntryDaoProvider.get(), recordMorningCheckInProvider.get(), recordEveningCheckInProvider.get(), updateEntryProvider.get());
  }

  public static TodayViewModel_Factory create(Provider<WorkEntryDao> workEntryDaoProvider,
      Provider<RecordMorningCheckIn> recordMorningCheckInProvider,
      Provider<RecordEveningCheckIn> recordEveningCheckInProvider,
      Provider<UpdateEntry> updateEntryProvider) {
    return new TodayViewModel_Factory(workEntryDaoProvider, recordMorningCheckInProvider, recordEveningCheckInProvider, updateEntryProvider);
  }

  public static TodayViewModel newInstance(WorkEntryDao workEntryDao,
      RecordMorningCheckIn recordMorningCheckIn, RecordEveningCheckIn recordEveningCheckIn,
      UpdateEntry updateEntry) {
    return new TodayViewModel(workEntryDao, recordMorningCheckIn, recordEveningCheckIn, updateEntry);
  }
}
