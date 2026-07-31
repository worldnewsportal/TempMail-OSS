package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import com.example.ads.UnityAdsManager
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.db.EmailAccountEntity
import com.example.data.db.MessageEntity
import com.example.ui.components.ChangeEmailDialog
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.theme.ColorAttachmentBadge
import com.example.ui.theme.ColorExpiredText
import com.example.ui.theme.ColorSpamBadge
import com.example.ui.theme.ColorUnreadDot
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay

@Composable
fun InboxScreen(
    viewModel: MainViewModel,
    onNavigateToDetail: (MessageEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeEmail by viewModel.activeAccount.collectAsState(initial = null)
    val messagesList by viewModel.messages.collectAsState(initial = emptyList())
    val availableDomains by viewModel.availableDomains.collectAsState()
    
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("all") } // all, unread, attachments, archived
    var showDeleteAllConfirm by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val filteredMessages = remember(messagesList, selectedFilter) {
        when (selectedFilter) {
            "unread" -> messagesList.filter { !it.isRead && !it.isArchived }
            "attachments" -> messagesList.filter { it.hasAttachments && !it.isArchived }
            "archived" -> messagesList.filter { it.isArchived }
            else -> messagesList.filter { !it.isArchived }
        }
    }

    Scaffold(
        topBar = {
            InboxTopBar(
                activeEmail = activeEmail,
                onRefresh = { viewModel.refreshInbox() },
                onDeleteAll = { if (activeEmail != null) showDeleteAllConfirm = true }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Address Showcase Card
            AddressCard(
                activeEmail = activeEmail,
                onChangeEmail = { showCreateDialog = true },
                onCopyEmail = { address ->
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("Copied Email", address)
                    clipboard.setPrimaryClip(clip)
                    viewModel.showToast(context.getString(R.string.email_copied))
                },
                onShareEmail = { address ->
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, address)
                    }
                    context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_email)))
                }
            )

            // Filter Tabs Row
            FilterRow(
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it }
            )

            // Inbox Messages List
            if (activeEmail == null) {
                EmptyStateView(
                    title = stringResource(id = R.string.no_active_email),
                    subtitle = stringResource(id = R.string.inbox_empty_subtitle),
                    onCreateEmail = { showCreateDialog = true }
                )
            } else if (filteredMessages.isEmpty()) {
                EmptyStateView(
                    title = stringResource(id = R.string.inbox_empty_title),
                    subtitle = stringResource(id = R.string.inbox_empty_subtitle),
                    onCreateEmail = null
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .testTag("inbox_messages_list")
                ) {
                    itemsIndexed(filteredMessages, key = { _, message -> message.id }) { index, message ->
                        MessageItemCard(
                            message = message,
                            onClick = {
                                viewModel.selectMessage(message)
                                onNavigateToDetail(message)
                            },
                            onDelete = { viewModel.deleteMessage(message) },
                            onToggleArchive = { viewModel.archiveMessage(message.id, !message.isArchived) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        
                        // Show banner ad after every 3rd message
                        if ((index + 1) % 3 == 0) {
                            UnityAdsManager.UnityAdBanner()
                        }
                    }
                }
            }

            // Unobtrusive bottom ad banner
            UnityAdsManager.UnityAdBanner()
        }
    }

    if (showCreateDialog) {
        ChangeEmailDialog(
            availableDomains = availableDomains,
            onDismiss = { showCreateDialog = false },
            onCreate = { custom, domain ->
                viewModel.generateEmail(custom, domain)
                showCreateDialog = false
            }
        )
    }

    if (showDeleteAllConfirm && activeEmail != null) {
        ConfirmDeleteDialog(
            title = stringResource(id = R.string.delete_all_messages),
            message = stringResource(id = R.string.confirm_delete_all),
            onDismiss = { showDeleteAllConfirm = false },
            onConfirm = {
                viewModel.deleteAllMessages(activeEmail!!.id)
                showDeleteAllConfirm = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxTopBar(
    activeEmail: EmailAccountEntity?,
    onRefresh: () -> Unit,
    onDeleteAll: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(id = R.string.nav_inbox),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        actions = {
            IconButton(onClick = onRefresh, modifier = Modifier.testTag("refresh_btn")) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh")
            }
            if (activeEmail != null) {
                IconButton(onClick = onDeleteAll, modifier = Modifier.testTag("delete_all_btn")) {
                    Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = "Delete All")
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground
        )
    )
}

@Composable
fun AddressCard(
    activeEmail: EmailAccountEntity?,
    onChangeEmail: () -> Unit,
    onCopyEmail: (String) -> Unit,
    onShareEmail: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (activeEmail != null) {
                Text(
                    text = activeEmail.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = activeEmail.address,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Timer countdown
                CountdownTimerView(expiresAt = activeEmail.expiresAt)

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FilledTonalButton(
                        onClick = { onCopyEmail(activeEmail.address) },
                        modifier = Modifier.testTag("copy_address_btn")
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = stringResource(id = R.string.copy_email))
                    }

                    FilledTonalButton(
                        onClick = { onShareEmail(activeEmail.address) }
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = stringResource(id = R.string.share_email))
                    }
                }
            } else {
                Text(
                    text = stringResource(id = R.string.no_active_email),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onChangeEmail,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("change_email_btn")
            ) {
                Icon(imageVector = Icons.Default.Email, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(id = R.string.change_email),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
fun CountdownTimerView(expiresAt: Long) {
    var remainingText by remember { mutableStateOf("") }
    var isExpired by remember { mutableStateOf(false) }

    LaunchedEffect(expiresAt) {
        while (true) {
            val now = System.currentTimeMillis()
            val diff = expiresAt - now
            if (diff <= 0) {
                remainingText = "Expired"
                isExpired = true
                break
            } else {
                val days = diff / (24 * 60 * 60 * 1000)
                val hours = (diff % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000)
                val minutes = (diff % (60 * 60 * 1000)) / (60 * 1000)
                val seconds = (diff % (60 * 1000)) / 1000
                remainingText = String.format("%02dd %02dh %02dm %02ds", days, hours, minutes, seconds)
                isExpired = false
            }
            delay(1000L)
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isExpired) ColorExpiredText.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Timer,
            contentDescription = null,
            tint = if (isExpired) ColorExpiredText else MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = stringResource(id = R.string.expires_in, remainingText),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (isExpired) ColorExpiredText else MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
fun FilterRow(
    selectedFilter: String,
    onFilterSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val filters = listOf(
            "all" to R.string.filter_all,
            "unread" to R.string.filter_unread,
            "attachments" to R.string.filter_attachments,
            "archived" to R.string.filter_archived
        )

        filters.forEach { (key, stringResId) ->
            val isSelected = selectedFilter == key
            FilterChip(
                selected = isSelected,
                onClick = { onFilterSelected(key) },
                label = { Text(text = stringResource(id = stringResId)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.testTag("filter_chip_$key")
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageItemCard(
    message: MessageEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onToggleArchive: () -> Unit
) {
    var expandedMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { expandedMenu = true }
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (message.isRead) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sender Avatar
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (message.isRead) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            else MaterialTheme.colorScheme.primary
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val initials = message.senderName.take(1).uppercase()
                    Text(
                        text = initials.ifEmpty { "?" },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (message.isRead) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Text details
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = message.senderName,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = if (message.isRead) FontWeight.Normal else FontWeight.Bold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        
                        // Small unread status dot
                        if (!message.isRead) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(ColorUnreadDot)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = message.subject,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (message.isRead) FontWeight.Normal else FontWeight.SemiBold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = message.preview,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Badges row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (message.isSpam) {
                            BadgeLabel(text = stringResource(id = R.string.badge_spam), color = ColorSpamBadge)
                        }
                        if (message.hasAttachments) {
                            BadgeLabel(text = stringResource(id = R.string.badge_attachment), color = ColorAttachmentBadge)
                        }
                    }
                }
            }

            DropdownMenu(
                expanded = expandedMenu,
                onDismissRequest = { expandedMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text(text = if (message.isArchived) "Unarchive" else "Archive") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Archive, contentDescription = null) },
                    onClick = {
                        onToggleArchive()
                        expandedMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text(text = stringResource(id = R.string.btn_delete)) },
                    leadingIcon = { Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    onClick = {
                        onDelete()
                        expandedMenu = false
                    }
                )
            }
        }
    }
}

@Composable
fun BadgeLabel(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = color
        )
    }
}

@Composable
fun EmptyStateView(
    title: String,
    subtitle: String,
    onCreateEmail: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.size(160.dp),
            shape = RoundedCornerShape(40.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Inbox,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        if (onCreateEmail != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onCreateEmail) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(id = R.string.generate_email_title))
            }
        }
    }
}
