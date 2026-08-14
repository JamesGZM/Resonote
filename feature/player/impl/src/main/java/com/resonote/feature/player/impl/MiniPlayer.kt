package com.resonote.feature.player.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.resonote.core.designsystem.component.ResonoteArtwork
import com.resonote.core.designsystem.component.ResonoteArtworkBadge
import com.resonote.core.designsystem.component.ResonoteArtworkState
import com.resonote.core.designsystem.component.ResonoteIconButton
import com.resonote.core.designsystem.tokens.ResonoteTokens
import com.resonote.feature.player.impl.R
import coil3.compose.AsyncImage

@Immutable
data class MiniPlayerUiState(
    val mediaId: String,
    val title: String,
    val artist: String,
    val qualityLabel: String? = null,
    val isVip: Boolean = false,
    val isPlaying: Boolean = false,
    val progress: Float = 0f,
    val artworkColors: List<Color>,
    val coverUrl: String? = null,
)

@Composable
fun ResonoteMiniPlayer(
    state: MiniPlayerUiState,
    onOpenPlayer: () -> Unit,
    onTogglePlay: () -> Unit,
    onOpenQueue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerHeightModifier = if (LocalDensity.current.fontScale < 1.75f) {
        Modifier.height(72.dp)
    } else {
        Modifier.heightIn(min = 72.dp)
    }
    Surface(
        onClick = onOpenPlayer,
        modifier = modifier
            .fillMaxWidth()
            .then(containerHeightModifier),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp,
        shadowElevation = ResonoteTokens.elevation.level3.maximumShadow,
    ) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ResonoteArtwork(
                        state = ResonoteArtworkState.LOADED,
                        contentDescription = stringResource(R.string.feature_player_impl_artwork, state.title),
                        modifier = Modifier.size(56.dp),
                        shape = ResonoteTokens.artworkShapes.standard,
                    ) {
                        Box(
                            Modifier
                                .matchParentSize()
                                .background(Brush.linearGradient(state.artworkColors)),
                        ) {
                            if (!state.coverUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = state.coverUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.matchParentSize(),
                                )
                            }
                        }
                        ResonoteArtworkBadge(
                            qualityLabel = state.qualityLabel,
                            isVip = state.isVip,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(4.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = state.title,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = state.artist,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                ResonoteIconButton(
                    label = stringResource(
                        if (state.isPlaying) R.string.feature_player_impl_pause else R.string.feature_player_impl_play,
                    ),
                    onClick = onTogglePlay,
                    icon = {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                )
                ResonoteIconButton(
                    label = stringResource(R.string.feature_player_impl_queue),
                    onClick = onOpenQueue,
                    icon = {
                        Icon(
                            Icons.AutoMirrored.Rounded.QueueMusic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
            }
            LinearProgressIndicator(
                progress = { state.progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(2.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.Transparent,
            )
        }
    }
}
