package com.genoma.mines

import android.app.Application
import com.genoma.mines.ads.RewardedAdsManager
import com.genoma.mines.core.remoteconfig.RemoteConfigManager
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MinesApplication : Application() {

    lateinit var remoteConfigManager: RemoteConfigManager
        private set

    lateinit var rewardedAdsManager: RewardedAdsManager
        private set

    private val applicationScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        remoteConfigManager = RemoteConfigManager()
        remoteConfigManager.initialize()

        rewardedAdsManager = RewardedAdsManager()

        initializeMobileAds()
    }

    private fun initializeMobileAds() {
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
                rewardedAdsManager.onSdkInitializationFailed(
                    exception.message ?: "Google Mobile Ads initialization failed"
                )
            }
        }
    }
}