package com.example.data.provider

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class MailTmDomainItem(
    val id: String,
    val domain: String,
    val isActive: Boolean = true
)

@JsonClass(generateAdapter = true)
data class MailTmDomainsResponse(
    @Json(name = "hydra:member") val member: List<MailTmDomainItem>? = null,
    val id: String? = null,
    val domain: String? = null
)

@JsonClass(generateAdapter = true)
data class MailTmCreateAccountRequest(
    val address: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class MailTmAccountResponse(
    val id: String,
    val address: String
)

@JsonClass(generateAdapter = true)
data class MailTmTokenRequest(
    val address: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class MailTmTokenResponse(
    val id: String? = null,
    val token: String
)

@JsonClass(generateAdapter = true)
data class MailTmSender(
    val address: String? = null,
    val name: String? = null
)

@JsonClass(generateAdapter = true)
data class MailTmMessageSummaryItem(
    val id: String,
    val from: MailTmSender? = null,
    val subject: String? = null,
    val intro: String? = null,
    val createdAt: String? = null,
    val hasAttachments: Boolean = false
)

@JsonClass(generateAdapter = true)
data class MailTmMessagesResponse(
    @Json(name = "hydra:member") val member: List<MailTmMessageSummaryItem>? = null
)

@JsonClass(generateAdapter = true)
data class MailTmAttachmentItem(
    val id: String,
    val filename: String? = null,
    val contentType: String? = null,
    val size: Long = 0L,
    val downloadUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class MailTmMessageDetailResponse(
    val id: String,
    val from: MailTmSender? = null,
    val subject: String? = null,
    val intro: String? = null,
    val text: String? = null,
    val html: List<String>? = null,
    val createdAt: String? = null,
    val attachments: List<MailTmAttachmentItem>? = null
)

@JsonClass(generateAdapter = true)
data class MailTmSendMessageRequest(
    val to: List<String>,
    val cc: List<String> = emptyList(),
    val bcc: List<String> = emptyList(),
    val subject: String,
    val text: String,
    val html: String? = null,
    val attachments: List<MailTmSendAttachmentItem> = emptyList(),
    val inReplyTo: String? = null,
    val references: String? = null
)

@JsonClass(generateAdapter = true)
data class MailTmSendAttachmentItem(
    val filename: String,
    val contentType: String,
    val content: String,
    val disposition: String = "attachment",
    val encoding: String = "base64"
)

@JsonClass(generateAdapter = true)
data class MailTmSendMessageResponse(
    val id: String
)

interface MailTmApi {
    @GET("domains")
    suspend fun getDomains(@Query("page") page: Int = 1): Response<MailTmDomainsResponse>

    @POST("accounts")
    suspend fun createAccount(@Body request: MailTmCreateAccountRequest): Response<MailTmAccountResponse>

    @POST("token")
    suspend fun getToken(@Body request: MailTmTokenRequest): Response<MailTmTokenResponse>

    @GET("messages")
    suspend fun getMessages(@Header("Authorization") authHeader: String): Response<MailTmMessagesResponse>

    @GET("messages/{id}")
    suspend fun getMessageDetail(
        @Header("Authorization") authHeader: String,
        @Path("id") messageId: String
    ): Response<MailTmMessageDetailResponse>

    @DELETE("messages/{id}")
    suspend fun deleteMessage(
        @Header("Authorization") authHeader: String,
        @Path("id") messageId: String
    ): Response<Unit>

    @POST("messages")
    suspend fun sendMessage(
        @Header("Authorization") authHeader: String,
        @Body request: MailTmSendMessageRequest
    ): Response<MailTmSendMessageResponse>
}
