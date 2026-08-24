package com.resonote.feature.player.impl

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.resonote.core.model.AudioQuality
import com.resonote.core.model.ContentFailure
import com.resonote.core.model.OnlinePlaybackQuality
import com.resonote.core.model.PlaybackMode
import com.resonote.core.model.PlaybackSpeed
import com.resonote.core.playback.PlaybackFormat
import com.resonote.core.playback.PlaybackStatus

@Composable
internal fun PlayerProgress(
    positionMillis: Long,
    bufferedPositionMillis: Long,
    durationMillis: Long,
    palette: PlayerPalette,
    onSeek: (Long) -> Unit,
) {
    val duration = durationMillis.coerceAtLeast(1L)
    var pendingFraction by remember { mutableStateOf<Float?>(null) }
    val visiblePosition = pendingFraction?.let { (it * duration).toLong() } ?: positionMillis
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        ThinPlayerSeekBar(
            visiblePosition,
            bufferedPositionMillis,
            duration,
            palette,
            { pendingFraction = it },
            {
                pendingFraction?.let { onSeek((it * duration).toLong()) }
                pendingFraction = null
            },
            Modifier.fillMaxWidth(),
        )
        Row(Modifier.fillMaxWidth().padding(horizontal = 7.dp)) {
            Text(
                visiblePosition.playerTimeLabel(),
                color = palette.accent,
                style = MaterialTheme.typography.labelMedium,
            )
            Spacer(Modifier.weight(1f))
            Text(durationMillis.playerTimeLabel(), color = palette.accent, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
internal fun ThinPlayerSeekBar(
    positionMillis: Long,
    bufferedPositionMillis: Long,
    durationMillis: Long,
    palette: PlayerPalette,
    onSeek: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var dragging by remember { mutableStateOf(false) }
    val thumb by animateDpAsState(if (dragging) 14.dp else 8.dp, label = "player seek thumb")
    val duration = durationMillis.coerceAtLeast(1L)
    val position = positionMillis.coerceIn(0L, duration)
    val played = position.toFloat() / duration
    val buffered = bufferedPositionMillis.coerceIn(position, duration).toFloat() / duration
    Canvas(
        modifier.height(48.dp)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(position.toFloat(), 0f..duration.toFloat())
                setProgress { target ->
                    onSeek((target.coerceIn(0f, duration.toFloat()) / duration).coerceIn(0f, 1f))
                    onSeekFinished()
                    true
                }
            }
            .pointerInput(duration) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    dragging = true
                    fun seek(x: Float) {
                        val inset = 7.dp.toPx()
                        onSeek(((x - inset) / (size.width - inset * 2f).coerceAtLeast(1f)).coerceIn(0f, 1f))
                    }
                    try {
                        seek(down.position.x)
                        drag(down.id) { change ->
                            change.consume()
                            seek(change.position.x)
                        }
                    } finally {
                        dragging = false
                        onSeekFinished()
                    }
                }
            },
    ) {
        val inset = 7.dp.toPx()
        val start = inset
        val width = (size.width - inset * 2f).coerceAtLeast(0f)
        val y = size.height - 8.dp.toPx()
        fun line(end: Float, color: Color) = drawLine(
            color,
            Offset(start, y),
            Offset(start + width * end, y),
            3.dp.toPx(),
            StrokeCap.Round,
        )
        line(1f, palette.contentMuted.copy(alpha = 0.45f))
        line(buffered, palette.contentSecondary.copy(alpha = 0.55f))
        line(played, palette.accent)
        drawCircle(palette.accent, thumb.toPx() / 2f, Offset(start + width * played, y))
    }
}

