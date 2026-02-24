package de.montagezeit.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.preferences.ReminderSettingsManager
import de.montagezeit.app.domain.usecase.ConfirmOffDay
import de.montagezeit.app.domain.usecase.DeleteDayEntry
import de.montagezeit.app.domain.usecase.ConfirmWorkDay
import de.montagezeit.app.domain.usecase.RecordDailyManualCheckIn
import de.montagezeit.app.domain.usecase.RecordEveningCheckIn
import de.montagezeit.app.domain.usecase.RecordMorningCheckIn
import de.montagezeit.app.domain.usecase.ResolveDayLocationPrefill
import de.montagezeit.app.domain.usecase.SetDayLocation
import de.montagezeit.app.domain.usecase.SetDayType
import de.montagezeit.app.domain.usecase.SetTravelEvent
import de.montagezeit.app.domain.usecase.UpdateEntry

@Module
@InstallIn(SingletonComponent::class)
object ApplicationModule {

    @Provides
    fun provideRecordMorningCheckIn(workEntryDao: WorkEntryDao): RecordMorningCheckIn {
        return RecordMorningCheckIn(workEntryDao)
    }

    @Provides
    fun provideRecordEveningCheckIn(workEntryDao: WorkEntryDao): RecordEveningCheckIn {
        return RecordEveningCheckIn(workEntryDao)
    }

    @Provides
    fun provideSetDayType(workEntryDao: WorkEntryDao): SetDayType {
        return SetDayType(workEntryDao)
    }

    @Provides
    fun provideUpdateEntry(workEntryDao: WorkEntryDao): UpdateEntry {
        return UpdateEntry(workEntryDao)
    }

    @Provides
    fun provideSetTravelEvent(workEntryDao: WorkEntryDao): SetTravelEvent {
        return SetTravelEvent(workEntryDao)
    }

    @Provides
    fun provideConfirmWorkDay(
        workEntryDao: WorkEntryDao,
        reminderSettingsManager: ReminderSettingsManager
    ): ConfirmWorkDay {
        return ConfirmWorkDay(workEntryDao, reminderSettingsManager)
    }

    @Provides
    fun provideConfirmOffDay(workEntryDao: WorkEntryDao): ConfirmOffDay {
        return ConfirmOffDay(workEntryDao)
    }

    @Provides
    fun provideSetDayLocation(
        workEntryDao: WorkEntryDao,
        reminderSettingsManager: ReminderSettingsManager
    ): SetDayLocation {
        return SetDayLocation(workEntryDao, reminderSettingsManager)
    }

    @Provides
    fun provideResolveDayLocationPrefill(workEntryDao: WorkEntryDao): ResolveDayLocationPrefill {
        return ResolveDayLocationPrefill(workEntryDao)
    }

    @Provides
    fun provideDeleteDayEntry(workEntryDao: WorkEntryDao): DeleteDayEntry {
        return DeleteDayEntry(workEntryDao)
    }

    @Provides
    fun provideRecordDailyManualCheckIn(
        workEntryDao: WorkEntryDao,
        reminderSettingsManager: ReminderSettingsManager
    ): RecordDailyManualCheckIn {
        return RecordDailyManualCheckIn(workEntryDao, reminderSettingsManager)
    }
}
