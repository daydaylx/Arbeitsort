package de.montagezeit.app.ui.screen.edit;

import androidx.lifecycle.SavedStateHandle;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class EditEntryViewModel_Factory implements Factory<EditEntryViewModel> {
  private final Provider<WorkEntryDao> workEntryDaoProvider;

  private final Provider<UpdateEntry> updateEntryProvider;

  private final Provider<SavedStateHandle> savedStateHandleProvider;

  public EditEntryViewModel_Factory(Provider<WorkEntryDao> workEntryDaoProvider,
      Provider<UpdateEntry> updateEntryProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    this.workEntryDaoProvider = workEntryDaoProvider;
    this.updateEntryProvider = updateEntryProvider;
    this.savedStateHandleProvider = savedStateHandleProvider;
  }

  @Override
  public EditEntryViewModel get() {
    return newInstance(workEntryDaoProvider.get(), updateEntryProvider.get(), savedStateHandleProvider.get());
  }

  public static EditEntryViewModel_Factory create(Provider<WorkEntryDao> workEntryDaoProvider,
      Provider<UpdateEntry> updateEntryProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    return new EditEntryViewModel_Factory(workEntryDaoProvider, updateEntryProvider, savedStateHandleProvider);
  }

  public static EditEntryViewModel newInstance(WorkEntryDao workEntryDao, UpdateEntry updateEntry,
      SavedStateHandle savedStateHandle) {
    return new EditEntryViewModel(workEntryDao, updateEntry, savedStateHandle);
  }
}
