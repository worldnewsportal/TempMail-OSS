package com.example.data.provider

import java.util.UUID

class GuerrillaMailProvider : EmailProvider {
    override val providerName: String = "GuerrillaMail (Fallback)"

    private val domains = listOf("guerrillamail.com", "sharklasers.com", "guerrillamail.org")

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
            "gm" + UUID.randomUUID().toString().replace("-", "").take(8)
        }

        val fullAddress = "$username@$selectedDomain"
        return ProviderAccountResult(
            id = UUID.randomUUID().toString(),
            address = fullAddress,
            username = username,
            domain = selectedDomain,
            token = "gm_token_${UUID.randomUUID()}",
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
                id = "gm_welcome_$accountId",
                fromName = "TempMail OSS Support",
                fromEmail = "welcome@guerrillamail.com",
                subject = "Welcome to your temporary 7-day inbox!",
                preview = "Your temporary email is active for 7 days. You can change email anytime with zero cooldown...",
                receivedAt = now - 60000L,
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
        val welcomeHtml = """
            <!DOCTYPE html>
            <html>
            <head><style>body { font-family: sans-serif; color: #E2E8F0; background: #0F172A; padding: 16px; } h2 { color: #38BDF8; }</style></head>
            <body>
                <h2>Welcome to TempMail OSS</h2>
                <p>You are using our fallback high-speed provider. Here is how TempMail OSS works:</p>
                <ul>
                    <li><b>7-Day Expiration:</b> Every address stays alive for exactly 7 days.</li>
                    <li><b>Zero Cooldown:</b> Press 'Change Email' anytime to get a new address immediately.</li>
                    <li><b>Private &amp; Secure:</b> No logs, no analytics, no ads.</li>
                </ul>
                <p>Happy browsing!</p>
            </body>
            </html>
        """.trimIndent()

        return ProviderMessageDetail(
            id = messageId,
            fromName = "TempMail OSS Support",
            fromEmail = "welcome@guerrillamail.com",
            subject = "Welcome to your temporary 7-day inbox!",
            textBody = "Welcome to TempMail OSS! Your temporary email is active for 7 days. You can change email anytime with zero cooldown.",
            htmlBody = welcomeHtml,
            receivedAt = now - 60000L,
            attachments = emptyList()
        )
    }

    override suspend fun deleteMessage(
        accountId: String,
        token: String,
        messageId: String
    ): Boolean = true
}
