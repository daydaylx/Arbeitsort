package de.montagezeit.app.di;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import de.montagezeit.app.domain.location.LocationCalculator;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
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
public final class ApplicationModule_ProvideLocationCalculatorFactory implements Factory<LocationCalculator> {
  @Override
  public LocationCalculator get() {
    return provideLocationCalculator();
  }

  public static ApplicationModule_ProvideLocationCalculatorFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static LocationCalculator provideLocationCalculator() {
    return Preconditions.checkNotNullFromProvides(ApplicationModule.INSTANCE.provideLocationCalculator());
  }

  private static final class InstanceHolder {
    private static final ApplicationModule_ProvideLocationCalculatorFactory INSTANCE = new ApplicationModule_ProvideLocationCalculatorFactory();
  }
}
