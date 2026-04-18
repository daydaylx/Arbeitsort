package de.montagezeit.app.di

import android.content.Context
import androidx.work.WorkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import de.montagezeit.app.data.preferences.ReminderSettingsManager
import de.montagezeit.app.data.repository.WorkEntryRepository
import de.montagezeit.app.domain.usecase.ConfirmOffDay
import de.montagezeit.app.domain.usecase.ConfirmWorkDay
import de.montagezeit.app.domain.usecase.DeleteDayEntry
import de.montagezeit.app.domain.usecase.RecordDailyManualCheckIn
import de.montagezeit.app.domain.usecase.RecordEveningCheckIn
import de.montagezeit.app.domain.usecase.RecordMorningCheckIn
import de.montagezeit.app.domain.usecase.SetDayLocation
import de.montagezeit.app.domain.usecase.SetTravelEvent
import de.montagezeit.app.domain.util.NonWorkingDayChecker
import de.montagezeit.app.work.DefaultNonWorkingDayChecker
import java.time.Clock

@Module
@InstallIn(SingletonComponent::class)
object ApplicationModule {
    @Provides
    fun provideClock(): Clock = Clock.systemDefaultZone()

    @Provides
    fun provideRecordMorningCheckIn(repository: WorkEntryRepository): RecordMorningCheckIn {
        return RecordMorningCheckIn(repository)
    }

    @Provides
    fun provideRecordEveningCheckIn(repository: WorkEntryRepository): RecordEveningCheckIn {
        return RecordEveningCheckIn(repository)
    }

    @Provides
    fun provideSetTravelEvent(repository: WorkEntryRepository): SetTravelEvent {
        return SetTravelEvent(repository)
    }

    @Provides
    fun provideConfirmWorkDay(
        repository: WorkEntryRepository,
        reminderSettingsManager: ReminderSettingsManager
    ): ConfirmWorkDay {
        return ConfirmWorkDay(repository, reminderSettingsManager)
    }

    @Provides
    fun provideConfirmOffDay(repository: WorkEntryRepository): ConfirmOffDay {
        return ConfirmOffDay(repository)
    }

    @Provides
    fun provideSetDayLocation(
        repository: WorkEntryRepository,
        reminderSettingsManager: ReminderSettingsManager
    ): SetDayLocation {
        return SetDayLocation(repository, reminderSettingsManager)
    }

    @Provides
    fun provideNonWorkingDayChecker(impl: DefaultNonWorkingDayChecker): NonWorkingDayChecker {
        return impl
    }

    @Provides
    fun provideDeleteDayEntry(repository: WorkEntryRepository): DeleteDayEntry {
        return DeleteDayEntry(repository)
    }

    @Provides
    fun provideRecordDailyManualCheckIn(
        repository: WorkEntryRepository,
        reminderSettingsManager: ReminderSettingsManager
    ): RecordDailyManualCheckIn {
        return RecordDailyManualCheckIn(repository, reminderSettingsManager)
    }

    @Provides
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }
}
