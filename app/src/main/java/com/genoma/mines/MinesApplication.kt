package com.genoma.mines

import android.app.Application
import com.genoma.mines.ads.AdConsentManager
import com.genoma.mines.ads.RewardedAdsManager
import com.genoma.mines.core.remoteconfig.RemoteConfigManager
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class MinesApplication : Application() {

    lateinit var remoteConfigManager: RemoteConfigManager
        private set

    lateinit var rewardedAdsManager: RewardedAdsManager
        private set

    lateinit var adConsentManager: AdConsentManager
        private set

    private val applicationScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val adsInitializationStarted = AtomicBoolean(false)

    override fun onCreate() {
        super.onCreate()

        remoteConfigManager = RemoteConfigManager()
        remoteConfigManager.initialize()

        rewardedAdsManager = RewardedAdsManager()
        adConsentManager = AdConsentManager(this)

        // Consent from a previous session is enough to start straight away;
        // otherwise MainActivity starts ads once the consent flow completes.
        if (adConsentManager.canRequestAds.value) {
            initializeMobileAds()
        }
    }

    /**
     * Initializes the Google Mobile Ads SDK once, and only after UMP says ads
     * may be requested. Safe to call repeatedly.
     */
    fun initializeMobileAds() {
        if (!adConsentManager.canRequestAds.value) return
        if (!adsInitializationStarted.compareAndSet(false, true)) return

        applicationScope.launch {
            try {
                MobileAds.initialize(
                    this@MinesApplication,
                    InitializationConfig.Builder(
                        RewardedAdsManager.TEST_APP_ID
                    ).build()
                ) {
                    rewardedAdsManager.onSdkInitialized()
                }
            } catch (exception: Exception) {
                adsInitializationStarted.set(false)
                rewardedAdsManager.onSdkInitializationFailed(
                    exception.message ?: "Google Mobile Ads initialization failed"
                )
            }
        }
    }
}
