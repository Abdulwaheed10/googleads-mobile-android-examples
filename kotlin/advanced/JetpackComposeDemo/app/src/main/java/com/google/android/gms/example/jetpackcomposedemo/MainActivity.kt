/*
 * Copyright 2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.gms.example.jetpackcomposedemo

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.example.jetpackcomposedemo.ui.TextButton
import com.google.android.gms.example.jetpackcomposedemo.ui.theme.ColorStateError
import com.google.android.gms.example.jetpackcomposedemo.ui.theme.ColorStateLoaded
import com.google.android.gms.example.jetpackcomposedemo.ui.theme.ColorStateUnloaded
import com.google.android.gms.example.jetpackcomposedemo.ui.theme.JetpackComposeDemoTheme
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentRequestParameters

class MainActivity : ComponentActivity() {

  companion object {
    const val TAG = "GoogleMobileAdsSample"
    var adsManager = MobileAdsManager()
  }

  fun InitMobileAds() {
    // Always use test ads: https://developers.google.com/admob/android/test-ads#kotlin
    val testDeviceIds = listOf("33BE2250B43518CCDA7DE426D04EE231")

    // Configure RequestConfiguration.
    val configuration = RequestConfiguration.Builder().setTestDeviceIds(testDeviceIds).build()
    MobileAds.setRequestConfiguration(configuration)

    // Configure ConsentRequestParameters.
    val debugSettings = ConsentDebugSettings.Builder(this)
    // To debug GDPR, uncomment this line to set debug geography to EEA.
    // debugSettings.setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
    testDeviceIds.forEach { deviceId -> debugSettings.addTestDeviceHashedId(deviceId) }
    val consentRequestParameters =
      ConsentRequestParameters.Builder().setConsentDebugSettings(debugSettings.build()).build()

    adsManager.initialize(this, consentRequestParameters = consentRequestParameters)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Configure the Google Mobile Ads SDK.
    if (adsManager.mobileAdsState.value != MobileAdsManager.MobileAdsState.INITIALIZED) {
      InitMobileAds()
    }

    setContent {
      JetpackComposeDemoTheme {
        Surface(modifier = Modifier.fillMaxHeight(), color = colorScheme.background) {
          MainScreen(adsManager = adsManager)
        }
      }
    }
  }

  @Composable
  @Preview
  fun MainScreenPreview() {
    val adsManager = MobileAdsManager()
    JetpackComposeDemoTheme {
      Surface(modifier = Modifier.fillMaxHeight(), color = colorScheme.background) {
        MainScreen(adsManager = adsManager)
      }
    }
  }

  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  fun MainScreen(adsManager: MobileAdsManager) {
    val context = LocalContext.current
    val activity = this
    var mobileAdsStatus by remember { mutableStateOf(adsManager.mobileAdsState.value) }
    var canRequestAds by remember { mutableStateOf(adsManager.canRequestAds()) }
    var canResetConsent by remember { mutableStateOf(adsManager.enablePrivacyOptionsForm()) }

    // Observe changes in MobileAdsState.
    adsManager.onMobileAdsStateChanged = { it ->
      canRequestAds = adsManager.canRequestAds()
      canResetConsent = adsManager.enablePrivacyOptionsForm()
      mobileAdsStatus = it
    }

    Column(
      modifier = Modifier.verticalScroll(rememberScrollState()),
      content = {
        // Render title.
        TopAppBar(title = { Text(text = "Google Mobile Ads Sample") })
        // Render mobile ads status.
        Box(modifier = Modifier.fillMaxSize().background(mobileAdsStatus.messageColor())) {
          Text(text = mobileAdsStatus.messageText(), style = typography.bodyLarge)
        }
        // Show Consent Form.
        TextButton(name = "Show Consent Form", enabled = canResetConsent) {
          adsManager.showPrivacyOptionsForm(activity)
        }
        // Reset Consent Information.
        TextButton(name = "Reset Consent Information") { adsManager.resetConsentInformation() }
        // Open Ad Inspector.
        TextButton(name = "Ad Inspector") {
          MobileAds.openAdInspector(context) { error ->
            if (error != null) {
              val errorMessage = "Failed to open ad inspector with error: ${error.message}"
              Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
              Log.e(TAG, errorMessage)
            }
          }
        }
      },
    )
  }

  // Extend MobileAdsState with message color.
  private fun MobileAdsManager.MobileAdsState.messageColor(): Color {
    return when (this) {
      MobileAdsManager.MobileAdsState.CONSENT_OBTAINED -> {
        ColorStateUnloaded
      }
      MobileAdsManager.MobileAdsState.CONSENT_ERROR -> {
        ColorStateError
      }
      MobileAdsManager.MobileAdsState.INITIALIZED -> {
        ColorStateLoaded
      }
      else -> ColorStateUnloaded
    }
  }

  // Extend MobileAdsState with message text.
  private fun MobileAdsManager.MobileAdsState.messageText(): String {
    when (this) {
      MobileAdsManager.MobileAdsState.UNINITIALIZED -> {
        return "Google Mobile Ads SDK is not initialized."
      }
      MobileAdsManager.MobileAdsState.CONSENT_REQUIRED -> {
        return "Google Mobile Ads SDK requires consent."
      }
      MobileAdsManager.MobileAdsState.CONSENT_OBTAINED -> {
        return "Google Mobile Ads SDK obtained consent."
      }
      MobileAdsManager.MobileAdsState.CONSENT_ERROR -> {
        return "Google Mobile Ads SDK has a consent error."
      }
      MobileAdsManager.MobileAdsState.INITIALIZED -> {
        return "Google Mobile Ads SDK is initialized."
      }
    }
  }
}
