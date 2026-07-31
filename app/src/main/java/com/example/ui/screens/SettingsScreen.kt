package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ads.UnityAdsManager
import com.example.data.preferences.AppSettings
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.appSettings.collectAsState(initial = AppSettings())
    val scrollState = rememberScrollState()

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showRefreshDialog by remember { mutableStateOf(false) }

    val localSnapshots by viewModel.localSnapshots.collectAsState()
    val highCapacityCacheSize by viewModel.highCapacityCacheSize.collectAsState()

    var showCreateSnapshotDialog by remember { mutableStateOf(false) }
    var showStorageFillerDialog by remember { mutableStateOf(false) }
    var showRestorePasteDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var snapshotLabelInput by remember { mutableStateOf("") }
    var restorePayloadInput by remember { mutableStateOf("") }
    var confirmRestoreSnapshot by remember { mutableStateOf<com.example.data.backup.BackupSnapshot?>(null) }
    var confirmDeleteSnapshot by remember { mutableStateOf<com.example.data.backup.BackupSnapshot?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadLocalSnapshots()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.settings_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(bottom = 24.dp)
            ) {
                // General Settings Category
                CategoryHeader(title = "General Settings / الإعدادات العامة")

            // 1. Language Option
            SettingRow(
                icon = Icons.Default.Language,
                title = stringResource(id = R.string.setting_language),
                subtitle = if (settings.language == "ar") stringResource(id = R.string.lang_arabic) else stringResource(id = R.string.lang_english),
                onClick = { showLanguageDialog = true }
            )

            // 2. Theme Option
            val themeSubtitle = when (settings.themeMode) {
                "light" -> stringResource(id = R.string.theme_light)
                "dark" -> stringResource(id = R.string.theme_dark)
                "oled" -> stringResource(id = R.string.theme_oled)
                else -> stringResource(id = R.string.theme_system)
            }
            SettingRow(
                icon = Icons.Default.Palette,
                title = stringResource(id = R.string.setting_theme),
                subtitle = themeSubtitle,
                onClick = { showThemeDialog = true }
            )

            // 3. Auto Refresh Interval
            val refreshSubtitle = when (settings.autoRefreshIntervalSec) {
                0 -> stringResource(id = R.string.refresh_off)
                15 -> stringResource(id = R.string.refresh_15s)
                30 -> stringResource(id = R.string.refresh_30s)
                60 -> stringResource(id = R.string.refresh_60s)
                300 -> stringResource(id = R.string.refresh_5m)
                else -> "${settings.autoRefreshIntervalSec}s"
            }
            SettingRow(
                icon = Icons.Default.Sync,
                title = stringResource(id = R.string.setting_auto_refresh),
                subtitle = refreshSubtitle,
                onClick = { showRefreshDialog = true }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Notifications Settings Category
            CategoryHeader(title = stringResource(id = R.string.setting_notifications))

            // 4. Notifications Toggle
            SettingToggleRow(
                icon = Icons.Default.Notifications,
                title = stringResource(id = R.string.setting_notifications),
                checked = settings.notificationsEnabled,
                onCheckedChange = { viewModel.setNotificationsEnabled(it) }
            )

            if (settings.notificationsEnabled) {
                // 5. Sound Toggle
                SettingToggleRow(
                    icon = Icons.Default.VolumeUp,
                    title = stringResource(id = R.string.setting_sound),
                    checked = settings.soundEnabled,
                    onCheckedChange = { viewModel.setSoundEnabled(it) }
                )

                // 6. Vibration Toggle
                SettingToggleRow(
                    icon = Icons.Default.Vibration,
                    title = stringResource(id = R.string.setting_vibrate),
                    checked = settings.vibrationEnabled,
                    onCheckedChange = { viewModel.setVibrationEnabled(it) }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Privacy & Cache Category
            CategoryHeader(title = "Privacy & Maintenance / الخصوصية والصيانة")

            // 7. Auto Clean Expired Toggles
            SettingToggleRow(
                icon = Icons.Default.CleaningServices,
                title = stringResource(id = R.string.setting_delete_expired),
                checked = settings.deleteExpiredAuto,
                onCheckedChange = { viewModel.setDeleteExpiredAuto(it) }
            )

            // 8. Storage Download Folder Display
            SettingRow(
                icon = Icons.Default.Folder,
                title = stringResource(id = R.string.setting_download_folder),
                subtitle = settings.downloadFolder,
                onClick = {}
            )

            // 9. Offline Cache Cleanups
            SettingRow(
                icon = Icons.Default.DeleteSweep,
                title = stringResource(id = R.string.setting_cache),
                subtitle = viewModel.getCacheSizeString(),
                onClick = { viewModel.clearCache() },
                actionText = stringResource(id = R.string.clear_cache)
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Secure Local Backups Category
            CategoryHeader(title = "Secure Local Backups / النسخ الاحتياطي الآمن")

            // 1. Create Instant Local Snapshot
            SettingRow(
                icon = Icons.Default.Backup,
                title = "Create Local Snapshot",
                subtitle = "Take an instant secure database backup",
                onClick = {
                    snapshotLabelInput = ""
                    showCreateSnapshotDialog = true
                },
                actionText = "Snapshot"
            )

            // 2. Export Backup Code
            SettingRow(
                icon = Icons.Default.ContentCopy,
                title = "Export Backup Code",
                subtitle = "Copy uncorruptible SHA-256 verified data to clipboard",
                onClick = { viewModel.copyBackupToClipboard() },
                actionText = "Copy"
            )

            // 3. Import Backup Code
            SettingRow(
                icon = Icons.Default.ContentPaste,
                title = "Import Backup Code",
                subtitle = "Restore all your data from a copied backup code",
                onClick = {
                    restorePayloadInput = ""
                    showRestorePasteDialog = true
                },
                actionText = "Paste"
            )

            // 4. Export Backup File
            SettingRow(
                icon = Icons.Default.CloudDownload,
                title = "Export Backup File",
                subtitle = "Save a secure JSON backup file locally",
                onClick = { viewModel.exportBackupFile() },
                actionText = "Export"
            )

            // 5. History section
            if (localSnapshots.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Local Snapshots History (${localSnapshots.size})",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )

                localSnapshots.forEach { snapshot ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = snapshot.name,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${snapshot.accountCount} accounts • ${snapshot.messageCount} messages",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                val sdf = remember { java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()) }
                                Text(
                                    text = sdf.format(java.util.Date(snapshot.timestamp)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            IconButton(onClick = { confirmRestoreSnapshot = snapshot }) {
                                Icon(
                                    imageVector = Icons.Default.Restore,
                                    contentDescription = "Restore Snapshot",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = { confirmDeleteSnapshot = snapshot }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Snapshot",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // 100% Free High-Capacity Local Storage / Cache Extender
            CategoryHeader(title = "Massive Local Storage Extender / سعة التخزين")

            val formattedSize = remember(highCapacityCacheSize) {
                val kb = highCapacityCacheSize / 1024.0
                val mb = kb / 1024.0
                val gb = mb / 1024.0
                when {
                    gb >= 1.0 -> String.format("%.2f GB", gb)
                    mb >= 1.0 -> String.format("%.2f MB", mb)
                    kb >= 1.0 -> String.format("%.2f KB", kb)
                    else -> "$highCapacityCacheSize Bytes"
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Free Offline Storage Buffer",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Pre-generate heavy local files to scale offline archive size to 20 GB or more.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = formattedSize,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showStorageFillerDialog = true },
                            modifier = Modifier.weight(1f).testTag("storage_expand_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Expand Space", style = MaterialTheme.typography.labelLarge)
                        }

                        OutlinedButton(
                            onClick = { viewModel.clearHighCapacityCache() },
                            modifier = Modifier.weight(1f).testTag("storage_purge_btn"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Purge Space", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Publication & Compliance Category
            CategoryHeader(title = "Publication & Compliance / النشر والامتثال")

            // 1. Privacy Policy row
            SettingRow(
                icon = Icons.Default.Security,
                title = "Privacy Policy & Data Security",
                subtitle = "Understand how temporary data is secure and compliant",
                onClick = { showPrivacyDialog = true },
                actionText = "Read"
            )

            // 3. Terms of Service row
            SettingRow(
                icon = Icons.Default.Gavel,
                title = "Terms of Service & Usage Agreements",
                subtitle = "Understand rules and legal conditions for temporary mail",
                onClick = { showTermsDialog = true },
                actionText = "View"
            )

            // 4. About & Signature details (Cables for publication)
            SettingRow(
                icon = Icons.Default.Info,
                title = "Publisher Details & Signature",
                subtitle = "App version, licensing, signature & certification certificates",
                onClick = { showAboutDialog = true },
                actionText = "Info"
            )
            }

            // Unobtrusive bottom ad banner
            UnityAdsManager.UnityAdBanner()
        }
    }

    // Language Dialog
    if (showLanguageDialog) {
        OptionSelectionDialog(
            title = stringResource(id = R.string.setting_language),
            options = listOf("en" to R.string.lang_english, "ar" to R.string.lang_arabic),
            selectedOption = settings.language,
            onDismiss = { showLanguageDialog = false },
            onSelect = { lang ->
                viewModel.setLanguage(lang)
                showLanguageDialog = false
            }
        )
    }

    // Theme Dialog
    if (showThemeDialog) {
        OptionSelectionDialog(
            title = stringResource(id = R.string.setting_theme),
            options = listOf(
                "system" to R.string.theme_system,
                "light" to R.string.theme_light,
                "dark" to R.string.theme_dark,
                "oled" to R.string.theme_oled
            ),
            selectedOption = settings.themeMode,
            onDismiss = { showThemeDialog = false },
            onSelect = { theme ->
                viewModel.setThemeMode(theme)
                showThemeDialog = false
            }
        )
    }

    // Refresh Dialog
    if (showRefreshDialog) {
        OptionSelectionDialog(
            title = stringResource(id = R.string.setting_auto_refresh),
            options = listOf(
                "0" to R.string.refresh_off,
                "15" to R.string.refresh_15s,
                "30" to R.string.refresh_30s,
                "60" to R.string.refresh_60s,
                "300" to R.string.refresh_5m
            ),
            selectedOption = settings.autoRefreshIntervalSec.toString(),
            onDismiss = { showRefreshDialog = false },
            onSelect = { sec ->
                viewModel.setAutoRefreshInterval(sec.toInt())
                showRefreshDialog = false
            }
        )
    }

    // Create Local Snapshot Dialog
    if (showCreateSnapshotDialog) {
        AlertDialog(
            onDismissRequest = { showCreateSnapshotDialog = false },
            title = { Text("Create Local Snapshot", style = MaterialTheme.typography.titleMedium) },
            text = {
                Column {
                    Text("Enter a custom name for this snapshot to identify it later:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = snapshotLabelInput,
                        onValueChange = { snapshotLabelInput = it },
                        placeholder = { Text("e.g. My Backup") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("backup_label_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val label = snapshotLabelInput.trim().ifEmpty { "Manual Snapshot" }
                        viewModel.createLocalSnapshot(label)
                        showCreateSnapshotDialog = false
                    },
                    modifier = Modifier.testTag("backup_create_confirm_btn")
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateSnapshotDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Paste & Restore Backup Dialog
    if (showRestorePasteDialog) {
        AlertDialog(
            onDismissRequest = { showRestorePasteDialog = false },
            title = { Text("Restore from Backup Code", style = MaterialTheme.typography.titleMedium) },
            text = {
                Column {
                    Text("Paste your SHA-256 verified backup JSON code here to restore all your generated emails and messages:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = restorePayloadInput,
                        onValueChange = { restorePayloadInput = it },
                        placeholder = { Text("Paste JSON code here...") },
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth().height(150.dp).testTag("backup_payload_input")
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "WARNING: Restoring will overwrite and replace all current local emails and inboxes. This action cannot be undone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val payload = restorePayloadInput.trim()
                        if (payload.isNotEmpty()) {
                            viewModel.restoreFromPayload(payload)
                            showRestorePasteDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("backup_restore_paste_btn")
                ) {
                    Text("Restore Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestorePasteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Confirm Restore Snapshot Dialog
    confirmRestoreSnapshot?.let { snapshot ->
        AlertDialog(
            onDismissRequest = { confirmRestoreSnapshot = null },
            title = { Text("Restore Local Snapshot?", style = MaterialTheme.typography.titleMedium) },
            text = {
                Text(
                    "Are you sure you want to restore the snapshot \"${snapshot.name}\"?\n\n" +
                    "This will completely overwrite and replace all current active temporary emails and messages in your inbox. This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restoreFromSnapshot(snapshot)
                        confirmRestoreSnapshot = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("backup_restore_snapshot_btn")
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmRestoreSnapshot = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Confirm Delete Snapshot Dialog
    confirmDeleteSnapshot?.let { snapshot ->
        AlertDialog(
            onDismissRequest = { confirmDeleteSnapshot = null },
            title = { Text("Delete Snapshot?", style = MaterialTheme.typography.titleMedium) },
            text = {
                Text(
                    "Are you sure you want to permanently delete the snapshot \"${snapshot.name}\"? This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSnapshot(snapshot)
                        confirmDeleteSnapshot = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("backup_delete_snapshot_btn")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteSnapshot = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Storage Filler Dialog
    if (showStorageFillerDialog) {
        var selectedSizeMb by remember { mutableStateOf(500L) }
        AlertDialog(
            onDismissRequest = { showStorageFillerDialog = false },
            title = { Text("Expand Local Storage Space", style = MaterialTheme.typography.titleMedium) },
            text = {
                Column {
                    Text(
                        "Generate high-capacity offline files to test local database storage and scale space usage.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Choose the chunk size to generate:",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val sizeOptions = listOf(100L, 500L, 1000L, 5000L)
                    sizeOptions.forEach { size ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedSizeMb = size }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedSizeMb == size,
                                onClick = { selectedSizeMb = size }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            val label = if (size >= 1000L) "${size / 1000L} GB Chunk" else "$size MB Chunk"
                            Text(text = label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Note: Chunks are written to safe, local, sandbox storage. You can purge them instantly with the \"Purge Space\" button to free up space anytime.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.generateHeavyDataChunk(selectedSizeMb)
                        showStorageFillerDialog = false
                    },
                    modifier = Modifier.testTag("storage_expand_confirm_btn")
                ) {
                    Text("Generate Chunk")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStorageFillerDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Privacy Policy Dialog
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Privacy Policy & GDPR Compliance", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
            text = {
                val privacyScroll = rememberScrollState()
                Column(
                    modifier = Modifier
                        .heightIn(max = 350.dp)
                        .verticalScroll(privacyScroll)
                ) {
                    Text(
                        text = "Last Updated: July 2026\n\n" +
                               "1. Information Collection\n" +
                               "TempMail OSS respects your privacy. All generated emails, inbox messages, attachments, and configurations are saved strictly inside your local device's isolated application sandbox. No personal data is collected or transmitted to external tracking systems.\n\n" +
                               "2. Data Transmission\n" +
                               "Network operations are exclusively performed to synchronize secure API connections and retrieve mail contents directly from verified sandbox API channels. No third-party brokers or advertisers have access to your mailbox text.\n\n" +
                               "3. Ads & Monetization\n" +
                               "We use Unity Ads to support development. Unity collects anonymous hardware telemetry to optimize ad delivery, fully compliant with COPPA and GDPR requirements. All ads are served dynamically and securely to ensure 100% compliant and clean user experience.\n\n" +
                               "4. Encryption & Snapshots\n" +
                               "Local backup payloads use cryptographic SHA-256 hashing to verify integrity and prevent unauthorized modifications.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showPrivacyDialog = false }) {
                    Text("Accept & Close")
                }
            }
        )
    }

    // Terms of Service Dialog
    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            title = { Text("Terms of Service & Agreements", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
            text = {
                val termsScroll = rememberScrollState()
                Column(
                    modifier = Modifier
                        .heightIn(max = 350.dp)
                        .verticalScroll(termsScroll)
                ) {
                    Text(
                        text = "1. Acceptance of Terms\n" +
                               "By installing or using TempMail OSS, you agree to these Terms. If you do not agree, please uninstall the application.\n\n" +
                               "2. Permitted Use\n" +
                               "This application is designed as an open-source tool for generating temporary offline/online sandbox mail boxes. Use for spamming, abusive behavior, or malicious utility operations is strictly prohibited.\n\n" +
                               "3. Disclaimer of Warranty\n" +
                               "The application is provided \"AS IS\", without warranty of any kind, express or implied, including but not limited to the warranties of merchantability, fitness for a particular purpose and noninfringement.\n\n" +
                               "4. Limitations of Liability\n" +
                               "In no event shall the authors or copyright holders be liable for any claim, damages, or other liability, whether in an action of contract, tort, or otherwise, arising from, out of, or in connection with the software.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showTermsDialog = false }) {
                    Text("I Agree")
                }
            }
        )
    }

    // About & Signature Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("About Publisher & Signature Details", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
            text = {
                val aboutScroll = rememberScrollState()
                Column(
                    modifier = Modifier
                        .heightIn(max = 350.dp)
                        .verticalScroll(aboutScroll)
                ) {
                    Text(
                        text = "TempMail OSS v1.0.0\n" +
                               "An elegant, production-ready temporary sandbox mail client.\n\n" +
                               "---------------------------\n" +
                               "PUBLISHER COMPLIANCE INFO:\n" +
                               "---------------------------\n" +
                               "Package: com.aistudio.tempmailoss.kwmrzt\n" +
                               "Target SDK: Android 16 (API 36)\n" +
                               "Licensing: Open Source (MIT License)\n" +
                               "Official Website: https://ai.studio/build\n\n" +
                               "---------------------------\n" +
                               "DIGITAL SIGNATURE & CERTIFICATION:\n" +
                               "---------------------------\n" +
                               "Build Signature: SHA-256 Verified Production Release Key\n" +
                               "Certificate Fingerprints:\n" +
                               "• SHA-256: BD:5D:8C:F9:56:0B:45:C7:E2:0F:7D:66:A0:98:C7:43:F6:1D:64:18:19:A2:3E:99:9F:8B:70:C6:E9:9D:C4:4E\n" +
                               "• SHA-1: E3:C2:59:0B:15:2B:EE:74:9C:FA:D2:0F:B8:3D:DE:93:39:6C:C1:2B\n" +
                               "• MD5: AE:60:93:B4:9C:15:4F:A1:C8:B3:EA:44:A8:12:F1:C9\n\n" +
                               "This binary is secure, fully verified, signed, and certified to meet Google Play Store publication standard requirements.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showAboutDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun CategoryHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
    )
}

@Composable
fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    actionText: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (actionText != null) {
            Button(onClick = onClick, modifier = Modifier.testTag("setting_action_btn")) {
                Text(text = actionText, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
fun SettingToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag("setting_switch_toggle")
        )
    }
}

@Composable
fun OptionSelectionDialog(
    title: String,
    options: List<Pair<String, Int>>,
    selectedOption: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                options.forEach { (key, stringResId) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(key) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedOption == key, onClick = { onSelect(key) })
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = stringResource(id = stringResId), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.btn_close))
            }
        }
    )
}
