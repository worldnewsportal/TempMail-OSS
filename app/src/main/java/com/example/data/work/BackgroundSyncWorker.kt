package com.example.data.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.db.AppDatabase
import com.example.data.network.RetrofitClient
import com.example.data.preferences.AppSettingsRepository
import com.example.data.provider.MailTmProvider
import com.example.data.provider.ProviderManager
import com.example.data.repository.EmailRepository
import com.example.ui.notification.NotificationHelper
import kotlinx.coroutines.flow.firstOrNull

class BackgroundSyncWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.i("BackgroundSyncWorker", "Background execution started.")
        val database = AppDatabase.getInstance(context)
        val mailTmProvider = MailTmProvider(RetrofitClient.mailTmApi)
        val providerManager = ProviderManager(mailTmProvider)
        val repo = EmailRepository(database, providerManager, context)
        val settingsRepo = AppSettingsRepository(context)

        val settings = settingsRepo.settingsFlow.firstOrNull() ?: com.example.data.preferences.AppSettings()

        // 1. Clean expired accounts automatically if configured
        if (settings.deleteExpiredAuto) {
            repo.autoCleanExpired()
        }

        // 2. Sync active email inbox
        val activeAccount = repo.activeAccount.firstOrNull()
        if (activeAccount != null && !activeAccount.isExpired) {
            // Read unread count before sync
            val messagesBefore = database.messageDao().getMessagesForAccount(activeAccount.id).firstOrNull() ?: emptyList()
            val beforeIds = messagesBefore.map { it.id }.toSet()

            val syncResult = repo.syncInbox(activeAccount.id)
            if (syncResult.isSuccess) {
                // Read messages after sync
                val messagesAfter = database.messageDao().getMessagesForAccount(activeAccount.id).firstOrNull() ?: emptyList()
                val newMessages = messagesAfter.filter { it.id !in beforeIds && !it.isRead }

                // 3. Notify user of any new messages
                if (newMessages.isNotEmpty() && settings.notificationsEnabled) {
                    for (msg in newMessages) {
                        NotificationHelper.showNewEmailNotification(
                            context = context,
                            message = msg,
                            emailAddress = activeAccount.address,
                            soundEnabled = settings.soundEnabled,
                            vibrationEnabled = settings.vibrationEnabled
                        )
                    }
                }
            }
        }

        return Result.success()
    }
}
