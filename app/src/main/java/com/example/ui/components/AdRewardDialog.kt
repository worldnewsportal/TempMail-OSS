package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ads.UnityAdsManager
import java.util.concurrent.TimeUnit

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
    // States: "select" -> "loading" -> "watching" -> "success" / "error"
    var screenState by remember { mutableStateOf("select") }
    var errorMessage by remember { mutableStateOf("") }
    val context = androidx.compose.ui.platform.LocalContext.current

    Dialog(
        onDismissRequest = { if (screenState != "watching") onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = (screenState != "watching"),
            dismissOnClickOutside = (screenState != "watching"),
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .testTag("ad_reward_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (screenState) {
                    "select" -> RewardSelectionScreen(
                        activeEmailAddress = activeEmailAddress,
                        activeEmailExpiresAt = activeEmailExpiresAt,
                        adFreeUntil = adFreeUntil,
                        premiumDomainsUnlockedUntil = premiumDomainsUnlockedUntil,
                        selectedType = selectedType,
                        onTypeSelected = { selectedType = it },
                        onStartAd = {
                            val activity = generateSequence(context) {
                                if (it is android.content.ContextWrapper) it.baseContext else null
                            }.filterIsInstance<android.app.Activity>().firstOrNull()

                            if (activity == null) {
                                errorMessage = "Cannot show ad - invalid activity"
                                screenState = "error"
                                return@RewardSelectionScreen
                            }

                            if (!UnityAdsManager.isInitialized) {
                                errorMessage = "Ads are not ready yet. Please try again in a moment."
                                screenState = "error"
                                return@RewardSelectionScreen
                            }

                            screenState = "loading"
                            UnityAdsManager.showRewardedAd(
                                activity = activity,
                                onComplete = {
                                    screenState = "success"
                                },
                                onFailed = {
                                    errorMessage = "Ad failed to load. This can happen if:\n- No ads available right now\n- Poor internet connection\n- Ad placements not configured in Unity Dashboard\n\nPlease try again later."
                                    screenState = "error"
                                }
                            )
                        },
                        onDismiss = onDismiss
                    )
                    "loading" -> AdLoadingScreen()
                    "watching" -> AdWatchingScreen()
                    "success" -> SuccessRewardScreen(
                        selectedType = selectedType,
                        onClaim = {
                            onRewardClaimed(selectedType)
                            onDismiss()
                        }
                    )
                    "error" -> AdErrorScreen(
                        message = errorMessage,
                        onRetry = {
                            screenState = "select"
                        },
                        onDismiss = onDismiss
                    )
                }
            }
        }
    }
}

// ========== REWARD SELECTION SCREEN ==========

