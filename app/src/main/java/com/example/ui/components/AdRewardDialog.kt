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

    // Simulated sponsor state
    val sponsor = remember(selectedType) {
        when (selectedType) {
            RewardType.EXTEND_LIFETIME -> SponsorAd(
                title = "CyberShield Premium VPN",
                description = "تشفير عالي الدرجة لحماية خصوصية اتصالاتك ورسائلك المؤقتة. بنقرة واحدة تكون آمناً 100٪.",
                cta = "تحميل وتأمين مجاني",
                colorStart = Color(0xFF1D4ED8),
                colorEnd = Color(0xFF1E3A8A)
            )
            RewardType.AD_FREE -> SponsorAd(
                title = "Unity Mobile Ads Engine",
                description = "تقنية الإعلانات الذكية والآمنة لدعم المطورين المستقلين حول العالم. أداء وسرعة لا مثيل لهما.",
                cta = "اكتشف الحلول الإعلانية",
                colorStart = Color(0xFF047857),
                colorEnd = Color(0xFF065F46)
            )
            RewardType.PREMIUM_DOMAINS -> SponsorAd(
                title = "CloudHost NVMe VPS Container",
                description = "استضف قواعد بياناتك ومشاريعك على خوادم فائقة السرعة مع حماية DDoS متكاملة مجاناً.",
                cta = "توليد خادم تجريبي",
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
                            onStartAd = { screenState = "playing" },
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
                Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق")
            }
            Text(
                text = "مكافآت مجانية 🎁",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(44.dp)) // Spacer to balance the top row
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "شاهد إعلاناً واحداً (لمدة دقيقة كاملة) لدعم التطبيق وتفعيل أي من الميزات الممتازة التالية مجاناً 100٪ وبدون أي تكلفة!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Option 1: Extend lifetime
        RewardOptionCard(
            title = "تمديد صلاحية البريد النشط (+1 ساعة)",
            description = if (activeEmailAddress != null) "إضافة ساعة كاملة إلى عمر بريدك: $activeEmailAddress" else "يجب إنشاء بريد إلكتروني أولاً لاستخدام هذه الميزة",
            icon = Icons.Default.Timer,
            isSelected = selectedType == RewardType.EXTEND_LIFETIME,
            statusText = if (activeEmailExpiresAt != null) {
                val diff = activeEmailExpiresAt - now
                if (diff <= 0) "منتهي الصلاحية" else "متبقي: ${formatMillisToShort(diff)}"
            } else "لا يوجد بريد نشط",
            enabled = activeEmailAddress != null,
            onClick = { onTypeSelected(RewardType.EXTEND_LIFETIME) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Option 2: Remove Ads
        RewardOptionCard(
            title = "وضع خالي من الإعلانات (24 ساعة)",
            description = "إزالة كافة الإعلانات واللافتات والبنرات الإعلانية بالكامل من جميع الشاشات.",
            icon = Icons.Default.DoNotDisturb,
            isSelected = selectedType == RewardType.AD_FREE,
            statusText = if (isAdFreeActive) "نشط (ينتهي خلال: ${formatMillisToShort(adFreeUntil - now)})" else "غير نشط",
            enabled = true,
            onClick = { onTypeSelected(RewardType.AD_FREE) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Option 3: Premium Domains
        RewardOptionCard(
            title = "النطاقات المميزة والنادرة (ساعتين)",
            description = "فتح نطاقات ممتازة ونادرة (.vip, .pro, .premium) عند إنشاء حساباتك الجديدة.",
            icon = Icons.Default.Star,
            isSelected = selectedType == RewardType.PREMIUM_DOMAINS,
            statusText = if (isPremiumDomainsActive) "مفتوح (ينتهي خلال: ${formatMillisToShort(premiumDomainsUnlockedUntil - now)})" else "مغلق",
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
                text = "بدء تشغيل الإعلان (دقيقة واحدة) ⚡",
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
                        text = "بث إعلان يونتي الحقيقي",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.Red
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "متبقي: $secondsRemaining ثانية",
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
                                text = "إعلان ممول",
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
                text = "يجب عدم إغلاق النافذة حتى اكتمال الدقيقة بالكامل لتتمكن من استلام مكافأتك 🛡️",
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
            text = "تهانينا! اكتملت المشاهدة 🎉",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        val rewardText = when (selectedType) {
            RewardType.EXTEND_LIFETIME -> "تمديد صلاحية بريدك الإلكتروني الحالي بساعة إضافية مجاناً!"
            RewardType.AD_FREE -> "تفعيل الوضع الخالي من الإعلانات بالكامل في التطبيق لمدة 24 ساعة!"
            RewardType.PREMIUM_DOMAINS -> "فتح وتفعيل النطاقات المميزة والنادرة الجديدة مجاناً لمدة ساعتين!"
        }

        Text(
            text = "لقد شاهدت الإعلان بالكامل واستوفيت الشرط. اضغط على الزر أدناه لتفعيل مكافأتك الآن:\n\n$rewardText",
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
                text = "تفعيل الميزة الآن ✅",
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
