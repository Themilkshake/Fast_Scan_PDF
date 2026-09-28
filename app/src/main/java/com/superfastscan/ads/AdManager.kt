package com.superfastscan.ads

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.superfastscan.BuildConfig
import com.superfastscan.domain.manager.PremiumManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AdManager"

const val INTERSTITIAL_SCAN_AD_UNIT_ID = "ca-app-pub-3810552981107741/2466085035"

const val INTERSTITIAL_TOOLS_AD_UNIT_ID = "ca-app-pub-3810552981107741/4277089646"

private const val PREFS_NAME = "superfastscan_ads_prefs"
private const val KEY_TOOL_ACTION_COUNT = "pdf_tool_action_count"
private const val KEY_DOCUMENT_SCAN_COUNT = "document_scan_count"

// Ad trigger frequency: 1 interstitial ad every 2 tool completions
private const val TOOL_ACTIONS_PER_AD = 2
// Ad trigger frequency: 1 interstitial ad every 5 document scans
private const val SCANS_PER_AD = 5

@Singleton
class AdManager @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val premiumManager: PremiumManager
) {
    private var scanInterstitialAd: InterstitialAd? = null
    private var toolsInterstitialAd: InterstitialAd? = null
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private var actionCount = prefs.getInt(KEY_TOOL_ACTION_COUNT, 0)
    private var scanCount = prefs.getInt(KEY_DOCUMENT_SCAN_COUNT, 0)
    private var isScanAdLoading = false
    private var isToolsAdLoading = false
    private var isInitialized = false

    private val _showAdOverlay = MutableStateFlow(false)
    val showAdOverlay: StateFlow<Boolean> = _showAdOverlay.asStateFlow()

    fun initialize(context: Context) {
        if (isInitialized) return

        CoroutineScope(Dispatchers.IO).launch {
            if (BuildConfig.DEBUG) {
                val testDeviceIds = listOf(
                    AdRequest.DEVICE_ID_EMULATOR,
                    "5C62584158FD721384047C90F7B70E00"
                )
                val requestConfig = RequestConfiguration.Builder()
                    .setTestDeviceIds(testDeviceIds)
                    .build()
                MobileAds.setRequestConfiguration(requestConfig)
            }

            MobileAds.initialize(context.applicationContext) { initializationStatus ->
                Log.d(TAG, "AdMob initialized: $initializationStatus")
                isInitialized = true

                if (!premiumManager.isPremium.value) {
                    CoroutineScope(Dispatchers.Main).launch {
                        loadScanInterstitialAd()
                        loadToolsInterstitialAd()
                    }
                }
            }
        }
    }

    private fun loadScanInterstitialAd() {
        if (scanInterstitialAd != null || isScanAdLoading || premiumManager.isPremium.value) {
            return
        }

        isScanAdLoading = true
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            appContext,
            INTERSTITIAL_SCAN_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Scan Interstitial ad loaded successfully.")
                    scanInterstitialAd = ad
                    isScanAdLoading = false
                    setupScanAdCallback(ad)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(TAG, "Scan Interstitial ad failed to load: ${loadAdError.message}")
                    scanInterstitialAd = null
                    isScanAdLoading = false
                }
            }
        )
    }

    private fun setupScanAdCallback(ad: InterstitialAd) {
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Scan Interstitial ad was dismissed.")
                scanInterstitialAd = null
                loadScanInterstitialAd()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "Scan Interstitial ad failed to show: ${adError.message}")
                scanInterstitialAd = null
                loadScanInterstitialAd()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Scan Interstitial ad showed successfully.")
                scanInterstitialAd = null
            }
        }
    }

    private fun loadToolsInterstitialAd() {
        if (toolsInterstitialAd != null || isToolsAdLoading || premiumManager.isPremium.value) {
            return
        }

        isToolsAdLoading = true
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            appContext,
            INTERSTITIAL_TOOLS_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Tools Interstitial ad loaded successfully.")
                    toolsInterstitialAd = ad
                    isToolsAdLoading = false
                    setupToolsAdCallback(ad)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(TAG, "Tools Interstitial ad failed to load: ${loadAdError.message}")
                    toolsInterstitialAd = null
                    isToolsAdLoading = false
                }
            }
        )
    }

    private fun setupToolsAdCallback(ad: InterstitialAd) {
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Tools Interstitial ad was dismissed.")
                toolsInterstitialAd = null
                loadToolsInterstitialAd()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "Tools Interstitial ad failed to show: ${adError.message}")
                toolsInterstitialAd = null
                loadToolsInterstitialAd()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Tools Interstitial ad showed successfully.")
                toolsInterstitialAd = null
            }
        }
    }

    fun trackToolActionAndShowAdIfNeeded(activity: Activity) {
        if (premiumManager.isPremium.value) {
            Log.d(TAG, "User is premium. Ad tracking ignored.")
            return
        }

        actionCount++
        prefs.edit().putInt(KEY_TOOL_ACTION_COUNT, actionCount).apply()
        Log.d(TAG, "PDF tool action tracked. Current count: $actionCount (Ad triggers every $TOOL_ACTIONS_PER_AD uses)")

        if (actionCount % TOOL_ACTIONS_PER_AD == 0) {
            Log.d(TAG, "Showing interstitial ad for tool completion (count=$actionCount)...")
            showToolsInterstitialAd(activity)
        }
    }

    fun trackScanAndShowAdIfNeeded(activity: Activity, isReviewMilestone: Boolean = false) {
        if (premiumManager.isPremium.value) {
            Log.d(TAG, "User is premium. Scan ad tracking ignored.")
            return
        }

        scanCount++
        prefs.edit().putInt(KEY_DOCUMENT_SCAN_COUNT, scanCount).apply()
        Log.d(TAG, "Document scan tracked. Current count: $scanCount (Ad triggers every $SCANS_PER_AD scans)")

        if (isReviewMilestone) {
            Log.d(TAG, "Scan #10 review milestone active. Suppressing interstitial ad to prioritize review.")
            return
        }

        if (scanCount % SCANS_PER_AD == 0) {
            Log.d(TAG, "Showing interstitial ad for document scan milestone (count=$scanCount)...")
            showScanInterstitialAd(activity)
        }
    }

    fun trackActionAndShowAdIfNeeded(activity: Activity) {
        trackToolActionAndShowAdIfNeeded(activity)
    }

    private fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    private fun showScanInterstitialAd(activity: Activity) {
        if (premiumManager.isPremium.value) {
            Log.d(TAG, "User is premium. showScanInterstitialAd aborted.")
            return
        }

        activity.runOnUiThread {
            if (activity.isFinishing || activity.isDestroyed) {
                Log.w(TAG, "Activity is finishing or destroyed. Skipping ad.")
                return@runOnUiThread
            }

            val currentAd = scanInterstitialAd
            if (currentAd != null) {
                setupScanAdCallback(currentAd)
                currentAd.show(activity)
            } else {
                if (!isInternetAvailable(activity.applicationContext)) {
                    Log.w(TAG, "No internet connection. Skipping ad to avoid blocking UI.")
                    return@runOnUiThread
                }

                Log.w(TAG, "Scan interstitial ad was not ready. Blocking UI to fetch...")
                _showAdOverlay.value = true
                isScanAdLoading = true

                val adRequest = AdRequest.Builder().build()
                InterstitialAd.load(
                    appContext,
                    INTERSTITIAL_SCAN_AD_UNIT_ID,
                    adRequest,
                    object : InterstitialAdLoadCallback() {
                        override fun onAdLoaded(ad: InterstitialAd) {
                            Log.d(TAG, "Scan interstitial ad fetched on demand.")
                            scanInterstitialAd = ad
                            isScanAdLoading = false
                            _showAdOverlay.value = false

                            if (!activity.isFinishing && !activity.isDestroyed) {
                                setupScanAdCallback(ad)
                                ad.show(activity)
                            }
                        }

                        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                            Log.e(TAG, "Failed to fetch scan ad on demand: ${loadAdError.message}")
                            scanInterstitialAd = null
                            isScanAdLoading = false
                            _showAdOverlay.value = false
                        }
                    }
                )
            }
        }
    }

    private fun showToolsInterstitialAd(activity: Activity) {
        if (premiumManager.isPremium.value) {
            Log.d(TAG, "User is premium. showToolsInterstitialAd aborted.")
            return
        }

        activity.runOnUiThread {
            if (activity.isFinishing || activity.isDestroyed) {
                Log.w(TAG, "Activity is finishing or destroyed. Skipping ad.")
                return@runOnUiThread
            }

            val currentAd = toolsInterstitialAd
            if (currentAd != null) {
                setupToolsAdCallback(currentAd)
                currentAd.show(activity)
            } else {
                if (!isInternetAvailable(activity.applicationContext)) {
                    Log.w(TAG, "No internet connection. Skipping ad to avoid blocking UI.")
                    return@runOnUiThread
                }

                Log.w(TAG, "Tools interstitial ad was not ready. Blocking UI to fetch...")
                _showAdOverlay.value = true
                isToolsAdLoading = true

                val adRequest = AdRequest.Builder().build()
                InterstitialAd.load(
                    appContext,
                    INTERSTITIAL_TOOLS_AD_UNIT_ID,
                    adRequest,
                    object : InterstitialAdLoadCallback() {
                        override fun onAdLoaded(ad: InterstitialAd) {
                            toolsInterstitialAd = ad
                            isToolsAdLoading = false
                            _showAdOverlay.value = false

                            if (!activity.isFinishing && !activity.isDestroyed) {
                                setupToolsAdCallback(ad)
                                ad.show(activity)
                            }
                        }

                        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                            Log.e(TAG, "Failed to fetch tools ad on demand: ${loadAdError.message}")
                            toolsInterstitialAd = null
                            isToolsAdLoading = false
                            _showAdOverlay.value = false
                        }
                    }
                )
            }
        }
    }
}
