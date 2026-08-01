package com.example.data.provider

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ProviderManager - Manages email providers with automatic failover.
 *
 * Only REAL providers are used:
 * - Mail.tm (primary) - real API, supports custom username
 * - 1secmail (fallback) - real API, supports custom username
 * - 1secmail-alt (fallback) - real API, supports custom username
 * - Offline (last resort) - no API, empty inbox
 *
 * No fake "Premium" domains. All domains are from real email services.
 */
class ProviderManager(
    private val mailTmProvider: MailTmProvider,
    private val guerrillaMailProvider: GuerrillaMailProvider = GuerrillaMailProvider(),
    private val oneSecMailProvider: OneSecMailProvider = OneSecMailProvider(),
    private val simulationProvider: SimulationProvider = SimulationProvider()
) {
    private val providers = listOf(mailTmProvider, guerrillaMailProvider, oneSecMailProvider, simulationProvider)
    private var currentProviderIndex = 0

    suspend fun getActiveProvider(): EmailProvider = withContext(Dispatchers.IO) {
        val current = providers[currentProviderIndex]
        if (current is SimulationProvider) {
            return@withContext current
        }
        if (current.healthCheck()) {
            return@withContext current
        }

        Log.w("ProviderManager", "Provider ${current.providerName} failed health check. Failover initiated.")
        for (i in providers.indices) {
            val provider = providers[i]
            if (provider is SimulationProvider) continue
            if (provider.healthCheck()) {
                currentProviderIndex = i
                Log.i("ProviderManager", "Switched to provider: ${provider.providerName}")
                return@withContext provider
            }
        }
        guerrillaMailProvider
    }

    /**
     * Get all available domains from REAL providers only.
     * No fake/simulated domains.
     */
    suspend fun getAllAvailableDomains(): List<String> = withContext(Dispatchers.IO) {
        val domainsList = mutableListOf<String>()
        for (provider in providers) {
            if (provider is SimulationProvider) continue
            try {
                val pDomains = provider.getAvailableDomains()
                domainsList.addAll(pDomains)
            } catch (e: Exception) {
                // Ignore provider domain failures
            }
        }
        domainsList.distinct()
    }

    suspend fun createAccountWithFailover(
        customUsername: String? = null,
        domain: String? = null
    ): ProviderAccountResult = withContext(Dispatchers.IO) {
        var lastException: Exception? = null
        // Try real providers first, skip SimulationProvider
        for (i in providers.indices) {
            val provider = providers[(currentProviderIndex + i) % providers.size]
            if (provider is SimulationProvider) continue
            try {
                val account = provider.createAccount(customUsername, domain)
                currentProviderIndex = providers.indexOf(provider)
                return@withContext account
            } catch (e: Exception) {
                lastException = e
                Log.w("ProviderManager", "createAccount failed on ${provider.providerName}: ${e.message}. Trying next...")
            }
        }
        // If all real providers fail, use offline provider
        Log.w("ProviderManager", "All real providers failed. Using offline provider.")
        simulationProvider.createAccount(customUsername, domain)
    }

    fun getProviderByName(name: String): EmailProvider {
        return providers.find { it.providerName.equals(name, ignoreCase = true) } ?: guerrillaMailProvider
    }

    /**
     * Check if the current active provider supports sending emails.
     */
    suspend fun currentProviderSupportsSending(): Boolean {
        return getActiveProvider().supportsSending()
    }

    /**
     * Get a provider that supports sending emails.
     * Returns null if no provider supports sending.
     */
    suspend fun getSendingProvider(): EmailProvider? {
        val current = getActiveProvider()
        if (current.supportsSending()) return current
        return providers.find { it.supportsSending() }
    }

    /**
     * Send an email message using the best available provider.
     */
    suspend fun sendMessage(
        accountId: String,
        token: String,
        providerName: String,
        to: List<String>,
        cc: List<String> = emptyList(),
        bcc: List<String> = emptyList(),
        subject: String,
        textBody: String,
        htmlBody: String? = null,
        attachments: List<SendAttachment> = emptyList(),
        replyToMessageId: String? = null
    ): ProviderSendResult = withContext(Dispatchers.IO) {
        val provider = getProviderByName(providerName)
        if (provider.supportsSending()) {
            return@withContext provider.sendMessage(
                accountId, token, to, cc, bcc, subject, textBody, htmlBody, attachments, replyToMessageId
            )
        }
        // Try to find a provider that supports sending
        val sendingProvider = getSendingProvider()
        if (sendingProvider != null) {
            return@withContext sendingProvider.sendMessage(
                accountId, token, to, cc, bcc, subject, textBody, htmlBody, attachments, replyToMessageId
            )
        }
        ProviderSendResult(
            isSuccess = false,
            errorMessage = "No email provider supports sending. Please create a Mail.tm account to send emails."
        )
    }
}
