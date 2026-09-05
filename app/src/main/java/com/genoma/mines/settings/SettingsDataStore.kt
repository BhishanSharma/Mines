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

        val DARK_THEME_ENABLED =
            booleanPreferencesKey("dark_theme_enabled")
    }

    val soundEnabled: Flow<Boolean> =
        context.settingsDataStore.data.map { preferences ->
            preferences[SOUND_ENABLED] ?: true
        }

    val hapticsEnabled: Flow<Boolean> =
        context.settingsDataStore.data.map { preferences ->
            preferences[HAPTICS_ENABLED] ?: true
        }


    // Null means "no preference saved yet" — the caller falls back to the
    // system's current dark/light setting in that case.
    val darkThemeEnabled: Flow<Boolean?> =
        context.settingsDataStore.data.map { preferences ->
            preferences[DARK_THEME_ENABLED]
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


    suspend fun setDarkThemeEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[DARK_THEME_ENABLED] = enabled
        }
    }
}