package de.montagezeit.app.export;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class CsvExporter_Factory implements Factory<CsvExporter> {
  private final Provider<Context> contextProvider;

  public CsvExporter_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public CsvExporter get() {
    return newInstance(contextProvider.get());
  }

  public static CsvExporter_Factory create(Provider<Context> contextProvider) {
    return new CsvExporter_Factory(contextProvider);
  }

  public static CsvExporter newInstance(Context context) {
    return new CsvExporter(context);
  }
}
