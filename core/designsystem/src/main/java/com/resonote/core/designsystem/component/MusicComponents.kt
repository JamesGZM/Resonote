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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Album
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
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
    shape: Shape = ResonoteTokens.artworkShapes.compact,
    artwork: @Composable BoxScope.() -> Unit = {},
) {
    val placeholderColor = if (state == ResonoteArtworkState.LOADED) {
        Color.Transparent
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest
    }
    Box(
        modifier = modifier
            .clip(shape)
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
fun ResonoteRemoteArtwork(
    model: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    fallback: @Composable BoxScope.() -> Unit = {
        Box(
            modifier = Modifier.matchParentSize().background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            ArtworkPlaceholderMarks()
        }
    },
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        fallback()
        model?.takeIf(String::isNotBlank)?.let { url ->
            AsyncImage(
                model = url,
                contentDescription = contentDescription,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
            )
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
    artworkUrl: String? = null,
    artwork: @Composable BoxScope.() -> Unit = { DefaultSongArtwork(title) },
    qualityLabel: String? = null,
    isVip: Boolean = false,
    isPlaying: Boolean = false,
    isSelected: Boolean = false,
    enabled: Boolean = true,
) {
    val effectiveArtworkState = if (!artworkUrl.isNullOrBlank()) ResonoteArtworkState.LOADED else artworkState
    val colors = MaterialTheme.colorScheme
    val containerColor = when {
        isPlaying -> lerp(colors.surface, colors.primary, 0.08f)
        isSelected -> lerp(colors.surface, colors.primary, 0.06f)
        else -> Color.Transparent
    }
    val titleColor = if (isPlaying) colors.primary else colors.onSurface
    val statusSlotWidth = with(LocalDensity.current) {
        when {
            fontScale >= 1.75f -> 56.dp
            fontScale >= 1.25f -> 48.dp
            else -> 44.dp
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 80.dp)
            .clip(MaterialTheme.shapes.large)
            .background(containerColor)
            .semantics { selected = isSelected }
            .clickable(enabled = enabled, onClick = onClick)
            .padding(
                start = 8.dp,
                top = 8.dp,
                end = if (onMoreClick == null) 8.dp else 0.dp,
                bottom = 8.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ResonoteArtwork(
            state = effectiveArtworkState,
            contentDescription = stringResource(R.string.core_designsystem_song_artwork, title),
            modifier = Modifier.size(64.dp),
            shape = ResonoteTokens.artworkShapes.standard,
            artwork = {
                if (artworkUrl.isNullOrBlank()) {
                    artwork()
                } else {
                    ResonoteRemoteArtwork(
                        model = artworkUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        fallback = artwork,
                    )
                }
            },
        )
        Spacer(Modifier.width(12.dp))
        if (effectiveArtworkState == ResonoteArtworkState.LOADING) {
            LoadingMusicText(modifier = Modifier.weight(1f))
        } else {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = title,
                        modifier = Modifier.weight(1f),
                        color = titleColor,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                    qualityLabel?.takeIf(String::isNotBlank)?.let { ResonoteQualityBadge(it) }
                    if (isVip) ResonoteVipBadge()
                }
                Text(
                    text = supportingText,
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (onMoreClick == null) 0.dp else (-8).dp),
        ) {
            Box(
                modifier = Modifier.width(statusSlotWidth),
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
                        text = if (effectiveArtworkState == ResonoteArtworkState.LOADING) "--:--" else duration,
                        color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium,
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
            }
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
data class ResonotePlaylistMetadata(val title: String, val playCount: String? = null)

@Composable
fun ResonotePlaylistItem(
    metadata: ResonotePlaylistMetadata,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    artworkState: ResonoteArtworkState = ResonoteArtworkState.LOADED,
    artworkUrl: String? = null,
    artwork: @Composable BoxScope.() -> Unit = { DefaultPlaylistArtwork(metadata.title) },
    enabled: Boolean = true,
) {
    val effectiveArtworkState = if (!artworkUrl.isNullOrBlank()) ResonoteArtworkState.LOADED else artworkState
    Column(
        modifier = modifier
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Box {
            ResonoteArtwork(
                state = effectiveArtworkState,
                contentDescription = stringResource(R.string.core_designsystem_playlist_artwork, metadata.title),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(ResonoteTokens.artworkShapes.standard),
                artwork = {
                    if (artworkUrl.isNullOrBlank()) {
                        artwork()
                    } else {
                        ResonoteRemoteArtwork(
                            model = artworkUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            fallback = artwork,
                        )
                    }
                },
            )
            if (effectiveArtworkState == ResonoteArtworkState.LOADED && metadata.playCount != null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp),
                    color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.58f),
                    contentColor = ResonoteTokens.systemColors.onScrim,
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
        if (effectiveArtworkState == ResonoteArtworkState.LOADING) {
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

@Composable
private fun DefaultSongArtwork(seed: String) {
    DefaultArtwork(seed) {
        Icon(
            imageVector = Icons.Rounded.Album,
            contentDescription = null,
            modifier = Modifier.size(30.dp),
            tint = ResonoteTokens.systemColors.onMediaCanvas.copy(alpha = 0.9f),
        )
    }
}

@Composable
private fun DefaultPlaylistArtwork(seed: String) {
    DefaultArtwork(seed) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = ResonoteTokens.systemColors.onMediaCanvas.copy(alpha = 0.9f),
        )
    }
}

@Composable
private fun DefaultArtwork(seed: String, icon: @Composable () -> Unit) {
    val palettes = listOf(
        listOf(Color(0xFF5A061B), Color(0xFFE31353), Color(0xFFFF8DA9)),
        listOf(Color(0xFF042E48), Color(0xFF0879BC), Color(0xFFBBD9F4)),
        listOf(Color(0xFF20164B), Color(0xFF786EDB), Color(0xFFF4A9BC)),
        listOf(Color(0xFF6F2E19), Color(0xFFE38A52), Color(0xFFFFD9A8)),
        listOf(Color(0xFF123D36), Color(0xFF3A8068), Color(0xFFC6D9A8)),
    )
    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.linearGradient(palettes[(seed.hashCode() and Int.MAX_VALUE) % palettes.size]),
        ),
        contentAlignment = Alignment.Center,
    ) {
        icon()
    }
}
