package de.montagezeit.app.ui.screen.history;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import de.montagezeit.app.data.local.dao.WorkEntryDao;
import de.montagezeit.app.domain.usecase.ExportDataUseCase;
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
public final class HistoryViewModel_Factory implements Factory<HistoryViewModel> {
  private final Provider<WorkEntryDao> workEntryDaoProvider;

  private final Provider<ExportDataUseCase> exportDataUseCaseProvider;

  public HistoryViewModel_Factory(Provider<WorkEntryDao> workEntryDaoProvider,
      Provider<ExportDataUseCase> exportDataUseCaseProvider) {
    this.workEntryDaoProvider = workEntryDaoProvider;
    this.exportDataUseCaseProvider = exportDataUseCaseProvider;
  }

  @Override
  public HistoryViewModel get() {
    return newInstance(workEntryDaoProvider.get(), exportDataUseCaseProvider.get());
  }

  public static HistoryViewModel_Factory create(Provider<WorkEntryDao> workEntryDaoProvider,
      Provider<ExportDataUseCase> exportDataUseCaseProvider) {
    return new HistoryViewModel_Factory(workEntryDaoProvider, exportDataUseCaseProvider);
  }

  public static HistoryViewModel newInstance(WorkEntryDao workEntryDao,
      ExportDataUseCase exportDataUseCase) {
    return new HistoryViewModel(workEntryDao, exportDataUseCase);
  }
}
