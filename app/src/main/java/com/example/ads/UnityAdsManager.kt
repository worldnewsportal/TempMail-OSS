package com.example.ads

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.UnityAds
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.services.banners.BannerView
import com.unity3d.services.banners.UnityBannerSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import com.example.data.preferences.AppSettingsRepository

/**
 * Unity Ads Manager — Handles initialization, banner ads, interstitial ads,
 * and rewarded video ads using the Unity Ads SDK.
 *
 * IMPORTANT NOTES FOR PRODUCTION:
 * - Make sure Placements (Banner_Android, Interstitial_Android, Rewarded_Android)
 *   are created and enabled in Unity Dashboard for Game ID 6043972
 * - Banner ads may have low fill rate in some regions. Interstitial ads
 *   typically have much better fill rates.
 * - It can take up to 1 hour after first init for Unity's servers to
 *   start serving real ads to a new Game ID / Placement.
 * - If ads don't show immediately, check logcat for "UnityAdsManager" tags.
 */
object UnityAdsManager {
    private const val TAG = "UnityAdsManager"

    // Production Unity Ads Game ID
    private const val GAME_ID_ANDROID = "6043972"

    // Placement IDs — MUST match exactly what's configured in Unity Dashboard
    private const val BANNER_PLACEMENT = "Banner_Android"
    private const val INTERSTITIAL_PLACEMENT = "Interstitial_Android"
    private const val REWARDED_PLACEMENT = "Rewarded_Android"

    var isInitialized = false
        private set

    private var isInitializing = false

    // ========== INITIALIZATION ==========

