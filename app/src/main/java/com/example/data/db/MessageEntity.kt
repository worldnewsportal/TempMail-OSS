package com.example.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    indices = [Index(value = ["accountId"]), Index(value = ["receivedAt"])],
    foreignKeys = [
        ForeignKey(
            entity = EmailAccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val accountId: String,
    val senderName: String,
    val senderEmail: String,
    val subject: String,
    val preview: String,
    val textBody: String,
    val htmlBody: String? = null,
    val receivedAt: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val isArchived: Boolean = false,
    val isSpam: Boolean = false,
    val hasAttachments: Boolean = false,
    val attachmentsJson: String? = null // JSON array of AttachmentMetadata
)
