package com.resonote.feature.video.impl

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.Player
import com.resonote.feature.video.api.VideoNavKey
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
internal fun VideoPlayerControls(
    key: VideoNavKey,
    player: Player,
    fullscreen: Boolean,
    onBack: () -> Unit,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gestureController = rememberVideoGestureController()
    var controlsVisible by remember(player) { mutableStateOf(true) }
    var isPlaying by remember(player) { mutableStateOf(player.isPlaying) }
    var positionMillis by remember(player) { mutableLongStateOf(player.currentPosition.coerceAtLeast(0L)) }
    var durationMillis by remember(player, key.durationMillis) {
        mutableLongStateOf(player.duration.playableDurationOr(key.durationMillis))
    }
    var bufferedPositionMillis by remember(player) {
        mutableLongStateOf(player.bufferedPosition.coerceAtLeast(0L))
    }
    var pendingSeek by remember(player) { mutableStateOf<Float?>(null) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                isPlaying = player.isPlaying
                durationMillis = player.duration.playableDurationOr(key.durationMillis)
                positionMillis = player.currentPosition.coerceAtLeast(0L)
                bufferedPositionMillis = player.bufferedPosition.coerceAtLeast(0L)
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }
    LaunchedEffect(player, isPlaying) {
        while (true) {
            positionMillis = player.currentPosition.coerceAtLeast(0L)
            bufferedPositionMillis = player.bufferedPosition.coerceAtLeast(0L)
            delay(PROGRESS_UPDATE_MILLIS)
        }
    }
    LaunchedEffect(controlsVisible, isPlaying) {
        if (controlsVisible && isPlaying) {
            delay(CONTROLS_TIMEOUT_MILLIS)
            controlsVisible = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(player) {
                detectTapGestures(
                    onTap = { controlsVisible = !controlsVisible },
                    onDoubleTap = { offset ->
                        val delta = if (offset.x < size.width / 2f) -SEEK_STEP_MILLIS else SEEK_STEP_MILLIS
                        player.seekTo((player.currentPosition + delta).coerceIn(0L, durationMillis.coerceAtLeast(0L)))
                        controlsVisible = true
                    },
                )
            }
            .pointerInput(gestureController) {
                detectVerticalDragGestures(
                    onDragStart = { gestureController.start(it, size) },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        gestureController.drag(dragAmount, size.height)
                    },
                    onDragCancel = gestureController::end,
                    onDragEnd = gestureController::end,
                )
            },
    ) {
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(Modifier.fillMaxSize().background(controlScrim())) {
                PlayerTopBar(key = key, onBack = onBack)
                CenterPlaybackControls(
                    isPlaying = isPlaying,
                    onSeekBack = {
                        player.seekTo((player.currentPosition - SEEK_STEP_MILLIS).coerceAtLeast(0L))
                        controlsVisible = true
                    },
                    onPlayPause = {
                        if (isPlaying) player.pause() else player.play()
                        controlsVisible = true
                    },
                    onSeekForward = {
                        player.seekTo((player.currentPosition + SEEK_STEP_MILLIS).coerceAtMost(durationMillis))
                        controlsVisible = true
                    },
                    modifier = Modifier.align(Alignment.Center),
                )
                PlayerBottomBar(
                    positionMillis = pendingSeek?.toLong() ?: positionMillis,
                    bufferedPositionMillis = bufferedPositionMillis,
                    durationMillis = durationMillis,
                    fullscreen = fullscreen,
                    onSeek = { pendingSeek = it },
                    onSeekFinished = {
                        pendingSeek?.let { player.seekTo(it.toLong()) }
                        pendingSeek = null
                        controlsVisible = true
                    },
                    onToggleFullscreen = onToggleFullscreen,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
        GestureHud(
            gesture = gestureController.activeGesture,
            level = gestureController.level,
            modifier = Modifier.align(
                if (gestureController.activeGesture == VideoVerticalGesture.BRIGHTNESS) {
                    Alignment.CenterStart
                } else {
                    Alignment.CenterEnd
                },
            ),
        )
    }
}

@Composable
private fun PlayerTopBar(key: VideoNavKey, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.72f), Color.Transparent)))
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.testTag("video-back")) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowBack,
                stringResource(R.string.feature_video_impl_back),
                tint = Color.White,
            )
        }
        Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
            Text(
                text = key.title,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = key.singer?.takeIf(String::isNotBlank)
                    ?: stringResource(R.string.feature_video_impl_unknown_artist),
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.size(48.dp))
    }
}

