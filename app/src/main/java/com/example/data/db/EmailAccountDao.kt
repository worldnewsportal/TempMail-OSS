package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EmailAccountDao {
    @Query("SELECT * FROM email_accounts ORDER BY createdAt DESC")
    fun getAllAccounts(): Flow<List<EmailAccountEntity>>

    @Query("SELECT * FROM email_accounts WHERE isActive = 1 LIMIT 1")
    fun getActiveAccount(): Flow<EmailAccountEntity?>

    @Query("SELECT * FROM email_accounts WHERE id = :id LIMIT 1")
    suspend fun getAccountById(id: String): EmailAccountEntity?

    @Query("SELECT * FROM email_accounts WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteAccounts(): Flow<List<EmailAccountEntity>>

    @Query("SELECT * FROM email_accounts")
    suspend fun getAllAccountsList(): List<EmailAccountEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<EmailAccountEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: EmailAccountEntity)

    @Update
    suspend fun updateAccount(account: EmailAccountEntity)

    @Query("DELETE FROM email_accounts WHERE id = :id")
    suspend fun deleteAccountById(id: String)

    @Query("DELETE FROM email_accounts WHERE expiresAt <= :now")
    suspend fun deleteExpiredAccounts(now: Long = System.currentTimeMillis()): Int

    @Query("UPDATE email_accounts SET isActive = 0")
    suspend fun deactivateAll()

    @Query("UPDATE email_accounts SET isActive = 1 WHERE id = :id")
    suspend fun activateAccount(id: String)

    @Transaction
    suspend fun setActiveAccount(id: String) {
        deactivateAll()
        activateAccount(id)
    }

    @Query("UPDATE email_accounts SET label = :newLabel WHERE id = :id")
    suspend fun updateLabel(id: String, newLabel: String)

    @Query("UPDATE email_accounts SET isFavorite = :favorite WHERE id = :id")
    suspend fun updateFavorite(id: String, favorite: Boolean)
}
