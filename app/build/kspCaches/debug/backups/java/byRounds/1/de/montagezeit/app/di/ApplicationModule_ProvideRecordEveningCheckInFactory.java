package de.montagezeit.app.di;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import de.montagezeit.app.data.local.dao.WorkEntryDao;
import de.montagezeit.app.data.location.LocationProvider;
import de.montagezeit.app.data.preferences.ReminderSettingsManager;
import de.montagezeit.app.domain.location.LocationCalculator;
import de.montagezeit.app.domain.usecase.RecordEveningCheckIn;
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
public final class ApplicationModule_ProvideRecordEveningCheckInFactory implements Factory<RecordEveningCheckIn> {
  private final Provider<WorkEntryDao> workEntryDaoProvider;

  private final Provider<LocationProvider> locationProvider;

  private final Provider<LocationCalculator> locationCalculatorProvider;

  private final Provider<ReminderSettingsManager> reminderSettingsManagerProvider;

  public ApplicationModule_ProvideRecordEveningCheckInFactory(
      Provider<WorkEntryDao> workEntryDaoProvider, Provider<LocationProvider> locationProvider,
      Provider<LocationCalculator> locationCalculatorProvider,
      Provider<ReminderSettingsManager> reminderSettingsManagerProvider) {
    this.workEntryDaoProvider = workEntryDaoProvider;
    this.locationProvider = locationProvider;
    this.locationCalculatorProvider = locationCalculatorProvider;
    this.reminderSettingsManagerProvider = reminderSettingsManagerProvider;
  }

  @Override
  public RecordEveningCheckIn get() {
    return provideRecordEveningCheckIn(workEntryDaoProvider.get(), locationProvider.get(), locationCalculatorProvider.get(), reminderSettingsManagerProvider.get());
  }

  public static ApplicationModule_ProvideRecordEveningCheckInFactory create(
      Provider<WorkEntryDao> workEntryDaoProvider, Provider<LocationProvider> locationProvider,
      Provider<LocationCalculator> locationCalculatorProvider,
      Provider<ReminderSettingsManager> reminderSettingsManagerProvider) {
    return new ApplicationModule_ProvideRecordEveningCheckInFactory(workEntryDaoProvider, locationProvider, locationCalculatorProvider, reminderSettingsManagerProvider);
  }

  public static RecordEveningCheckIn provideRecordEveningCheckIn(WorkEntryDao workEntryDao,
      LocationProvider locationProvider, LocationCalculator locationCalculator,
      ReminderSettingsManager reminderSettingsManager) {
    return Preconditions.checkNotNullFromProvides(ApplicationModule.INSTANCE.provideRecordEveningCheckIn(workEntryDao, locationProvider, locationCalculator, reminderSettingsManager));
  }
}
