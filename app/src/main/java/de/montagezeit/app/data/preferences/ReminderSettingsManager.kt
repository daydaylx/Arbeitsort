package de.montagezeit.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
                ?: LocalTime.of(22, 30)
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
        morningReminderEnabled: Boolean? = null,
        morningWindowStart: LocalTime? = null,
        morningWindowEnd: LocalTime? = null,
        morningCheckIntervalMinutes: Int? = null,
        eveningReminderEnabled: Boolean? = null,
        eveningWindowStart: LocalTime? = null,
        eveningWindowEnd: LocalTime? = null,
        eveningCheckIntervalMinutes: Int? = null,
        fallbackEnabled: Boolean? = null,
        fallbackTime: LocalTime? = null
    ) {
        dataStore.edit { preferences ->
            workStart?.let { preferences[ReminderSettingsKeys.WORK_START] = it.toPrefString() }
            workEnd?.let { preferences[ReminderSettingsKeys.WORK_END] = it.toPrefString() }
            breakMinutes?.let { preferences[ReminderSettingsKeys.BREAK_MINUTES] = it }
            locationRadiusKm?.let { preferences[ReminderSettingsKeys.LOCATION_RADIUS_KM] = it }
            
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
