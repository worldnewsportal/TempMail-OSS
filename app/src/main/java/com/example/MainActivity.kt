package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ads.UnityAdsManager
import com.example.data.di.AppContainer
import com.example.data.preferences.AppSettings
import com.example.ui.localization.LocalizedApp
import com.example.ui.notification.NotificationHelper
import com.example.ui.screens.MainAppContainerScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val appContainer by lazy { AppContainer(applicationContext) }

    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory(application, appContainer)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Unity Ads
        UnityAdsManager.initialize(this)

        // Pre-create WebView cache directories to prevent Chromium opendir errors on startup
        try {
            val webViewCacheDir = java.io.File(cacheDir, "WebView/Default/HTTP Cache/Code Cache")
            java.io.File(webViewCacheDir, "js").mkdirs()
            java.io.File(webViewCacheDir, "wasm").mkdirs()
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to pre-create WebView cache directories", e)
        }

        // Create the notification channels
        NotificationHelper.createNotificationChannel(applicationContext)

        setContent {
            val settings by viewModel.appSettings.collectAsState(initial = AppSettings())

            LocalizedApp(languageCode = settings.language) {
                MyApplicationTheme(themeMode = settings.themeMode) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        MainAppContainerScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
