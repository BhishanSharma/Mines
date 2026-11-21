package com.genoma.mines.core.remoteconfig

import android.util.Log
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Central access point for Firebase Remote Config.
 *
 * Registers defaults, fetches and activates values, listens for real-time
 * updates, and exposes a validated, typed StateFlow snapshot.
 *
 * Offline or before the first fetch, the snapshot holds the last activated
 * values (Firebase persists them) or the in-app defaults. The game never
 * waits for Remote Config to start.
 *
 * This class does not grant wallet rewards or verify transactions.
 */
class RemoteConfigManager(
    private val remoteConfig: FirebaseRemoteConfig =
        FirebaseRemoteConfig.getInstance()
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _snapshot = MutableStateFlow(RemoteConfigSnapshot.DEFAULT)
    val snapshot: StateFlow<RemoteConfigSnapshot> = _snapshot.asStateFlow()

    @Volatile
    private var initialized = false

    fun initialize() {
        if (initialized) return
        initialized = true

        scope.launch {
            try {
                remoteConfig.setConfigSettingsAsync(
                    FirebaseRemoteConfigSettings.Builder()
                        .setMinimumFetchIntervalInSeconds(DEFAULT_FETCH_INTERVAL_SECONDS)
                        .build()
                ).await()
                remoteConfig.setDefaultsAsync(RemoteConfigParser.DEFAULTS).await()
                // Publish cached/default values straight away, then refresh.
                publishSnapshot()
                remoteConfig.fetchAndActivate().await()
            } catch (exception: Exception) {
                Log.w(TAG, "Remote Config fetch failed; using cached or default values", exception)
            } finally {
                publishSnapshot()
            }
        }

        registerRealTimeListener()
    }

    private fun registerRealTimeListener() {
        remoteConfig.addOnConfigUpdateListener(
            object : ConfigUpdateListener {

                override fun onUpdate(configUpdate: ConfigUpdate) {
                    remoteConfig.activate()
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                publishSnapshot()
                            } else {
                                Log.w(
                                    TAG,
                                    "Could not activate real-time Remote Config update",
                                    task.exception
                                )
                            }
                        }
                }

                override fun onError(error: FirebaseRemoteConfigException) {
                    Log.w(TAG, "Remote Config real-time listener error", error)
                }
            }
        )
    }

    private fun publishSnapshot() {
        val parsed = RemoteConfigParser.parse { key ->
            val value = remoteConfig.getValue(key)
            if (value.source == FirebaseRemoteConfig.VALUE_SOURCE_STATIC) null else value.asString()
        }

        parsed.issues.forEach { issue ->
            Log.w(TAG, "Remote Config '${issue.key}': ${issue.problem}")
        }

        _snapshot.value = parsed.snapshot
    }

    private companion object {
        const val TAG = "RemoteConfigManager"
        const val DEFAULT_FETCH_INTERVAL_SECONDS = 3600L
    }
}
