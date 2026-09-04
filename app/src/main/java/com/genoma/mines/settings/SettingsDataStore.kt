package com.genoma.mines.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(
    name = "settings"
)

class SettingsDataStore(
    private val context: Context
) {

    private companion object {
        val SOUND_ENABLED =
            booleanPreferencesKey("sound_enabled")

        val HAPTICS_ENABLED =
            booleanPreferencesKey("haptics_enabled")
    }

    val soundEnabled: Flow<Boolean> =
        context.settingsDataStore.data.map { preferences ->
            preferences[SOUND_ENABLED] ?: true
        }

    val hapticsEnabled: Flow<Boolean> =
        context.settingsDataStore.data.map { preferences ->
            preferences[HAPTICS_ENABLED] ?: true
        }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[SOUND_ENABLED] = enabled
        }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[HAPTICS_ENABLED] = enabled
        }
    }
}