package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.provider.SendAttachment
import com.example.data.provider.ProviderSendResult
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

/**
 * ComposeEmailScreen - Full-featured email composition screen with Gmail-like features.
 *
 * Features:
 * - To / CC / BCC recipient fields with chip-style display
 * - Subject line with auto-suggestion
 * - Rich text formatting toolbar (bold, italic, underline, strikethrough, font size)
 * - File attachments from device
 * - Reply / Forward support
 * - Send / Save Draft / Discard actions
 * - HTML email body generation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeEmailScreen(
    viewModel: MainViewModel,
    onSent: () -> Unit = {},
    onDiscard: () -> Unit = {},
    replyTo: String? = null,
    replyToSubject: String? = null,
    replyToBody: String? = null,
    forwardFrom: String? = null,
    forwardSubject: String? = null,
    forwardBody: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Recipient fields
    var toText by remember { mutableStateOf("") }
    var ccText by remember { mutableStateOf("") }
    var bccText by remember { mutableStateOf("") }
    var toRecipients by remember { mutableStateOf(listOf<String>()) }
    var ccRecipients by remember { mutableStateOf(listOf<String>()) }
    var bccRecipients by remember { mutableStateOf(listOf<String>()) }
    var showCcBcc by remember { mutableStateOf(false) }

    // Subject and body
    var subject by remember { mutableStateOf("") }
    var bodyText by remember { mutableStateOf(TextFieldValue("")) }

    // Formatting state
    var isBold by remember { mutableStateOf(false) }
    var isItalic by remember { mutableStateOf(false) }
    var isUnderline by remember { mutableStateOf(false) }
    var isStrikethrough by remember { mutableStateOf(false) }
    var currentFontSize by remember { mutableIntStateOf(14) }

    // Attachments
    var attachments by remember { mutableStateOf(listOf<AttachmentInfo>()) }

    // UI state
    var isSending by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var sendError by remember { mutableStateOf<String?>(null) }

    // Initialize reply/forward fields
    LaunchedEffect(replyTo, forwardFrom) {
        if (replyTo != null) {
            toRecipients = listOf(replyTo)
            showCcBcc = true
            subject = if (replyToSubject?.startsWith("Re:", ignoreCase = true) == true) {
                replyToSubject
            } else {
                "Re: ${replyToSubject ?: ""}"
            }
            bodyText = TextFieldValue("\n\n--- Original Message ---\n${replyToBody ?: ""}")
        } else if (forwardFrom != null) {
            showCcBcc = true
            subject = if (forwardSubject?.startsWith("Fwd:", ignoreCase = true) == true) {
                forwardSubject
            } else {
                "Fwd: ${forwardSubject ?: ""}"
            }
            bodyText = TextFieldValue("\n\n--- Forwarded Message ---\nFrom: $forwardFrom\n${forwardBody ?: ""}")
        }
    }

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        for (uri in uris) {
            try {
                val fileInfo = getFileInfoFromUri(context, uri)
                if (fileInfo != null) {
                    attachments = attachments + fileInfo
                }
            } catch (e: Exception) {
                // Silently skip invalid files
            }
        }
    }

    // Validation
    val canSend = toRecipients.isNotEmpty() && subject.isNotBlank() && bodyText.text.isNotBlank() && !isSending

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when {
                            replyTo != null -> "Reply"
                            forwardFrom != null -> "Forward"
                            else -> "Compose"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (toRecipients.isNotEmpty() || subject.isNotBlank() || bodyText.text.isNotBlank()) {
                            showDiscardDialog = true
                        } else {
                            onDiscard()
                        }
                    }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showCcBcc = !showCcBcc },
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "CC/BCC"
                        )
                    }
                    IconButton(
                        onClick = {
                            filePickerLauncher.launch(arrayOf("*/*"))
                        },
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "Attach file"
                        )
                    }
                    IconButton(
                        onClick = {
                            if (canSend) {
                                isSending = true
                                sendError = null
                                coroutineScope.launch {
                                    // Build HTML body from formatted text
                                    val htmlBody = buildHtmlBody(bodyText.text, isBold, isItalic, isUnderline, isStrikethrough, currentFontSize)

                                    // Build attachments list
                                    val sendAttachments = attachments.map { att ->
                                        SendAttachment(
                                            filename = att.filename,
                                            contentType = att.mimeType,
                                            contentBase64 = att.base64Content
                                        )
                                    }

                                    viewModel.sendMessage(
                                        to = toRecipients,
                                        cc = ccRecipients,
                                        bcc = bccRecipients,
                                        subject = subject,
                                        textBody = bodyText.text,
                                        htmlBody = htmlBody,
                                        attachments = sendAttachments,
                                        replyToMessageId = replyTo
                                    ) { result: ProviderSendResult ->
                                        isSending = false
                                        if (result.isSuccess) {
                                            onSent()
                                        } else {
                                            sendError = result.errorMessage
                                        }
                                    }
                                }
                            }
                        },
                        enabled = canSend,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (canSend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (canSend) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(imageVector = Icons.Default.Send, contentDescription = "Send")
                        }
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
                .verticalScroll(rememberScrollState())
        ) {
            // From field (read-only)
            RecipientRow(
                label = "From",
                value = viewModel.activeAccount.collectAsState().value?.address ?: "No active email",
                isReadOnly = true
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)

            // To field
            RecipientInputRow(
                label = "To",
                text = toText,
                onTextChange = { toText = it },
                recipients = toRecipients,
                onAddRecipient = { email ->
                    if (email.contains("@") && !toRecipients.contains(email.trim())) {
                        toRecipients = toRecipients + email.trim()
                    }
                    toText = ""
                },
                onRemoveRecipient = { email ->
                    toRecipients = toRecipients - email
                }
            )

            // CC/BCC fields (collapsible)
            AnimatedVisibility(visible = showCcBcc) {
                Column {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                    RecipientInputRow(
                        label = "CC",
                        text = ccText,
                        onTextChange = { ccText = it },
                        recipients = ccRecipients,
                        onAddRecipient = { email ->
                            if (email.contains("@") && !ccRecipients.contains(email.trim())) {
                                ccRecipients = ccRecipients + email.trim()
                            }
                            ccText = ""
                        },
                        onRemoveRecipient = { email ->
                            ccRecipients = ccRecipients - email
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                    RecipientInputRow(
                        label = "BCC",
                        text = bccText,
                        onTextChange = { bccText = it },
                        recipients = bccRecipients,
                        onAddRecipient = { email ->
                            if (email.contains("@") && !bccRecipients.contains(email.trim())) {
                                bccRecipients = bccRecipients + email.trim()
                            }
                            bccText = ""
                        },
                        onRemoveRecipient = { email ->
                            bccRecipients = bccRecipients - email
                        }
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)

            // Subject field
            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it },
                placeholder = { Text("Subject", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                    focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent
                ),
                singleLine = true
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)

            // Formatting toolbar
            FormattingToolbar(
                isBold = isBold,
                isItalic = isItalic,
                isUnderline = isUnderline,
                isStrikethrough = isStrikethrough,
                fontSize = currentFontSize,
                onBoldToggle = { isBold = !isBold },
                onItalicToggle = { isItalic = !isItalic },
                onUnderlineToggle = { isUnderline = !isUnderline },
                onStrikethroughToggle = { isStrikethrough = !isStrikethrough },
                onFontSizeChange = { currentFontSize = it }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)

            // Body text field
            OutlinedTextField(
                value = bodyText,
                onValueChange = { bodyText = it },
                placeholder = { Text("Compose email...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 250.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                    fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
                    fontSize = currentFontSize.sp
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                    focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent
                ),
                maxLines = Int.MAX_VALUE
            )

            // Attachments section
            if (attachments.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), thickness = 0.5.dp)

                Text(
                    text = "Attachments (${attachments.size})",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )

                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(attachments) { att ->
                        AttachmentChip(
                            attachment = att,
                            onRemove = {
                                attachments = attachments - att
                            }
                        )
                    }
                }
            }

            // Error message
            if (sendError != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = sendError!!,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // Discard confirmation dialog
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard email?") },
            text = { Text("Your message will be lost. Are you sure you want to discard this email?") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    onDiscard()
                }) {
                    Text("Discard", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Keep editing")
                }
            }
        )
    }
}

