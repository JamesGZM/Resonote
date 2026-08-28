package com.resonote.feature.player.impl

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.resonote.core.karaoke.KaraokeSessionState
import com.resonote.core.karaoke.KaraokeSessionStatus
import com.resonote.core.model.KaraokeSourceMode
import com.resonote.core.model.PlaybackMode
import com.resonote.core.model.PlaybackSpeed
import com.resonote.core.playback.PlaybackStatus

@Composable
internal fun PlayerBottomControls(
    karaokeEnabled: Boolean,
    karaoke: KaraokeSessionState,
    lyrics: LyricsUiState,
    positionMillis: Long,
    playbackStatus: PlaybackStatus,
    playbackMode: PlaybackMode,
    playbackSpeed: PlaybackSpeed,
    like: LikeUiState,
    palette: PlayerPalette,
    onToggleLike: () -> Unit,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onModeChange: (PlaybackMode) -> Unit,
    onStartKaraoke: () -> Unit,
    onPauseKaraoke: () -> Unit,
    onResumeKaraoke: () -> Unit,
    onPreviousKaraoke: () -> Unit,
    onNextKaraoke: () -> Unit,
    onStopKaraoke: () -> Unit,
    onSelectKaraokeSource: (KaraokeSourceMode) -> Unit,
    onSkipIntro: (Long) -> Unit,
    onOpenFormat: () -> Unit,
    onOpenSpeed: () -> Unit,
    onOpenQueue: () -> Unit,
) {
    AnimatedContent(
        targetState = karaokeEnabled,
        transitionSpec = {
            (fadeIn(tween(280)) + slideInVertically(tween(280)) { it / 12 }) togetherWith
                (fadeOut(tween(160)) + slideOutVertically(tween(160)) { it / 14 }) using
                SizeTransform(clip = false)
        },
        label = "player karaoke controls",
    ) { enabled ->
        if (enabled) {
            KaraokeControls(
                state = karaoke,
                lyrics = lyrics,
                positionMillis = positionMillis,
                palette = palette,
                onStart = onStartKaraoke,
                onPause = onPauseKaraoke,
                onResume = onResumeKaraoke,
                onPrevious = onPreviousKaraoke,
                onNext = onNextKaraoke,
                onStop = onStopKaraoke,
                onSelectSource = onSelectKaraokeSource,
                onSkipIntro = onSkipIntro,
                onOpenQueue = onOpenQueue,
            )
        } else {
            Column {
                PlaybackControls(
                    playbackStatus,
                    playbackMode,
                    like,
                    palette,
                    onToggleLike,
                    onTogglePlay,
                    onPrevious,
                    onNext,
                    onModeChange,
                )
                PlayerToolRow(palette, playbackSpeed, onOpenFormat, onOpenSpeed, onOpenQueue)
            }
        }
    }
}

