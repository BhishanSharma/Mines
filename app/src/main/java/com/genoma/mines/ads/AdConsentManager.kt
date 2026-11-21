package com.genoma.mines.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Privacy consent through Google's User Messaging Platform (Stage 3A).
 *
 * Ads are requested only once UMP reports [canRequestAds]. Where a consent
 * message is required (for example the EEA and UK), UMP shows the form
 * configured in the AdMob "Privacy & messaging" console; elsewhere the
 * check completes silently. The privacy options entry point (Settings)
 * lets players change their choice at any time.
 */
class AdConsentManager(context: Context) {

    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(context.applicationContext)

    private val gathering = AtomicBoolean(false)

    private val _canRequestAds = MutableStateFlow(consentInformation.canRequestAds())
    val canRequestAds: StateFlow<Boolean> = _canRequestAds.asStateFlow()

    private val _privacyOptionsRequired = MutableStateFlow(isPrivacyOptionsRequired())
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    /**
     * Refreshes consent and shows the consent form if required. Call on
     * every app start from the launcher activity. [onReady] runs once ads
     * may be requested (immediately if consent was already given earlier).
     */
    fun gatherConsent(activity: Activity, onReady: () -> Unit) {
        if (consentInformation.canRequestAds()) onReady()
        if (!gathering.compareAndSet(false, true)) return

        val params = ConsentRequestParameters.Builder().build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) {
                        Log.w(TAG, "Consent form error ${formError.errorCode}: ${formError.message}")
                    }
                    publishState()
                    gathering.set(false)
                    if (consentInformation.canRequestAds()) onReady()
                }
            },
            { requestError ->
                Log.w(TAG, "Consent info update failed ${requestError.errorCode}: ${requestError.message}")
                publishState()
                gathering.set(false)
                // Offline or misconfigured: ads stay off unless consent was given before.
                if (consentInformation.canRequestAds()) onReady()
            }
        )
    }

    /** Shows the privacy options form so the player can review or change consent. */
    fun showPrivacyOptions(activity: Activity, onDismissed: () -> Unit = {}) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            if (formError != null) {
                Log.w(TAG, "Privacy options error ${formError.errorCode}: ${formError.message}")
            }
            publishState()
            onDismissed()
        }
    }

    private fun isPrivacyOptionsRequired(): Boolean =
        consentInformation.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    private fun publishState() {
        _canRequestAds.value = consentInformation.canRequestAds()
        _privacyOptionsRequired.value = isPrivacyOptionsRequired()
    }

    private companion object {
        const val TAG = "AdConsentManager"
    }
}
