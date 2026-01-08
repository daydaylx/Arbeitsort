package de.montagezeit.app.di;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import de.montagezeit.app.data.local.dao.WorkEntryDao;
import de.montagezeit.app.domain.usecase.SetTravelEvent;
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
public final class ApplicationModule_ProvideSetTravelEventFactory implements Factory<SetTravelEvent> {
  private final Provider<WorkEntryDao> workEntryDaoProvider;

  public ApplicationModule_ProvideSetTravelEventFactory(
      Provider<WorkEntryDao> workEntryDaoProvider) {
    this.workEntryDaoProvider = workEntryDaoProvider;
  }

  @Override
  public SetTravelEvent get() {
    return provideSetTravelEvent(workEntryDaoProvider.get());
  }

  public static ApplicationModule_ProvideSetTravelEventFactory create(
      Provider<WorkEntryDao> workEntryDaoProvider) {
    return new ApplicationModule_ProvideSetTravelEventFactory(workEntryDaoProvider);
  }

  public static SetTravelEvent provideSetTravelEvent(WorkEntryDao workEntryDao) {
    return Preconditions.checkNotNullFromProvides(ApplicationModule.INSTANCE.provideSetTravelEvent(workEntryDao));
  }
}
