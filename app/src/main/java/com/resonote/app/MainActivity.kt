package com.resonote.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resonote.core.designsystem.theme.ResonoteTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) {
            viewModel.handleExternalImportIntent(intent, finishTaskOnBack = true)
        }
        setContent {
            val themePreferences by viewModel.themePreferences.collectAsStateWithLifecycle()
            ResonoteTheme(
                themeMode = themePreferences.themeMode,
                dynamicColorEnabled = themePreferences.dynamicColorEnabled,
            ) {
                ResonoteApp(viewModel = viewModel, onFinishExternalTask = ::finish)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        viewModel.handleExternalImportIntent(intent, finishTaskOnBack = false)
    }
}
