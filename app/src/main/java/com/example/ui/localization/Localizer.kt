package com.example.ui.localization

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import java.util.Locale

@Composable
fun LocalizedApp(
    languageCode: String,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val locale = Locale(languageCode)
    Locale.setDefault(locale)

    val config = Configuration(LocalConfiguration.current).apply {
        setLocale(locale)
        setLayoutDirection(locale)
    }

    val localizedContext = context.createConfigurationContext(config)
    val layoutDirection = if (languageCode == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides config,
        LocalLayoutDirection provides layoutDirection
    ) {
        content()
    }
}
