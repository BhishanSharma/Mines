package com.genoma.mines.ads

import android.app.Activity
import android.os.Handler
import android.os.Looper
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

sealed interface RewardedAdState {
    data object Initializing : RewardedAdState
    data object Loading : RewardedAdState
    data object Ready : RewardedAdState
    data object Showing : RewardedAdState
    data class Unavailable(val reason: String) : RewardedAdState
}

class RewardedAdsManager {

    companion object {
        /**
         * Google's sample AdMob application ID.
         * Replace with the real application ID before production release.
         */
        const val TEST_APP_ID =
            "ca-app-pub-3940256099942544~3347511713"

        /**
         * Google's GMA Next-Gen test rewarded ad unit.
         * Never use a production ad unit while developing or testing.
         */
        const val TEST_REWARDED_AD_UNIT_ID =
            "/21775744923/example/rewarded"

        private const val RETRY_BASE_DELAY_MILLIS = 5_000L
        private const val RETRY_MAX_DELAY_MILLIS = 5 * 60_000L
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    private val mutableState =
        MutableStateFlow<RewardedAdState>(RewardedAdState.Initializing)

    val state: StateFlow<RewardedAdState> = mutableState.asStateFlow()

    private var sdkInitialized = false
    private var isLoading = false
    private var rewardedAd: RewardedAd? = null

    // Failed loads are retried with exponential backoff so one failure
    // (offline at launch, a no-fill) does not disable ads for the session.
    private var failedLoadAttempts = 0
    private var retryScheduled = false
    private val retryLoad = Runnable {
        retryScheduled = false
        loadRewardedAdInternal()
    }

    /**
     * Called once the Google Mobile Ads SDK initialization callback completes.
     */
    fun onSdkInitialized() {
        runOnMain {
            sdkInitialized = true
            loadRewardedAdInternal()
        }
    }

    /**
     * Called if SDK initialization throws an exception.
     */
    fun onSdkInitializationFailed(reason: String) {
        runOnMain {
            sdkInitialized = false
            rewardedAd = null
            isLoading = false
            mutableState.value = RewardedAdState.Unavailable(reason)
        }
    }

    /**
     * Loads one rewarded ad if the SDK is ready and no ad is already loading
     * or cached.
     */
    fun loadRewardedAd() {
        runOnMain {
            // An explicit request (for example the win screen appearing)
            // skips any pending backoff and tries straight away.
            if (retryScheduled) {
                mainHandler.removeCallbacks(retryLoad)
                retryScheduled = false
            }
            loadRewardedAdInternal()
        }
    }

    private fun loadRewardedAdInternal() {
        if (!sdkInitialized || isLoading || rewardedAd != null) {
            return
        }

        isLoading = true
        mutableState.value = RewardedAdState.Loading

        try {
            val request = AdRequest.Builder(
                TEST_REWARDED_AD_UNIT_ID
            ).build()

            RewardedAd.load(
                request,
                object : AdLoadCallback<RewardedAd> {

                    override fun onAdLoaded(ad: RewardedAd) {
                        runOnMain {
                            isLoading = false
                            failedLoadAttempts = 0
                            rewardedAd = ad
                            mutableState.value = RewardedAdState.Ready
                        }
                    }

                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        runOnMain {
                            isLoading = false
                            rewardedAd = null
                            mutableState.value =
                                RewardedAdState.Unavailable(adError.message)
                            scheduleRetry()
                        }
                    }
                }
            )
        } catch (exception: Exception) {
            isLoading = false
            rewardedAd = null
            mutableState.value = RewardedAdState.Unavailable(
                exception.message ?: "Unable to load rewarded ad"
            )
            scheduleRetry()
        }
    }

    private fun scheduleRetry() {
        if (retryScheduled || !sdkInitialized) return

        val delay = (RETRY_BASE_DELAY_MILLIS shl failedLoadAttempts.coerceAtMost(6))
            .coerceAtMost(RETRY_MAX_DELAY_MILLIS)
        failedLoadAttempts++
        retryScheduled = true
        mainHandler.postDelayed(retryLoad, delay)
    }

    /**
     * Shows the cached rewarded ad.
     *
     * onRewardEarned is called only when Google's reward callback confirms
     * that the user earned the reward. Dismissing an ad does not grant a reward.
     *
     * onAdClosed is called after the full-screen ad is dismissed.
     * onAdFailed is called if the ad cannot be shown.
     *
     * Returns false immediately when no ad is ready or another ad is showing.
     * Returns true once the request is handled; a failure while showing is
     * reported through [onAdFailed] only, so callers show a single message.
     */
    fun showRewardedAd(
        activity: Activity,
        onRewardEarned: () -> Unit,
        onAdClosed: () -> Unit = {},
        onAdFailed: (String) -> Unit = {}
    ): Boolean {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            return false
        }

        val ad = rewardedAd ?: return false

        if (mutableState.value == RewardedAdState.Showing) {
            return false
        }

        rewardedAd = null
        mutableState.value = RewardedAdState.Showing

        val rewardDelivered = AtomicBoolean(false)
        val completionDelivered = AtomicBoolean(false)

        fun finishAndReload() {
            if (completionDelivered.compareAndSet(false, true)) {
                rewardedAd = null
                mutableState.value = RewardedAdState.Loading
                onAdClosed()
                loadRewardedAdInternal()
            }
        }

        ad.adEventCallback = object : RewardedAdEventCallback {

            override fun onAdDismissedFullScreenContent() {
                runOnMain {
                    finishAndReload()
                }
            }

            override fun onAdFailedToShowFullScreenContent(
                fullScreenContentError: FullScreenContentError
            ) {
                runOnMain {
                    if (completionDelivered.compareAndSet(false, true)) {
                        rewardedAd = null
                        mutableState.value = RewardedAdState.Unavailable(
                            fullScreenContentError.message
                        )
                        onAdFailed(fullScreenContentError.message)
                        loadRewardedAdInternal()
                    }
                }
            }

            override fun onAdShowedFullScreenContent() {
                // The ad is now visible. Reward is not granted at this point.
            }

            override fun onAdImpression() {
                // Impression tracking can be added here if needed.
            }

            override fun onAdClicked() {
                // Click tracking can be added here if needed.
            }
        }

        return try {
            ad.show(activity) {
                if (rewardDelivered.compareAndSet(false, true)) {
                    runOnMain {
                        onRewardEarned()
                    }
                }
            }
            true
        } catch (exception: Exception) {
            if (completionDelivered.compareAndSet(false, true)) {
                rewardedAd = null
                mutableState.value = RewardedAdState.Unavailable(
                    exception.message ?: "Unable to show rewarded ad"
                )
                onAdFailed(exception.message ?: "Unable to show rewarded ad")
                loadRewardedAdInternal()
            }
            true
        }
    }

    private fun runOnMain(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action()
        } else {
            mainHandler.post(action)
        }
    }
}