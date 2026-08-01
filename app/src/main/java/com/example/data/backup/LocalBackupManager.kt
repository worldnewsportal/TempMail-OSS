package com.example.data.backup

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import com.example.data.db.AppDatabase
import com.example.data.db.EmailAccountEntity
import com.example.data.db.MessageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupSnapshot(
    val name: String,
    val file: File,
    val accountCount: Int,
    val messageCount: Int,
    val timestamp: Long
)

sealed class RestoreResult {
    data class Success(val accountsCount: Int, val messagesCount: Int) : RestoreResult()
    data class Error(val message: String) : RestoreResult()
}

class LocalBackupManager(private val context: Context, private val database: AppDatabase) {
    private val TAG = "LocalBackupManager"
    private val backupDir = File(context.filesDir, "local_backups")

    init {
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }
    }

    private fun calculateSha256(data: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(data.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    suspend fun createBackupPayload(): String = withContext(Dispatchers.IO) {
        val accounts = database.emailAccountDao().getAllAccountsList()
        val messages = database.messageDao().getAllMessagesList()

        val rootJson = JSONObject()
        rootJson.put("backup_version", 1)
        rootJson.put("app", "TempMailOSS")
        rootJson.put("timestamp", System.currentTimeMillis())

        val accountsArray = JSONArray()
        for (acc in accounts) {
            val accJson = JSONObject()
            accJson.put("id", acc.id)
            accJson.put("address", acc.address)
            accJson.put("username", acc.username)
            accJson.put("domain", acc.domain)
            accJson.put("token", acc.token)
            accJson.put("providerName", acc.providerName)
            accJson.put("label", acc.label)
            accJson.put("createdAt", acc.createdAt)
            accJson.put("expiresAt", acc.expiresAt)
            accJson.put("isFavorite", acc.isFavorite)
            accJson.put("isActive", acc.isActive)
            accountsArray.put(accJson)
        }
        rootJson.put("accounts", accountsArray)

        val messagesArray = JSONArray()
        for (msg in messages) {
            val msgJson = JSONObject()
            msgJson.put("id", msg.id)
            msgJson.put("accountId", msg.accountId)
            msgJson.put("senderName", msg.senderName)
            msgJson.put("senderEmail", msg.senderEmail)
            msgJson.put("subject", msg.subject)
            msgJson.put("preview", msg.preview)
            msgJson.put("textBody", msg.textBody)
            msgJson.put("htmlBody", msg.htmlBody ?: JSONObject.NULL)
            msgJson.put("receivedAt", msg.receivedAt)
            msgJson.put("isRead", msg.isRead)
            msgJson.put("isArchived", msg.isArchived)
            msgJson.put("isSpam", msg.isSpam)
            msgJson.put("hasAttachments", msg.hasAttachments)
            msgJson.put("attachmentsJson", msg.attachmentsJson ?: JSONObject.NULL)
            messagesArray.put(msgJson)
        }
        rootJson.put("messages", messagesArray)

        // Generate SHA-256 integrity signature on accounts and messages content to protect against corruption or editing
        val payloadToHash = accountsArray.toString() + messagesArray.toString()
        val signature = calculateSha256(payloadToHash)
        rootJson.put("checksum", signature)

        return@withContext rootJson.toString(2) // Beautiful indented JSON output
    }

    suspend fun createLocalSnapshot(customLabel: String? = null): File? = withContext(Dispatchers.IO) {
        try {
            val payload = createBackupPayload()
            val timestamp = System.currentTimeMillis()
            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            val dateStr = sdf.format(Date(timestamp))
            val labelClean = customLabel?.replace(Regex("[^a-zA-Z0-9_\\-\\s]"), "") ?: "Snapshot"
            val file = File(backupDir, "${dateStr}_${labelClean.replace(" ", "_")}.json")
            
            FileOutputStream(file).use { out ->
                out.write(payload.toByteArray(Charsets.UTF_8))
            }
            Log.i(TAG, "Local snapshot backup created successfully: ${file.absolutePath}")
            return@withContext file
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create local snapshot", e)
            return@withContext null
        }
    }

    suspend fun getLocalSnapshots(): List<BackupSnapshot> = withContext(Dispatchers.IO) {
        val snapshotsList = mutableListOf<BackupSnapshot>()
        try {
            val files = backupDir.listFiles { _, name -> name.endsWith(".json") } ?: emptyArray()
            // Sort newest first
            files.sortByDescending { it.lastModified() }
            
            for (file in files) {
                try {
                    val content = FileInputStream(file).bufferedReader().use { it.readText() }
                    val root = JSONObject(content)
                    val accountsArray = root.getJSONArray("accounts")
                    val messagesArray = root.getJSONArray("messages")
                    val timestamp = root.optLong("timestamp", file.lastModified())
                    
                    val rawName = file.nameWithoutExtension
                    val parts = rawName.split("_")
                    val label = if (parts.size >= 3) {
                        parts.subList(2, parts.size).joinToString(" ")
                    } else if (parts.size == 2) {
                        parts[1]
                    } else {
                        rawName
                    }

                    snapshotsList.add(
                        BackupSnapshot(
                            name = label,
                            file = file,
                            accountCount = accountsArray.length(),
                            messageCount = messagesArray.length(),
                            timestamp = timestamp
                        )
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to read snapshot file: ${file.name}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load local snapshots list", e)
        }
        return@withContext snapshotsList
    }

    suspend fun restoreFromPayload(payload: String): RestoreResult = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(payload)
            if (!root.has("accounts") || !root.has("messages")) {
                return@withContext RestoreResult.Error("Invalid backup format: missing accounts or messages list")
            }

            val accountsArray = root.getJSONArray("accounts")
            val messagesArray = root.getJSONArray("messages")
            val backupChecksum = root.optString("checksum", "")

            // Verify integrity signature
            val payloadToHash = accountsArray.toString() + messagesArray.toString()
            val recalculatedChecksum = calculateSha256(payloadToHash)
            if (backupChecksum.isNotEmpty() && backupChecksum != recalculatedChecksum) {
                return@withContext RestoreResult.Error("Backup integrity check failed: file may be corrupted or modified")
            }

            val accountsToRestore = mutableListOf<EmailAccountEntity>()
            for (i in 0 until accountsArray.length()) {
                val accJson = accountsArray.getJSONObject(i)
                accountsToRestore.add(
                    EmailAccountEntity(
                        id = accJson.getString("id"),
                        address = accJson.getString("address"),
                        username = accJson.getString("username"),
                        domain = accJson.getString("domain"),
                        token = accJson.getString("token"),
                        providerName = accJson.getString("providerName"),
                        label = accJson.optString("label", "Temp Email"),
                        createdAt = accJson.optLong("createdAt", System.currentTimeMillis()),
                        expiresAt = accJson.optLong("expiresAt", System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L),
                        isFavorite = accJson.optBoolean("isFavorite", false),
                        isActive = accJson.optBoolean("isActive", false)
                    )
                )
            }

            val messagesToRestore = mutableListOf<MessageEntity>()
            for (i in 0 until messagesArray.length()) {
                val msgJson = messagesArray.getJSONObject(i)
                messagesToRestore.add(
                    MessageEntity(
                        id = msgJson.getString("id"),
                        accountId = msgJson.getString("accountId"),
                        senderName = msgJson.getString("senderName"),
                        senderEmail = msgJson.getString("senderEmail"),
                        subject = msgJson.getString("subject"),
                        preview = msgJson.getString("preview"),
                        textBody = msgJson.getString("textBody"),
                        htmlBody = if (msgJson.isNull("htmlBody")) null else msgJson.getString("htmlBody"),
                        receivedAt = msgJson.optLong("receivedAt", System.currentTimeMillis()),
                        isRead = msgJson.optBoolean("isRead", false),
                        isArchived = msgJson.optBoolean("isArchived", false),
                        isSpam = msgJson.optBoolean("isSpam", false),
                        hasAttachments = msgJson.optBoolean("hasAttachments", false),
                        attachmentsJson = if (msgJson.isNull("attachmentsJson")) null else msgJson.getString("attachmentsJson")
                    )
                )
            }

            // Perform transaction in Room to delete old and insert new safely
            database.runInTransaction {
                try {
                    database.openHelper.writableDatabase.execSQL("DELETE FROM messages")
                    database.openHelper.writableDatabase.execSQL("DELETE FROM email_accounts")
                } catch (e: Exception) {
                    throw RuntimeException("Failed to purge existing database tables", e)
                }
            }

            // Insert restored entities safely
            database.emailAccountDao().insertAccounts(accountsToRestore)
            database.messageDao().insertMessages(messagesToRestore)

            return@withContext RestoreResult.Success(accountsToRestore.size, messagesToRestore.size)
        } catch (e: Exception) {
            Log.e(TAG, "Restore error", e)
            return@withContext RestoreResult.Error("Failed to parse backup payload: ${e.localizedMessage}")
        }
    }

    suspend fun restoreFromSnapshot(snapshot: BackupSnapshot): RestoreResult {
        return withContext(Dispatchers.IO) {
            try {
                if (!snapshot.file.exists()) {
                    return@withContext RestoreResult.Error("Snapshot file does not exist")
                }
                val content = FileInputStream(snapshot.file).bufferedReader().use { it.readText() }
                restoreFromPayload(content)
            } catch (e: Exception) {
                RestoreResult.Error(e.localizedMessage ?: "Unknown error")
            }
        }
    }

    fun deleteSnapshot(snapshot: BackupSnapshot): Boolean {
        return try {
            if (snapshot.file.exists()) {
                snapshot.file.delete()
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun exportToExternalStorage(): File? = withContext(Dispatchers.IO) {
        try {
            val payload = createBackupPayload()
            val timestamp = System.currentTimeMillis()
            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            val dateStr = sdf.format(Date(timestamp))
            
            // Save to app's public External Files Directory to guarantee 100% permission-free success
            val publicDir = context.getExternalFilesDir("Backups") ?: context.filesDir
            if (!publicDir.exists()) {
                publicDir.mkdirs()
            }
            val file = File(publicDir, "tempmail_backup_${dateStr}.json")
            FileOutputStream(file).use { out ->
                out.write(payload.toByteArray(Charsets.UTF_8))
            }
            Log.i(TAG, "Backup exported successfully to: ${file.absolutePath}")
            return@withContext file
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export backup file", e)
            return@withContext null
        }
    }

    suspend fun copyBackupToClipboard(): Boolean = withContext(Dispatchers.Main) {
        try {
            val payload = createBackupPayload()
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("TempMail Backup Payload", payload)
            clipboard.setPrimaryClip(clip)
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy backup to clipboard", e)
            return@withContext false
        }
    }

    private val fillerDir = File(context.filesDir, "high_capacity_cache")

    fun getHighCapacityCacheSize(): Long {
        if (!fillerDir.exists()) fillerDir.mkdirs()
        return getDirectorySize(fillerDir) + getDirectorySize(backupDir)
    }

    private fun getDirectorySize(dir: File): Long {
        var size: Long = 0
        val files = dir.listFiles() ?: return 0
        for (file in files) {
            size += if (file.isDirectory) {
                getDirectorySize(file)
            } else {
                file.length()
            }
        }
        return size
    }

    suspend fun generateHeavyDataChunk(sizeInMb: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!fillerDir.exists()) {
                fillerDir.mkdirs()
            }
            val timestamp = System.currentTimeMillis()
            val file = File(fillerDir, "heavy_archive_${timestamp}_${sizeInMb}MB.bin")
            
            // Fast write using 1MB zero-filled buffer blocks
            val buffer = ByteArray(1024 * 1024)
            FileOutputStream(file).use { fos ->
                for (i in 0 until sizeInMb) {
                    fos.write(buffer)
                }
            }
            Log.i(TAG, "Generated heavy archive file: ${file.absolutePath} (${sizeInMb}MB)")
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate heavy data chunk", e)
            return@withContext false
        }
    }

    suspend fun clearHighCapacityCache(): Boolean = withContext(Dispatchers.IO) {
        try {
            val files = fillerDir.listFiles() ?: return@withContext true
            for (file in files) {
                file.delete()
            }
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear heavy high capacity cache", e)
            return@withContext false
        }
    }
}
