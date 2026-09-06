package com.genoma.mines.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userSessionDataStore by preferencesDataStore(name = "user_session")

class UserSessionStore(private val context: Context) {

    private companion object {
        val USER_ID = stringPreferencesKey("user_id")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val USER_PHOTO = stringPreferencesKey("user_photo")
    }

    val userProfile: Flow<UserProfile?> = context.userSessionDataStore.data.map { prefs ->
        val id = prefs[USER_ID] ?: return@map null
        UserProfile(
            id = id,
            displayName = prefs[USER_NAME],
            email = prefs[USER_EMAIL],
            photoUrl = prefs[USER_PHOTO]
        )
    }

    suspend fun save(profile: UserProfile) {
        context.userSessionDataStore.edit { prefs ->
            prefs[USER_ID] = profile.id
            profile.displayName?.let { prefs[USER_NAME] = it }
            profile.email?.let { prefs[USER_EMAIL] = it }
            profile.photoUrl?.let { prefs[USER_PHOTO] = it }
        }
    }

    suspend fun clear() {
        context.userSessionDataStore.edit { it.clear() }
    }
}