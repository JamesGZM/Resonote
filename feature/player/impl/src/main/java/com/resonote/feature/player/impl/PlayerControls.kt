package com.resonote.feature.player.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.resonote.core.designsystem.component.ResonoteQualityBadge
import com.resonote.core.designsystem.component.ResonoteVipBadge
import com.resonote.core.designsystem.tokens.ResonoteTokens
import com.resonote.core.model.ContentFailure
import com.resonote.core.model.PlaybackSpeed
import com.resonote.core.playback.PlaybackMetadata
import com.resonote.core.playback.PlaybackMode
import com.resonote.core.playback.PlaybackStatus

@Composable
internal fun SongIdentity(song: PlaybackMetadata) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            song.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            modifier = Modifier.padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                song.artist.orEmpty().ifBlank { stringResource(R.string.feature_player_impl_unknown_artist) },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            song.format.badgeLabel()?.let { label ->
                Spacer(Modifier.width(8.dp))
                ResonoteQualityBadge(label)
            }
            if (song.isVip) {
                Spacer(Modifier.width(6.dp))
                ResonoteVipBadge()
            }
        }
    }
}

@Composable
internal fun PlayerProgress(positionMillis: Long, durationMillis: Long, onSeek: (Long) -> Unit) {
    val duration = durationMillis.coerceAtLeast(1L)
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }
    val progress = if (dragging) dragValue else (positionMillis.toFloat() / duration).coerceIn(0f, 1f)
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        Slider(
            value = progress,
            onValueChange = {
                dragging = true
                dragValue = it
            },
            onValueChangeFinished = {
                onSeek((dragValue * duration).toLong())
                dragging = false
            },
        )
        Row(Modifier.fillMaxWidth()) {
            Text(
                (if (dragging) (dragValue * duration).toLong() else positionMillis).playerTimeLabel(),
                style = MaterialTheme.typography.labelMedium,
            )
            Spacer(Modifier.weight(1f))
            Text(durationMillis.playerTimeLabel(), style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
internal fun PlaybackControls(
    status: PlaybackStatus,
    mode: PlaybackMode,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onModeChange: (PlaybackMode) -> Unit,
    onOpenQueue: () -> Unit,
) {
    val isPlaying = status == PlaybackStatus.Playing
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = { onModeChange(mode.next()) }, modifier = Modifier.size(48.dp)) {
            Icon(mode.icon(), mode.label(), tint = MaterialTheme.colorScheme.primary)
        }
        IconButton(onClick = onPrevious, modifier = Modifier.size(56.dp)) {
            Icon(
                Icons.Rounded.SkipPrevious,
                stringResource(R.string.feature_player_impl_previous),
                Modifier.size(34.dp),
            )
        }
        Surface(
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shadowElevation = ResonoteTokens.elevation.level3.maximumShadow,
            onClick = onTogglePlay,
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (status == PlaybackStatus.Resolving || status == PlaybackStatus.Buffering) {
                    CircularProgressIndicator(
                        Modifier.size(28.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 3.dp,
                    )
                } else {
                    Icon(
                        if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        stringResource(
                            if (isPlaying) {
                                R.string.feature_player_impl_pause
                            } else {
                                R.string.feature_player_impl_play
                            },
                        ),
                        Modifier.size(38.dp),
                    )
                }
            }
        }
        IconButton(onClick = onNext, modifier = Modifier.size(56.dp)) {
            Icon(Icons.Rounded.SkipNext, stringResource(R.string.feature_player_impl_next), Modifier.size(34.dp))
        }
        IconButton(onClick = onOpenQueue, modifier = Modifier.size(48.dp)) {
            Icon(Icons.AutoMirrored.Rounded.QueueMusic, stringResource(R.string.feature_player_impl_queue))
        }
    }
}

@Composable
internal fun EmptyPlayer(onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Rounded.StopCircle, contentDescription = null, modifier = Modifier.size(48.dp))
        Text(stringResource(R.string.feature_player_impl_empty_queue), modifier = Modifier.padding(top = 16.dp))
        Button(onClick = onBack, modifier = Modifier.padding(top = 20.dp)) {
            Text(stringResource(R.string.feature_player_impl_back))
        }
    }
}

@Composable
internal fun LyricsMessage(message: String) {
    Text(
        message,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
    )
}

@Composable
internal fun ContentFailure.lyricsMessage(): String = stringResource(
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

internal fun PlaybackMode.next(): PlaybackMode = when (this) {
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
internal fun PlaybackMode.label(): String = stringResource(
    when (this) {
        PlaybackMode.ListLoop -> R.string.feature_player_impl_mode_list_loop
        PlaybackMode.Shuffle -> R.string.feature_player_impl_mode_shuffle
        PlaybackMode.SingleLoop -> R.string.feature_player_impl_mode_single_loop
        PlaybackMode.Sequential -> R.string.feature_player_impl_mode_sequential
    },
)

@Composable
internal fun PlaybackSpeed.label(): String = stringResource(
    R.string.feature_player_impl_speed_value,
    when (this) {
        PlaybackSpeed.Half -> "0.5"
        PlaybackSpeed.ThreeQuarters -> "0.75"
        PlaybackSpeed.Normal -> "1"
        PlaybackSpeed.OneAndQuarter -> "1.25"
        PlaybackSpeed.OneAndHalf -> "1.5"
        PlaybackSpeed.Double -> "2"
    },
)

internal fun Long.playerTimeLabel(): String {
    val totalSeconds = coerceAtLeast(0) / 1_000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