@Composable
internal fun PlaybackControls(
    status: PlaybackStatus,
    mode: PlaybackMode,
    like: LikeUiState,
    palette: PlayerPalette,
    onToggleLike: () -> Unit,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onModeChange: (PlaybackMode) -> Unit,
) {
    val playing = status == PlaybackStatus.Playing
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onToggleLike, Modifier.size(48.dp), enabled = like !is LikeUiState.Loading) {
            if (like is LikeUiState.Available && like.isUpdating) {
                CircularProgressIndicator(Modifier.size(22.dp), color = palette.accent, strokeWidth = 2.dp)
            } else {
                val liked = (like as? LikeUiState.Available)?.isLiked == true
                Icon(
                    if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    stringResource(R.string.feature_player_impl_like),
                    tint = if (liked) palette.accent else palette.contentPrimary,
                )
            }
        }
        IconButton(onClick = onPrevious, Modifier.size(54.dp)) {
            Icon(
                Icons.Rounded.SkipPrevious,
                stringResource(R.string.feature_player_impl_previous),
                Modifier.size(34.dp),
                palette.contentPrimary,
            )
        }
        Surface(
            onClick = onTogglePlay,
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            color = palette.accent,
            contentColor = palette.contentOnAccent,
            shadowElevation = 8.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (status == PlaybackStatus.Resolving || status == PlaybackStatus.Buffering) {
                    CircularProgressIndicator(Modifier.size(28.dp), color = palette.contentOnAccent, strokeWidth = 3.dp)
                } else {
                    Icon(
                        if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        stringResource(
                            if (playing) R.string.feature_player_impl_pause else R.string.feature_player_impl_play,
                        ),
                        Modifier.size(38.dp),
                    )
                }
            }
        }
        IconButton(onClick = onNext, Modifier.size(54.dp)) {
            Icon(
                Icons.Rounded.SkipNext,
                stringResource(R.string.feature_player_impl_next),
                Modifier.size(34.dp),
                palette.contentPrimary,
            )
        }
        IconButton(onClick = { onModeChange(mode.next()) }, Modifier.size(48.dp)) {
            Icon(mode.icon(), mode.label(), tint = palette.accent)
        }
    }
}

@Composable
internal fun PlayerToolRow(
    palette: PlayerPalette,
    playbackSpeed: PlaybackSpeed,
    onOpenFormat: () -> Unit,
    onOpenSpeed: () -> Unit,
    onOpenQueue: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(start = 40.dp, top = 4.dp, end = 40.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        PlayerTool(
            R.drawable.feature_player_impl_ic_quality,
            stringResource(R.string.feature_player_impl_current_format),
            palette,
            onOpenFormat,
        )
        PlayerTool(
            playbackSpeed.iconRes(),
            stringResource(R.string.feature_player_impl_playback_speed),
            palette,
            onOpenSpeed,
            stateValue = playbackSpeed.valueLabel(),
            iconSize = 32.dp,
        )
        PlayerTool(
            R.drawable.feature_player_impl_ic_queue,
            stringResource(R.string.feature_player_impl_queue),
            palette,
            onOpenQueue,
        )
    }
}

@Composable
private fun PlayerTool(
    iconRes: Int,
    label: String,
    palette: PlayerPalette,
    onClick: () -> Unit,
    stateValue: String? = null,
    iconSize: androidx.compose.ui.unit.Dp = 32.dp,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(64.dp).then(
            if (stateValue != null) Modifier.semantics { stateDescription = stateValue } else Modifier,
        ),
        shape = CircleShape,
        color = Color.Transparent,
        contentColor = palette.contentPrimary,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                painterResource(iconRes),
                label,
                Modifier.size(iconSize),
                tint = palette.contentPrimary,
            )
        }
    }
}

private fun PlaybackSpeed.iconRes(): Int = when (this) {
    PlaybackSpeed.Half -> R.drawable.feature_player_impl_ic_speed_050
    PlaybackSpeed.ThreeQuarters -> R.drawable.feature_player_impl_ic_speed_075
    PlaybackSpeed.Normal -> R.drawable.feature_player_impl_ic_speed_100
    PlaybackSpeed.OneAndQuarter -> R.drawable.feature_player_impl_ic_speed_125
    PlaybackSpeed.OneAndHalf -> R.drawable.feature_player_impl_ic_speed_150
    PlaybackSpeed.Double -> R.drawable.feature_player_impl_ic_speed_200
}

@Composable
internal fun PlaybackFormat.toolLabel() = when (this) {
    is PlaybackFormat.Online -> stringResource(
        when (quality) {
            AudioQuality.Standard -> R.string.feature_player_impl_standard_quality
            AudioQuality.HighQuality -> R.string.feature_player_impl_high_quality
            AudioQuality.HighResolution -> R.string.feature_player_impl_high_resolution
            AudioQuality.Lossless -> R.string.feature_player_impl_lossless_quality
        },
    )
    is PlaybackFormat.Cloud -> extension?.uppercase() ?: stringResource(R.string.feature_player_impl_cloud_format)
    is PlaybackFormat.Local -> extension?.uppercase() ?: stringResource(R.string.feature_player_impl_local_format)
}

