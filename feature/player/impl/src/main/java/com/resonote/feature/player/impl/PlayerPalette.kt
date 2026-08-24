package com.resonote.feature.player.impl

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.palette.graphics.Palette
import coil3.ImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import com.resonote.core.designsystem.tokens.ResonoteTokens
import com.resonote.core.playback.PlaybackController
import com.resonote.core.playback.PlaybackMetadata
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Immutable
data class PlayerPaletteSeed(val mediaId: String, val artworkUri: String, val backgroundArgb: Int, val accentArgb: Int)

@Immutable
data class PlayerPalette(
    val background: Color,
    val backgroundElevated: Color,
    val accent: Color,
    val contentPrimary: Color,
    val contentSecondary: Color,
    val contentMuted: Color,
    val contentOnAccent: Color,
) {
    companion object {
        fun fromSeed(seed: PlayerPaletteSeed): PlayerPalette {
            val background = Color(seed.backgroundArgb)
            val primaryArgb = if (ColorUtils.calculateContrast(android.graphics.Color.WHITE, seed.backgroundArgb) >=
                4.5
            ) {
                android.graphics.Color.WHITE
            } else {
                android.graphics.Color.BLACK
            }
            val onAccentArgb = if (ColorUtils.calculateContrast(android.graphics.Color.BLACK, seed.accentArgb) >= 4.5) {
                android.graphics.Color.BLACK
            } else {
                android.graphics.Color.WHITE
            }
            val primary = Color(primaryArgb)
            return PlayerPalette(
                background = background,
                backgroundElevated = Color(ColorUtils.blendARGB(seed.backgroundArgb, primaryArgb, 0.08f)),
                accent = Color(seed.accentArgb),
                contentPrimary = primary,
                contentSecondary = primary.copy(alpha = 0.72f),
                contentMuted = primary.copy(alpha = 0.42f),
                contentOnAccent = Color(onAccentArgb),
            )
        }
    }
}

@Composable
fun defaultPlayerPalette(): PlayerPalette {
    val colors = androidx.compose.material3.MaterialTheme.colorScheme
    return PlayerPalette(
        background = colors.background,
        backgroundElevated = colors.surfaceContainer,
        accent = colors.primary,
        contentPrimary = colors.onBackground,
        contentSecondary = colors.onSurfaceVariant,
        contentMuted = colors.onSurfaceVariant.copy(alpha = 0.55f),
        contentOnAccent = colors.onPrimary,
    )
}

@Composable
internal fun animatePlayerPalette(target: PlayerPalette): PlayerPalette {
    val motion = ResonoteTokens.motion.effectsSlow<Color>()
    val background by animateColorAsState(target.background, motion, label = "player background")
    val backgroundElevated by animateColorAsState(
        target.backgroundElevated,
        motion,
        label = "player elevated background",
    )
    val accent by animateColorAsState(target.accent, motion, label = "player accent")
    val contentPrimary by animateColorAsState(target.contentPrimary, motion, label = "player primary content")
    val contentSecondary by animateColorAsState(target.contentSecondary, motion, label = "player secondary content")
    val contentMuted by animateColorAsState(target.contentMuted, motion, label = "player muted content")
    val contentOnAccent by animateColorAsState(target.contentOnAccent, motion, label = "player accent content")
    return PlayerPalette(
        background,
        backgroundElevated,
        accent,
        contentPrimary,
        contentSecondary,
        contentMuted,
        contentOnAccent,
    )
}

@Singleton
class PlayerPaletteCoordinator @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val imageLoader: ImageLoader,
) {
    private val cache = ConcurrentHashMap<String, PlayerPaletteSeed>()

    suspend fun prepare(metadata: PlaybackMetadata): PlayerPaletteSeed? {
        val uri = metadata.artworkUri?.takeIf(String::isNotBlank) ?: return null
        val key = "${metadata.mediaId}|$uri"
        cache[key]?.let { return it }
        val result = imageLoader.execute(
            ImageRequest.Builder(context)
                .data(uri)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .networkCachePolicy(CachePolicy.DISABLED)
                .build(),
        ) as? SuccessResult ?: return null
        return withContext(Dispatchers.Default) {
            val bitmap = result.image.toBitmap().paletteCompatibleBitmap() ?: return@withContext null
            val palette = runCatching {
                Palette.from(bitmap).maximumColorCount(16).generate()
            }.getOrNull() ?: return@withContext null
            val background = palette.darkMutedSwatch?.rgb
                ?: palette.darkVibrantSwatch?.rgb
                ?: palette.dominantSwatch?.rgb
                ?: return@withContext null
            val safeBackground = ColorUtils.blendARGB(background, android.graphics.Color.BLACK, 0.56f)
            val accent = palette.vibrantSwatch?.rgb
                ?: palette.lightVibrantSwatch?.rgb
                ?: ColorUtils.blendARGB(background, android.graphics.Color.WHITE, 0.36f)
            PlayerPaletteSeed(metadata.mediaId, uri, safeBackground, accent).also { cache[key] = it }
        }
    }
}

internal fun Bitmap.paletteCompatibleBitmap(): Bitmap? = runCatching {
    if (config == Bitmap.Config.HARDWARE) copy(Bitmap.Config.ARGB_8888, false) else this
}.getOrNull()

@HiltViewModel
class PlayerPaletteViewModel @Inject constructor(
    playbackController: PlaybackController,
    private val coordinator: PlayerPaletteCoordinator,
) : ViewModel() {
    private val mutablePrepared = MutableStateFlow<PlayerPaletteSeed?>(null)
    val prepared: StateFlow<PlayerPaletteSeed?> = mutablePrepared.asStateFlow()

    init {
        viewModelScope.launch {
            playbackController.state.map { it.currentMetadata }.distinctUntilChanged().collectLatest { metadata ->
                mutablePrepared.value = null
                if (metadata != null) {
                    repeat(CACHE_RETRY_COUNT) { attempt ->
                        coordinator.prepare(metadata)?.let {
                            mutablePrepared.value = it
                            return@collectLatest
                        }
                        if (attempt < CACHE_RETRY_COUNT - 1) delay(CACHE_RETRY_DELAY_MILLIS)
                    }
                }
            }
        }
    }

    private companion object {
        const val CACHE_RETRY_COUNT = 4
        const val CACHE_RETRY_DELAY_MILLIS = 250L
    }
}
