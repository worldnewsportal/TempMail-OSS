package com.example.data.provider

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

class MailTmProvider(private val api: MailTmApi) : EmailProvider {
    override val providerName: String = "Mail.tm"

    private val defaultDomains = listOf("mail.tm", "freemail.org", "tmpmail.net")

    override suspend fun healthCheck(): Boolean {
        return try {
            val response = api.getDomains(1)
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("MailTmProvider", "HealthCheck failed: ${e.message}")
            false
        }
    }

    override suspend fun getAvailableDomains(): List<String> {
        return try {
            val response = api.getDomains(1)
            if (response.isSuccessful && response.body() != null) {
                val memberList = response.body()?.member
                val domainNames = memberList?.filter { it.isActive }?.map { it.domain } ?: emptyList()
                if (domainNames.isNotEmpty()) domainNames else defaultDomains
            } else {
                defaultDomains
            }
        } catch (e: Exception) {
            Log.w("MailTmProvider", "getAvailableDomains failed, using defaults: ${e.message}")
            defaultDomains
        }
    }

    override suspend fun createAccount(
        customUsername: String?,
        domain: String?
    ): ProviderAccountResult {
        val domains = getAvailableDomains()
        val selectedDomain = if (!domain.isNullOrBlank() && domains.contains(domain)) {
            domain
        } else {
            domains.firstOrNull() ?: "mail.tm"
        }

        val username = if (!customUsername.isNullOrBlank()) {
            customUsername.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]"), "")
        } else {
            val randomString = UUID.randomUUID().toString().replace("-", "").take(10)
            "u$randomString"
        }

        val fullAddress = "$username@$selectedDomain"
        val password = "Oss@${UUID.randomUUID().toString().take(12)}"

        val createResp = api.createAccount(MailTmCreateAccountRequest(fullAddress, password))
        if (!createResp.isSuccessful || createResp.body() == null) {
            throw IllegalStateException("Failed to create Mail.tm account: HTTP ${createResp.code()}")
        }

        val accountId = createResp.body()!!.id
        val tokenResp = api.getToken(MailTmTokenRequest(fullAddress, password))
        val token = if (tokenResp.isSuccessful && tokenResp.body() != null) {
            tokenResp.body()!!.token
        } else {
            password // store password as fallback token if token call fails
        }

        return ProviderAccountResult(
            id = accountId,
            address = fullAddress,
            username = username,
            domain = selectedDomain,
            token = token,
            providerName = providerName
        )
    }

    override suspend fun fetchInbox(
        accountId: String,
        token: String
    ): List<ProviderMessageSummary> {
        val response = api.getMessages("Bearer $token")
        if (!response.isSuccessful || response.body() == null) {
            return emptyList()
        }

        val items = response.body()!!.member ?: emptyList()
        return items.map { item ->
            ProviderMessageSummary(
                id = item.id,
                fromName = item.from?.name ?: item.from?.address ?: "Unknown",
                fromEmail = item.from?.address ?: "unknown@sender",
                subject = item.subject?.takeIf { it.isNotBlank() } ?: "(No Subject)",
                preview = item.intro ?: "",
                receivedAt = parseIsoTimestamp(item.createdAt),
                hasAttachments = item.hasAttachments
            )
        }
    }

    override suspend fun fetchMessageDetails(
        accountId: String,
        token: String,
        messageId: String
    ): ProviderMessageDetail {
        val response = api.getMessageDetail("Bearer $token", messageId)
        if (!response.isSuccessful || response.body() == null) {
            throw IllegalStateException("Failed to load message details: HTTP ${response.code()}")
        }
        val detail = response.body()!!
        val htmlContent = detail.html?.joinToString("\n") ?: ""
        val textContent = detail.text ?: detail.intro ?: ""

        val attachmentList = detail.attachments?.map { att ->
            ProviderAttachment(
                id = att.id,
                filename = att.filename ?: "attachment.dat",
                contentType = att.contentType ?: "application/octet-stream",
                sizeBytes = att.size,
                downloadUrl = att.downloadUrl
            )
        } ?: emptyList()

        return ProviderMessageDetail(
            id = detail.id,
            fromName = detail.from?.name ?: detail.from?.address ?: "Unknown",
            fromEmail = detail.from?.address ?: "unknown@sender",
            subject = detail.subject?.takeIf { it.isNotBlank() } ?: "(No Subject)",
            textBody = textContent,
            htmlBody = htmlContent.takeIf { it.isNotBlank() },
            receivedAt = parseIsoTimestamp(detail.createdAt),
            attachments = attachmentList
        )
    }

    override suspend fun deleteMessage(
        accountId: String,
        token: String,
        messageId: String
    ): Boolean {
        return try {
            val response = api.deleteMessage("Bearer $token", messageId)
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    private fun parseIsoTimestamp(isoString: String?): Long {
        if (isoString.isNullOrBlank()) return System.currentTimeMillis()
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            format.timeZone = TimeZone.getTimeZone("UTC")
            val cleanStr = isoString.substringBefore(".")
            format.parse(cleanStr)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}
