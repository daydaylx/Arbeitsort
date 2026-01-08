package de.montagezeit.app.work;

import android.content.Context;
import androidx.work.WorkerParameters;
import dagger.internal.DaggerGenerated;
import dagger.internal.InstanceFactory;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class WindowCheckWorker_AssistedFactory_Impl implements WindowCheckWorker_AssistedFactory {
  private final WindowCheckWorker_Factory delegateFactory;

  WindowCheckWorker_AssistedFactory_Impl(WindowCheckWorker_Factory delegateFactory) {
    this.delegateFactory = delegateFactory;
  }

  @Override
  public WindowCheckWorker create(Context p0, WorkerParameters p1) {
    return delegateFactory.get(p0, p1);
  }

  public static Provider<WindowCheckWorker_AssistedFactory> create(
      WindowCheckWorker_Factory delegateFactory) {
    return InstanceFactory.create(new WindowCheckWorker_AssistedFactory_Impl(delegateFactory));
  }
}
