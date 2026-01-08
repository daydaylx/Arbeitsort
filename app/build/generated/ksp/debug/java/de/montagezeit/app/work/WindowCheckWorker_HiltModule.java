package de.montagezeit.app.work;

import androidx.hilt.work.WorkerAssistedFactory;
import androidx.work.ListenableWorker;
import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.codegen.OriginatingElement;
import dagger.hilt.components.SingletonComponent;
import dagger.multibindings.IntoMap;
import dagger.multibindings.StringKey;
import javax.annotation.processing.Generated;

@Generated("androidx.hilt.AndroidXHiltProcessor")
@Module
@InstallIn(SingletonComponent.class)
@OriginatingElement(
    topLevelClass = WindowCheckWorker.class
)
public interface WindowCheckWorker_HiltModule {
  @Binds
  @IntoMap
  @StringKey("de.montagezeit.app.work.WindowCheckWorker")
  WorkerAssistedFactory<? extends ListenableWorker> bind(WindowCheckWorker_AssistedFactory factory);
}
