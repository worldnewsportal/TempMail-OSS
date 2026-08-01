package com.example.data.provider

import java.util.UUID

/**
 * SimulationProvider - Offline/Fallback provider
 *
 * This provider does NOT connect to any real API.
 * It creates local-only accounts that cannot receive real email.
 * No fake messages are generated - inbox is always empty.
 *
 * Used only as a last resort when all real providers fail.
 */
class SimulationProvider : EmailProvider {
    override val providerName: String = "Offline (No API)"

    private val domains = listOf("offline.local")

    override suspend fun healthCheck(): Boolean = true

    override suspend fun getAvailableDomains(): List<String> = domains

    override suspend fun createAccount(
        customUsername: String?,
        domain: String?
    ): ProviderAccountResult {
        val selectedDomain = domains.first()
        val username = if (!customUsername.isNullOrBlank()) {
            customUsername.lowercase().replace(Regex("[^a-z0-9]"), "")
        } else {
            "user" + UUID.randomUUID().toString().replace("-", "").take(8)
        }

        val fullAddress = "$username@$selectedDomain"
        return ProviderAccountResult(
            id = "offline_${UUID.randomUUID()}",
            address = fullAddress,
            username = username,
            domain = selectedDomain,
            token = "offline_${System.currentTimeMillis()}",
            providerName = providerName
        )
    }

    override suspend fun fetchInbox(
        accountId: String,
        token: String
    ): List<ProviderMessageSummary> {
        // No fake messages - empty inbox
        return emptyList()
    }

    override suspend fun fetchMessageDetails(
        accountId: String,
        token: String,
        messageId: String
    ): ProviderMessageDetail {
        throw IllegalStateException("No messages available in offline mode")
    }

    override suspend fun deleteMessage(
        accountId: String,
        token: String,
        messageId: String
    ): Boolean = true
}
