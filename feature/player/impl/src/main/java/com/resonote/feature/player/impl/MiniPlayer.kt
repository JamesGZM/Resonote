package com.resonote.feature.player.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.SkipNext
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.resonote.core.designsystem.component.ResonoteArtwork
import com.resonote.core.designsystem.component.ResonoteArtworkState
import com.resonote.core.designsystem.component.ResonoteIconButton
import com.resonote.core.designsystem.component.ResonoteQualityBadge
import com.resonote.core.designsystem.component.ResonoteTonalIconButton
import com.resonote.core.designsystem.component.ResonoteVipBadge
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
    onNext: () -> Unit,
    onOpenQueue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        shadowElevation = ResonoteTokens.elevation.level3.defaultShadow,
    ) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 4.dp, top = 8.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onOpenPlayer),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ResonoteArtwork(
                        state = ResonoteArtworkState.LOADED,
                        contentDescription = stringResource(R.string.feature_player_impl_artwork, state.title),
                        modifier = Modifier.size(56.dp),
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
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = state.title,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (state.qualityLabel != null) {
                                Spacer(Modifier.width(4.dp))
                                ResonoteQualityBadge(state.qualityLabel)
                            }
                            if (state.isVip) {
                                Spacer(Modifier.width(4.dp))
                                ResonoteVipBadge()
                            }
                        }
                        Text(
                            text = state.artist,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                ResonoteTonalIconButton(
                    label = stringResource(
                        if (state.isPlaying) R.string.feature_player_impl_pause else R.string.feature_player_impl_play,
                    ),
                    onClick = onTogglePlay,
                    icon = {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = null,
                        )
                    },
                )
                ResonoteIconButton(
                    label = stringResource(R.string.feature_player_impl_next),
                    onClick = onNext,
                    icon = { Icon(Icons.Rounded.SkipNext, contentDescription = null) },
                )
                ResonoteIconButton(
                    label = stringResource(R.string.feature_player_impl_queue),
                    onClick = onOpenQueue,
                    icon = { Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = null) },
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