    fun initialize(context: Context) {
        if (isInitialized || isInitializing) return
        isInitializing = true

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settingsRepo = AppSettingsRepository(context)
                val settings = settingsRepo.settingsFlow.first()
                val testMode = settings.adsTestMode
                withContext(Dispatchers.Main) {
                    doInitialize(context.applicationContext, testMode)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.w(TAG, "Failed to read settings, defaulting to production mode", e)
                    doInitialize(context.applicationContext, false)
                }
            }
        }
    }

    private fun doInitialize(context: Context, testMode: Boolean) {
        if (isInitialized) return

        Log.i(TAG, "========================================")
        Log.i(TAG, "Initializing Unity Ads SDK...")
        Log.i(TAG, "  Game ID: $GAME_ID_ANDROID")
        Log.i(TAG, "  Test Mode: $testMode")
        Log.i(TAG, "  Banner Placement: $BANNER_PLACEMENT")
        Log.i(TAG, "  Interstitial Placement: $INTERSTITIAL_PLACEMENT")
        Log.i(TAG, "  Rewarded Placement: $REWARDED_PLACEMENT")
        Log.i(TAG, "========================================")

        try {
            UnityAds.initialize(
                context,
                GAME_ID_ANDROID,
                testMode,
                object : IUnityAdsInitializationListener {
                    override fun onInitializationComplete() {
                        isInitialized = true
                        isInitializing = false
                        Log.i(TAG, "✅ Unity Ads SDK initialized successfully!")
                        Log.i(TAG, "   Test mode: $testMode")
                        Log.i(TAG, "   If this is a new Game ID, real ads may take up to 1 hour to start serving.")
                        Log.i(TAG, "   Check Unity Dashboard > Monetization > Placements to verify setup.")
                    }

                    override fun onInitializationFailed(
                        error: UnityAds.UnityAdsInitializationError?,
                        message: String?
                    ) {
                        isInitialized = false
                        isInitializing = false
                        Log.e(TAG, "❌ Unity Ads initialization FAILED!")
                        Log.e(TAG, "   Error: $error")
                        Log.e(TAG, "   Message: $message")
                        Log.e(TAG, "   Game ID: $GAME_ID_ANDROID")
                        when (error) {
                            UnityAds.UnityAdsInitializationError.INTERNAL_ERROR ->
                                Log.e(TAG, "   → Internal error. Check internet connection and try again.")
                            UnityAds.UnityAdsInitializationError.INVALID_ARGUMENT ->
                                Log.e(TAG, "   → Invalid argument. Check Game ID and make sure initialize() is called with application context.")
                            UnityAds.UnityAdsInitializationError.AD_BLOCKER_DETECTED ->
                                Log.e(TAG, "   → Ad blocker detected! Disable ad blockers to use Unity Ads.")
                            else ->
                                Log.e(TAG, "   → Unknown error. Verify Game ID at https://dashboard.unity3d.com")
                        }
                    }
                }
            )
        } catch (e: Throwable) {
            isInitialized = false
            isInitializing = false
            Log.e(TAG, "❌ Exception during Unity Ads init: ${e.message}", e)
        }
    }

    // ========== REWARDED ADS ==========

    fun showRewardedAd(
        activity: android.app.Activity,
        placementId: String = REWARDED_PLACEMENT,
        onComplete: () -> Unit,
        onFailed: () -> Unit
    ) {
        if (!isInitialized) {
            Log.e(TAG, "Cannot show rewarded ad — SDK not initialized")
            onFailed()
            return
        }

        Log.d(TAG, "Loading rewarded ad: $placementId")
        UnityAds.load(placementId, object : IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(placement: String?) {
                Log.i(TAG, "✅ Rewarded ad loaded, showing...")
                UnityAds.show(activity, placementId, object : IUnityAdsShowListener {
                    override fun onUnityAdsShowFailure(placement: String?, error: UnityAds.UnityAdsShowError?, message: String?) {
                        Log.e(TAG, "❌ Rewarded ad show failed: $message (error: $error)")
                        onFailed()
                    }
                    override fun onUnityAdsShowStart(placement: String?) {
                        Log.d(TAG, "Rewarded ad started")
                    }
                    override fun onUnityAdsShowClick(placement: String?) {
                        Log.d(TAG, "Rewarded ad clicked")
                    }
                    override fun onUnityAdsShowComplete(placement: String?, state: UnityAds.UnityAdsShowCompletionState?) {
                        if (state == UnityAds.UnityAdsShowCompletionState.COMPLETED) {
                            Log.i(TAG, "✅ Rewarded ad watched completely — granting reward")
                            onComplete()
                        } else {
                            Log.w(TAG, "Rewarded ad not completed fully (state: $state)")
                            onFailed()
                        }
                    }
                })
            }
            override fun onUnityAdsFailedToLoad(placement: String?, error: UnityAds.UnityAdsLoadError?, message: String?) {
                Log.e(TAG, "❌ Rewarded ad failed to load: $message (error: $error)")
                onFailed()
            }
        })
    }

    // ========== INTERSTITIAL ADS ==========

    fun showInterstitialAd(
        activity: android.app.Activity,
        placementId: String = INTERSTITIAL_PLACEMENT,
        onClosed: () -> Unit = {},
        onFailed: () -> Unit = {}
    ) {
        if (!isInitialized) {
            Log.e(TAG, "Cannot show interstitial ad — SDK not initialized")
            onFailed()
            return
        }

        Log.d(TAG, "Loading interstitial ad: $placementId")
        UnityAds.load(placementId, object : IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(placement: String?) {
                Log.i(TAG, "✅ Interstitial ad loaded, showing...")
                UnityAds.show(activity, placementId, object : IUnityAdsShowListener {
                    override fun onUnityAdsShowFailure(placement: String?, error: UnityAds.UnityAdsShowError?, message: String?) {
                        Log.e(TAG, "❌ Interstitial ad show failed: $message (error: $error)")
                        onFailed()
                    }
                    override fun onUnityAdsShowStart(placement: String?) {
                        Log.d(TAG, "Interstitial ad started")
                    }
                    override fun onUnityAdsShowClick(placement: String?) {
                        Log.d(TAG, "Interstitial ad clicked")
                    }
                    override fun onUnityAdsShowComplete(placement: String?, state: UnityAds.UnityAdsShowCompletionState?) {
                        Log.i(TAG, "Interstitial ad closed")
                        onClosed()
                    }
                })
            }
            override fun onUnityAdsFailedToLoad(placement: String?, error: UnityAds.UnityAdsLoadError?, message: String?) {
                Log.e(TAG, "❌ Interstitial ad failed to load: $message (error: $error)")
                onFailed()
            }
        })
    }

    // ========== BANNER AD COMPOSABLE ==========

    /**
     * Real Unity Banner Ad composable.
     * Shows ONLY real Unity Ads banners — NO simulated/fake/placeholder ads.
     * While loading: shows a compact loading indicator.
     * If failed: shows a small "Ad unavailable" text (NOT a fake ad).
     * When ad-free mode is active: shows nothing.
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

        if (isAdFree) {
            Spacer(modifier = Modifier.height(0.dp))
            return
        }

        var isLoaded by remember { mutableStateOf(false) }
        var adLoadFailed by remember { mutableStateOf(false) }
        var retryKey by remember { mutableIntStateOf(0) }

        // Initialize if needed
        LaunchedEffect(Unit) {
            if (!isInitialized && !isInitializing) {
                initialize(context)
            }
        }

        // Not yet initialized — show loading
        if (!isInitialized) {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                    .testTag("ad_init_loading"),
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

        // Ad failed to load — show minimal retry area (NOT a fake ad)
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
                    .testTag("ad_retry")
            ) {
                Text(
                    text = "Ad • Tap to retry",
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
                .height(50.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .testTag("unity_banner_container"),
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
                                }.filterIsInstance<android.app.Activity>().firstOrNull()

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
                                        Log.i(TAG, "REAL BANNER AD loaded! Placement: $placementId")
                                    }
                                    override fun onBannerClick(view: BannerView?) {
                                        Log.d(TAG, "Banner clicked")
                                    }
                                    override fun onBannerFailedToLoad(
                                        view: BannerView?,
                                        error: com.unity3d.services.banners.BannerErrorInfo?
                                    ) {
                                        adLoadFailed = true
                                        Log.e(TAG, "Banner FAILED to load! Placement: $placementId")
                                        Log.e(TAG, "   Error: ${error?.errorCode} — ${error?.errorMessage}")
                                        Log.e(TAG, "   This is COMMON for banner ads in Unity Ads.")
                                        Log.e(TAG, "   Banner fill rates can be low, especially for new apps.")
                                        Log.e(TAG, "   Interstitial and Rewarded ads typically have better fill rates.")
                                        Log.e(TAG, "   Make sure placement '$placementId' exists in Unity Dashboard.")
                                    }
                                    override fun onBannerLeftApplication(view: BannerView?) {}
                                    override fun onBannerShown(view: BannerView?) {
                                        Log.i(TAG, "REAL BANNER AD displayed on screen!")
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
