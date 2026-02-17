package de.montagezeit.app.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

private val Context.reminderFlagsDataStore: DataStore<Preferences>
    by preferencesDataStore(name = "reminder_flags_v2")

/**
 * Speichert pro Tag, ob eine bestimmte Reminder-Art bereits gezeigt wurde.
 *
 * Persistenz: DataStore (Preferences), migriert von SharedPreferences.
 * Die alten SP-Keys (z.B. "morning_reminded_2026-02-17") werden beim ersten
 * Zugriff einmalig migriert und dann gelöscht.
 *
 * Datenhaltung: Pro Flag-Typ ein StringSet mit ISO-Datums-Strings.
 */
@Singleton
class ReminderFlagsStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.reminderFlagsDataStore

    companion object {
        private const val SP_NAME = "reminder_flags"  // alter SP-Name

        // DataStore Keys: StringSet von "yyyy-MM-dd"-Dates
        val KEY_MORNING_DATES  = stringSetPreferencesKey("morning_reminded_dates")
        val KEY_EVENING_DATES  = stringSetPreferencesKey("evening_reminded_dates")
        val KEY_FALLBACK_DATES = stringSetPreferencesKey("fallback_reminded_dates")
        val KEY_DAILY_DATES    = stringSetPreferencesKey("daily_reminded_dates")
        val KEY_MIGRATION_DONE = stringSetPreferencesKey("_migration_done")  // Sentinel

        // SP-Präfixe für die Migration
        private const val SP_PREFIX_MORNING  = "morning_reminded_"
        private const val SP_PREFIX_EVENING  = "evening_reminded_"
        private const val SP_PREFIX_FALLBACK = "fallback_reminded_"
        private const val SP_PREFIX_DAILY    = "daily_reminded_"
    }

    // -------------------------------------------------------------------------
    // Einmalige Migration SharedPreferences → DataStore
    // -------------------------------------------------------------------------
    private var migrationChecked = false

    private suspend fun ensureMigrated() {
        if (migrationChecked) return
        migrationChecked = true

        val current = dataStore.data.first()
        if (current[KEY_MIGRATION_DONE]?.contains("done") == true) return

        val sp: SharedPreferences = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
        val allEntries = sp.all
        if (allEntries.isEmpty()) {
            // Nichts zu migrieren – Sentinel setzen
            dataStore.edit { prefs ->
                prefs[KEY_MIGRATION_DONE] = setOf("done")
            }
            return
        }

        val morningDates  = mutableSetOf<String>()
        val eveningDates  = mutableSetOf<String>()
        val fallbackDates = mutableSetOf<String>()
        val dailyDates    = mutableSetOf<String>()

        for ((key, value) in allEntries) {
            if (value != true) continue
            when {
                key.startsWith(SP_PREFIX_MORNING)  -> morningDates.add(key.removePrefix(SP_PREFIX_MORNING))
                key.startsWith(SP_PREFIX_EVENING)  -> eveningDates.add(key.removePrefix(SP_PREFIX_EVENING))
                key.startsWith(SP_PREFIX_FALLBACK) -> fallbackDates.add(key.removePrefix(SP_PREFIX_FALLBACK))
                key.startsWith(SP_PREFIX_DAILY)    -> dailyDates.add(key.removePrefix(SP_PREFIX_DAILY))
            }
        }

        dataStore.edit { prefs ->
            if (morningDates.isNotEmpty())  prefs[KEY_MORNING_DATES]  = morningDates
            if (eveningDates.isNotEmpty())  prefs[KEY_EVENING_DATES]  = eveningDates
            if (fallbackDates.isNotEmpty()) prefs[KEY_FALLBACK_DATES] = fallbackDates
            if (dailyDates.isNotEmpty())    prefs[KEY_DAILY_DATES]    = dailyDates
            prefs[KEY_MIGRATION_DONE] = setOf("done")
        }

        // Alte SharedPreferences nach erfolgreicher Migration löschen
        sp.edit().clear().apply()
    }

    // -------------------------------------------------------------------------
    // Öffentliche API
    // -------------------------------------------------------------------------

    suspend fun isMorningReminded(date: LocalDate): Boolean {
        ensureMigrated()
        return dataStore.data.first()[KEY_MORNING_DATES]?.contains(date.toString()) == true
    }

    suspend fun setMorningReminded(date: LocalDate) {
        ensureMigrated()
        dataStore.edit { prefs ->
            prefs[KEY_MORNING_DATES] = (prefs[KEY_MORNING_DATES] ?: emptySet()) + date.toString()
        }
    }

    suspend fun isEveningReminded(date: LocalDate): Boolean {
        ensureMigrated()
        return dataStore.data.first()[KEY_EVENING_DATES]?.contains(date.toString()) == true
    }

    suspend fun setEveningReminded(date: LocalDate) {
        ensureMigrated()
        dataStore.edit { prefs ->
            prefs[KEY_EVENING_DATES] = (prefs[KEY_EVENING_DATES] ?: emptySet()) + date.toString()
        }
    }

    suspend fun isFallbackReminded(date: LocalDate): Boolean {
        ensureMigrated()
        return dataStore.data.first()[KEY_FALLBACK_DATES]?.contains(date.toString()) == true
    }

    suspend fun setFallbackReminded(date: LocalDate) {
        ensureMigrated()
        dataStore.edit { prefs ->
            prefs[KEY_FALLBACK_DATES] = (prefs[KEY_FALLBACK_DATES] ?: emptySet()) + date.toString()
        }
    }

    suspend fun isDailyReminded(date: LocalDate): Boolean {
        ensureMigrated()
        return dataStore.data.first()[KEY_DAILY_DATES]?.contains(date.toString()) == true
    }

    suspend fun setDailyReminded(date: LocalDate) {
        ensureMigrated()
        dataStore.edit { prefs ->
            prefs[KEY_DAILY_DATES] = (prefs[KEY_DAILY_DATES] ?: emptySet()) + date.toString()
        }
    }

    suspend fun setAllReminded(date: LocalDate) {
        ensureMigrated()
        val dateStr = date.toString()
        dataStore.edit { prefs ->
            prefs[KEY_MORNING_DATES]  = (prefs[KEY_MORNING_DATES]  ?: emptySet()) + dateStr
            prefs[KEY_EVENING_DATES]  = (prefs[KEY_EVENING_DATES]  ?: emptySet()) + dateStr
            prefs[KEY_FALLBACK_DATES] = (prefs[KEY_FALLBACK_DATES] ?: emptySet()) + dateStr
            prefs[KEY_DAILY_DATES]    = (prefs[KEY_DAILY_DATES]    ?: emptySet()) + dateStr
        }
    }
}