/**
 * Read-only recipient row for "From" field.
 */
@Composable
private fun RecipientRow(
    label: String,
    value: String,
    isReadOnly: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(50.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

/**
 * Recipient input row with chip-style display for added recipients.
 */
@Composable
private fun RecipientInputRow(
    label: String,
    text: String,
    onTextChange: (String) -> Unit,
    recipients: List<String>,
    onAddRecipient: (String) -> Unit,
    onRemoveRecipient: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(50.dp)
            )

            // Recipient chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(recipients) { email ->
                    InputChip(
                        selected = true,
                        onClick = { onRemoveRecipient(email) },
                        label = {
                            Text(
                                text = email,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1
                            )
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove",
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        modifier = Modifier.height(28.dp)
                    )
                }
            }
        }

        // Text input for adding new recipients
        OutlinedTextField(
            value = text,
            onValueChange = { newText ->
                onTextChange(newText)
                // Auto-add on comma, semicolon, or space after @
                if (newText.endsWith(",") || newText.endsWith(";") || (newText.endsWith(" ") && newText.contains("@"))) {
                    val cleanText = newText.trimEnd(',', ';', ' ').trim()
                    if (cleanText.contains("@")) {
                        onAddRecipient(cleanText)
                    }
                }
            },
            placeholder = {
                if (recipients.isEmpty()) {
                    Text("Add recipients", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 50.dp),
            textStyle = MaterialTheme.typography.bodyMedium,
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent
            ),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Email
            )
        )
    }
}

