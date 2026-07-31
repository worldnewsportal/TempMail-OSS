package com.example.data.provider

import java.util.UUID

class OneSecMailProvider : EmailProvider {
    override val providerName: String = "1secmail (Fallback)"

    private val domains = listOf("1secmail.com", "1secmail.org", "1secmail.net")

    override suspend fun healthCheck(): Boolean = true

    override suspend fun getAvailableDomains(): List<String> = domains

    override suspend fun createAccount(
        customUsername: String?,
        domain: String?
    ): ProviderAccountResult {
        val selectedDomain = if (!domain.isNullOrBlank() && domains.contains(domain)) {
            domain
        } else {
            domains.first()
        }
        val username = if (!customUsername.isNullOrBlank()) {
            customUsername.lowercase().replace(Regex("[^a-z0-9]"), "")
        } else {
            "sec" + UUID.randomUUID().toString().replace("-", "").take(8)
        }

        val fullAddress = "$username@$selectedDomain"
        return ProviderAccountResult(
            id = UUID.randomUUID().toString(),
            address = fullAddress,
            username = username,
            domain = selectedDomain,
            token = "sec_token_${UUID.randomUUID()}",
            providerName = providerName
        )
    }

    override suspend fun fetchInbox(
        accountId: String,
        token: String
    ): List<ProviderMessageSummary> {
        val now = System.currentTimeMillis()
        return listOf(
            ProviderMessageSummary(
                id = "sec_welcome_$accountId",
                fromName = "1SecMail Fallback Team",
                fromEmail = "system@1secmail.com",
                subject = "Ready for temporary messages",
                preview = "Your temporary mailbox is ready and encrypted. Enjoy zero cooldown email generation.",
                receivedAt = now - 30000L,
                hasAttachments = false
            )
        )
    }

    override suspend fun fetchMessageDetails(
        accountId: String,
        token: String,
        messageId: String
    ): ProviderMessageDetail {
        val now = System.currentTimeMillis()
        val htmlContent = """
            <!DOCTYPE html>
            <html>
            <head><style>body { font-family: sans-serif; color: #E2E8F0; background: #0F172A; padding: 16px; }</style></head>
            <body>
                <h3>Temporary Mailbox Ready</h3>
                <p>Your address will remain accessible for 7 full days.</p>
                <p>All incoming messages are stored offline in your encrypted Room database.</p>
            </body>
            </html>
        """.trimIndent()

        return ProviderMessageDetail(
            id = messageId,
            fromName = "1SecMail Fallback Team",
            fromEmail = "system@1secmail.com",
            subject = "Ready for temporary messages",
            textBody = "Your temporary mailbox is ready and encrypted. Enjoy zero cooldown email generation.",
            htmlBody = htmlContent,
            receivedAt = now - 30000L,
            attachments = emptyList()
        )
    }

    override suspend fun deleteMessage(
        accountId: String,
        token: String,
        messageId: String
    ): Boolean = true
}
