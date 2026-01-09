package de.montagezeit.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.routingSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "routing_settings"
)

class RoutingSettingsManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val dataStore = context.routingSettingsDataStore

    companion object {
        private val API_KEY = stringPreferencesKey("routing_api_key")
    }

    val apiKey: Flow<String?> = dataStore.data.map { preferences ->
        preferences[API_KEY]
    }

    suspend fun getApiKey(): String? {
        return apiKey.first()
    }

    suspend fun setApiKey(value: String?) {
        dataStore.edit { preferences ->
            if (value.isNullOrBlank()) {
                preferences.remove(API_KEY)
            } else {
                preferences[API_KEY] = value
            }
        }
    }
}