@Composable
private fun KaraokeControls(
    state: KaraokeSessionState,
    lyrics: LyricsUiState,
    positionMillis: Long,
    palette: PlayerPalette,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onStop: () -> Unit,
    onSelectSource: (KaraokeSourceMode) -> Unit,
    onSkipIntro: (Long) -> Unit,
    onOpenQueue: () -> Unit,
) {
    val firstLyricMillis = (lyrics as? LyricsUiState.Content)?.document?.lines
        ?.firstOrNull { it.text.isNotBlank() && it.timeMillis > 0L }
        ?.timeMillis
    val introTarget = firstLyricMillis?.minus(INTRO_LEAD_IN_MILLIS)?.coerceAtLeast(0L)
    val canSkipIntro = state.status !is KaraokeSessionStatus.Preparing &&
        state.status !is KaraokeSessionStatus.Failed &&
        !state.savingInProgress &&
        introTarget != null &&
        positionMillis < introTarget

    Column {
        Row(
            Modifier.fillMaxWidth().padding(start = 22.dp, top = 4.dp, end = 22.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            ControlSlot {
                if (canSkipIntro) {
                    IconButton(onClick = { onSkipIntro(checkNotNull(introTarget)) }, Modifier.size(48.dp)) {
                        Icon(
                            Icons.Rounded.FastForward,
                            stringResource(R.string.feature_player_impl_karaoke_skip_intro),
                            tint = palette.contentPrimary,
                        )
                    }
                }
            }
            val transportEnabled = !state.savingInProgress && state.status !is KaraokeSessionStatus.Countdown
            IconButton(onClick = onPrevious, Modifier.size(54.dp), enabled = transportEnabled) {
                Icon(
                    Icons.Rounded.SkipPrevious,
                    stringResource(R.string.feature_player_impl_previous),
                    Modifier.size(34.dp),
                    palette.contentPrimary,
                )
            }
            KaraokePrimaryAction(state, palette, onStart, onPause, onResume, onStop)
            IconButton(onClick = onNext, Modifier.size(54.dp), enabled = transportEnabled) {
                Icon(
                    Icons.Rounded.SkipNext,
                    stringResource(R.string.feature_player_impl_next),
                    Modifier.size(34.dp),
                    palette.contentPrimary,
                )
            }
            ControlSlot {
                if (state.continuousRecordingArmed) {
                    IconButton(onClick = onStop, Modifier.size(48.dp), enabled = !state.savingInProgress) {
                        Icon(
                            Icons.Rounded.Stop,
                            stringResource(R.string.feature_player_impl_karaoke_stop),
                            tint = palette.contentPrimary,
                        )
                    }
                }
            }
        }
        KaraokeToolRow(state, palette, onSelectSource, onOpenQueue)
    }
}

@Composable
private fun KaraokePrimaryAction(
    state: KaraokeSessionState,
    palette: PlayerPalette,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
) {
    val loading = state.status is KaraokeSessionStatus.Preparing || state.savingInProgress
    val status = state.status
    val onClick = when (status) {
        is KaraokeSessionStatus.Recording -> onPause
        is KaraokeSessionStatus.Paused -> onResume
        is KaraokeSessionStatus.Countdown -> onStop
        else -> onStart
    }
    Surface(
        onClick = onClick,
        enabled = !loading,
        modifier = Modifier.size(72.dp),
        shape = CircleShape,
        color = palette.accent,
        contentColor = palette.contentOnAccent,
        shadowElevation = 8.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            when {
                loading -> CircularProgressIndicator(
                    Modifier.size(28.dp),
                    color = palette.contentOnAccent,
                    strokeWidth = 3.dp,
                )
                status is KaraokeSessionStatus.Countdown -> Text(
                    status.secondsRemaining.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                status is KaraokeSessionStatus.Recording -> Icon(
                    Icons.Rounded.Pause,
                    stringResource(R.string.feature_player_impl_pause),
                    Modifier.size(38.dp),
                )
                status is KaraokeSessionStatus.Paused -> Icon(
                    Icons.Rounded.PlayArrow,
                    stringResource(R.string.feature_player_impl_play),
                    Modifier.size(38.dp),
                )
                else -> Icon(
                    Icons.Rounded.Mic,
                    stringResource(R.string.feature_player_impl_karaoke_start),
                    Modifier.size(34.dp),
                )
            }
        }
    }
}

@Composable
private fun KaraokeToolRow(
    state: KaraokeSessionState,
    palette: PlayerPalette,
    onSelectSource: (KaraokeSourceMode) -> Unit,
    onOpenQueue: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(start = 34.dp, top = 8.dp, end = 34.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SourceChoice(
            mode = KaraokeSourceMode.Accompaniment,
            icon = Icons.Rounded.GraphicEq,
            label = stringResource(R.string.feature_player_impl_karaoke_accompaniment),
            state = state,
            palette = palette,
            onClick = onSelectSource,
        )
        SourceChoice(
            mode = KaraokeSourceMode.Original,
            icon = Icons.Rounded.RecordVoiceOver,
            label = stringResource(R.string.feature_player_impl_karaoke_original),
            state = state,
            palette = palette,
            onClick = onSelectSource,
        )
        Surface(
            onClick = onOpenQueue,
            modifier = Modifier.size(64.dp),
            shape = CircleShape,
            color = Color.Transparent,
            contentColor = palette.contentPrimary,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painterResource(R.drawable.feature_player_impl_ic_queue),
                    stringResource(R.string.feature_player_impl_queue),
                    Modifier.size(34.dp),
                )
            }
        }
    }
}

@Composable
private fun SourceChoice(
    mode: KaraokeSourceMode,
    icon: ImageVector,
    label: String,
    state: KaraokeSessionState,
    palette: PlayerPalette,
    onClick: (KaraokeSourceMode) -> Unit,
) {
    val selected = state.selectedSourceMode == mode
    val loading = state.sourceChangeInProgress && !selected
    val enabled = mode in state.availableSourceModes && !state.savingInProgress && !state.sourceChangeInProgress
    Surface(
        onClick = { onClick(mode) },
        enabled = enabled,
        modifier = Modifier.width(96.dp).height(48.dp).alpha(if (enabled || selected || loading) 1f else 0.38f),
        shape = RoundedCornerShape(24.dp),
        color = if (selected) palette.accent.copy(alpha = 0.18f) else Color.Transparent,
        contentColor = if (selected) palette.accent else palette.contentPrimary,
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (loading) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = palette.accent)
            } else {
                Icon(icon, null, Modifier.size(19.dp))
            }
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun ControlSlot(content: @Composable () -> Unit) {
    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) { content() }
}

private const val INTRO_LEAD_IN_MILLIS = 2_000L
