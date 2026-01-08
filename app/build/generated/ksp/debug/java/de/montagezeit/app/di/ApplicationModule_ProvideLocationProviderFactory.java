package de.montagezeit.app.di;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import de.montagezeit.app.data.location.LocationProvider;
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
public final class ApplicationModule_ProvideLocationProviderFactory implements Factory<LocationProvider> {
  private final Provider<Context> contextProvider;

  public ApplicationModule_ProvideLocationProviderFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public LocationProvider get() {
    return provideLocationProvider(contextProvider.get());
  }

  public static ApplicationModule_ProvideLocationProviderFactory create(
      Provider<Context> contextProvider) {
    return new ApplicationModule_ProvideLocationProviderFactory(contextProvider);
  }

  public static LocationProvider provideLocationProvider(Context context) {
    return Preconditions.checkNotNullFromProvides(ApplicationModule.INSTANCE.provideLocationProvider(context));
  }
}
