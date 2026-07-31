package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tempmail_settings")

data class AppSettings(
    val language: String = "en",
    val themeMode: String = "system",
    val autoRefreshIntervalSec: Int = 30,
    val notificationsEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val deleteExpiredAuto: Boolean = true,
    val downloadFolder: String = "Downloads/TempMailOSS",
    val adsTestMode: Boolean = false,
    val adFreeUntil: Long = 0L,
    val premiumDomainsUnlockedUntil: Long = 0L
)

class AppSettingsRepository(private val context: Context) {

    companion object {
        private val KEY_LANGUAGE = stringPreferencesKey("language")
        private val KEY_THEME = stringPreferencesKey("theme_mode")
        private val KEY_AUTO_REFRESH = intPreferencesKey("auto_refresh_interval_sec")
        private val KEY_NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
        private val KEY_SOUND = booleanPreferencesKey("sound_enabled")
        private val KEY_VIBRATION = booleanPreferencesKey("vibration_enabled")
        private val KEY_DELETE_EXPIRED = booleanPreferencesKey("delete_expired_auto")
        private val KEY_DOWNLOAD_FOLDER = stringPreferencesKey("download_folder")
        private val KEY_ADS_TEST_MODE = booleanPreferencesKey("ads_test_mode")
        private val KEY_AD_FREE_UNTIL = androidx.datastore.preferences.core.longPreferencesKey("ad_free_until")
        private val KEY_PREMIUM_DOMAINS_UNTIL = androidx.datastore.preferences.core.longPreferencesKey("premium_domains_until")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            language = prefs[KEY_LANGUAGE] ?: "en",
            themeMode = prefs[KEY_THEME] ?: "system",
            autoRefreshIntervalSec = prefs[KEY_AUTO_REFRESH] ?: 30,
            notificationsEnabled = prefs[KEY_NOTIFICATIONS] ?: true,
            soundEnabled = prefs[KEY_SOUND] ?: true,
            vibrationEnabled = prefs[KEY_VIBRATION] ?: true,
            deleteExpiredAuto = prefs[KEY_DELETE_EXPIRED] ?: true,
            downloadFolder = prefs[KEY_DOWNLOAD_FOLDER] ?: "Downloads/TempMailOSS",
            adsTestMode = prefs[KEY_ADS_TEST_MODE] ?: false,
            adFreeUntil = prefs[KEY_AD_FREE_UNTIL] ?: 0L,
            premiumDomainsUnlockedUntil = prefs[KEY_PREMIUM_DOMAINS_UNTIL] ?: 0L
        )
    }

    suspend fun updateLanguage(language: String) {
        context.dataStore.edit { it[KEY_LANGUAGE] = language }
    }

    suspend fun updateThemeMode(themeMode: String) {
        context.dataStore.edit { it[KEY_THEME] = themeMode }
    }

    suspend fun updateAutoRefreshInterval(seconds: Int) {
        context.dataStore.edit { it[KEY_AUTO_REFRESH] = seconds }
    }

    suspend fun updateNotifications(enabled: Boolean) {
        context.dataStore.edit { it[KEY_NOTIFICATIONS] = enabled }
    }

    suspend fun updateSound(enabled: Boolean) {
        context.dataStore.edit { it[KEY_SOUND] = enabled }
    }

    suspend fun updateVibration(enabled: Boolean) {
        context.dataStore.edit { it[KEY_VIBRATION] = enabled }
    }

    suspend fun updateDeleteExpiredAuto(autoDelete: Boolean) {
        context.dataStore.edit { it[KEY_DELETE_EXPIRED] = autoDelete }
    }

    suspend fun updateAdsTestMode(testMode: Boolean) {
        context.dataStore.edit { it[KEY_ADS_TEST_MODE] = testMode }
    }

    suspend fun updateAdFreeUntil(timestamp: Long) {
        context.dataStore.edit { it[KEY_AD_FREE_UNTIL] = timestamp }
    }

    suspend fun updatePremiumDomainsUnlockedUntil(timestamp: Long) {
        context.dataStore.edit { it[KEY_PREMIUM_DOMAINS_UNTIL] = timestamp }
    }

    fun calculateCacheSizeBytes(): Long {
        var size = 0L
        try {
            val cacheDir = context.cacheDir
            if (cacheDir.exists()) {
                cacheDir.walkTopDown().forEach { file ->
                    if (file.isFile) size += file.length()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return size
    }

    fun clearCache(): Boolean {
        return try {
            val cacheDir = context.cacheDir
            if (cacheDir.exists()) {
                cacheDir.deleteRecursively()
                cacheDir.mkdirs()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
