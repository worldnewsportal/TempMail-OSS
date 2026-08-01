package com.example.data.di

import android.content.Context
import com.example.data.backup.LocalBackupManager
import com.example.data.db.AppDatabase
import com.example.data.network.RetrofitClient
import com.example.data.preferences.AppSettingsRepository
import com.example.data.provider.GuerrillaMailProvider
import com.example.data.provider.MailTmProvider
import com.example.data.provider.OneSecMailProvider
import com.example.data.provider.ProviderManager
import com.example.data.repository.EmailRepository

class AppContainer(private val context: Context) {
    val database: AppDatabase by lazy {
        AppDatabase.getInstance(context)
    }

    val backupManager: LocalBackupManager by lazy {
        LocalBackupManager(context, database)
    }

    val mailTmProvider: MailTmProvider by lazy {
        MailTmProvider(RetrofitClient.mailTmApi)
    }

    val guerrillaMailProvider: GuerrillaMailProvider by lazy {
        GuerrillaMailProvider()
    }

    val oneSecMailProvider: OneSecMailProvider by lazy {
        OneSecMailProvider()
    }

    val providerManager: ProviderManager by lazy {
        ProviderManager(mailTmProvider, guerrillaMailProvider, oneSecMailProvider)
    }

    val emailRepository: EmailRepository by lazy {
        EmailRepository(database, providerManager, context)
    }

    val settingsRepository: AppSettingsRepository by lazy {
        AppSettingsRepository(context)
    }
}
