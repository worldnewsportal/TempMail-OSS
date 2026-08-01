package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.db.AppDatabase
import com.example.data.db.EmailAccountEntity
import com.example.data.db.MessageEntity
import com.example.data.provider.ProviderManager
import com.example.data.provider.ProviderMessageDetail
import com.example.data.provider.ProviderSendResult
import com.example.data.provider.SendAttachment
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class EmailRepository(
    private val database: AppDatabase,
    private val providerManager: ProviderManager,
    private val context: Context
) {
    private val emailAccountDao = database.emailAccountDao()
    private val messageDao = database.messageDao()
    private val moshi = Moshi.Builder().build()

    val allAccounts: Flow<List<EmailAccountEntity>> = emailAccountDao.getAllAccounts()
    val activeAccount: Flow<EmailAccountEntity?> = emailAccountDao.getActiveAccount()
    val favoriteAccounts: Flow<List<EmailAccountEntity>> = emailAccountDao.getFavoriteAccounts()

    fun getMessagesForAccount(accountId: String): Flow<List<MessageEntity>> {
        return messageDao.getMessagesForAccount(accountId)
    }

    fun getMessageByIdFlow(messageId: String): Flow<MessageEntity?> {
        return messageDao.getMessageByIdFlow(messageId)
    }

    fun getUnreadCount(accountId: String): Flow<Int> {
        return messageDao.getUnreadCount(accountId)
    }

    fun searchMessages(accountId: String, query: String): Flow<List<MessageEntity>> {
        return if (query.isBlank()) {
            getMessagesForAccount(accountId)
        } else {
            messageDao.searchMessages(accountId, query)
        }
    }

    fun searchAllMessages(query: String): Flow<List<MessageEntity>> {
        return messageDao.searchAllMessages(query)
    }

    suspend fun getAvailableDomains(): List<String> {
        return providerManager.getAllAvailableDomains()
    }

    suspend fun generateNewEmail(customUsername: String? = null, domain: String? = null): EmailAccountEntity = withContext(Dispatchers.IO) {
        val result = providerManager.createAccountWithFailover(customUsername, domain)
        val newAccount = EmailAccountEntity(
            id = result.id,
            address = result.address,
            username = result.username,
            domain = result.domain,
            token = result.token,
            providerName = result.providerName,
            isActive = true
        )
        // Store in local DB and set as active (deactivating others)
        emailAccountDao.setActiveAccount(newAccount.id)
        emailAccountDao.insertAccount(newAccount)
        newAccount
    }

    suspend fun switchActiveEmail(accountId: String) = withContext(Dispatchers.IO) {
        emailAccountDao.setActiveAccount(accountId)
    }

    suspend fun extendEmailExpiration(accountId: String, durationMillis: Long) = withContext(Dispatchers.IO) {
        val account = emailAccountDao.getAccountById(accountId)
        if (account != null) {
            val currentExpiresAt = account.expiresAt
            val now = System.currentTimeMillis()
            val baseTime = if (currentExpiresAt > now) currentExpiresAt else now
            val newExpiresAt = baseTime + durationMillis
            emailAccountDao.updateAccount(account.copy(expiresAt = newExpiresAt))
        }
    }

    suspend fun deleteEmail(accountId: String) = withContext(Dispatchers.IO) {
        emailAccountDao.deleteAccountById(accountId)
        messageDao.deleteAllForAccount(accountId)
    }

    suspend fun updateLabel(accountId: String, newLabel: String) = withContext(Dispatchers.IO) {
        emailAccountDao.updateLabel(accountId, newLabel)
    }

    suspend fun toggleFavorite(accountId: String) = withContext(Dispatchers.IO) {
        val account = emailAccountDao.getAccountById(accountId)
        if (account != null) {
            emailAccountDao.updateFavorite(accountId, !account.isFavorite)
        }
    }

    suspend fun syncInbox(accountId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val account = emailAccountDao.getAccountById(accountId)
                ?: return@withContext Result.failure(Exception("Account not found"))

            if (account.isExpired) {
                return@withContext Result.failure(Exception("Account has expired"))
            }

            val provider = providerManager.getProviderByName(account.providerName)
            val remoteSummaries = provider.fetchInbox(account.id, account.token)

            val existingMessages = messageDao.getMessagesForAccount(account.id).firstOrNull() ?: emptyList()
            val existingIds = existingMessages.map { it.id }.toSet()

            // Fetch detail and cache for any new messages
            val newMessages = remoteSummaries.filter { it.id !in existingIds }.map { summary ->
                try {
                    val details = provider.fetchMessageDetails(account.id, account.token, summary.id)
                    convertDetailToEntity(account.id, details)
                } catch (e: Exception) {
                    // Fail gracefully for single message load, create basic summary message instead
                    MessageEntity(
                        id = summary.id,
                        accountId = account.id,
                        senderName = summary.fromName,
                        senderEmail = summary.fromEmail,
                        subject = summary.subject,
                        preview = summary.preview,
                        textBody = summary.preview,
                        receivedAt = summary.receivedAt,
                        hasAttachments = summary.hasAttachments
                    )
                }
            }

            if (newMessages.isNotEmpty()) {
                messageDao.insertMessages(newMessages)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("EmailRepository", "syncInbox failed for $accountId: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun fetchMessageDetailsAndCache(accountId: String, messageId: String): MessageEntity? = withContext(Dispatchers.IO) {
        try {
            val account = emailAccountDao.getAccountById(accountId) ?: return@withContext null
            val message = messageDao.getMessageById(messageId)

            // If we already have html body or details, return cached
            if (message != null && (!message.htmlBody.isNullOrBlank() || message.textBody.length > 500)) {
                return@withContext message
            }

            val provider = providerManager.getProviderByName(account.providerName)
            val details = provider.fetchMessageDetails(account.id, account.token, messageId)
            val updated = convertDetailToEntity(account.id, details).copy(isRead = message?.isRead ?: false)
            messageDao.insertMessage(updated)
            updated
        } catch (e: Exception) {
            Log.e("EmailRepository", "fetchMessageDetailsAndCache failed: ${e.message}")
            messageDao.getMessageById(messageId)
        }
    }

    suspend fun markAsRead(messageId: String, isRead: Boolean = true) = withContext(Dispatchers.IO) {
        messageDao.markAsRead(messageId, isRead)
    }

    suspend fun markAllAsRead(accountId: String) = withContext(Dispatchers.IO) {
        messageDao.markAllAsRead(accountId)
    }

    suspend fun archiveMessage(messageId: String, archived: Boolean = true) = withContext(Dispatchers.IO) {
        messageDao.archiveMessage(messageId, archived)
    }

    suspend fun deleteMessage(accountId: String, messageId: String) = withContext(Dispatchers.IO) {
        messageDao.deleteMessageById(messageId)
        try {
            val account = emailAccountDao.getAccountById(accountId)
            if (account != null && !account.isExpired) {
                val provider = providerManager.getProviderByName(account.providerName)
                provider.deleteMessage(account.id, account.token, messageId)
            }
        } catch (e: Exception) {
            Log.e("EmailRepository", "Failed to delete remote message: ${e.message}")
        }
    }

    suspend fun deleteAllMessagesForAccount(accountId: String) = withContext(Dispatchers.IO) {
        messageDao.deleteAllForAccount(accountId)
    }

    suspend fun deleteMultipleMessages(accountId: String, messageIds: List<String>) = withContext(Dispatchers.IO) {
        messageDao.deleteMessagesByIds(messageIds)
        // Also try to delete from remote
        try {
            val account = emailAccountDao.getAccountById(accountId)
            if (account != null && !account.isExpired) {
                val provider = providerManager.getProviderByName(account.providerName)
                for (msgId in messageIds) {
                    try {
                        provider.deleteMessage(account.id, account.token, msgId)
                    } catch (_: Exception) { }
                }
            }
        } catch (e: Exception) {
            Log.e("EmailRepository", "Failed to delete remote messages: ${e.message}")
        }
    }

    suspend fun getMessageCount(accountId: String): Int = withContext(Dispatchers.IO) {
        messageDao.getMessageCount(accountId)
    }

    /**
     * Send an email message from the given account.
     */
    suspend fun sendMessage(
        accountId: String,
        to: List<String>,
        cc: List<String> = emptyList(),
        bcc: List<String> = emptyList(),
        subject: String,
        textBody: String,
        htmlBody: String? = null,
        attachments: List<SendAttachment> = emptyList(),
        replyToMessageId: String? = null
    ): ProviderSendResult = withContext(Dispatchers.IO) {
        try {
            val account = emailAccountDao.getAccountById(accountId)
                ?: return@withContext ProviderSendResult(false, errorMessage = "Account not found")

            providerManager.sendMessage(
                accountId = account.id,
                token = account.token,
                providerName = account.providerName,
                to = to,
                cc = cc,
                bcc = bcc,
                subject = subject,
                textBody = textBody,
                htmlBody = htmlBody,
                attachments = attachments,
                replyToMessageId = replyToMessageId
            )
        } catch (e: Exception) {
            Log.e("EmailRepository", "sendMessage failed: ${e.message}")
            ProviderSendResult(false, errorMessage = e.message ?: "Failed to send message")
        }
    }

    /**
     * Check if the current active account supports sending emails.
     */
    suspend fun activeAccountSupportsSending(): Boolean = withContext(Dispatchers.IO) {
        val account = activeAccount.firstOrNull() ?: return@withContext false
        providerManager.getProviderByName(account.providerName).supportsSending()
    }

    suspend fun autoCleanExpired() = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val deletedCount = emailAccountDao.deleteExpiredAccounts(now)
        if (deletedCount > 0) {
            Log.i("EmailRepository", "Automatically cleaned up $deletedCount expired accounts.")
        }
    }

    private fun convertDetailToEntity(accountId: String, details: ProviderMessageDetail): MessageEntity {
        // Serialize attachments list to JSON
        val attachmentsType = Types.newParameterizedType(List::class.java, com.example.data.provider.ProviderAttachment::class.java)
        val adapter = moshi.adapter<List<com.example.data.provider.ProviderAttachment>>(attachmentsType)
        val attachmentsJson = if (details.attachments.isNotEmpty()) adapter.toJson(details.attachments) else null

        // Check if message looks like spam (basic heuristic, e.g. certain topics)
        val subjectLower = details.subject.lowercase()
        val isSpam = subjectLower.contains("spam") || subjectLower.contains("buy now") || subjectLower.contains("advertisement")

        return MessageEntity(
            id = details.id,
            accountId = accountId,
            senderName = details.fromName,
            senderEmail = details.fromEmail,
            subject = details.subject,
            preview = details.textBody.take(150),
            textBody = details.textBody,
            htmlBody = details.htmlBody,
            receivedAt = details.receivedAt,
            isRead = false,
            isArchived = false,
            isSpam = isSpam,
            hasAttachments = details.attachments.isNotEmpty(),
            attachmentsJson = attachmentsJson
        )
    }
}
