package com.resonote.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.resonote.core.designsystem.R
import com.resonote.core.designsystem.tokens.ResonoteTokens

enum class ResonoteArtworkState {
    LOADED,
    LOADING,
    MISSING,
}

@Composable
fun ResonoteArtwork(
    state: ResonoteArtworkState,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    artwork: @Composable BoxScope.() -> Unit = {},
) {
    val placeholderColor = when (state) {
        ResonoteArtworkState.LOADING -> MaterialTheme.colorScheme.surfaceContainerHigh
        ResonoteArtworkState.MISSING -> MaterialTheme.colorScheme.surfaceContainerHighest
        ResonoteArtworkState.LOADED -> Color.Transparent
    }
    Box(
        modifier = modifier
            .clip(ResonoteTokens.artworkShapes.compact)
            .background(placeholderColor)
            .semantics {
                if (contentDescription != null) this.contentDescription = contentDescription
            },
        contentAlignment = Alignment.Center,
    ) {
        if (state == ResonoteArtworkState.LOADED) {
            artwork()
        } else {
            ArtworkPlaceholderMarks()
        }
    }
}

@Composable
private fun ArtworkPlaceholderMarks() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ResonoteTokens.spacing.space2),
    ) {
        Box(
            Modifier
                .width(32.dp)
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.22f)),
        )
        Box(
            Modifier
                .width(20.dp)
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.16f)),
        )
    }
}

@Composable
fun ResonoteQualityBadge(label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.heightIn(min = 20.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = MaterialTheme.shapes.extraSmall,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
fun ResonoteVipBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.heightIn(min = 20.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.extraSmall,
    ) {
        Text(
            text = "VIP",
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
        )
    }
}

@Composable
fun ResonoteMusicItem(
    title: String,
    supportingText: String,
    duration: String,
    onClick: () -> Unit,
    onMoreClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    artworkState: ResonoteArtworkState = ResonoteArtworkState.LOADED,
    artwork: @Composable BoxScope.() -> Unit = {},
    qualityLabel: String? = null,
    isVip: Boolean = false,
    isPlaying: Boolean = false,
    isSelected: Boolean = false,
    enabled: Boolean = true,
) {
    val containerColor = when {
        isPlaying -> MaterialTheme.colorScheme.primaryContainer
        isSelected -> MaterialTheme.colorScheme.secondaryContainer
        else -> Color.Transparent
    }
    val titleColor = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clip(MaterialTheme.shapes.large)
            .background(containerColor)
            .semantics { selected = isSelected }
            .clickable(enabled = enabled, onClick = onClick)
            .padding(start = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ResonoteArtwork(
            state = artworkState,
            contentDescription = stringResource(R.string.core_designsystem_song_artwork, title),
            modifier = Modifier.size(56.dp),
            artwork = artwork,
        )
        Spacer(Modifier.width(12.dp))
        if (artworkState == ResonoteArtworkState.LOADING) {
            LoadingMusicText(modifier = Modifier.weight(1f))
        } else {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        modifier = Modifier.weight(1f),
                        color = titleColor,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (qualityLabel != null) {
                        Spacer(Modifier.width(8.dp))
                        ResonoteQualityBadge(qualityLabel)
                    }
                    if (isVip) {
                        Spacer(Modifier.width(8.dp))
                        ResonoteVipBadge()
                    }
                }
                Text(
                    text = supportingText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Box(
            modifier = Modifier.width(48.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (isPlaying) {
                Icon(
                    imageVector = Icons.Rounded.Equalizer,
                    contentDescription = stringResource(R.string.core_designsystem_playing),
                    tint = MaterialTheme.colorScheme.primary,
                )
            } else {
                Text(
                    text = if (artworkState == ResonoteArtworkState.LOADING) "--:--" else duration,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                )
            }
        }
        if (onMoreClick != null) {
            ResonoteIconButton(
                label = stringResource(R.string.core_designsystem_more_actions, title),
                onClick = onMoreClick,
                enabled = enabled,
                icon = { Icon(Icons.Rounded.MoreVert, contentDescription = null) },
            )
        } else {
            Spacer(Modifier.width(16.dp))
        }
    }
}

@Composable
private fun LoadingMusicText(modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier
                .fillMaxWidth(0.56f)
                .height(12.dp)
                .clip(MaterialTheme.shapes.extraSmall)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.14f)),
        )
        Box(
            Modifier
                .fillMaxWidth(0.78f)
                .height(10.dp)
                .clip(MaterialTheme.shapes.extraSmall)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)),
        )
    }
}

@Immutable
data class ResonotePlaylistMetadata(
    val title: String,
    val playCount: String? = null,
)

@Composable
fun ResonotePlaylistItem(
    metadata: ResonotePlaylistMetadata,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    artworkState: ResonoteArtworkState = ResonoteArtworkState.LOADED,
    artwork: @Composable BoxScope.() -> Unit = {},
    enabled: Boolean = true,
) {
    Column(
        modifier = modifier
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Box {
            ResonoteArtwork(
                state = artworkState,
                contentDescription = stringResource(R.string.core_designsystem_playlist_artwork, metadata.title),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(ResonoteTokens.artworkShapes.standard),
                artwork = artwork,
            )
            if (artworkState == ResonoteArtworkState.LOADED && metadata.playCount != null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp),
                    color = Color.Black.copy(alpha = 0.58f),
                    contentColor = Color.White,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(metadata.playCount, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        if (artworkState == ResonoteArtworkState.LOADING) {
            Box(
                Modifier
                    .fillMaxWidth(0.8f)
                    .height(14.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.14f)),
            )
        } else {
            Text(
                text = metadata.title,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
