package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.text.Html
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ads.UnityAdsManager
import com.example.data.db.MessageEntity
import com.example.data.provider.ProviderAttachment
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.viewmodel.MainViewModel
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailDetailsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onReply: (MessageEntity) -> Unit = {},
    onForward: (MessageEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val message by viewModel.selectedMessage.collectAsState()
    var isHtmlTab by remember { mutableStateOf(true) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val context = LocalContext.current

    if (message == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Select a message to view details")
        }
        return
    }

    val activeMsg = message!!

    // Parse attachments
    val attachments = remember(activeMsg.attachmentsJson) {
        if (!activeMsg.attachmentsJson.isNullOrBlank()) {
            try {
                val moshi = Moshi.Builder().build()
                val type = Types.newParameterizedType(List::class.java, ProviderAttachment::class.java)
                val adapter = moshi.adapter<List<ProviderAttachment>>(type)
                adapter.fromJson(activeMsg.attachmentsJson) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_btn")) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onReply(activeMsg) }) {
                        Icon(imageVector = Icons.Default.Reply, contentDescription = "Reply")
                    }
                    IconButton(onClick = { onForward(activeMsg) }) {
                        Icon(imageVector = Icons.Default.Forward, contentDescription = "Forward")
                    }
                    IconButton(onClick = {
                        val isArchived = activeMsg.isArchived
                        viewModel.archiveMessage(activeMsg.id, !isArchived)
                    }) {
                        Icon(
                            imageVector = if (activeMsg.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                            contentDescription = "Archive"
                        )
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
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
                    .verticalScroll(rememberScrollState())
            ) {
                // Header: Subject Line
                Text(
                    text = activeMsg.subject,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )

                // Sender Showcase Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = activeMsg.senderName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeMsg.senderName,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = activeMsg.senderEmail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = formatDate(activeMsg.receivedAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Copy sender icon
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Sender",
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        val clip = android.content.ClipData.newPlainText("Sender Email", activeMsg.senderEmail)
                                        clipboard.setPrimaryClip(clip)
                                        viewModel.showToast(context.getString(R.string.sender_copied))
                                    },
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Body rendering options tabs
            if (!activeMsg.htmlBody.isNullOrBlank()) {
                TabRow(
                    selectedTabIndex = if (isHtmlTab) 0 else 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    containerColor = Color.Transparent
                ) {
                    Tab(
                        selected = isHtmlTab,
                        onClick = { isHtmlTab = true },
                        text = { Text(text = stringResource(id = R.string.view_html)) }
                    )
                    Tab(
                        selected = !isHtmlTab,
                        onClick = { isHtmlTab = false },
                        text = { Text(text = stringResource(id = R.string.view_text)) }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            } else {
                isHtmlTab = false
            }

            // Body Text Canvas Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                if (isHtmlTab && !activeMsg.htmlBody.isNullOrBlank()) {
                    // Safe rich text rendering of HTML elements
                    val formattedHtml = parseHtmlToAnnotatedString(activeMsg.htmlBody ?: "")
                    SelectionContainerText(text = formattedHtml)
                } else {
                    SelectionContainerText(text = buildAnnotatedString { append(activeMsg.textBody) })
                }
            }

            // Messages Toolbar - Reply/Forward
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Reply button
                OutlinedButton(
                    onClick = { onReply(activeMsg) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Reply, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Reply")
                }

                // Forward button
                OutlinedButton(
                    onClick = { onForward(activeMsg) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Forward, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Forward")
                }
            }

            // Copy/Share Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("Message Body", activeMsg.textBody)
                        clipboard.setPrimaryClip(clip)
                        viewModel.showToast(context.getString(R.string.message_copied))
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Copy Body")
                }

                Button(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, activeMsg.subject)
                            putExtra(Intent.EXTRA_TEXT, "From: ${activeMsg.senderName} (${activeMsg.senderEmail})\nSubject: ${activeMsg.subject}\n\n${activeMsg.textBody}")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Message"))
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Share")
                }
            }

            // Attachments Section
            if (attachments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(id = R.string.attachments_title, attachments.size),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                attachments.forEach { att ->
                    AttachmentCard(
                        attachment = att,
                        onDownload = {
                            if (att.downloadUrl != null) {
                                viewModel.downloadAttachment(att.filename, att.downloadUrl)
                            } else {
                                viewModel.showToast("Attachment URL unavailable")
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            }

            // Unobtrusive bottom ad banner
            UnityAdsManager.UnityAdBanner()
        }
    }

    if (showDeleteConfirm) {
        ConfirmDeleteDialog(
            title = stringResource(id = R.string.btn_delete),
            message = "Are you sure you want to delete this message permanently?",
            onDismiss = { showDeleteConfirm = false },
            onConfirm = {
                viewModel.deleteMessage(activeMsg)
                showDeleteConfirm = false
                onBack()
            }
        )
    }
}

@Composable
fun SelectionContainerText(text: androidx.compose.ui.text.AnnotatedString) {
    androidx.compose.foundation.text.selection.SelectionContainer {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun AttachmentCard(
    attachment: ProviderAttachment,
    onDownload: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when {
                    attachment.contentType.contains("image") -> Icons.Default.Image
                    attachment.contentType.contains("pdf") -> Icons.Default.PictureAsPdf
                    else -> Icons.Default.AttachFile
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = attachment.filename,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatBytes(attachment.sizeBytes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            IconButton(onClick = onDownload) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = stringResource(id = R.string.download_attachment),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format(Locale.ROOT, "%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

/**
 * Parses simple HTML formatting into structured Compose AnnotatedString.
 * Prevents vulnerabilities of HTML views while maintaining beautiful styling.
 */
@Composable
fun parseHtmlToAnnotatedString(html: String): androidx.compose.ui.text.AnnotatedString {
    return remember(html) {
        // Strip out style tags and script blocks
        val cleanHtml = html
            .replace(Regex("<style>[\\s\\S]*?</style>"), "")
            .replace(Regex("<script>[\\s\\S]*?</script>"), "")

        val spanned = Html.fromHtml(cleanHtml, Html.FROM_HTML_MODE_LEGACY)
        
        buildAnnotatedString {
            val textString = spanned.toString()
            append(textString)

            // Apply basic bold formatting
            val length = textString.length
            var start = 0
            while (start < length) {
                val nextBold = cleanHtml.indexOf("<b>", start)
                if (nextBold == -1) break
                val endBold = cleanHtml.indexOf("</b>", nextBold)
                if (endBold == -1) break
                
                // Approximate translation to text offset
                val textStart = (nextBold * textString.length / cleanHtml.length).coerceIn(0, length)
                val textEnd = (endBold * textString.length / cleanHtml.length).coerceIn(0, length)
                if (textStart < textEnd) {
                    addStyle(SpanStyle(fontWeight = FontWeight.Bold), textStart, textEnd)
                }
                start = endBold + 4
            }
        }
    }
}
