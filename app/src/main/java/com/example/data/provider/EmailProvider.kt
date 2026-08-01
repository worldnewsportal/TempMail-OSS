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
}
