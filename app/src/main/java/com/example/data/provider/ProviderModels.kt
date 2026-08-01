package com.example.data.provider

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ProviderAccountResult(
    val id: String,
    val address: String,
    val username: String,
    val domain: String,
    val token: String,
    val providerName: String
)

@JsonClass(generateAdapter = true)
data class ProviderAttachment(
    val id: String,
    val filename: String,
    val contentType: String,
    val sizeBytes: Long,
    val downloadUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class ProviderMessageSummary(
    val id: String,
    val fromName: String,
    val fromEmail: String,
    val subject: String,
    val preview: String,
    val receivedAt: Long,
    val hasAttachments: Boolean
)

@JsonClass(generateAdapter = true)
data class ProviderMessageDetail(
    val id: String,
    val fromName: String,
    val fromEmail: String,
    val subject: String,
    val textBody: String,
    val htmlBody: String? = null,
    val receivedAt: Long,
    val attachments: List<ProviderAttachment> = emptyList()
)

/**
 * Result of a send message operation.
 */
data class ProviderSendResult(
    val isSuccess: Boolean,
    val messageId: String? = null,
    val errorMessage: String? = null
)

/**
 * Represents an attachment to be sent with an email.
 */
data class SendAttachment(
    val filename: String,
    val contentType: String,
    val contentBase64: String
)

