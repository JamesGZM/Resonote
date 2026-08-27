package com.resonote.app

import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resonote.core.designsystem.theme.ResonoteTheme
import com.resonote.core.playback.DesktopLyricsController
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject lateinit var desktopLyricsController: DesktopLyricsController

    private val viewModel: MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            val remainingDurationMillis = remainingSplashAnimationDurationMillis(
                animationStartMillis = splashScreenView.iconAnimationStartMillis,
                animationDurationMillis = splashScreenView.iconAnimationDurationMillis,
                currentTimeMillis = System.currentTimeMillis(),
                animationsEnabled = ValueAnimator.areAnimatorsEnabled(),
            )
            if (remainingDurationMillis == 0L) {
                splashScreenView.remove()
            } else {
                splashScreenView.view.postDelayed(splashScreenView::remove, remainingDurationMillis)
            }
        }
        enableEdgeToEdge()
        if (savedInstanceState == null) {
            viewModel.handleExternalImportIntent(intent, finishTaskOnBack = true)
            viewModel.handleDesktopLyricsIntent(intent)
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
        viewModel.handleDesktopLyricsIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        desktopLyricsController.restoreIfEnabled()
    }
}

internal fun remainingSplashAnimationDurationMillis(
    animationStartMillis: Long,
    animationDurationMillis: Long,
    currentTimeMillis: Long,
    animationsEnabled: Boolean,
): Long {
    if (!animationsEnabled || animationStartMillis <= 0L || animationDurationMillis <= 0L) {
        return 0L
    }
    return (
        animationStartMillis +
            animationDurationMillis +
            SPLASH_COMPLETION_HOLD_MILLIS -
            currentTimeMillis
        ).coerceAtLeast(0L)
}

private const val SPLASH_COMPLETION_HOLD_MILLIS = 100L
