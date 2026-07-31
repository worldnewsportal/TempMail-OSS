package com.example.data.provider

import com.squareup.moshi.JsonClass

data class ProviderAccountResult(
    val id: String,
    val address: String,
    val username: String,
    val domain: String,
    val token: String,
    val providerName: String
)

data class ProviderAttachment(
    val id: String,
    val filename: String,
    val contentType: String,
    val sizeBytes: Long,
    val downloadUrl: String? = null
)

data class ProviderMessageSummary(
    val id: String,
    val fromName: String,
    val fromEmail: String,
    val subject: String,
    val preview: String,
    val receivedAt: Long,
    val hasAttachments: Boolean
)

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
