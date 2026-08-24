@file:androidx.media3.common.util.UnstableApi

package com.resonote.feature.video.impl

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.ContentFrame
import coil3.compose.AsyncImage
import com.resonote.core.designsystem.component.ResonoteButton
import com.resonote.core.designsystem.component.ResonoteHeroKeys
import com.resonote.core.designsystem.component.resonoteHero
import com.resonote.core.designsystem.tokens.ResonoteTokens
import com.resonote.core.model.ContentFailure
import com.resonote.feature.video.api.VideoNavKey

@Composable
fun VideoRoute(
    key: VideoNavKey,
    onBack: () -> Unit,
    onFullscreenChange: (Boolean) -> Unit,
    viewModel: VideoViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var fullscreen by remember { mutableStateOf(false) }
    val view = LocalView.current

    LaunchedEffect(key.hash) { viewModel.load(key.hash) }
    DisposableEffect(view) {
        val previousKeepScreenOn = view.keepScreenOn
        view.keepScreenOn = true
        onDispose {
            view.keepScreenOn = previousKeepScreenOn
            onFullscreenChange(false)
        }
    }

    val exitFullscreen = {
        fullscreen = false
        onFullscreenChange(false)
    }
    val handleBack = {
        dispatchVideoBack(
            fullscreen = fullscreen,
            onExitFullscreen = exitFullscreen,
            onNavigateBack = onBack,
        )
    }
    BackHandler(enabled = fullscreen, onBack = handleBack)

    val toggleFullscreen = {
        fullscreen = !fullscreen
        onFullscreenChange(fullscreen)
    }
    VideoPlayerHost(
        key = key,
        state = state,
        fullscreen = fullscreen,
        onBack = handleBack,
        onToggleFullscreen = toggleFullscreen,
        onRetry = viewModel::retry,
    )
}

internal fun dispatchVideoBack(fullscreen: Boolean, onExitFullscreen: () -> Unit, onNavigateBack: () -> Unit) {
    if (fullscreen) onExitFullscreen() else onNavigateBack()
}

@Composable
private fun VideoPlayerHost(
    key: VideoNavKey,
    state: VideoUiState,
    fullscreen: Boolean,
    onBack: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onRetry: () -> Unit,
) {
    val context = LocalContext.current
    val ready = state as? VideoUiState.Ready
    val player = remember(ready?.url) {
        ready?.let {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(it.url))
                prepare()
                playWhenReady = true
            }
        }
    }
    var playbackFailed by remember(ready?.url) { mutableStateOf(false) }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                playbackFailed = true
            }
        }
        player?.addListener(listener)
        onDispose {
            player?.removeListener(listener)
            player?.release()
        }
    }

    VideoScreen(
        key = key,
        state = state,
        player = player,
        playbackFailed = playbackFailed,
        fullscreen = fullscreen,
        onBack = onBack,
        onToggleFullscreen = onToggleFullscreen,
        onRetry = {
            if (playbackFailed && player != null) {
                playbackFailed = false
                player.prepare()
                player.play()
            } else {
                onRetry()
            }
        },
    )
}

@Composable
internal fun VideoScreen(
    key: VideoNavKey,
    state: VideoUiState,
    player: Player?,
    playbackFailed: Boolean,
    fullscreen: Boolean,
    onBack: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val mediaColors = ResonoteTokens.systemColors
    Box(
        modifier
            .fillMaxSize()
            .background(mediaColors.mediaCanvas)
            .testTag(if (fullscreen) "video-fullscreen" else "video-screen"),
    ) {
        VideoStage(
            state = state,
            player = player,
            playbackFailed = playbackFailed,
            coverUrl = key.coverUrl,
            onRetry = onRetry,
            modifier = Modifier.resonoteHero(ResonoteHeroKeys.video(key.hash)).fillMaxSize(),
        )
        if (state is VideoUiState.Ready && player != null && !playbackFailed) {
            VideoPlayerControls(
                key = key,
                player = player,
                fullscreen = fullscreen,
                onBack = onBack,
                onToggleFullscreen = onToggleFullscreen,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            VideoLoadingTopBar(key = key, onBack = onBack)
        }
    }
}

@Composable
private fun VideoStage(
    state: VideoUiState,
    player: Player?,
    playbackFailed: Boolean,
    coverUrl: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val mediaColors = ResonoteTokens.systemColors
    Box(
        modifier = modifier.background(mediaColors.mediaCanvas).testTag("video-stage"),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (coverUrl != null && state !is VideoUiState.Ready) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = 0.32f,
                )
            }
            when {
                playbackFailed -> VideoStageMessage(
                    title = stringResource(R.string.feature_video_impl_playback_error_title),
                    body = stringResource(R.string.feature_video_impl_playback_error_body),
                    onRetry = onRetry,
                )
                state is VideoUiState.Ready && player != null -> ContentFrame(
                    player = player,
                    modifier = Modifier.fillMaxSize(),
                )
                state is VideoUiState.Loading || state is VideoUiState.Idle -> LoadingStage()
                state is VideoUiState.Unavailable -> VideoStageMessage(
                    title = stringResource(R.string.feature_video_impl_unavailable_title),
                    body = stringResource(R.string.feature_video_impl_unavailable_body),
                    onRetry = onRetry,
                )
                state is VideoUiState.Failed -> VideoStageMessage(
                    title = stringResource(R.string.feature_video_impl_error_title),
                    body = state.failure.message(),
                    onRetry = onRetry,
                )
            }
        }
    }
}

@Composable
private fun LoadingStage() {
    val onMediaCanvas = ResonoteTokens.systemColors.onMediaCanvas
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primaryContainer)
        Text(
            stringResource(R.string.feature_video_impl_loading),
            modifier = Modifier.padding(top = 18.dp),
            color = onMediaCanvas,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            stringResource(R.string.feature_video_impl_loading_body),
            modifier = Modifier.padding(top = 4.dp),
            color = onMediaCanvas.copy(alpha = 0.68f),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun VideoStageMessage(title: String, body: String, onRetry: () -> Unit) {
    val onMediaCanvas = ResonoteTokens.systemColors.onMediaCanvas
    Column(
        modifier = Modifier.padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Rounded.CloudOff, contentDescription = null, modifier = Modifier.size(34.dp), tint = onMediaCanvas)
        Text(
            title,
            modifier = Modifier.padding(top = 12.dp),
            color = onMediaCanvas,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            body,
            modifier = Modifier.padding(top = 5.dp),
            color = onMediaCanvas.copy(alpha = 0.72f),
            style = MaterialTheme.typography.bodySmall,
        )
        ResonoteButton(
            label = stringResource(R.string.feature_video_impl_retry),
            onClick = onRetry,
            leadingIcon = { Icon(Icons.Rounded.Refresh, contentDescription = null) },
            modifier = Modifier.padding(top = 14.dp),
        )
    }
}

@Composable
private fun ContentFailure.message(): String = stringResource(
    when (this) {
        ContentFailure.AuthenticationRequired -> R.string.feature_video_impl_error_auth
        ContentFailure.Network -> R.string.feature_video_impl_error_network
        is ContentFailure.RiskVerificationRequired, ContentFailure.RiskBlocked -> R.string.feature_video_impl_error_risk
        ContentFailure.Protocol, ContentFailure.ServiceRejected -> R.string.feature_video_impl_error_generic
    },
)
