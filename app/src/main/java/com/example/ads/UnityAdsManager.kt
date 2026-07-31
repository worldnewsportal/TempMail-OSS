package com.example.ads

import android.content.Context
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.UnityAds
import com.unity3d.services.banners.BannerView
import com.unity3d.services.banners.UnityBannerSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import com.example.data.preferences.AppSettingsRepository

object UnityAdsManager {
    private const val TAG = "UnityAdsManager"
    
    // Official Unity Ads Test Game IDs
    private const val GAME_ID_ANDROID = "800110518" 
    private const val BANNER_PLACEMENT = "Banner_Android"
    
    var isInitialized = false
        private set

    fun showRewardedAd(
        activity: android.app.Activity,
        placementId: String = "Rewarded_Android",
        onComplete: () -> Unit,
        onFailed: () -> Unit
    ) {
        if (!isInitialized) {
            onFailed()
            return
        }

        UnityAds.load(placementId, object : com.unity3d.ads.IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(loadedPlacementId: String?) {
                UnityAds.show(activity, placementId, object : com.unity3d.ads.IUnityAdsShowListener {
                    override fun onUnityAdsShowFailure(showPlacementId: String?, error: UnityAds.UnityAdsShowError?, message: String?) {
                        Log.e(TAG, "Rewarded ad show failure: $message")
                        onFailed()
                    }
                    override fun onUnityAdsShowStart(showPlacementId: String?) {}
                    override fun onUnityAdsShowClick(showPlacementId: String?) {}
                    override fun onUnityAdsShowComplete(showPlacementId: String?, state: UnityAds.UnityAdsShowCompletionState?) {
                        if (state == UnityAds.UnityAdsShowCompletionState.COMPLETED) {
                            onComplete()
                        } else {
                            onFailed()
                        }
                    }
                })
            }
            override fun onUnityAdsFailedToLoad(loadPlacementId: String?, error: UnityAds.UnityAdsLoadError?, message: String?) {
                Log.e(TAG, "Rewarded ad failed to load: $message")
                onFailed()
            }
        })
    }

    fun initialize(context: Context) {
        if (isInitialized) return
        initializeWithTestMode(context.applicationContext, false)
    }

    private fun initializeWithTestMode(context: Context, testMode: Boolean) {
        if (isInitialized) return
        try {
            Log.d(TAG, "Initializing Unity Ads with Game ID: $GAME_ID_ANDROID, testMode: $testMode")
            UnityAds.initialize(
                context.applicationContext,
                GAME_ID_ANDROID,
                testMode,
                object : IUnityAdsInitializationListener {
                    override fun onInitializationComplete() {
                        isInitialized = true
                        Log.i(TAG, "Unity Ads initialized successfully (testMode=$testMode)!")
                    }

                    override fun onInitializationFailed(
                        error: UnityAds.UnityAdsInitializationError?,
                        message: String?
                    ) {
                        isInitialized = false
                        Log.e(TAG, "Unity Ads initialization failed: $message (Error: $error)")
                    }
                }
            )
        } catch (e: Throwable) {
            isInitialized = false
            Log.e(TAG, "Error initializing Unity Ads: ${e.message}")
        }
    }

    @Composable
    fun UnityAdBanner(
        modifier: Modifier = Modifier,
        placementId: String = BANNER_PLACEMENT
    ) {
        val context = LocalContext.current
        val settingsRepo = remember(context) { AppSettingsRepository(context) }
        val settings by settingsRepo.settingsFlow.collectAsState(initial = com.example.data.preferences.AppSettings())
        val isAdFree = System.currentTimeMillis() < settings.adFreeUntil

        if (isAdFree) {
            // Unobtrusive empty space so user feels the premium ad-free experience instantly!
            Spacer(modifier = Modifier.height(0.dp))
            return
        }

        var adLoadFailed by remember { mutableStateOf(false) }
        var isLoaded by remember { mutableStateOf(false) }

        // Start initialization if not already done
        LaunchedEffect(Unit) {
            if (!isInitialized) {
                initialize(context)
            }
        }

        if (isInitialized && !adLoadFailed) {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .testTag("unity_banner_container"),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        FrameLayout(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            try {
                                val activity = generateSequence(ctx) { if (it is android.content.ContextWrapper) it.baseContext else null }
                                    .filterIsInstance<android.app.Activity>()
                                    .firstOrNull()

                                if (activity == null) {
                                    Log.e(TAG, "Context is not an Activity")
                                    adLoadFailed = true
                                    return@apply
                                }

                                val bannerView = BannerView(activity, placementId, UnityBannerSize(320, 50))
                                bannerView.listener = object : BannerView.IListener {
                                    override fun onBannerLoaded(view: BannerView?) {
                                        isLoaded = true
                                        Log.d(TAG, "Banner loaded successfully")
                                    }

                                    override fun onBannerClick(view: BannerView?) {
                                        Log.d(TAG, "Banner clicked")
                                    }

                                    override fun onBannerFailedToLoad(
                                        view: BannerView?,
                                        error: com.unity3d.services.banners.BannerErrorInfo?
                                    ) {
                                        adLoadFailed = true
                                        Log.w(TAG, "Banner failed to load: ${error?.errorMessage}")
                                    }

                                    override fun onBannerLeftApplication(view: BannerView?) {}

                                    override fun onBannerShown(view: BannerView?) {
                                        Log.d(TAG, "Banner shown on screen")
                                    }
                                }
                                bannerView.load()
                                addView(bannerView)
                            } catch (e: Throwable) {
                                adLoadFailed = true
                                Log.e(TAG, "Error creating BannerView: ${e.message}")
                            }
                        }
                    }
                )
            }
        } else {
            // High-fidelity material-designed simulated native/unobtrusive ad card
            SimulatedUnobtrusiveAd(modifier = modifier)
        }
    }

    @Composable
    fun SimulatedUnobtrusiveAd(modifier: Modifier = Modifier) {
        // Pool of beautifully curated ad sponsors to make the app feel alive and extremely premium
        val sponsors = remember {
            listOf(
                SponsorInfo(
                    title = "Unity Ads Engine",
                    desc = "Monetize and acquire high-value players across top iOS & Android games seamlessly.",
                    cta = "Learn More",
                    colorStart = Color(0xFF047857),
                    colorEnd = Color(0xFF065F46)
                ),
                SponsorInfo(
                    title = "CyberShield Security VPN",
                    desc = "Protect your online credentials and temporary email traffic with grade-A encryption.",
                    cta = "Get Protected",
                    colorStart = Color(0xFF1D4ED8),
                    colorEnd = Color(0xFF1E3A8A)
                ),
                SponsorInfo(
                    title = "CloudHost Pro",
                    desc = "Host production servers, APIs, and databases on secure NVMe cloud containers globally.",
                    cta = "Deploy Free",
                    colorStart = Color(0xFF7C3AED),
                    colorEnd = Color(0xFF5B21B6)
                )
            )
        }
        val sponsor = remember { sponsors.random() }

        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .testTag("simulated_ad_card"),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                sponsor.colorStart.copy(alpha = 0.08f),
                                sponsor.colorEnd.copy(alpha = 0.03f)
                            )
                        )
                    )
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Small "Ad" badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Sponsored",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = sponsor.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Ad Info",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = sponsor.desc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { /* Simulated action */ },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("ad_cta_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = sponsor.cta,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Launch,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    private class SponsorInfo(
        val title: String,
        val desc: String,
        val cta: String,
        val colorStart: Color,
        val colorEnd: Color
    )
}
