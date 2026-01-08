package de.montagezeit.app.di;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import de.montagezeit.app.data.local.dao.WorkEntryDao;
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
public final class ApplicationModule_ProvideUpdateEntryFactory implements Factory<UpdateEntry> {
  private final Provider<WorkEntryDao> workEntryDaoProvider;

  public ApplicationModule_ProvideUpdateEntryFactory(Provider<WorkEntryDao> workEntryDaoProvider) {
    this.workEntryDaoProvider = workEntryDaoProvider;
  }

  @Override
  public UpdateEntry get() {
    return provideUpdateEntry(workEntryDaoProvider.get());
  }

  public static ApplicationModule_ProvideUpdateEntryFactory create(
      Provider<WorkEntryDao> workEntryDaoProvider) {
    return new ApplicationModule_ProvideUpdateEntryFactory(workEntryDaoProvider);
  }

  public static UpdateEntry provideUpdateEntry(WorkEntryDao workEntryDao) {
    return Preconditions.checkNotNullFromProvides(ApplicationModule.INSTANCE.provideUpdateEntry(workEntryDao));
  }
}
