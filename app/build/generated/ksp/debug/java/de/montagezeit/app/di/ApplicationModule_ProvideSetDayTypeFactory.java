package de.montagezeit.app.di;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import de.montagezeit.app.data.local.dao.WorkEntryDao;
import de.montagezeit.app.domain.usecase.SetDayType;
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
public final class ApplicationModule_ProvideSetDayTypeFactory implements Factory<SetDayType> {
  private final Provider<WorkEntryDao> workEntryDaoProvider;

  public ApplicationModule_ProvideSetDayTypeFactory(Provider<WorkEntryDao> workEntryDaoProvider) {
    this.workEntryDaoProvider = workEntryDaoProvider;
  }

  @Override
  public SetDayType get() {
    return provideSetDayType(workEntryDaoProvider.get());
  }

  public static ApplicationModule_ProvideSetDayTypeFactory create(
      Provider<WorkEntryDao> workEntryDaoProvider) {
    return new ApplicationModule_ProvideSetDayTypeFactory(workEntryDaoProvider);
  }

  public static SetDayType provideSetDayType(WorkEntryDao workEntryDao) {
    return Preconditions.checkNotNullFromProvides(ApplicationModule.INSTANCE.provideSetDayType(workEntryDao));
  }
}
