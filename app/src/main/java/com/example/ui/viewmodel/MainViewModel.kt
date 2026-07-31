package com.example.ui.viewmodel

import android.app.Application
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.backup.BackupSnapshot
import com.example.data.backup.RestoreResult
import com.example.data.di.AppContainer
import com.example.data.db.EmailAccountEntity
import com.example.data.db.MessageEntity
import com.example.data.preferences.AppSettings
import com.example.data.work.BackgroundSyncWorker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

sealed interface UiState {
    object Idle : UiState
    object Loading : UiState
    data class Success(val message: String) : UiState
    data class Error(val errorMessage: String) : UiState
}

class MainViewModel(
    private val application: Application,
    private val container: AppContainer
) : AndroidViewModel(application) {

    private val repo = container.emailRepository
    private val settingsRepo = container.settingsRepository
    val backupManager = container.backupManager

    private val _localSnapshots = MutableStateFlow<List<BackupSnapshot>>(emptyList())
    val localSnapshots: StateFlow<List<BackupSnapshot>> = _localSnapshots.asStateFlow()

    private val _highCapacityCacheSize = MutableStateFlow<Long>(0L)
    val highCapacityCacheSize: StateFlow<Long> = _highCapacityCacheSize.asStateFlow()

    // Settings
    val appSettings = settingsRepo.settingsFlow

    // Accounts
    val activeAccount = repo.activeAccount
    val allAccounts = repo.allAccounts
    val favoriteAccounts = repo.favoriteAccounts

    // Messages
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val messages = activeAccount.flatMapLatest { account ->
        if (account != null) {
            _searchQuery.flatMapLatest { query ->
                repo.searchMessages(account.id, query)
            }
        } else {
            flowOf(emptyList())
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val unreadCount = activeAccount.flatMapLatest { account ->
        if (account != null) {
            repo.getUnreadCount(account.id)
        } else {
            flowOf(0)
        }
    }

    // Dynamic state
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _availableDomains = MutableStateFlow<List<String>>(emptyList())
    val availableDomains: StateFlow<List<String>> = _availableDomains.asStateFlow()

    private val _selectedMessage = MutableStateFlow<MessageEntity?>(null)
    val selectedMessage: StateFlow<MessageEntity?> = _selectedMessage.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    // Auto-refresh loops
    private var refreshJob: Job? = null

    init {
        // Fetch domains on launch
        fetchAvailableDomains()
        // Auto clean expired accounts
        viewModelScope.launch {
            repo.autoCleanExpired()
        }
        // Load initial local snapshots
        loadLocalSnapshots()
        refreshHighCapacityCacheSize()
        // Start refresh cycle according to settings
        appSettings.onEach { settings ->
            setupAutoRefresh(settings.autoRefreshIntervalSec)
            setupBackgroundWork(settings.autoRefreshIntervalSec)
        }.launchIn(viewModelScope)
    }

    fun showToast(msg: String) {
        viewModelScope.launch {
            _toastMessage.emit(msg)
        }
    }

    fun loadLocalSnapshots() {
        viewModelScope.launch {
            _localSnapshots.value = backupManager.getLocalSnapshots()
            refreshHighCapacityCacheSize()
        }
    }

    fun refreshHighCapacityCacheSize() {
        viewModelScope.launch {
            _highCapacityCacheSize.value = backupManager.getHighCapacityCacheSize()
        }
    }

    fun generateHeavyDataChunk(sizeInMb: Long) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val success = backupManager.generateHeavyDataChunk(sizeInMb)
            if (success) {
                refreshHighCapacityCacheSize()
                _uiState.value = UiState.Success("Generated $sizeInMb MB local file")
                showToast("Successfully generated $sizeInMb MB local file!")
            } else {
                _uiState.value = UiState.Error("Failed to generate heavy data")
                showToast("Failed to generate heavy local data")
            }
        }
    }

    fun clearHighCapacityCache() {
        viewModelScope.launch {
            val success = backupManager.clearHighCapacityCache()
            if (success) {
                refreshHighCapacityCacheSize()
                showToast("High-capacity local cache cleared completely!")
            } else {
                showToast("Failed to clear local cache")
            }
        }
    }

    fun createLocalSnapshot(customLabel: String? = null) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val file = backupManager.createLocalSnapshot(customLabel)
            if (file != null) {
                _uiState.value = UiState.Success("Snapshot created: ${file.name}")
                showToast("Local snapshot created successfully!")
                loadLocalSnapshots()
            } else {
                _uiState.value = UiState.Error("Failed to create snapshot")
                showToast("Error: Failed to create snapshot")
            }
        }
    }

    fun restoreFromSnapshot(snapshot: BackupSnapshot) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val result = backupManager.restoreFromSnapshot(snapshot)
            when (result) {
                is RestoreResult.Success -> {
                    _uiState.value = UiState.Success("Restored ${result.accountsCount} accounts")
                    showToast("Restore complete! Loaded ${result.accountsCount} accounts.")
                    selectMessage(null)
                    refreshInbox()
                }
                is RestoreResult.Error -> {
                    _uiState.value = UiState.Error(result.message)
                    showToast("Restore failed: ${result.message}")
                }
            }
        }
    }

    fun restoreFromPayload(payload: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val result = backupManager.restoreFromPayload(payload)
            when (result) {
                is RestoreResult.Success -> {
                    _uiState.value = UiState.Success("Restored ${result.accountsCount} accounts")
                    showToast("Restore complete! Loaded ${result.accountsCount} accounts.")
                    selectMessage(null)
                    refreshInbox()
                }
                is RestoreResult.Error -> {
                    _uiState.value = UiState.Error(result.message)
                    showToast("Restore failed: ${result.message}")
                }
            }
        }
    }

    fun deleteSnapshot(snapshot: BackupSnapshot) {
        viewModelScope.launch {
            val deleted = backupManager.deleteSnapshot(snapshot)
            if (deleted) {
                showToast("Snapshot deleted")
                loadLocalSnapshots()
            } else {
                showToast("Failed to delete snapshot")
            }
        }
    }

    fun copyBackupToClipboard() {
        viewModelScope.launch {
            val success = backupManager.copyBackupToClipboard()
            if (success) {
                showToast("Data backup copied to clipboard!")
            } else {
                showToast("Failed to copy backup")
            }
        }
    }

    fun exportBackupFile() {
        viewModelScope.launch {
            val file = backupManager.exportToExternalStorage()
            if (file != null) {
                showToast("Backup exported to: ${file.name}")
            } else {
                showToast("Failed to export backup file")
            }
        }
    }

    fun clearUiState() {
        _uiState.value = UiState.Idle
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private fun fetchAvailableDomains() {
        viewModelScope.launch {
            try {
                val domains = repo.getAvailableDomains()
                _availableDomains.value = domains
            } catch (e: Exception) {
                _availableDomains.value = listOf("mail.tm", "guerrillamail.com")
            }
        }
    }

    fun selectMessage(message: MessageEntity?) {
        _selectedMessage.value = message
        if (message != null && !message.isRead) {
            viewModelScope.launch {
                repo.markAsRead(message.id, true)
                // fetch details and cache
                val updated = repo.fetchMessageDetailsAndCache(message.accountId, message.id)
                if (updated != null && _selectedMessage.value?.id == message.id) {
                    _selectedMessage.value = updated
                }
            }
        }
    }

    fun generateEmail(customUsername: String? = null, domain: String? = null) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val account = repo.generateNewEmail(customUsername, domain)
                _uiState.value = UiState.Success("Created ${account.address}")
                showToast("Email address generated successfully")
                refreshInbox()
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to generate email")
            }
        }
    }

    fun switchActiveEmail(accountId: String) {
        viewModelScope.launch {
            repo.switchActiveEmail(accountId)
            selectMessage(null)
            showToast("Switched email address")
            refreshInbox()
        }
    }

    fun deleteEmail(accountId: String) {
        viewModelScope.launch {
            repo.deleteEmail(accountId)
            selectMessage(null)
            showToast("Email address removed")
        }
    }

    fun updateLabel(accountId: String, label: String) {
        viewModelScope.launch {
            repo.updateLabel(accountId, label)
            showToast("Label updated")
        }
    }

    fun toggleFavorite(accountId: String) {
        viewModelScope.launch {
            repo.toggleFavorite(accountId)
        }
    }

    fun refreshInbox() {
        viewModelScope.launch {
            val current = repo.activeAccount.firstOrNull() ?: return@launch
            _uiState.value = UiState.Loading
            val result = repo.syncInbox(current.id)
            if (result.isSuccess) {
                _uiState.value = UiState.Idle
            } else {
                _uiState.value = UiState.Error(result.exceptionOrNull()?.message ?: "Sync failed")
            }
        }
    }

    fun deleteMessage(message: MessageEntity) {
        viewModelScope.launch {
            repo.deleteMessage(message.accountId, message.id)
            if (_selectedMessage.value?.id == message.id) {
                _selectedMessage.value = null
            }
            showToast("Message deleted")
        }
    }

    fun markAllAsRead(accountId: String) {
        viewModelScope.launch {
            repo.markAllAsRead(accountId)
            showToast("Marked all as read")
        }
    }

    fun deleteAllMessages(accountId: String) {
        viewModelScope.launch {
            repo.deleteAllMessagesForAccount(accountId)
            selectMessage(null)
            showToast("All messages deleted")
        }
    }

    fun archiveMessage(messageId: String, archive: Boolean) {
        viewModelScope.launch {
            repo.archiveMessage(messageId, archive)
            showToast(if (archive) "Message archived" else "Message unarchived")
        }
    }

    // Settings adjustments
    fun setLanguage(language: String) {
        viewModelScope.launch {
            settingsRepo.updateLanguage(language)
        }
    }

    fun setThemeMode(themeMode: String) {
        viewModelScope.launch {
            settingsRepo.updateThemeMode(themeMode)
        }
    }

    fun setAutoRefreshInterval(seconds: Int) {
        viewModelScope.launch {
            settingsRepo.updateAutoRefreshInterval(seconds)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.updateNotifications(enabled)
        }
    }

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.updateSound(enabled)
        }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.updateVibration(enabled)
        }
    }

    fun setDeleteExpiredAuto(autoDelete: Boolean) {
        viewModelScope.launch {
            settingsRepo.updateDeleteExpiredAuto(autoDelete)
        }
    }

    fun setAdsTestMode(testMode: Boolean) {
        viewModelScope.launch {
            settingsRepo.updateAdsTestMode(testMode)
        }
    }

    fun getCacheSizeString(): String {
        val bytes = settingsRepo.calculateCacheSizeBytes()
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return if (mb > 1.0) {
            String.format("%.2f MB", mb)
        } else if (kb > 1.0) {
            String.format("%.2f KB", kb)
        } else {
            "$bytes Bytes"
        }
    }

    fun clearCache() {
        if (settingsRepo.clearCache()) {
            showToast("Offline cache cleared successfully")
        } else {
            showToast("Failed to clear cache")
        }
    }

    fun extendActiveEmailLifetime(durationMillis: Long = 1 * 60 * 60 * 1000L) { // default 1 hour
        viewModelScope.launch {
            val currentActive = repo.activeAccount.firstOrNull()
            if (currentActive != null) {
                repo.extendEmailExpiration(currentActive.id, durationMillis)
                showToast("تم تمديد صلاحية البريد الإلكتروني بنجاح!")
            } else {
                showToast("لا يوجد بريد نشط لتمديده!")
            }
        }
    }

    fun unlockAdFree(hours: Int = 24) {
        viewModelScope.launch {
            val durationMillis = hours * 60 * 60 * 1000L
            val now = System.currentTimeMillis()
            settingsRepo.updateAdFreeUntil(now + durationMillis)
            showToast("تم تفعيل الوضع الخالي من الإعلانات لمدة $hours ساعة!")
        }
    }

    fun unlockPremiumDomains(hours: Int = 2) {
        viewModelScope.launch {
            val durationMillis = hours * 60 * 60 * 1000L
            val now = System.currentTimeMillis()
            settingsRepo.updatePremiumDomainsUnlockedUntil(now + durationMillis)
            showToast("تم فتح النطاقات المميزة بنجاح لمدة $hours ساعتين!")
        }
    }

    fun downloadAttachment(filename: String, downloadUrl: String) {
        try {
            val downloadManager = application.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val uri = Uri.parse(downloadUrl)
            val request = DownloadManager.Request(uri).apply {
                setTitle(filename)
                setDescription("Downloading attachment from TempMail OSS")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "TempMailOSS/$filename")
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }
            downloadManager.enqueue(request)
            showToast("Downloading attachment: $filename")
        } catch (e: Exception) {
            showToast("Download failed: ${e.message}")
        }
    }

    private fun setupAutoRefresh(seconds: Int) {
        refreshJob?.cancel()
        if (seconds <= 0) return

        refreshJob = viewModelScope.launch {
            while (true) {
                delay(seconds * 1000L)
                val current = repo.activeAccount.firstOrNull()
                if (current != null && !current.isExpired) {
                    repo.syncInbox(current.id)
                }
            }
        }
    }

    private fun setupBackgroundWork(intervalSec: Int) {
        val workManager = WorkManager.getInstance(application)
        workManager.cancelAllWorkByTag("sync")

        if (intervalSec <= 0) return

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicRequest = PeriodicWorkRequestBuilder<BackgroundSyncWorker>(
            intervalSec.toLong().coerceAtLeast(15 * 60L), // WorkManager min interval is 15 minutes
            TimeUnit.SECONDS
        )
        .setConstraints(constraints)
        .addTag("sync")
        .build()

        workManager.enqueue(periodicRequest)
    }

    class Factory(
        private val application: Application,
        private val container: AppContainer
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                return MainViewModel(application, container) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
