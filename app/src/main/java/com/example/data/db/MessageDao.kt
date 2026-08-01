package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages")
    suspend fun getAllMessagesList(): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE accountId = :accountId ORDER BY receivedAt DESC")
    fun getMessagesForAccount(accountId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE id = :messageId LIMIT 1")
    fun getMessageByIdFlow(messageId: String): Flow<MessageEntity?>

    @Query("SELECT * FROM messages WHERE id = :messageId LIMIT 1")
    suspend fun getMessageById(messageId: String): MessageEntity?

    @Query("SELECT COUNT(*) FROM messages WHERE accountId = :accountId AND isRead = 0")
    fun getUnreadCount(accountId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("UPDATE messages SET isRead = :read WHERE id = :messageId")
    suspend fun markAsRead(messageId: String, read: Boolean = true)

    @Query("UPDATE messages SET isRead = 1 WHERE accountId = :accountId")
    suspend fun markAllAsRead(accountId: String)

    @Query("UPDATE messages SET isArchived = :archived WHERE id = :messageId")
    suspend fun archiveMessage(messageId: String, archived: Boolean = true)

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessageById(messageId: String)

    @Query("DELETE FROM messages WHERE accountId = :accountId")
    suspend fun deleteAllForAccount(accountId: String)

    @Query("DELETE FROM messages WHERE id IN (:messageIds)")
    suspend fun deleteMessagesByIds(messageIds: List<String>)

    @Query("SELECT COUNT(*) FROM messages WHERE accountId = :accountId")
    suspend fun getMessageCount(accountId: String): Int

    @Query("""
        SELECT * FROM messages 
        WHERE accountId = :accountId AND (
            senderName LIKE '%' || :query || '%' OR 
            senderEmail LIKE '%' || :query || '%' OR 
            subject LIKE '%' || :query || '%' OR 
            textBody LIKE '%' || :query || '%'
        )
        ORDER BY receivedAt DESC
    """)
    fun searchMessages(accountId: String, query: String): Flow<List<MessageEntity>>

    @Query("""
        SELECT * FROM messages 
        WHERE (
            senderName LIKE '%' || :query || '%' OR 
            senderEmail LIKE '%' || :query || '%' OR 
            subject LIKE '%' || :query || '%' OR 
            textBody LIKE '%' || :query || '%'
        )
        ORDER BY receivedAt DESC
    """)
    fun searchAllMessages(query: String): Flow<List<MessageEntity>>
}
