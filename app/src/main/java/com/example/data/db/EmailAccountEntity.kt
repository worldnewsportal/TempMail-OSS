package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "email_accounts")
data class EmailAccountEntity(
    @PrimaryKey
    val id: String,
    val address: String,
    val username: String,
    val domain: String,
    val token: String,
    val providerName: String,
    val label: String = "Temp Email",
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = createdAt + 7L * 24L * 60L * 60L * 1000L, // Exactly 7 days
    val isFavorite: Boolean = false,
    val isActive: Boolean = false
) {
    val isExpired: Boolean
        get() = System.currentTimeMillis() >= expiresAt

    val remainingMillis: Long
        get() = (expiresAt - System.currentTimeMillis()).coerceAtLeast(0L)
}
