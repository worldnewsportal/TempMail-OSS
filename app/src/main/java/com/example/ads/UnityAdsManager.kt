package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.preferences.AppSettingsRepository
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import com.unity3d.services.banners.BannerView
import com.unity3d.services.banners.UnityBannerSize

/**
 * Unity Ads Manager - PRODUCTION ONLY
 *
 * This manager is hardcoded to PRODUCTION mode. Test mode is NEVER used.
 *
 * IMPORTANT: If you still see test ads (with "Live Unity Ad" badge or debug timer),
 * you MUST check the Unity Dashboard:
 *
 *   1. Go to https://dashboard.unity3d.com
 *   2. Select your project (Game ID: 6043972)
 *   3. Go to Monetization > Project Settings > Test Mode
 *   4. For Android: Set "Override Client Test Mode" -> "Force Test Mode OFF"
 *   5. Save and wait up to 24 hours for changes to propagate
 *
 * The Dashboard test mode override takes PRECEDENCE over the SDK parameter.
 * If the Dashboard is set to "Force Test Mode ON", real ads will NEVER show
 * regardless of what the code says.
 */
object UnityAdsManager {

    private const val TAG = "UnityAdsManager"

    // Production Unity Ads Game ID
    private const val GAME_ID = "6043972"

    // Placement IDs - must match Unity Dashboard exactly
    private const val BANNER_PLACEMENT = "Banner_Android"
    private const val INTERSTITIAL_PLACEMENT = "Interstitial_Android"
    private const val REWARDED_PLACEMENT = "Rewarded_Android"

    // PRODUCTION MODE - hardcoded, never test mode
    private const val TEST_MODE = false

    var isInitialized = false
        private set

    private var isInitializing = false

    // ========== INITIALIZATION ==========

    /**
     * Initialize Unity Ads SDK in PRODUCTION mode.
     * Test mode is NEVER enabled - hardcoded to false.
     */
    fun initialize(context: Context) {
        if (isInitialized || isInitializing) return
        isInitializing = true

        Log.i(TAG, "============================================")
        Log.i(TAG, "Unity Ads SDK - PRODUCTION initialization")
        Log.i(TAG, "  Game ID: $GAME_ID")
        Log.i(TAG, "  Test Mode: $TEST_MODE (hardcoded)")
        Log.i(TAG, "  Banner: $BANNER_PLACEMENT")
        Log.i(TAG, "  Interstitial: $INTERSTITIAL_PLACEMENT")
        Log.i(TAG, "  Rewarded: $REWARDED_PLACEMENT")
        Log.i(TAG, "============================================")

        val appContext = context.applicationContext

        try {
            UnityAds.initialize(
                appContext,
                GAME_ID,
                TEST_MODE,
                object : IUnityAdsInitializationListener {
                    override fun onInitializationComplete() {
                        isInitialized = true
                        isInitializing = false
                        Log.i(TAG, "Unity Ads initialized - PRODUCTION MODE")
                        Log.i(TAG, "If you still see test ads, check Unity Dashboard:")
                        Log.i(TAG, "  Monetization > Project Settings > Test Mode > Force OFF")
                    }

                    override fun onInitializationFailed(
                        error: UnityAds.UnityAdsInitializationError?,
                        message: String?
                    ) {
                        isInitialized = false
                        isInitializing = false
                        Log.e(TAG, "Unity Ads initialization FAILED!")
                        Log.e(TAG, "  Error: $error")
                        Log.e(TAG, "  Message: $message")
                        Log.e(TAG, "  Game ID: $GAME_ID")
                        when (error) {
                            UnityAds.UnityAdsInitializationError.INTERNAL_ERROR ->
                                Log.e(TAG, "  -> Internal error. Check internet connection.")
                            UnityAds.UnityAdsInitializationError.INVALID_ARGUMENT ->
                                Log.e(TAG, "  -> Invalid argument. Check Game ID.")
                            UnityAds.UnityAdsInitializationError.AD_BLOCKER_DETECTED ->
                                Log.e(TAG, "  -> Ad blocker detected!")
                            else ->
                                Log.e(TAG, "  -> Unknown error. Verify Game ID at dashboard.unity3d.com")
                        }
                    }
                }
            )
        } catch (e: Throwable) {
            isInitialized = false
            isInitializing = false
            Log.e(TAG, "Exception during Unity Ads init: ${e.message}", e)
        }
    }

