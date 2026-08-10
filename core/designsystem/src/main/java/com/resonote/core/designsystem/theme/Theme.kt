package com.resonote.core.designsystem.theme

import android.animation.ValueAnimator
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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

enum class ResonoteThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    AMOLED,
}

@Composable
fun ResonoteTheme(
    themeMode: ResonoteThemeMode = ResonoteThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val motionScheme = if (ValueAnimator.areAnimatorsEnabled()) {
        ResonoteMotionScheme.Standard
    } else {
        ResonoteMotionScheme.Reduced
    }
    val colorScheme = when (themeMode) {
        ResonoteThemeMode.SYSTEM -> if (systemDark) ResonoteDarkColorScheme else ResonoteLightColorScheme
        ResonoteThemeMode.LIGHT -> ResonoteLightColorScheme
        ResonoteThemeMode.DARK -> ResonoteDarkColorScheme
        ResonoteThemeMode.AMOLED -> ResonoteAmoledColorScheme
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
