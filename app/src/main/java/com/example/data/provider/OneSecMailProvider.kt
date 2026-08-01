package com.example.data.provider

import android.util.Log
import java.util.UUID

/**
 * OneSecMailProvider - DEPRECATED, now uses the same real 1secmail API
 * as GuerrillaMailProvider but with different default domains.
 *
 * Custom username support: YES - the username is sent to the real API.
 */
class OneSecMailProvider : EmailProvider {
    override val providerName: String = "1secmail-alt"

    private val domains = listOf("1secmail.com", "1secmail.org", "1secmail.net", "esiix.com", "wwjmp.com")

    override suspend fun healthCheck(): Boolean = true

    override suspend fun getAvailableDomains(): List<String> = domains

    override suspend fun createAccount(
        customUsername: String?,
        domain: String?
    ): ProviderAccountResult {
        val selectedDomain = if (!domain.isNullOrBlank() && domains.contains(domain)) {
            domain
        } else {
            domains.random()
        }
        val username = if (!customUsername.isNullOrBlank()) {
            customUsername.lowercase().replace(Regex("[^a-z0-9._-]"), "")
        } else {
            "alt" + UUID.randomUUID().toString().replace("-", "").take(10)
        }

        val fullAddress = "$username@$selectedDomain"
        return ProviderAccountResult(
            id = fullAddress,
            address = fullAddress,
            username = username,
            domain = selectedDomain,
            token = "$username@$selectedDomain",
            providerName = providerName
        )
    }

    override suspend fun fetchInbox(
        accountId: String,
        token: String
    ): List<ProviderMessageSummary> {
        return try {
            val parts = token.split("@")
            if (parts.size != 2) return emptyList()
            val login = parts[0]
            val domain = parts[1]

            val url = "https://www.1secmail.com/api/v1/?action=getMessages&login=$login&domain=$domain"
            val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            if (connection.responseCode != 200) return emptyList()

            val response = connection.inputStream.bufferedReader().readText()
            connection.disconnect()

            val messages = mutableListOf<ProviderMessageSummary>()
            val items = parseJsonArray(response)
            for (item in items) {
                val id = itemRegex.find(item)?.groupValues?.get(1) ?: continue
                val from = extractJsonString(item, "from") ?: "Unknown"
                val subject = extractJsonString(item, "subject") ?: "(No Subject)"

                messages.add(
                    ProviderMessageSummary(
                        id = id,
                        fromName = from.substringBefore("@"),
                        fromEmail = from,
                        subject = subject,
                        preview = "",
                        receivedAt = System.currentTimeMillis(),
                        hasAttachments = false
                    )
                )
            }
            messages
        } catch (e: Exception) {
            Log.e("OneSecMailProvider", "fetchInbox failed: ${e.message}")
            emptyList()
        }
    }

    override suspend fun fetchMessageDetails(
        accountId: String,
        token: String,
        messageId: String
    ): ProviderMessageDetail {
        val parts = token.split("@")
        if (parts.size != 2) throw IllegalStateException("Invalid token format")
        val login = parts[0]
        val domain = parts[1]

        val url = "https://www.1secmail.com/api/v1/?action=readMessage&login=$login&domain=$domain&id=$messageId"
        val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10000
        connection.readTimeout = 10000

        if (connection.responseCode != 200) throw IllegalStateException("Failed to fetch message")

        val response = connection.inputStream.bufferedReader().readText()
        connection.disconnect()

        val from = extractJsonString(response, "from") ?: "Unknown"
        val subject = extractJsonString(response, "subject") ?: "(No Subject)"
        val textBody = extractJsonString(response, "textBody") ?: ""
        val htmlBody = extractJsonString(response, "htmlBody")

        return ProviderMessageDetail(
            id = messageId,
            fromName = from.substringBefore("@"),
            fromEmail = from,
            subject = subject,
            textBody = textBody.ifBlank { subject },
            htmlBody = htmlBody,
            receivedAt = System.currentTimeMillis()
        )
    }

    override suspend fun deleteMessage(
        accountId: String,
        token: String,
        messageId: String
    ): Boolean = true

    private fun parseJsonArray(json: String): List<String> {
        val content = json.trim().removeSurrounding("[", "]")
        if (content.isBlank()) return emptyList()
        val items = mutableListOf<String>()
        var depth = 0
        var start = -1
        for (i in content.indices) {
            if (content[i] == '{') {
                if (depth == 0) start = i
                depth++
            } else if (content[i] == '}') {
                depth--
                if (depth == 0 && start >= 0) {
                    items.add(content.substring(start, i + 1))
                    start = -1
                }
            }
        }
        return items
    }

    private val itemRegex = Regex("\"id\"\\s*:\\s*(\\d+)")

    private fun extractJsonString(json: String, key: String): String? {
        val regex = Regex("\"$key\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"")
        val match = regex.find(json) ?: return null
        return match.groupValues[1]
            .replace("\\n", "\n")
            .replace("\\t", "\t")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
    }
}
