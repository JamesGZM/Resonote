package com.resonote.feature.video.impl

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.media.AudioManager
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

internal enum class VideoVerticalGesture { BRIGHTNESS, VOLUME }

@Stable
internal class VideoGestureController(
    context: Context,
    private val activity: Activity?,
    private val coroutineScope: CoroutineScope,
) {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val originalBrightness = activity?.window?.attributes?.screenBrightness
    private var dismissJob: Job? = null

    var activeGesture by mutableStateOf<VideoVerticalGesture?>(null)
        private set
    var level by mutableFloatStateOf(0f)
        private set

    fun start(offset: Offset, size: IntSize) {
        dismissJob?.cancel()
        activeGesture = if (offset.x < size.width / 2f) {
            VideoVerticalGesture.BRIGHTNESS
        } else {
            VideoVerticalGesture.VOLUME
        }
        level = when (activeGesture) {
            VideoVerticalGesture.BRIGHTNESS -> currentBrightness()
            VideoVerticalGesture.VOLUME -> currentVolume()
            null -> 0f
        }
    }

    fun drag(verticalDelta: Float, height: Int) {
        if (activeGesture == null || height <= 0) return
        level = (level - verticalDelta / height * GESTURE_RANGE_MULTIPLIER).coerceIn(0f, 1f)
        when (activeGesture) {
            VideoVerticalGesture.BRIGHTNESS -> setBrightness(level)
            VideoVerticalGesture.VOLUME -> setVolume(level)
            null -> Unit
        }
    }

    fun end() {
        dismissJob?.cancel()
        dismissJob = coroutineScope.launch {
            delay(HUD_DISMISS_DELAY_MILLIS)
            activeGesture = null
        }
    }

    fun restoreBrightness() {
        val window = activity?.window ?: return
        originalBrightness?.let { original ->
            window.attributes = window.attributes.apply { screenBrightness = original }
        }
    }

    private fun currentBrightness(): Float {
        val windowBrightness = activity?.window?.attributes?.screenBrightness ?: -1f
        if (windowBrightness >= 0f) return windowBrightness.coerceIn(0f, 1f)
        val contentResolver = activity?.contentResolver ?: return DEFAULT_SYSTEM_BRIGHTNESS / MAX_SYSTEM_BRIGHTNESS
        val systemBrightness = runCatching {
            Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        }.getOrDefault(DEFAULT_SYSTEM_BRIGHTNESS)
        return (systemBrightness / MAX_SYSTEM_BRIGHTNESS).coerceIn(0f, 1f)
    }

    private fun setBrightness(value: Float) {
        val window = activity?.window ?: return
        window.attributes = window.attributes.apply {
            screenBrightness = value.coerceAtLeast(MIN_WINDOW_BRIGHTNESS)
        }
    }

    private fun currentVolume(): Float {
        val manager = audioManager ?: return 0f
        val max = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        return manager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max
    }

    private fun setVolume(value: Float) {
        val manager = audioManager ?: return
        val max = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        manager.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            (value * max).roundToInt().coerceIn(0, max),
            0,
        )
    }

    private companion object {
        const val DEFAULT_SYSTEM_BRIGHTNESS = 128
        const val MAX_SYSTEM_BRIGHTNESS = 255f
        const val MIN_WINDOW_BRIGHTNESS = 0.01f
        const val GESTURE_RANGE_MULTIPLIER = 1.15f
        const val HUD_DISMISS_DELAY_MILLIS = 650L
    }
}

@Composable
internal fun rememberVideoGestureController(): VideoGestureController {
    val context = LocalContext.current
    val activity = context.findActivity()
    val coroutineScope = rememberCoroutineScope()
    val controller = remember(context, activity, coroutineScope) {
        VideoGestureController(context, activity, coroutineScope)
    }
    DisposableEffect(controller) {
        onDispose(controller::restoreBrightness)
    }
    return controller
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
