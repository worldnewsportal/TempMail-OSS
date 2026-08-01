package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlin.math.max

enum class RewardType {
    EXTEND_LIFETIME,
    AD_FREE,
    PREMIUM_DOMAINS
}

@Composable
fun AdRewardDialog(
    activeEmailAddress: String?,
    activeEmailExpiresAt: Long?,
    adFreeUntil: Long,
    premiumDomainsUnlockedUntil: Long,
    onDismiss: () -> Unit,
    onRewardClaimed: (RewardType) -> Unit,
    initialSelectedType: RewardType? = null
) {
    var selectedType by remember { mutableStateOf(initialSelectedType ?: RewardType.EXTEND_LIFETIME) }
    var screenState by remember { mutableStateOf("select") } // "select", "playing", "success"
    var secondsRemaining by remember { mutableStateOf(60) }
    val context = androidx.compose.ui.platform.LocalContext.current

    // Simulated sponsor state
    val sponsor = remember(selectedType) {
        when (selectedType) {
            RewardType.EXTEND_LIFETIME -> SponsorAd(
                title = "CyberShield Premium VPN",
                description = "Grade-A encryption for your credentials and temporary email traffic. Stay 100% secure with one tap.",
                cta = "Download & Secure Free",
                colorStart = Color(0xFF1D4ED8),
                colorEnd = Color(0xFF1E3A8A)
            )
            RewardType.AD_FREE -> SponsorAd(
                title = "Unity Mobile Ads Engine",
                description = "Smart and safe ad technology empowering indie developers worldwide. Unmatched performance and speed.",
                cta = "Discover Ad Solutions",
                colorStart = Color(0xFF047857),
                colorEnd = Color(0xFF065F46)
            )
            RewardType.PREMIUM_DOMAINS -> SponsorAd(
                title = "CloudHost NVMe VPS Container",
                description = "Host your databases and projects on ultra-fast servers with free integrated DDoS protection.",
                cta = "Spawn Trial Server",
                colorStart = Color(0xFF7C3AED),
                colorEnd = Color(0xFF5B21B6)
            )
        }
    }

    LaunchedEffect(screenState) {
        if (screenState == "playing") {
            secondsRemaining = 60
            while (secondsRemaining > 0) {
                delay(1000L)
                secondsRemaining--
            }
            screenState = "success"
        }
    }

    Dialog(
        onDismissRequest = { if (screenState != "playing") onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = (screenState != "playing"),
            dismissOnClickOutside = (screenState != "playing"),
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .wrapContentHeight()
                .testTag("ad_reward_dialog_card"),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            AnimatedContent(
                targetState = screenState,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "ad_reward_dialog_transition"
            ) { state ->
                when (state) {
                    "select" -> {
                        SelectRewardScreen(
                            activeEmailAddress = activeEmailAddress,
                            activeEmailExpiresAt = activeEmailExpiresAt,
                            adFreeUntil = adFreeUntil,
                            premiumDomainsUnlockedUntil = premiumDomainsUnlockedUntil,
                            selectedType = selectedType,
                            onTypeSelected = { selectedType = it },
                            onStartAd = {
                                val activity = generateSequence(context) { if (it is android.content.ContextWrapper) it.baseContext else null }
                                    .filterIsInstance<android.app.Activity>()
                                    .firstOrNull()
                                if (activity != null) {
                                    com.example.ads.UnityAdsManager.showRewardedAd(
                                        activity = activity,
                                        onComplete = {
                                            screenState = "success"
                                        },
                                        onFailed = {
                                            // Fallback to simulated ad if real ad fails (e.g. adblocker)
                                            screenState = "playing"
                                        }
                                    )
                                } else {
                                    screenState = "playing"
                                }
                            },
                            onDismiss = onDismiss
                        )
                    }
                    "playing" -> {
                        AdPlayerScreen(
                            secondsRemaining = secondsRemaining,
                            sponsor = sponsor
                        )
                    }
                    "success" -> {
                        SuccessRewardScreen(
                            selectedType = selectedType,
                            onClaim = {
                                onRewardClaimed(selectedType)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SelectRewardScreen(
    activeEmailAddress: String?,
    activeEmailExpiresAt: Long?,
    adFreeUntil: Long,
    premiumDomainsUnlockedUntil: Long,
    selectedType: RewardType,
    onTypeSelected: (RewardType) -> Unit,
    onStartAd: () -> Unit,
    onDismiss: () -> Unit
) {
    val now = System.currentTimeMillis()
    val isAdFreeActive = adFreeUntil > now
    val isPremiumDomainsActive = premiumDomainsUnlockedUntil > now

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss, modifier = Modifier.size(44.dp)) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
            }
            Text(
                text = "Free Reward",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(44.dp)) // Spacer to balance the top row
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Watch a single ad (for one full minute) to support the app and activate any of the following premium features 100% free with no cost!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Option 1: Extend lifetime
        RewardOptionCard(
            title = "Extend Active Email Lifetime (+1 hour)",
            description = if (activeEmailAddress != null) "Add a full hour to your email: $activeEmailAddress" else "Create an email first to use this feature",
            icon = Icons.Default.Timer,
            isSelected = selectedType == RewardType.EXTEND_LIFETIME,
            statusText = if (activeEmailExpiresAt != null) {
                val diff = activeEmailExpiresAt - now
                if (diff <= 0) "Expired" else "Remaining: ${formatMillisToShort(diff)}"
            } else "No active email",
            enabled = activeEmailAddress != null,
            onClick = { onTypeSelected(RewardType.EXTEND_LIFETIME) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Option 2: Remove Ads
        RewardOptionCard(
            title = "Ad-Free Mode (24 hours)",
            description = "Remove all ads, banners, and sponsored cards from every screen in the app.",
            icon = Icons.Default.DoNotDisturb,
            isSelected = selectedType == RewardType.AD_FREE,
            statusText = if (isAdFreeActive) "Active (expires in: ${formatMillisToShort(adFreeUntil - now)})" else "Inactive",
            enabled = true,
            onClick = { onTypeSelected(RewardType.AD_FREE) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Option 3: Premium Domains
        RewardOptionCard(
            title = "Premium & Rare Domains (2 hours)",
            description = "Unlock premium and rare domains (.vip, .pro, .premium) when creating new accounts.",
            icon = Icons.Default.Star,
            isSelected = selectedType == RewardType.PREMIUM_DOMAINS,
            statusText = if (isPremiumDomainsActive) "Unlocked (expires in: ${formatMillisToShort(premiumDomainsUnlockedUntil - now)})" else "Locked",
            enabled = true,
            onClick = { onTypeSelected(RewardType.PREMIUM_DOMAINS) }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onStartAd,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("start_ad_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Start Ad Playback (1 minute) ⚡",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Composable
fun RewardOptionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    statusText: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled) { onClick() }
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            else if (enabled) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.05f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    textAlign = TextAlign.Right
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 0.8f else 0.4f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Right
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            RadioButton(
                selected = isSelected,
                onClick = { if (enabled) onClick() },
                enabled = enabled,
                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
fun AdPlayerScreen(
    secondsRemaining: Int,
    sponsor: SponsorAd
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Red.copy(alpha = 0.1f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color.Red)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Live Unity Ad",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.Red
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Remaining: $secondsRemaining sec",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Outer progress bar
        LinearProgressIndicator(
            progress = { (60 - secondsRemaining) / 60f },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // High fidelity sponsor card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(sponsor.colorStart, sponsor.colorEnd)
                        )
                    )
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Sponsored",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                            Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                            Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                            Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                            Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = sponsor.title,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = sponsor.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f),
                            textAlign = TextAlign.Center,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }

                    Button(
                        onClick = { /* Open simulated ad landing */ },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = sponsor.cta,
                            color = sponsor.colorStart,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(imageVector = Icons.Default.Launch, contentDescription = null, tint = sponsor.colorStart, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Do not close this window until the minute fully completes to receive your reward 🛡️",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SuccessRewardScreen(
    selectedType: RewardType,
    onClaim: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Congratulations! Viewing Complete 🎉",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        val rewardText = when (selectedType) {
            RewardType.EXTEND_LIFETIME -> "Your active email lifetime has been extended by 1 extra hour for free!"
            RewardType.AD_FREE -> "Ad-free mode has been fully activated across the app for 24 hours!"
            RewardType.PREMIUM_DOMAINS -> "Premium and rare new domains have been unlocked for free for 2 hours!"
        }

        Text(
            text = "You watched the full ad and met the requirement. Tap the button below to claim your reward now:\n\n$rewardText",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onClaim,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("claim_reward_button"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Activate Feature Now ✅",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

class SponsorAd(
    val title: String,
    val description: String,
    val cta: String,
    val colorStart: Color,
    val colorEnd: Color
)

fun formatMillisToShort(millis: Long): String {
    val sec = max(0, millis / 1000)
    val min = sec / 60
    val hour = min / 60
    val day = hour / 24
    return if (day > 0) {
        String.format("%dd %02dh %02dm", day, hour % 24, min % 60)
    } else if (hour > 0) {
        String.format("%dh %02dm", hour, min % 60)
    } else {
        String.format("%dm %02ds", min, sec % 60)
    }
}