@Composable
internal fun OnlinePlaybackQuality.label() = stringResource(
    when (this) {
        OnlinePlaybackQuality.Standard -> R.string.feature_player_impl_standard_quality
        OnlinePlaybackQuality.HighQuality -> R.string.feature_player_impl_high_quality
        OnlinePlaybackQuality.Lossless -> R.string.feature_player_impl_lossless_quality
        OnlinePlaybackQuality.HighResolution -> R.string.feature_player_impl_high_resolution
        OnlinePlaybackQuality.ViperAtmos -> R.string.feature_player_impl_viper_atmos
        OnlinePlaybackQuality.ViperClear -> R.string.feature_player_impl_viper_clear
        OnlinePlaybackQuality.ViperTape -> R.string.feature_player_impl_viper_tape
    },
)

internal fun PlaybackFormat.defaultOnlineQuality(): OnlinePlaybackQuality? = when (this) {
    is PlaybackFormat.Online -> when (quality) {
        AudioQuality.Standard -> OnlinePlaybackQuality.Standard
        AudioQuality.HighQuality -> OnlinePlaybackQuality.HighQuality
        AudioQuality.Lossless -> OnlinePlaybackQuality.Lossless
        AudioQuality.HighResolution -> OnlinePlaybackQuality.HighResolution
    }
    is PlaybackFormat.Cloud,
    is PlaybackFormat.Local,
    -> null
}

@Composable
internal fun EmptyPlayer(onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Rounded.StopCircle, null, Modifier.size(48.dp))
        Text(stringResource(R.string.feature_player_impl_empty_queue), Modifier.padding(top = 16.dp))
        Button(onClick = onBack, Modifier.padding(top = 20.dp)) {
            Text(stringResource(R.string.feature_player_impl_back))
        }
    }
}

@Composable
internal fun LyricsMessage(message: String, palette: PlayerPalette) {
    Text(
        message,
        color = palette.contentSecondary,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
    )
}

@Composable
internal fun ContentFailure.lyricsMessage() = stringResource(
    when (this) {
        ContentFailure.AuthenticationRequired -> R.string.feature_player_impl_lyrics_auth
        ContentFailure.Network -> R.string.feature_player_impl_lyrics_network
        ContentFailure.ServiceRejected -> R.string.feature_player_impl_lyrics_service
        is ContentFailure.RiskVerificationRequired,
        ContentFailure.RiskBlocked,
        -> R.string.feature_player_impl_lyrics_risk
        ContentFailure.Protocol -> R.string.feature_player_impl_lyrics_protocol
    },
)

internal fun PlaybackMode.next() = when (this) {
    PlaybackMode.ListLoop -> PlaybackMode.Shuffle
    PlaybackMode.Shuffle -> PlaybackMode.SingleLoop
    PlaybackMode.SingleLoop -> PlaybackMode.Sequential
    PlaybackMode.Sequential -> PlaybackMode.ListLoop
}

internal fun PlaybackMode.icon() = when (this) {
    PlaybackMode.ListLoop -> Icons.Rounded.Repeat
    PlaybackMode.Shuffle -> Icons.Rounded.Shuffle
    PlaybackMode.SingleLoop -> Icons.Rounded.RepeatOne
    PlaybackMode.Sequential -> Icons.AutoMirrored.Rounded.PlaylistPlay
}

@Composable
internal fun PlaybackMode.label() = stringResource(
    when (this) {
        PlaybackMode.ListLoop -> R.string.feature_player_impl_mode_list_loop
        PlaybackMode.Shuffle -> R.string.feature_player_impl_mode_shuffle
        PlaybackMode.SingleLoop -> R.string.feature_player_impl_mode_single_loop
        PlaybackMode.Sequential -> R.string.feature_player_impl_mode_sequential
    },
)

@Composable
internal fun PlaybackSpeed.label() = stringResource(
    R.string.feature_player_impl_speed_value,
    numericValueLabel(),
)

internal fun PlaybackSpeed.valueLabel(): String = numericValueLabel() + "×"

private fun PlaybackSpeed.numericValueLabel(): String = when (this) {
    PlaybackSpeed.Half -> "0.5"
    PlaybackSpeed.ThreeQuarters -> "0.75"
    PlaybackSpeed.Normal -> "1"
    PlaybackSpeed.OneAndQuarter -> "1.25"
    PlaybackSpeed.OneAndHalf -> "1.5"
    PlaybackSpeed.Double -> "2"
}

internal fun Long.playerTimeLabel(): String {
    val seconds = coerceAtLeast(0) / 1_000
    return "%d:%02d".format(seconds / 60, seconds % 60)
}
