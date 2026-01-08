package de.montagezeit.app.domain.usecase;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import de.montagezeit.app.data.local.dao.WorkEntryDao;
import de.montagezeit.app.export.CsvExporter;
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
public final class ExportDataUseCase_Factory implements Factory<ExportDataUseCase> {
  private final Provider<WorkEntryDao> workEntryDaoProvider;

  private final Provider<CsvExporter> csvExporterProvider;

  public ExportDataUseCase_Factory(Provider<WorkEntryDao> workEntryDaoProvider,
      Provider<CsvExporter> csvExporterProvider) {
    this.workEntryDaoProvider = workEntryDaoProvider;
    this.csvExporterProvider = csvExporterProvider;
  }

  @Override
  public ExportDataUseCase get() {
    return newInstance(workEntryDaoProvider.get(), csvExporterProvider.get());
  }

  public static ExportDataUseCase_Factory create(Provider<WorkEntryDao> workEntryDaoProvider,
      Provider<CsvExporter> csvExporterProvider) {
    return new ExportDataUseCase_Factory(workEntryDaoProvider, csvExporterProvider);
  }

  public static ExportDataUseCase newInstance(WorkEntryDao workEntryDao, CsvExporter csvExporter) {
    return new ExportDataUseCase(workEntryDao, csvExporter);
  }
}