@Composable
internal fun VideoLoadingTopBar(key: VideoNavKey, onBack: () -> Unit) {
    PlayerTopBar(key = key, onBack = onBack)
}

@Composable
private fun CenterPlaybackControls(
    isPlaying: Boolean,
    onSeekBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSeekForward: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ControlIconButton(
            onClick = onSeekBack,
            label = stringResource(R.string.feature_video_impl_seek_back),
        ) { Icon(Icons.Rounded.Replay10, contentDescription = null) }
        Surface(
            onClick = onPlayPause,
            modifier = Modifier.size(68.dp).testTag("video-play-pause"),
            shape = CircleShape,
            color = Color.White,
            contentColor = Color.Black,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = stringResource(
                        if (isPlaying) R.string.feature_video_impl_pause else R.string.feature_video_impl_play,
                    ),
                    modifier = Modifier.size(36.dp),
                )
            }
        }
        ControlIconButton(
            onClick = onSeekForward,
            label = stringResource(R.string.feature_video_impl_seek_forward),
        ) { Icon(Icons.Rounded.Forward10, contentDescription = null) }
    }
}

@Composable
private fun ControlIconButton(onClick: () -> Unit, label: String, icon: @Composable () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(52.dp).semantics { contentDescription = label },
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.42f),
        contentColor = Color.White,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(Modifier.size(26.dp), contentAlignment = Alignment.Center) { icon() }
        }
    }
}

@Composable
private fun PlayerBottomBar(
    positionMillis: Long,
    bufferedPositionMillis: Long,
    durationMillis: Long,
    fullscreen: Boolean,
    onSeek: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f))))
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 16.dp, top = 28.dp, end = 8.dp, bottom = 6.dp),
    ) {
        VideoSeekBar(
            positionMillis = positionMillis,
            bufferedPositionMillis = bufferedPositionMillis,
            durationMillis = durationMillis,
            onSeek = onSeek,
            onSeekFinished = onSeekFinished,
            modifier = Modifier.fillMaxWidth().testTag("video-progress"),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${positionMillis.videoTime()} / ${durationMillis.videoTime()}",
                color = Color.White.copy(alpha = 0.82f),
                style = MaterialTheme.typography.labelMedium,
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onToggleFullscreen, modifier = Modifier.testTag("video-fullscreen-action")) {
                Icon(
                    if (fullscreen) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
                    contentDescription = stringResource(
                        if (fullscreen) {
                            R.string.feature_video_impl_exit_fullscreen
                        } else {
                            R.string.feature_video_impl_fullscreen
                        },
                    ),
                    tint = Color.White,
                )
            }
        }
    }
}