    // ========== REWARDED ADS ==========

    fun showRewardedAd(
        activity: Activity,
        placementId: String = REWARDED_PLACEMENT,
        onComplete: () -> Unit,
        onFailed: () -> Unit
    ) {
        if (!isInitialized) {
            Log.e(TAG, "Cannot show rewarded ad - SDK not initialized")
            onFailed()
            return
        }

        Log.d(TAG, "Loading rewarded ad: $placementId")
        UnityAds.load(placementId, object : IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(placement: String?) {
                Log.i(TAG, "Rewarded ad loaded, showing...")
                UnityAds.show(activity, placementId, object : IUnityAdsShowListener {
                    override fun onUnityAdsShowFailure(
                        placement: String?,
                        error: UnityAds.UnityAdsShowError?,
                        message: String?
                    ) {
                        Log.e(TAG, "Rewarded ad show failed: $message (error: $error)")
                        onFailed()
                    }

                    override fun onUnityAdsShowStart(placement: String?) {
                        Log.d(TAG, "Rewarded ad started")
                    }

                    override fun onUnityAdsShowClick(placement: String?) {
                        Log.d(TAG, "Rewarded ad clicked")
                    }

                    override fun onUnityAdsShowComplete(
                        placement: String?,
                        state: UnityAds.UnityAdsShowCompletionState?
                    ) {
                        if (state == UnityAds.UnityAdsShowCompletionState.COMPLETED) {
                            Log.i(TAG, "Rewarded ad completed - granting reward")
                            onComplete()
                        } else {
                            Log.w(TAG, "Rewarded ad not completed (state: $state)")
                            onFailed()
                        }
                    }
                })
            }

            override fun onUnityAdsFailedToLoad(
                placement: String?,
                error: UnityAds.UnityAdsLoadError?,
                message: String?
            ) {
                Log.e(TAG, "Rewarded ad failed to load: $message (error: $error)")
                onFailed()
            }
        })
    }

    // ========== INTERSTITIAL ADS ==========

    fun showInterstitialAd(
        activity: Activity,
        placementId: String = INTERSTITIAL_PLACEMENT,
        onClosed: () -> Unit = {},
        onFailed: () -> Unit = {}
    ) {
        if (!isInitialized) {
            Log.e(TAG, "Cannot show interstitial ad - SDK not initialized")
            onFailed()
            return
        }

        Log.d(TAG, "Loading interstitial ad: $placementId")
        UnityAds.load(placementId, object : IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(placement: String?) {
                Log.i(TAG, "Interstitial ad loaded, showing...")
                UnityAds.show(activity, placementId, object : IUnityAdsShowListener {
                    override fun onUnityAdsShowFailure(
                        placement: String?,
                        error: UnityAds.UnityAdsShowError?,
                        message: String?
                    ) {
                        Log.e(TAG, "Interstitial ad show failed: $message (error: $error)")
                        onFailed()
                    }

                    override fun onUnityAdsShowStart(placement: String?) {
                        Log.d(TAG, "Interstitial ad started")
                    }

                    override fun onUnityAdsShowClick(placement: String?) {
                        Log.d(TAG, "Interstitial ad clicked")
                    }

                    override fun onUnityAdsShowComplete(
                        placement: String?,
                        state: UnityAds.UnityAdsShowCompletionState?
                    ) {
                        Log.i(TAG, "Interstitial ad closed")
                        onClosed()
                    }
                })
            }

            override fun onUnityAdsFailedToLoad(
                placement: String?,
                error: UnityAds.UnityAdsLoadError?,
                message: String?
            ) {
                Log.e(TAG, "Interstitial ad failed to load: $message (error: $error)")
                onFailed()
            }
        })
    }

    // ========== BANNER AD COMPOSABLE ==========

    /**
     * Real Unity Banner Ad composable - PRODUCTION ONLY.
     *
     * Shows real Unity Ads banners. No test mode, no simulated ads.
     * - While loading: shows a compact loading indicator
     * - If failed: shows a small "Tap to retry" text
     * - Ad-free mode: shows nothing
     */
    @Composable
    fun UnityAdBanner(
        modifier: Modifier = Modifier,
        placementId: String = BANNER_PLACEMENT
    ) {
        val context = LocalContext.current
        val settingsRepo = remember(context) { AppSettingsRepository(context) }
        val settings by settingsRepo.settingsFlow.collectAsState(
            initial = com.example.data.preferences.AppSettings()
        )
        val isAdFree = System.currentTimeMillis() < settings.adFreeUntil

        if (isAdFree) return

        var isLoaded by remember { mutableStateOf(false) }
        var adLoadFailed by remember { mutableStateOf(false) }
        var retryKey by remember { mutableIntStateOf(0) }

        // Initialize SDK if needed
        LaunchedEffect(Unit) {
            if (!isInitialized && !isInitializing) {
                initialize(context)
            }
        }

        // Not yet initialized - show loading
        if (!isInitialized) {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .height(50.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 1.5.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            }
            return
        }

        // Ad failed to load - show retry
        if (adLoadFailed) {
            TextButton(
                onClick = {
                    adLoadFailed = false
                    isLoaded = false
                    retryKey++
                },
                modifier = modifier
                    .fillMaxWidth()
                    .height(32.dp)
            ) {
                Text(
                    text = "Ad - Tap to retry",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )
            }
            return
        }

        // Real Unity Banner
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(50.dp),
            contentAlignment = Alignment.Center
        ) {
            if (!isLoaded) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 1.5.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )
            }

            key(retryKey) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        FrameLayout(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            try {
                                val activity = generateSequence(ctx) {
                                    if (it is android.content.ContextWrapper) it.baseContext else null
                                }.filterIsInstance<Activity>().firstOrNull()

                                if (activity == null) {
                                    Log.e(TAG, "Banner: Context is not an Activity")
                                    adLoadFailed = true
                                    return@apply
                                }

                                val bannerView = BannerView(
                                    activity, placementId, UnityBannerSize(320, 50)
                                )
                                bannerView.listener = object : BannerView.IListener {
                                    override fun onBannerLoaded(view: BannerView?) {
                                        isLoaded = true
                                        Log.i(TAG, "Banner ad loaded: $placementId")
                                    }

                                    override fun onBannerClick(view: BannerView?) {
                                        Log.d(TAG, "Banner clicked")
                                    }

                                    override fun onBannerFailedToLoad(
                                        view: BannerView?,
                                        error: com.unity3d.services.banners.BannerErrorInfo?
                                    ) {
                                        adLoadFailed = true
                                        Log.e(TAG, "Banner failed: $placementId")
                                        Log.e(TAG, "  Error: ${error?.errorCode} - ${error?.errorMessage}")
                                    }

                                    override fun onBannerLeftApplication(view: BannerView?) {}

                                    override fun onBannerShown(view: BannerView?) {
                                        Log.i(TAG, "Banner displayed on screen")
                                    }
                                }
                                bannerView.load()
                                addView(bannerView)
                            } catch (e: Throwable) {
                                adLoadFailed = true
                                Log.e(TAG, "Banner exception: ${e.message}", e)
                            }
                        }
                    }
                )
            }
        }
    }
}
