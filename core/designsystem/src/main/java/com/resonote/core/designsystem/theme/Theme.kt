package com.resonote.core.designsystem.theme

import android.animation.ValueAnimator
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import com.resonote.core.designsystem.tokens.LocalResonoteArtwork
import com.resonote.core.designsystem.tokens.LocalResonoteArtworkShapes
import com.resonote.core.designsystem.tokens.LocalResonoteBorders
import com.resonote.core.designsystem.tokens.LocalResonoteElevation
import com.resonote.core.designsystem.tokens.LocalResonoteExtendedShapes
import com.resonote.core.designsystem.tokens.LocalResonoteIcons
import com.resonote.core.designsystem.tokens.LocalResonoteLayout
import com.resonote.core.designsystem.tokens.LocalResonoteMotion
import com.resonote.core.designsystem.tokens.LocalResonoteSpacing
import com.resonote.core.designsystem.tokens.LocalResonoteStateLayers
import com.resonote.core.designsystem.tokens.LocalResonoteSystemColors
import com.resonote.core.designsystem.tokens.LocalResonoteTouchTargets
import com.resonote.core.designsystem.tokens.ResonoteArtworkShapes
import com.resonote.core.designsystem.tokens.ResonoteArtworkTokens
import com.resonote.core.designsystem.tokens.ResonoteBorders
import com.resonote.core.designsystem.tokens.ResonoteElevation
import com.resonote.core.designsystem.tokens.ResonoteExtendedShapes
import com.resonote.core.designsystem.tokens.ResonoteIconTokens
import com.resonote.core.designsystem.tokens.ResonoteLayoutTokens
import com.resonote.core.designsystem.tokens.ResonoteMotionScheme
import com.resonote.core.designsystem.tokens.ResonoteSpacing
import com.resonote.core.designsystem.tokens.ResonoteStateLayers
import com.resonote.core.designsystem.tokens.ResonoteSystemColors
import com.resonote.core.designsystem.tokens.ResonoteTouchTargets
import com.resonote.core.model.ThemeMode

typealias ResonoteThemeMode = ThemeMode

@Composable
fun ResonoteTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColorEnabled: Boolean = false,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val context = LocalContext.current
    val motionScheme = if (ValueAnimator.areAnimatorsEnabled()) {
        ResonoteMotionScheme.Standard
    } else {
        ResonoteMotionScheme.Reduced
    }
    val useDynamicColor = dynamicColorEnabled &&
        themeMode != ThemeMode.AMOLED &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val useDarkColors = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.AMOLED -> true
    }
    val colorScheme = when {
        useDynamicColor && useDarkColors -> dynamicDarkColorScheme(context)
        useDynamicColor -> dynamicLightColorScheme(context)
        themeMode == ThemeMode.AMOLED -> ResonoteAmoledColorScheme
        useDarkColors -> ResonoteDarkColorScheme
        else -> ResonoteLightColorScheme
    }
    CompositionLocalProvider(
        LocalResonoteSpacing provides ResonoteSpacing(),
        LocalResonoteBorders provides ResonoteBorders(),
        LocalResonoteTouchTargets provides ResonoteTouchTargets(),
        LocalResonoteElevation provides ResonoteElevation(),
        LocalResonoteLayout provides ResonoteLayoutTokens(),
        LocalResonoteIcons provides ResonoteIconTokens(),
        LocalResonoteArtwork provides ResonoteArtworkTokens(),
        LocalResonoteArtworkShapes provides ResonoteArtworkShapes(),
        LocalResonoteExtendedShapes provides ResonoteExtendedShapes(),
        LocalResonoteMotion provides motionScheme,
        LocalResonoteStateLayers provides ResonoteStateLayers(),
        LocalResonoteSystemColors provides ResonoteSystemColors(),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ResonoteTypography,
            shapes = ResonoteShapes,
            content = content,
        )
    }
}
