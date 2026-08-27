package com.resonote.core.playback.service

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import coil3.ImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import com.resonote.core.model.ThemeMode
import com.resonote.core.model.ThemePreferences
import com.resonote.core.playback.PlaybackMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class DesktopLyricsPalette(
    val surfaceArgb: Int,
    val accentArgb: Int,
    val onSurfaceArgb: Int,
    val onAccentArgb: Int,
    val transparentContentArgb: Int,
    val transparentAccentArgb: Int,
) {
    val outlineArgb: Int
        get() = if (onSurfaceArgb == Color.WHITE) Color.BLACK else Color.WHITE

    companion object {
        fun fromColors(
            surfaceArgb: Int,
            accentArgb: Int,
            transparentContentArgb: Int = readableForeground(surfaceArgb),
            transparentAccentArgb: Int = accentArgb,
        ) = DesktopLyricsPalette(
            surfaceArgb = surfaceArgb,
            accentArgb = accentArgb,
            onSurfaceArgb = readableForeground(surfaceArgb),
            onAccentArgb = readableForeground(accentArgb),
            transparentContentArgb = transparentContentArgb,
            transparentAccentArgb = transparentAccentArgb,
        )
    }
}

internal fun interpolateDesktopLyricsPalette(
    from: DesktopLyricsPalette,
    to: DesktopLyricsPalette,
    fraction: Float,
): DesktopLyricsPalette {
    val amount = fraction.coerceIn(0f, 1f)
    return DesktopLyricsPalette(
        surfaceArgb = ColorUtils.blendARGB(from.surfaceArgb, to.surfaceArgb, amount),
        accentArgb = ColorUtils.blendARGB(from.accentArgb, to.accentArgb, amount),
        onSurfaceArgb = ColorUtils.blendARGB(from.onSurfaceArgb, to.onSurfaceArgb, amount),
        onAccentArgb = ColorUtils.blendARGB(from.onAccentArgb, to.onAccentArgb, amount),
        transparentContentArgb = ColorUtils.blendARGB(
            from.transparentContentArgb,
            to.transparentContentArgb,
            amount,
        ),
        transparentAccentArgb = ColorUtils.blendARGB(
            from.transparentAccentArgb,
            to.transparentAccentArgb,
            amount,
        ),
    )
}

internal class DesktopLyricsPaletteLoader(private val context: Context, private val imageLoader: ImageLoader) {
    suspend fun load(metadata: PlaybackMetadata?, theme: ThemePreferences): DesktopLyricsPalette {
        val fallback = themePalette(theme)
        val uri = metadata?.artworkUri?.takeIf(String::isNotBlank) ?: return fallback
        val result = imageLoader.execute(
            ImageRequest.Builder(context)
                .data(uri)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .networkCachePolicy(CachePolicy.DISABLED)
                .build(),
        ) as? SuccessResult ?: return fallback
        return withContext(Dispatchers.Default) {
            val bitmap = result.image.toBitmap().paletteCompatibleBitmap() ?: return@withContext fallback
            val palette = runCatching { Palette.from(bitmap).maximumColorCount(16).generate() }.getOrNull()
                ?: return@withContext fallback
            val extractedBackground = palette.darkMutedSwatch?.rgb
                ?: palette.darkVibrantSwatch?.rgb
                ?: palette.dominantSwatch?.rgb
                ?: return@withContext fallback
            val surface = ColorUtils.blendARGB(extractedBackground, Color.BLACK, 0.56f)
            val extractedAccent = palette.vibrantSwatch?.rgb
                ?: palette.lightVibrantSwatch?.rgb
                ?: ColorUtils.blendARGB(extractedBackground, Color.WHITE, 0.36f)
            val accent = ensureContrast(extractedAccent, surface, readableForeground(surface))
            val transparentAccent = ensureContrast(
                extractedAccent,
                fallback.surfaceArgb,
                fallback.transparentContentArgb,
            )
            DesktopLyricsPalette.fromColors(
                surfaceArgb = surface,
                accentArgb = accent,
                transparentContentArgb = fallback.transparentContentArgb,
                transparentAccentArgb = transparentAccent,
            )
        }
    }

    private fun themePalette(preferences: ThemePreferences): DesktopLyricsPalette {
        val isDark = when (preferences.themeMode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK, ThemeMode.AMOLED -> true
            ThemeMode.SYSTEM ->
                context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
                    Configuration.UI_MODE_NIGHT_YES
        }
        val surface = when {
            preferences.themeMode == ThemeMode.AMOLED -> 0xFF181212.toInt()
            isDark -> 0xFF241E1F.toInt()
            else -> 0xFFF8EBEB.toInt()
        }
        val accent = if (preferences.dynamicColorEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getColor(android.R.color.system_accent1_500)
        } else if (isDark) {
            0xFFFFB2BC.toInt()
        } else {
            0xFFAE2A4B.toInt()
        }
        return DesktopLyricsPalette.fromColors(
            surfaceArgb = surface,
            accentArgb = accent,
            transparentContentArgb = readableForeground(surface),
            transparentAccentArgb = ensureContrast(accent, surface, readableForeground(surface)),
        )
    }
}

private fun readableForeground(background: Int): Int = if (
    ColorUtils.calculateContrast(Color.WHITE, background) >= 4.5
) {
    Color.WHITE
} else {
    Color.BLACK
}

private fun ensureContrast(foreground: Int, background: Int, readable: Int): Int {
    var result = foreground
    repeat(6) {
        if (ColorUtils.calculateContrast(result, background) >= 3.0) return result
        result = ColorUtils.blendARGB(result, readable, 0.24f)
    }
    return readable
}

private fun Bitmap.paletteCompatibleBitmap(): Bitmap? = runCatching {
    if (config == Bitmap.Config.HARDWARE) copy(Bitmap.Config.ARGB_8888, false) else this
}.getOrNull()
