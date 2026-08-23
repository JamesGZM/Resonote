package com.resonote.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.resonote.core.designsystem.R
import com.resonote.core.designsystem.tokens.ResonoteTokens

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
    heroKey: String? = null,
    artworkAspectRatio: Float = 1f,
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
    heroKey = heroKey,
    artworkAspectRatio = artworkAspectRatio,
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
    heroKey: String? = null,
    artworkAspectRatio: Float = 1f,
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
                        .resonoteHero(heroKey)
                        .fillMaxWidth()
                        .aspectRatio(artworkAspectRatio)
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
    heroKey: String? = null,
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
                modifier = Modifier.resonoteHeroElement(heroKey).fillMaxWidth().aspectRatio(1f),
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
    heroKey: String? = null,
    enabled: Boolean = true,
) {
    val effectiveArtworkState = if (!artworkUrl.isNullOrBlank()) ResonoteArtworkState.LOADED else artworkState
    ResonotePlainAction(onClick = onClick, modifier = modifier, enabled = enabled) {
        Column {
            Box {
                ResonoteArtwork(
                    state = effectiveArtworkState,
                    contentDescription = stringResource(R.string.core_designsystem_video_artwork, metadata.title),
                    modifier = Modifier.resonoteHero(heroKey).fillMaxWidth().aspectRatio(16f / 9f),
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
