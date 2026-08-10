package com.resonote.catalog

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

@Composable
internal fun SyncSystemBars(darkTheme: Boolean) {
    val view = LocalView.current
    val activity = LocalContext.current.findComponentActivity()
    if (!view.isInEditMode && activity != null) {
        SideEffect {
            activity.enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.auto(
                    lightScrim = Color.TRANSPARENT,
                    darkScrim = Color.TRANSPARENT,
                ) { darkTheme },
                navigationBarStyle = SystemBarStyle.auto(
                    lightScrim = LightNavigationBarScrim,
                    darkScrim = DarkNavigationBarScrim,
                ) { darkTheme },
            )
        }
    }
}

private val LightNavigationBarScrim = Color.argb(0xE6, 0xFF, 0xFF, 0xFF)
private val DarkNavigationBarScrim = Color.argb(0x80, 0x1B, 0x1B, 0x1B)

private tailrec fun Context.findComponentActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findComponentActivity()
    else -> null
}