@Composable
internal fun VideoSeekBar(
    positionMillis: Long,
    bufferedPositionMillis: Long,
    durationMillis: Long,
    onSeek: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isDragging by remember { mutableStateOf(false) }
    val thumbDiameter by animateDpAsState(
        targetValue = if (isDragging) 14.dp else 8.dp,
        label = "video seek thumb",
    )
    val duration = durationMillis.coerceAtLeast(1L)
    val clampedPositionMillis = positionMillis.coerceIn(0L, duration)
    val positionFraction = clampedPositionMillis.toFloat() / duration
    val bufferedFraction = bufferedPositionMillis.coerceIn(clampedPositionMillis, duration)
        .toFloat() / duration
    val playedColor = MaterialTheme.colorScheme.primary
    val bufferedColor = Color.White.copy(alpha = 0.48f)
    val remainingColor = Color.White.copy(alpha = 0.24f)

    Canvas(
        modifier = modifier
            .height(48.dp)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = clampedPositionMillis.toFloat(),
                    range = 0f..duration.toFloat(),
                )
                setProgress { target ->
                    onSeek(target.coerceIn(0f, duration.toFloat()))
                    onSeekFinished()
                    true
                }
            }
            .pointerInput(duration) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    isDragging = true

                    fun seekTo(pointerX: Float) {
                        val horizontalPadding = 7.dp.toPx()
                        val trackWidth = (size.width - horizontalPadding * 2f).coerceAtLeast(1f)
                        val fraction = ((pointerX - horizontalPadding) / trackWidth).coerceIn(0f, 1f)
                        onSeek(fraction * duration)
                    }

                    try {
                        seekTo(down.position.x)
                        drag(down.id) { change ->
                            change.consume()
                            seekTo(change.position.x)
                        }
                    } finally {
                        isDragging = false
                        onSeekFinished()
                    }
                }
            },
    ) {
        val horizontalPadding = 7.dp.toPx()
        val trackStart = horizontalPadding
        val trackEnd = size.width - horizontalPadding
        val trackWidth = (trackEnd - trackStart).coerceAtLeast(0f)
        val centerY = size.height / 2f
        val trackStrokeWidth = 3.dp.toPx()
        val playedEnd = trackStart + trackWidth * positionFraction
        val bufferedEnd = trackStart + trackWidth * bufferedFraction

        drawLine(
            color = remainingColor,
            start = Offset(trackStart, centerY),
            end = Offset(trackEnd, centerY),
            strokeWidth = trackStrokeWidth,
            cap = StrokeCap.Round,
        )
        if (bufferedEnd > trackStart) {
            drawLine(
                color = bufferedColor,
                start = Offset(trackStart, centerY),
                end = Offset(bufferedEnd, centerY),
                strokeWidth = trackStrokeWidth,
                cap = StrokeCap.Round,
            )
        }
        if (playedEnd > trackStart) {
            drawLine(
                color = playedColor,
                start = Offset(trackStart, centerY),
                end = Offset(playedEnd, centerY),
                strokeWidth = trackStrokeWidth,
                cap = StrokeCap.Round,
            )
        }
        drawCircle(
            color = playedColor,
            radius = thumbDiameter.toPx() / 2f,
            center = Offset(playedEnd, centerY),
        )
    }
}

@Composable
private fun GestureHud(gesture: VideoVerticalGesture?, level: Float, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible = gesture != null, enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
        Surface(
            modifier = Modifier.padding(horizontal = 20.dp),
            shape = RoundedCornerShape(18.dp),
            color = Color.Black.copy(alpha = 0.72f),
            contentColor = Color.White,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Icon(
                    if (gesture ==
                        VideoVerticalGesture.BRIGHTNESS
                    ) {
                        Icons.Rounded.Brightness6
                    } else {
                        Icons.Rounded.VolumeUp
                    },
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = stringResource(
                        if (gesture == VideoVerticalGesture.BRIGHTNESS) {
                            R.string.feature_video_impl_brightness
                        } else {
                            R.string.feature_video_impl_volume
                        },
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.72f),
                )
                Text(
                    text = "${(level * 100).roundToInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

private fun controlScrim(): Brush = Brush.verticalGradient(
    0f to Color.Black.copy(alpha = 0.36f),
    0.42f to Color.Transparent,
    0.7f to Color.Transparent,
    1f to Color.Black.copy(alpha = 0.28f),
)

private fun Long.playableDurationOr(fallback: Long?): Long =
    takeIf { it != C.TIME_UNSET && it > 0L } ?: fallback?.coerceAtLeast(0L) ?: 0L

private fun Long.videoTime(): String {
    val totalSeconds = coerceAtLeast(0L) / 1_000L
    val hours = totalSeconds / 3_600L
    val minutes = totalSeconds % 3_600L / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) "%d:%02d:%02d".format(hours, minutes, seconds) else "%d:%02d".format(minutes, seconds)
}

private const val SEEK_STEP_MILLIS = 10_000L
private const val PROGRESS_UPDATE_MILLIS = 250L
private const val CONTROLS_TIMEOUT_MILLIS = 3_500L