@Composable
private fun RewardSelectionScreen(
    activeEmailAddress: String?,
    activeEmailExpiresAt: Long?,
    adFreeUntil: Long,
    premiumDomainsUnlockedUntil: Long,
    selectedType: RewardType,
    onTypeSelected: (RewardType) -> Unit,
    onStartAd: () -> Unit,
    onDismiss: () -> Unit
) {
    // Header
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Watch Ad & Earn Reward",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close")
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "Watch a short video ad to earn one of these rewards:",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Reward options
    RewardOptionCard(
        type = RewardType.EXTEND_LIFETIME,
        title = "Extend Email Lifetime",
        description = if (activeEmailAddress != null && activeEmailExpiresAt != null) {
            val remaining = activeEmailExpiresAt - System.currentTimeMillis()
            val mins = TimeUnit.MILLISECONDS.toMinutes(remaining)
            "Add 30 minutes to $activeEmailAddress\n(current: ${mins}m remaining)"
        } else {
            "Add 30 minutes to your active email address"
        },
        icon = Icons.Default.Schedule,
        color = Color(0xFF2563EB),
        selected = selectedType == RewardType.EXTEND_LIFETIME,
        onClick = { onTypeSelected(RewardType.EXTEND_LIFETIME) }
    )

    Spacer(modifier = Modifier.height(8.dp))

    RewardOptionCard(
        type = RewardType.AD_FREE,
        title = "Ad-Free Session",
        description = if (adFreeUntil > System.currentTimeMillis()) {
            val remaining = adFreeUntil - System.currentTimeMillis()
            val mins = TimeUnit.MILLISECONDS.toMinutes(remaining)
            "Remove ads for 1 hour\n(current: ${mins}m remaining)"
        } else {
            "Remove all ads for 1 hour"
        },
        icon = Icons.Default.Block,
        color = Color(0xFF059669),
        selected = selectedType == RewardType.AD_FREE,
        onClick = { onTypeSelected(RewardType.AD_FREE) }
    )

    Spacer(modifier = Modifier.height(8.dp))

    RewardOptionCard(
        type = RewardType.PREMIUM_DOMAINS,
        title = "Premium Domains",
        description = if (premiumDomainsUnlockedUntil > System.currentTimeMillis()) {
            val remaining = premiumDomainsUnlockedUntil - System.currentTimeMillis()
            val mins = TimeUnit.MILLISECONDS.toMinutes(remaining)
            "Unlock premium domains for 1 hour\n(current: ${mins}m remaining)"
        } else {
            "Unlock premium email domains for 1 hour"
        },
        icon = Icons.Default.VpnKey,
        color = Color(0xFF7C3AED),
        selected = selectedType == RewardType.PREMIUM_DOMAINS,
        onClick = { onTypeSelected(RewardType.PREMIUM_DOMAINS) }
    )

    Spacer(modifier = Modifier.height(20.dp))

    // Watch Ad button
    Button(
        onClick = onStartAd,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Icon(
            Icons.Default.PlayArrow,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Watch Ad Now",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun RewardOptionCard(
    type: RewardType,
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("reward_option_${type.name}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected)
                color.copy(alpha = 0.1f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = if (selected) CardDefaults.outlinedCardBorder() else null,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (selected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// ========== AD LOADING SCREEN ==========

@Composable
private fun AdLoadingScreen() {
    Spacer(modifier = Modifier.height(40.dp))

    CircularProgressIndicator(
        modifier = Modifier.size(48.dp),
        strokeWidth = 4.dp,
        color = MaterialTheme.colorScheme.primary
    )

    Spacer(modifier = Modifier.height(20.dp))

    Text(
        text = "Loading Ad...",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "Please wait while we load a video ad for you.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(40.dp))
}

// ========== AD WATCHING SCREEN ==========

@Composable
private fun AdWatchingScreen() {
    Spacer(modifier = Modifier.height(40.dp))

    Icon(
        Icons.Default.PlayCircleFilled,
        contentDescription = null,
        modifier = Modifier.size(64.dp),
        tint = MaterialTheme.colorScheme.primary
    )

    Spacer(modifier = Modifier.height(20.dp))

    Text(
        text = "Ad is playing...",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "Please watch the full ad to earn your reward.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(40.dp))
}

// ========== SUCCESS SCREEN ==========

@Composable
private fun SuccessRewardScreen(
    selectedType: RewardType,
    onClaim: () -> Unit
) {
    Spacer(modifier = Modifier.height(20.dp))

    Icon(
        Icons.Default.CheckCircle,
        contentDescription = null,
        modifier = Modifier.size(64.dp),
        tint = Color(0xFF22C55E)
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "Reward Earned!",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
    )

    Spacer(modifier = Modifier.height(8.dp))

    val rewardText = when (selectedType) {
        RewardType.EXTEND_LIFETIME -> "Your email lifetime has been extended by 30 minutes!"
        RewardType.AD_FREE -> "Ads have been removed for 1 hour!"
        RewardType.PREMIUM_DOMAINS -> "Premium domains are now unlocked for 1 hour!"
    }

    Text(
        text = rewardText,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(20.dp))

    Button(
        onClick = onClaim,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF22C55E)
        )
    ) {
        Icon(Icons.Default.CardGiftcard, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Claim Reward", fontWeight = FontWeight.Bold)
    }
}

// ========== ERROR SCREEN ==========

@Composable
private fun AdErrorScreen(
    message: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Spacer(modifier = Modifier.height(20.dp))

    Icon(
        Icons.Default.ErrorOutline,
        contentDescription = null,
        modifier = Modifier.size(64.dp),
        tint = MaterialTheme.colorScheme.error
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "Ad Not Available",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(20.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Close")
        }
        Button(
            onClick = onRetry,
            modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Retry")
        }
    }
}