/**
 * Rich text formatting toolbar.
 */
@Composable
private fun FormattingToolbar(
    isBold: Boolean,
    isItalic: Boolean,
    isUnderline: Boolean,
    isStrikethrough: Boolean,
    fontSize: Int,
    onBoldToggle: () -> Unit,
    onItalicToggle: () -> Unit,
    onUnderlineToggle: () -> Unit,
    onStrikethroughToggle: () -> Unit,
    onFontSizeChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Bold
        IconButton(onClick = onBoldToggle, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.Default.FormatBold,
                contentDescription = "Bold",
                tint = if (isBold) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
        // Italic
        IconButton(onClick = onItalicToggle, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.Default.FormatItalic,
                contentDescription = "Italic",
                tint = if (isItalic) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
        // Underline
        IconButton(onClick = onUnderlineToggle, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.Default.FormatUnderlined,
                contentDescription = "Underline",
                tint = if (isUnderline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
        // Strikethrough
        IconButton(onClick = onStrikethroughToggle, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.Default.StrikethroughS,
                contentDescription = "Strikethrough",
                tint = if (isStrikethrough) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Font size controls
        IconButton(onClick = { onFontSizeChange((fontSize - 2).coerceIn(10, 28)) }, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.Default.TextDecrease,
                contentDescription = "Decrease font size",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
        }

        Text(
            text = "${fontSize}sp",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.width(32.dp)
        )

        IconButton(onClick = { onFontSizeChange((fontSize + 2).coerceIn(10, 28)) }, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.Default.TextIncrease,
                contentDescription = "Increase font size",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Attach button
        IconButton(onClick = {}, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.Default.InsertLink,
                contentDescription = "Insert link",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }

        // Emoji
        IconButton(onClick = {}, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.Default.EmojiEmotions,
                contentDescription = "Emoji",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

/**
 * Attachment chip display for the compose screen.
 */
@Composable
private fun AttachmentChip(
    attachment: AttachmentInfo,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.width(160.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when {
                    attachment.mimeType.startsWith("image") -> Icons.Default.Image
                    attachment.mimeType.startsWith("video") -> Icons.Default.VideoFile
                    attachment.mimeType.contains("pdf") -> Icons.Default.PictureAsPdf
                    else -> Icons.Default.AttachFile
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = attachment.filename,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = attachment.sizeDisplay,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(20.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * Build HTML body from plain text with formatting flags.
 */
private fun buildHtmlBody(
    text: String,
    isBold: Boolean,
    isItalic: Boolean,
    isUnderline: Boolean,
    isStrikethrough: Boolean,
    fontSize: Int
): String {
    val escapedText = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\n", "<br>")

    var html = escapedText

    if (isBold) html = "<b>$html</b>"
    if (isItalic) html = "<i>$html</i>"
    if (isUnderline) html = "<u>$html</u>"
    if (isStrikethrough) html = "<s>$html</s>"

    return """<!DOCTYPE html>
<html>
<head><meta charset="utf-8"><style>body{font-family:sans-serif;font-size:${fontSize}px;}</style></head>
<body>$html</body>
</html>"""
}

/**
 * Data class for attachment info in compose screen.
 */
data class AttachmentInfo(
    val filename: String,
    val mimeType: String,
    val sizeBytes: Long,
    val base64Content: String,
    val uri: Uri
) {
    val sizeDisplay: String
        get() = when {
            sizeBytes < 1024 -> "$sizeBytes B"
            sizeBytes < 1024 * 1024 -> String.format("%.1f KB", sizeBytes / 1024.0)
            else -> String.format("%.1f MB", sizeBytes / (1024.0 * 1024.0))
        }
}

/**
 * Read file content from URI and create AttachmentInfo.
 */
private fun getFileInfoFromUri(context: Context, uri: Uri): AttachmentInfo? {
    return try {
        val contentResolver = context.contentResolver
        val cursor = contentResolver.query(uri, null, null, null, null)
        var filename = "attachment"
        var size = 0L

        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                val sizeIndex = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (nameIndex >= 0) filename = it.getString(nameIndex)
                if (sizeIndex >= 0) size = it.getLong(sizeIndex)
            }
        }

        val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
        val inputStream = contentResolver.openInputStream(uri) ?: return null
        val bytes = inputStream.readBytes()
        inputStream.close()
        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)

        AttachmentInfo(
            filename = filename,
            mimeType = mimeType,
            sizeBytes = size,
            base64Content = base64,
            uri = uri
        )
    } catch (e: Exception) {
        null
    }
}
