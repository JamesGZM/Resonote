package com.resonote.app

import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

@Composable
internal fun SyncSystemBars(navigationBarColor: Color) {
    val view = LocalView.current
    val activity = LocalContext.current.findComponentActivity()
    val statusBarIsDark = MaterialTheme.colorScheme.background.luminance() < DarkSurfaceLuminanceThreshold
    val navigationBarIsDark = navigationBarColor.luminance() < DarkSurfaceLuminanceThreshold
    if (!view.isInEditMode && activity != null) {
        SideEffect {
            activity.enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.auto(
                    lightScrim = android.graphics.Color.TRANSPARENT,
                    darkScrim = android.graphics.Color.TRANSPARENT,
                ) { statusBarIsDark },
                navigationBarStyle = navigationBarColor.toSystemBarStyle(navigationBarIsDark),
            )
            activity.applyNavigationBarColor(navigationBarColor)
        }
    }
}

@Suppress("DEPRECATION")
private fun ComponentActivity.applyNavigationBarColor(color: Color) {
    window.navigationBarColor = color.toArgb()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        window.isNavigationBarContrastEnforced = false
    }
}

private fun Color.toSystemBarStyle(isDark: Boolean): SystemBarStyle =
    if (isDark) {
        SystemBarStyle.dark(toArgb())
    } else {
        SystemBarStyle.light(scrim = toArgb(), darkScrim = toArgb())
    }

private const val DarkSurfaceLuminanceThreshold = 0.5f

private tailrec fun Context.findComponentActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findComponentActivity()
    else -> null
}
