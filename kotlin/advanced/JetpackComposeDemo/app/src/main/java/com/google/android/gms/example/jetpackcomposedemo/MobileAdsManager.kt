package com.google.android.gms.example.jetpackcomposedemo

import android.app.Activity
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.*
import java.util.concurrent.atomic.AtomicBoolean

/** This class manages the process of obtaining consent for and initializing Google Mobile Ads. */
class MobileAdsManager() {

  companion object {
    const val TAG = "GoogleMobileAdsSample"
  }

  /** Represents current initialization states for the Google Mobile Ads SDK. */
  var mobileAdsState = mutableStateOf(MobileAdsState.UNINITIALIZED)

  /** Lambda invoked when states for the Google Mobile Ads SDK changes. */
  var onMobileAdsStateChanged: ((MobileAdsState) -> Unit)? = null

  /** Represents potentially initialization states for the Google Mobile Ads SDK. */
  enum class MobileAdsState {
    UNINITIALIZED, // Initial start state
    CONSENT_REQUIRED, // User consent needs to be obtained
    CONSENT_OBTAINED, // User has granted consent
    CONSENT_ERROR, // An error occurred during the consent process
    INITIALIZED, // Google Mobile Ads SDK initialized successfully
  }

  private var isMobileAdsInitializeCalled = AtomicBoolean(false)

  private lateinit var consentInformation: ConsentInformation

  /**
   * Initiates the consent process and potentially initializes the Google Mobile Ads SDK.
   *
   * @param activity Activity responsible for initializing the Google Mobile Ads SDK.
   * @param consentRequestParameters Parameters for the consent request form.
   */
  fun initialize(activity: Activity, consentRequestParameters: ConsentRequestParameters) {

    if (isMobileAdsInitializeCalled.getAndSet(true)) {
      return
    }

    consentInformation = UserMessagingPlatform.getConsentInformation(activity)

    consentInformation.requestConsentInfoUpdate(
      activity,
      consentRequestParameters,
      {
        // Success callback.
        showConsentFormIfRequired(activity) { error ->
          if (error != null) {
            Log.w(TAG, "Consent form error: ${error.errorCode} - ${error.message}")
            mobileAdsState.value = MobileAdsState.CONSENT_ERROR
            onMobileAdsStateChanged?.invoke(mobileAdsState.value)
          } else {
            mobileAdsState.value = MobileAdsState.CONSENT_OBTAINED
            onMobileAdsStateChanged?.invoke(mobileAdsState.value)
            if (consentInformation.canRequestAds()) {
              initializeMobileAdsSdk(activity)
            }
          }
        }
      },
      {
        // Failure callback.
        Log.w(TAG, "Consent info update error: ${it.errorCode} - ${it.message}")
        mobileAdsState.value = MobileAdsState.CONSENT_ERROR
        onMobileAdsStateChanged?.invoke(mobileAdsState.value)
      },
    )
  }

  /**
   * Indicates whether the app has completed the necessary steps for gathering updated user consent.
   */
  fun canRequestAds(): Boolean {
    return consentInformation.canRequestAds()
  }

  /** Indicates whether the app should enable the update consent update button. */
  fun enablePrivacyOptionsForm(): Boolean {
    return consentInformation.privacyOptionsRequirementStatus ==
      ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
  }

  /** Shows the update consent update form. */
  fun showPrivacyOptionsForm(activity: Activity) {
    UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
      if (error != null) {
        mobileAdsState.value = MobileAdsState.CONSENT_ERROR
      }
    }
  }

  /** Resets consent information for the user. */
  fun resetConsentInformation() {
    consentInformation.reset()
  }

  /**
   * Initializes Mobile Ads SDK without consent.
   *
   * @param activity Activity responsible for initializing the Google Mobile Ads SDK.
   */
  fun initializeMobileAdsSdk(activity: Activity) {
    MobileAds.initialize(activity) {
      Log.d(TAG, "Mobile Ads SDK initialized")
      mobileAdsState.value = MobileAdsState.INITIALIZED
      onMobileAdsStateChanged?.invoke(mobileAdsState.value)
    }
  }

  private fun showConsentFormIfRequired(activity: Activity, onFormResult: (FormError?) -> Unit) {
    UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity, onFormResult)
  }
}
