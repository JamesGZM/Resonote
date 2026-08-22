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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    leadingContent: (@Composable () -> Unit)? = null,
    trailingAction: (@Composable () -> Unit)? = null,
) {
    val effectiveArtworkState = if (!artworkUrl.isNullOrBlank()) ResonoteArtworkState.LOADED else artworkState
    val hasTrailingAction = trailingAction != null || onMoreClick != null
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
                end = if (hasTrailingAction) 0.dp else 8.dp,
                bottom = 8.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingContent != null) {
            leadingContent()
            Spacer(Modifier.width(8.dp))
        }
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
                if (effectiveArtworkState == ResonoteArtworkState.LOADED) {
                    ResonoteArtworkBadge(
                        qualityLabel = qualityLabel,
                        isVip = isVip,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(4.dp),
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
                Text(
                    text = title,
                    color = titleColor,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
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
            horizontalArrangement = Arrangement.spacedBy(if (hasTrailingAction) (-8).dp else 0.dp),
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
            if (trailingAction != null) {
                trailingAction()
            } else if (onMoreClick != null) {
                ResonoteIconButton(
                    label = stringResource(R.string.core_designsystem_more_actions, title),
                    onClick = onMoreClick,
                    enabled = enabled,
                    icon = {
                        Icon(
                            Icons.Rounded.MoreVert,
                            contentDescription = null,
                            tint = colors.onSurfaceVariant,
                        )
                    },
                )
            }
        }
    }
}

