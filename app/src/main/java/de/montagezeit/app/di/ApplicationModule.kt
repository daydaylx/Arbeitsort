package de.montagezeit.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import de.montagezeit.app.data.preferences.ReminderSettingsManager
import de.montagezeit.app.data.repository.WorkEntryRepository
import de.montagezeit.app.domain.usecase.ConfirmOffDay
import de.montagezeit.app.domain.usecase.ConfirmWorkDay
import de.montagezeit.app.domain.usecase.DeleteDayEntry
import de.montagezeit.app.domain.usecase.DeleteWorkEntryByDate
import de.montagezeit.app.domain.usecase.GetWorkEntriesByDateRange
import de.montagezeit.app.domain.usecase.GetWorkEntriesWithTravelByDateRange
import de.montagezeit.app.domain.usecase.GetWorkEntryByDate
import de.montagezeit.app.domain.usecase.GetWorkEntryWithTravelByDate
import de.montagezeit.app.domain.usecase.ObserveWorkEntriesWithTravelByDateRange
import de.montagezeit.app.domain.usecase.ObserveWorkEntryByDate
import de.montagezeit.app.domain.usecase.ObserveWorkEntryWithTravelByDate
import de.montagezeit.app.domain.usecase.RecordDailyManualCheckIn
import de.montagezeit.app.domain.usecase.RecordEveningCheckIn
import de.montagezeit.app.domain.usecase.RecordMorningCheckIn
import de.montagezeit.app.domain.usecase.ReplaceWorkEntryWithTravelLegs
import de.montagezeit.app.domain.usecase.ResolveDayLocationPrefill
import de.montagezeit.app.domain.usecase.SetDayLocation
import de.montagezeit.app.domain.usecase.SetTravelEvent
import de.montagezeit.app.domain.usecase.UpsertWorkEntries
import de.montagezeit.app.domain.util.NonWorkingDayChecker
import de.montagezeit.app.work.DefaultNonWorkingDayChecker

@Module
@InstallIn(SingletonComponent::class)
object ApplicationModule {

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
    fun provideResolveDayLocationPrefill(repository: WorkEntryRepository): ResolveDayLocationPrefill {
        return ResolveDayLocationPrefill(repository)
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
    fun provideObserveWorkEntryByDate(repository: WorkEntryRepository): ObserveWorkEntryByDate =
        ObserveWorkEntryByDate(repository)

    @Provides
    fun provideObserveWorkEntryWithTravelByDate(repository: WorkEntryRepository): ObserveWorkEntryWithTravelByDate =
        ObserveWorkEntryWithTravelByDate(repository)

    @Provides
    fun provideObserveWorkEntriesWithTravelByDateRange(repository: WorkEntryRepository): ObserveWorkEntriesWithTravelByDateRange =
        ObserveWorkEntriesWithTravelByDateRange(repository)

    @Provides
    fun provideGetWorkEntryByDate(repository: WorkEntryRepository): GetWorkEntryByDate =
        GetWorkEntryByDate(repository)

    @Provides
    fun provideGetWorkEntryWithTravelByDate(repository: WorkEntryRepository): GetWorkEntryWithTravelByDate =
        GetWorkEntryWithTravelByDate(repository)

    @Provides
    fun provideGetWorkEntriesByDateRange(repository: WorkEntryRepository): GetWorkEntriesByDateRange =
        GetWorkEntriesByDateRange(repository)

    @Provides
    fun provideGetWorkEntriesWithTravelByDateRange(repository: WorkEntryRepository): GetWorkEntriesWithTravelByDateRange =
        GetWorkEntriesWithTravelByDateRange(repository)

    @Provides
    fun provideUpsertWorkEntries(repository: WorkEntryRepository): UpsertWorkEntries =
        UpsertWorkEntries(repository)

    @Provides
    fun provideDeleteWorkEntryByDate(repository: WorkEntryRepository): DeleteWorkEntryByDate =
        DeleteWorkEntryByDate(repository)

    @Provides
    fun provideReplaceWorkEntryWithTravelLegs(repository: WorkEntryRepository): ReplaceWorkEntryWithTravelLegs =
        ReplaceWorkEntryWithTravelLegs(repository)
}
