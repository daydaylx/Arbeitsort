package de.montagezeit.app.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import de.montagezeit.app.data.location.LocationProvider
import de.montagezeit.app.data.location.LocationProviderImpl
import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.preferences.ReminderSettingsManager
import de.montagezeit.app.domain.location.LocationCalculator
import de.montagezeit.app.domain.usecase.RecordEveningCheckIn
import de.montagezeit.app.domain.usecase.RecordMorningCheckIn
import de.montagezeit.app.domain.usecase.SetDayType
import de.montagezeit.app.domain.usecase.SetTravelEvent
import de.montagezeit.app.domain.usecase.UpdateEntry
import de.montagezeit.app.domain.usecase.ConfirmWorkDay
import de.montagezeit.app.domain.usecase.ConfirmOffDay
import de.montagezeit.app.domain.usecase.RecordDailyManualCheckIn
import de.montagezeit.app.domain.usecase.ResolveDayLocationPrefill
import de.montagezeit.app.domain.usecase.ResolveReview
import de.montagezeit.app.domain.usecase.SetDayLocation
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApplicationModule {
    
    @Provides
    @Singleton
    fun provideLocationProvider(@ApplicationContext context: Context): LocationProvider {
        return LocationProviderImpl(context)
    }
    
    @Provides
    @Singleton
    fun provideReminderSettingsManager(@ApplicationContext context: Context): ReminderSettingsManager {
        return ReminderSettingsManager(context)
    }
    
    @Provides
    @Singleton
    fun provideLocationCalculator(): LocationCalculator {
        return LocationCalculator()
    }
    
    @Provides
    fun provideRecordMorningCheckIn(
        workEntryDao: WorkEntryDao,
        locationProvider: LocationProvider,
        locationCalculator: LocationCalculator,
        reminderSettingsManager: ReminderSettingsManager
    ): RecordMorningCheckIn {
        return RecordMorningCheckIn(
            workEntryDao = workEntryDao,
            locationProvider = locationProvider,
            locationCalculator = locationCalculator,
            reminderSettingsManager = reminderSettingsManager
        )
    }
    
    @Provides
    fun provideRecordEveningCheckIn(
        workEntryDao: WorkEntryDao,
        locationProvider: LocationProvider,
        locationCalculator: LocationCalculator,
        reminderSettingsManager: ReminderSettingsManager
    ): RecordEveningCheckIn {
        return RecordEveningCheckIn(
            workEntryDao = workEntryDao,
            locationProvider = locationProvider,
            locationCalculator = locationCalculator,
            reminderSettingsManager = reminderSettingsManager
        )
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
        locationProvider: LocationProvider,
        locationCalculator: LocationCalculator,
        reminderSettingsManager: ReminderSettingsManager
    ): ConfirmWorkDay {
        return ConfirmWorkDay(
            workEntryDao = workEntryDao,
            locationProvider = locationProvider,
            locationCalculator = locationCalculator,
            reminderSettingsManager = reminderSettingsManager
        )
    }

    @Provides
    fun provideConfirmOffDay(
        workEntryDao: WorkEntryDao,
        reminderSettingsManager: ReminderSettingsManager
    ): ConfirmOffDay {
        return ConfirmOffDay(workEntryDao, reminderSettingsManager)
    }

    @Provides
    fun provideSetDayLocation(
        workEntryDao: WorkEntryDao,
        reminderSettingsManager: ReminderSettingsManager
    ): SetDayLocation {
        return SetDayLocation(workEntryDao, reminderSettingsManager)
    }

    @Provides
    fun provideRecordDailyManualCheckIn(
        workEntryDao: WorkEntryDao,
        reminderSettingsManager: ReminderSettingsManager,
        resolveDayLocationPrefill: ResolveDayLocationPrefill
    ): RecordDailyManualCheckIn {
        return RecordDailyManualCheckIn(workEntryDao, reminderSettingsManager, resolveDayLocationPrefill)
    }

    @Provides
    fun provideResolveDayLocationPrefill(
        workEntryDao: WorkEntryDao,
        reminderSettingsManager: ReminderSettingsManager
    ): ResolveDayLocationPrefill {
        return ResolveDayLocationPrefill(workEntryDao, reminderSettingsManager)
    }
    
    @Provides
    fun provideResolveReview(workEntryDao: WorkEntryDao): ResolveReview {
        return ResolveReview(workEntryDao)
    }
}