@Composable
fun ResonoteArtworkBadge(qualityLabel: String?, isVip: Boolean, modifier: Modifier = Modifier) {
    val label = listOfNotNull(
        qualityLabel?.toCompactQualityLabel(),
        "VIP".takeIf { isVip },
    ).joinToString(separator = " · ")
    if (label.isNotEmpty()) {
        Surface(
            modifier = modifier.testTag("resonote-artwork-badge"),
            color = Color.Black.copy(alpha = 0.62f),
            contentColor = Color.White,
            shape = RoundedCornerShape(4.dp),
        ) {
            val badgeFontSize = with(LocalDensity.current) { 8.dp.toSp() }
            val badgeLineHeight = with(LocalDensity.current) { 10.dp.toSp() }
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp),
                style = MaterialTheme.typography.labelSmall,
                fontSize = badgeFontSize,
                lineHeight = badgeLineHeight,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

private fun String.toCompactQualityLabel(): String? = when (val normalized = trim().uppercase()) {
    "LOSSLESS", "SQ" -> "SQ"
    "HIGH QUALITY", "HIGH_QUALITY", "HQ" -> "HQ"
    "HI-RES", "HI RES", "HIRES", "HIGH RESOLUTION", "HIGH_RESOLUTION", "HR" -> "HR"
    else -> normalized.takeIf { it.length <= 3 }
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
    val supportingText: String? = null,
)

@Immutable
data class ResonoteMediaCardMetadata(
    val title: String,
    val playCount: String? = null,
    val supportingText: String? = null,
)

@Immutable
data class ResonoteArtistMetadata(val title: String, val supportingText: String? = null)

@Immutable
data class ResonoteVideoMetadata(val title: String, val supportingText: String? = null, val duration: String? = null)

@Composable
fun ResonotePlaylistItem(
    metadata: ResonotePlaylistMetadata,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    artworkState: ResonoteArtworkState = ResonoteArtworkState.LOADED,
    artworkUrl: String? = null,
    artwork: @Composable BoxScope.() -> Unit = { DefaultPlaylistArtwork(metadata.title) },
    enabled: Boolean = true,
) = ResonoteMediaCardItem(
    metadata = ResonoteMediaCardMetadata(
        title = metadata.title,
        playCount = metadata.playCount,
        supportingText = metadata.supportingText,
    ),
    artworkContentDescription = stringResource(R.string.core_designsystem_playlist_artwork, metadata.title),
    onClick = onClick,
    modifier = modifier,
    artworkState = artworkState,
    artworkUrl = artworkUrl,
    artwork = artwork,
    enabled = enabled,
)

@Composable
fun ResonoteMediaCardItem(
    metadata: ResonoteMediaCardMetadata,
    artworkContentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    artworkState: ResonoteArtworkState = ResonoteArtworkState.LOADED,
    artworkUrl: String? = null,
    artwork: @Composable BoxScope.() -> Unit = { DefaultPlaylistArtwork(metadata.title) },
    enabled: Boolean = true,
) {
    val effectiveArtworkState = if (!artworkUrl.isNullOrBlank()) ResonoteArtworkState.LOADED else artworkState
    ResonotePlainAction(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
    ) {
        Column {
            Box {
                ResonoteArtwork(
                    state = effectiveArtworkState,
                    contentDescription = artworkContentDescription,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(ResonoteTokens.artworkShapes.hero),
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
                            .padding(ResonoteTokens.spacing.space2),
                        color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.58f),
                        contentColor = ResonoteTokens.systemColors.onScrim,
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                .offset(y = (-0.5).dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(1.dp))
                            Text(metadata.playCount, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
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
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = if (metadata.supportingText == null) 2 else 1,
                    overflow = TextOverflow.Ellipsis,
                )
                metadata.supportingText?.let {
                    Text(
                        text = it,
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
fun ResonoteArtistItem(
    metadata: ResonoteArtistMetadata,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    artworkState: ResonoteArtworkState = ResonoteArtworkState.LOADED,
    artworkUrl: String? = null,
    enabled: Boolean = true,
) {
    val effectiveArtworkState = if (!artworkUrl.isNullOrBlank()) ResonoteArtworkState.LOADED else artworkState
    ResonotePlainAction(onClick = onClick, modifier = modifier, enabled = enabled) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ResonoteArtwork(
                state = effectiveArtworkState,
                contentDescription = stringResource(R.string.core_designsystem_artist_artwork, metadata.title),
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                shape = CircleShape,
                artwork = {
                    ResonoteRemoteArtwork(
                        model = artworkUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                    )
                },
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = metadata.title,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            metadata.supportingText?.let { supportingText ->
                Text(
                    text = supportingText,
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun ResonoteVideoItem(
    metadata: ResonoteVideoMetadata,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    artworkState: ResonoteArtworkState = ResonoteArtworkState.LOADED,
    artworkUrl: String? = null,
    enabled: Boolean = true,
) {
    val effectiveArtworkState = if (!artworkUrl.isNullOrBlank()) ResonoteArtworkState.LOADED else artworkState
    ResonotePlainAction(onClick = onClick, modifier = modifier, enabled = enabled) {
        Column {
            Box {
                ResonoteArtwork(
                    state = effectiveArtworkState,
                    contentDescription = stringResource(R.string.core_designsystem_video_artwork, metadata.title),
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                    shape = ResonoteTokens.artworkShapes.hero,
                    artwork = {
                        ResonoteRemoteArtwork(
                            model = artworkUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                        )
                    },
                )
                if (effectiveArtworkState != ResonoteArtworkState.LOADING) {
                    Surface(
                        modifier = Modifier.align(Alignment.Center),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f),
                        contentColor = ResonoteTokens.systemColors.onScrim,
                    ) {
                        Icon(
                            Icons.Rounded.PlayArrow,
                            contentDescription = stringResource(R.string.core_designsystem_play_video, metadata.title),
                            modifier = Modifier.padding(6.dp).size(18.dp),
                        )
                    }
                    metadata.duration?.let { duration ->
                        Surface(
                            modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp),
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.66f),
                            contentColor = ResonoteTokens.systemColors.onScrim,
                        ) {
                            Text(
                                text = duration,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(7.dp))
            Text(
                text = metadata.title,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            metadata.supportingText?.let { supportingText ->
                Text(
                    text = supportingText,
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
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
