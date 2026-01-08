package de.montagezeit.app.logging;

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
public final class RingBufferLogger_Factory implements Factory<RingBufferLogger> {
  private final Provider<Context> contextProvider;

  public RingBufferLogger_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public RingBufferLogger get() {
    return newInstance(contextProvider.get());
  }

  public static RingBufferLogger_Factory create(Provider<Context> contextProvider) {
    return new RingBufferLogger_Factory(contextProvider);
  }

  public static RingBufferLogger newInstance(Context context) {
    return new RingBufferLogger(context);
  }
}
