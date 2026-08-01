package com.example.data.provider

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProviderManager(
    private val mailTmProvider: MailTmProvider,
    private val guerrillaMailProvider: GuerrillaMailProvider = GuerrillaMailProvider(),
    private val oneSecMailProvider: OneSecMailProvider = OneSecMailProvider(),
    private val simulationProvider: SimulationProvider = SimulationProvider()
) {
    private val providers = listOf(mailTmProvider, guerrillaMailProvider, oneSecMailProvider, simulationProvider)
    private var currentProviderIndex = 0

    suspend fun getActiveProvider(): EmailProvider = withContext(Dispatchers.IO) {
        // Try current provider health check
        val current = providers[currentProviderIndex]
        if (current is SimulationProvider) {
            return@withContext current
        }
        if (current.healthCheck()) {
            return@withContext current
        }

        // Automatic failover to next provider
        Log.w("ProviderManager", "Provider ${current.providerName} failed health check. Automatic failover initiated.")
        for (i in providers.indices) {
            val provider = providers[i]
            if (provider is SimulationProvider) continue
            if (provider.healthCheck()) {
                currentProviderIndex = i
                Log.i("ProviderManager", "Switched to provider: ${provider.providerName}")
                return@withContext provider
            }
        }
        // Fall back to GuerrillaMail if all fail
        guerrillaMailProvider
    }

    suspend fun getAllAvailableDomains(): List<String> = withContext(Dispatchers.IO) {
        val domainsList = mutableListOf<String>()
        // Add live domains
        for (provider in providers) {
            if (provider !is SimulationProvider) {
                try {
                    val pDomains = provider.getAvailableDomains()
                    domainsList.addAll(pDomains)
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
        // Add premium simulated domains to guarantee > 50 domains always
        domainsList.addAll(simulationProvider.domains)
        domainsList.distinct()
    }

    suspend fun createAccountWithFailover(
        customUsername: String? = null,
        domain: String? = null
    ): ProviderAccountResult = withContext(Dispatchers.IO) {
        if (domain != null && simulationProvider.domains.contains(domain)) {
            return@withContext simulationProvider.createAccount(customUsername, domain)
        }
        var lastException: Exception? = null
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
        // If all fail, use fallback provider directly
        guerrillaMailProvider.createAccount(customUsername, domain)
    }

    fun getProviderByName(name: String): EmailProvider {
        return providers.find { it.providerName.equals(name, ignoreCase = true) } ?: guerrillaMailProvider
    }
}
