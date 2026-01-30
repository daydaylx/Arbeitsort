package de.montagezeit.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataStore für Reminder-Settings
 */
private val Context.reminderDataStore: DataStore<Preferences> by preferencesDataStore(name = "reminder_settings")

@Singleton
class ReminderSettingsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val dataStore = context.reminderDataStore
    
    /**
     * Flow für Reminder-Settings
     */
    val settings: Flow<ReminderSettings> = dataStore.data.map { preferences ->
        ReminderSettings(
            // Arbeitszeit Defaults
            workStart = preferences[ReminderSettingsKeys.WORK_START]?.toLocalTime() 
                ?: LocalTime.of(8, 0),
            workEnd = preferences[ReminderSettingsKeys.WORK_END]?.toLocalTime() 
                ?: LocalTime.of(19, 0),
            breakMinutes = preferences[ReminderSettingsKeys.BREAK_MINUTES] ?: 60,
            locationRadiusKm = preferences[ReminderSettingsKeys.LOCATION_RADIUS_KM] ?: 30,

            // Standort
            defaultDayLocationLabel = preferences[ReminderSettingsKeys.DEFAULT_DAY_LOCATION_LABEL] ?: "Leipzig",
            preferGpsLocation = preferences[ReminderSettingsKeys.PREFER_GPS_LOCATION] ?: true,
            fallbackOnLowAccuracy = preferences[ReminderSettingsKeys.FALLBACK_ON_LOW_ACCURACY] ?: true,
            
            // Morning Window
            morningReminderEnabled = preferences[ReminderSettingsKeys.MORNING_REMINDER_ENABLED] ?: true,
            morningWindowStart = preferences[ReminderSettingsKeys.MORNING_WINDOW_START]?.toLocalTime() 
                ?: LocalTime.of(6, 0),
            morningWindowEnd = preferences[ReminderSettingsKeys.MORNING_WINDOW_END]?.toLocalTime() 
                ?: LocalTime.of(13, 0),
            morningCheckIntervalMinutes = preferences[ReminderSettingsKeys.MORNING_CHECK_INTERVAL] ?: 120,
            
            // Evening Window
            eveningReminderEnabled = preferences[ReminderSettingsKeys.EVENING_REMINDER_ENABLED] ?: true,
            eveningWindowStart = preferences[ReminderSettingsKeys.EVENING_WINDOW_START]?.toLocalTime() 
                ?: LocalTime.of(16, 0),
            eveningWindowEnd = preferences[ReminderSettingsKeys.EVENING_WINDOW_END]?.toLocalTime() 
                ?: LocalTime.of(22, 30),
            eveningCheckIntervalMinutes = preferences[ReminderSettingsKeys.EVENING_CHECK_INTERVAL] ?: 180,
            
            // Fallback
            fallbackEnabled = preferences[ReminderSettingsKeys.FALLBACK_ENABLED] ?: true,
            fallbackTime = preferences[ReminderSettingsKeys.FALLBACK_TIME]?.toLocalTime() 
                ?: LocalTime.of(22, 30),

            // Tägliche Erinnerung
            dailyReminderEnabled = preferences[ReminderSettingsKeys.DAILY_REMINDER_ENABLED] ?: true,
            dailyReminderTime = preferences[ReminderSettingsKeys.DAILY_REMINDER_TIME]?.toLocalTime()
                ?: LocalTime.of(18, 0),

            // Arbeitsfreie Tage
            autoOffWeekends = preferences[ReminderSettingsKeys.AUTO_OFF_WEEKENDS] ?: true,
            autoOffHolidays = preferences[ReminderSettingsKeys.AUTO_OFF_HOLIDAYS] ?: true,
            holidayDates = preferences[ReminderSettingsKeys.HOLIDAY_DATES].toLocalDateSet(),

            // PDF Export Settings
            pdfEmployeeName = preferences[ReminderSettingsKeys.PDF_EMPLOYEE_NAME],
            pdfCompany = preferences[ReminderSettingsKeys.PDF_COMPANY],
            pdfProject = preferences[ReminderSettingsKeys.PDF_PROJECT],
            pdfPersonnelNumber = preferences[ReminderSettingsKeys.PDF_PERSONNEL_NUMBER]
        )
    }
    
    /**
     * Aktualisiert die Reminder-Settings
     */
    suspend fun updateSettings(
        workStart: LocalTime? = null,
        workEnd: LocalTime? = null,
        breakMinutes: Int? = null,
        locationRadiusKm: Int? = null,
        defaultDayLocationLabel: String? = null,
        preferGpsLocation: Boolean? = null,
        fallbackOnLowAccuracy: Boolean? = null,
        morningReminderEnabled: Boolean? = null,
        morningWindowStart: LocalTime? = null,
        morningWindowEnd: LocalTime? = null,
        morningCheckIntervalMinutes: Int? = null,
        eveningReminderEnabled: Boolean? = null,
        eveningWindowStart: LocalTime? = null,
        eveningWindowEnd: LocalTime? = null,
        eveningCheckIntervalMinutes: Int? = null,
        fallbackEnabled: Boolean? = null,
        fallbackTime: LocalTime? = null,
        dailyReminderEnabled: Boolean? = null,
        dailyReminderTime: LocalTime? = null,
        autoOffWeekends: Boolean? = null,
        autoOffHolidays: Boolean? = null,
        holidayDates: Set<LocalDate>? = null,
        pdfEmployeeName: String? = null,
        pdfCompany: String? = null,
        pdfProject: String? = null,
        pdfPersonnelNumber: String? = null
    ) {
        dataStore.edit { preferences ->
            workStart?.let { preferences[ReminderSettingsKeys.WORK_START] = it.toPrefString() }
            workEnd?.let { preferences[ReminderSettingsKeys.WORK_END] = it.toPrefString() }
            breakMinutes?.let { preferences[ReminderSettingsKeys.BREAK_MINUTES] = it }
            locationRadiusKm?.let { preferences[ReminderSettingsKeys.LOCATION_RADIUS_KM] = it }

            defaultDayLocationLabel?.let { preferences[ReminderSettingsKeys.DEFAULT_DAY_LOCATION_LABEL] = it }
            preferGpsLocation?.let { preferences[ReminderSettingsKeys.PREFER_GPS_LOCATION] = it }
            fallbackOnLowAccuracy?.let { preferences[ReminderSettingsKeys.FALLBACK_ON_LOW_ACCURACY] = it }
            
            morningReminderEnabled?.let { preferences[ReminderSettingsKeys.MORNING_REMINDER_ENABLED] = it }
            morningWindowStart?.let { preferences[ReminderSettingsKeys.MORNING_WINDOW_START] = it.toPrefString() }
            morningWindowEnd?.let { preferences[ReminderSettingsKeys.MORNING_WINDOW_END] = it.toPrefString() }
            morningCheckIntervalMinutes?.let { preferences[ReminderSettingsKeys.MORNING_CHECK_INTERVAL] = it }
            
            eveningReminderEnabled?.let { preferences[ReminderSettingsKeys.EVENING_REMINDER_ENABLED] = it }
            eveningWindowStart?.let { preferences[ReminderSettingsKeys.EVENING_WINDOW_START] = it.toPrefString() }
            eveningWindowEnd?.let { preferences[ReminderSettingsKeys.EVENING_WINDOW_END] = it.toPrefString() }
            eveningCheckIntervalMinutes?.let { preferences[ReminderSettingsKeys.EVENING_CHECK_INTERVAL] = it }
            
            fallbackEnabled?.let { preferences[ReminderSettingsKeys.FALLBACK_ENABLED] = it }
            fallbackTime?.let { preferences[ReminderSettingsKeys.FALLBACK_TIME] = it.toPrefString() }

            dailyReminderEnabled?.let { preferences[ReminderSettingsKeys.DAILY_REMINDER_ENABLED] = it }
            dailyReminderTime?.let { preferences[ReminderSettingsKeys.DAILY_REMINDER_TIME] = it.toPrefString() }

            autoOffWeekends?.let { preferences[ReminderSettingsKeys.AUTO_OFF_WEEKENDS] = it }
            autoOffHolidays?.let { preferences[ReminderSettingsKeys.AUTO_OFF_HOLIDAYS] = it }
            holidayDates?.let { preferences[ReminderSettingsKeys.HOLIDAY_DATES] = it.toPrefString() }
            
            pdfEmployeeName?.let { preferences[ReminderSettingsKeys.PDF_EMPLOYEE_NAME] = it }
            pdfCompany?.let { preferences[ReminderSettingsKeys.PDF_COMPANY] = it }
            pdfProject?.let { preferences[ReminderSettingsKeys.PDF_PROJECT] = it }
            pdfPersonnelNumber?.let { preferences[ReminderSettingsKeys.PDF_PERSONNEL_NUMBER] = it }
        }
    }
    
    /**
     * Setzt alle Settings auf Default-Werte zurück
     */
    suspend fun resetToDefaults() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
