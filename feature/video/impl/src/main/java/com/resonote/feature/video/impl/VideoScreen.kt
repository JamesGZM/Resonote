@file:androidx.media3.common.util.UnstableApi

package com.resonote.feature.video.impl

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import coil3.compose.AsyncImage
import com.resonote.core.designsystem.component.ResonoteButton
import com.resonote.core.designsystem.tokens.ResonoteTokens
import com.resonote.core.model.ContentFailure
import com.resonote.feature.video.api.VideoNavKey
import androidx.media3.ui.compose.material3.Player as Media3Player

@Composable
fun VideoRoute(
    key: VideoNavKey,
    onBack: () -> Unit,
    onFullscreenChange: (Boolean) -> Unit,
    viewModel: VideoViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var fullscreen by remember { mutableStateOf(false) }

    LaunchedEffect(key.hash) { viewModel.load(key.hash) }
    DisposableEffect(Unit) {
        onDispose { onFullscreenChange(false) }
    }
    BackHandler(enabled = fullscreen) {
        fullscreen = false
        onFullscreenChange(false)
    }

    val toggleFullscreen = {
        fullscreen = !fullscreen
        onFullscreenChange(fullscreen)
    }
    VideoPlayerHost(
        key = key,
        state = state,
        fullscreen = fullscreen,
        onBack = onBack,
        onToggleFullscreen = toggleFullscreen,
        onRetry = viewModel::retry,
    )
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
    if (fullscreen) {
        FullscreenVideo(
            player = player,
            state = state,
            playbackFailed = playbackFailed,
            onBack = onBack,
            onToggleFullscreen = onToggleFullscreen,
            onRetry = onRetry,
            modifier = modifier,
        )
        return
    }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("video-screen"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.feature_video_impl_back))
                }
                Text(
                    key.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                IconButton(onClick = onToggleFullscreen, enabled = state is VideoUiState.Ready) {
                    Icon(Icons.Rounded.Fullscreen, stringResource(R.string.feature_video_impl_fullscreen))
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            VideoStage(
                state = state,
                player = player,
                playbackFailed = playbackFailed,
                coverUrl = key.coverUrl,
                onRetry = onRetry,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
            )
            VideoMetadata(key)
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Movie, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        stringResource(R.string.feature_video_impl_paused_music),
                        modifier = Modifier.padding(start = 12.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun FullscreenVideo(
    player: Player?,
    state: VideoUiState,
    playbackFailed: Boolean,
    onBack: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val mediaColors = ResonoteTokens.systemColors
    Box(modifier.fillMaxSize().background(mediaColors.mediaCanvas).testTag("video-fullscreen")) {
        VideoStage(
            state = state,
            player = player,
            playbackFailed = playbackFailed,
            coverUrl = null,
            onRetry = onRetry,
            modifier = Modifier.fillMaxSize(),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Surface(
                shape = CircleShape,
                color = mediaColors.mediaCanvas.copy(alpha = 0.52f),
                contentColor = mediaColors.onMediaCanvas,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.feature_video_impl_back))
                }
            }
            Surface(
                shape = CircleShape,
                color = mediaColors.mediaCanvas.copy(alpha = 0.52f),
                contentColor = mediaColors.onMediaCanvas,
            ) {
                IconButton(onClick = onToggleFullscreen) {
                    Icon(Icons.Rounded.FullscreenExit, stringResource(R.string.feature_video_impl_exit_fullscreen))
                }
            }
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
    Card(
        modifier = modifier.testTag("video-stage"),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = mediaColors.mediaCanvas),
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
                state is VideoUiState.Ready -> Media3Player(
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
private fun VideoMetadata(key: VideoNavKey) {
    Column {
        Text(
            stringResource(R.string.feature_video_impl_video_label),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            key.title,
            modifier = Modifier.padding(top = 6.dp),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Row(modifier = Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                key.singer?.takeIf(String::isNotBlank)
                    ?: stringResource(R.string.feature_video_impl_unknown_artist),
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (key.durationMillis > 0) {
                Text(
                    stringResource(
                        R.string.feature_video_impl_duration,
                        key.durationMillis / 60_000,
                        key.durationMillis / 1_000 % 60,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
        Spacer(Modifier.height(2.dp))
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
