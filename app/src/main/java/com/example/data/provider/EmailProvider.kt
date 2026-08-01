package com.example.data.provider

interface EmailProvider {
    val providerName: String

    suspend fun healthCheck(): Boolean

    suspend fun getAvailableDomains(): List<String>

    suspend fun createAccount(
        customUsername: String? = null,
        domain: String? = null
    ): ProviderAccountResult

    suspend fun fetchInbox(
        accountId: String,
        token: String
    ): List<ProviderMessageSummary>

    suspend fun fetchMessageDetails(
        accountId: String,
        token: String,
        messageId: String
    ): ProviderMessageDetail

    suspend fun deleteMessage(
        accountId: String,
        token: String,
        messageId: String
    ): Boolean

    /**
     * Send an email message from the current account.
     * @param accountId The account ID to send from
     * @param token The authentication token
     * @param to List of recipient email addresses
     * @param cc List of CC recipient email addresses
     * @param bcc List of BCC recipient email addresses
     * @param subject Email subject line
     * @param textBody Plain text body of the email
     * @param htmlBody Optional HTML body of the email
     * @param attachments List of attachment file paths or content
     * @param replyToMessageId Optional message ID to reply to
     * @return ProviderSendResult indicating success or failure
     */
    suspend fun sendMessage(
        accountId: String,
        token: String,
        to: List<String>,
        cc: List<String> = emptyList(),
        bcc: List<String> = emptyList(),
        subject: String,
        textBody: String,
        htmlBody: String? = null,
        attachments: List<SendAttachment> = emptyList(),
        replyToMessageId: String? = null
    ): ProviderSendResult {
        throw UnsupportedOperationException("Sending not supported by $providerName")
    }

    /**
     * Check if this provider supports sending emails.
     */
    fun supportsSending(): Boolean = false
}
