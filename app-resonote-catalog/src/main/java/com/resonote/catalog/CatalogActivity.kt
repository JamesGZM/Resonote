package com.resonote.catalog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.resonote.core.designsystem.theme.ResonoteTheme
import com.resonote.core.designsystem.theme.ResonoteThemeMode

class CatalogActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var themeMode by rememberSaveable { mutableStateOf(ResonoteThemeMode.SYSTEM) }
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                ResonoteThemeMode.SYSTEM -> systemDark
                ResonoteThemeMode.LIGHT -> false
                ResonoteThemeMode.DARK,
                ResonoteThemeMode.AMOLED,
                -> true
            }
            SyncSystemBars(darkTheme = darkTheme)
            ResonoteTheme(themeMode = themeMode) {
                AdaptiveCatalogScreen(
                    themeMode = themeMode,
                    onThemeModeChange = { themeMode = it },
                )
            }
        }
    }
}
