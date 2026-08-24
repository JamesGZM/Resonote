@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.resonote.feature.discover.impl

import android.icu.text.CompactDecimalFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.resonote.core.designsystem.component.ResonoteArtwork
import com.resonote.core.designsystem.component.ResonoteArtworkState
import com.resonote.core.designsystem.component.ResonoteErrorState
import com.resonote.core.designsystem.component.ResonoteHeroKeys
import com.resonote.core.designsystem.component.ResonotePlainAction
import com.resonote.core.designsystem.component.ResonoteRemoteArtwork
import com.resonote.core.designsystem.component.ResonoteShimmer
import com.resonote.core.designsystem.component.rememberResonoteShimmer
import com.resonote.core.designsystem.component.resonoteHero
import com.resonote.core.designsystem.component.resonoteShimmer
import com.resonote.core.model.Album
import com.resonote.core.model.AlbumRegion
import com.resonote.core.model.AudioQuality
import com.resonote.core.model.ContentFailure
import com.resonote.core.model.Ranking
import java.util.Locale

@Composable
internal fun RankingCard(ranking: Ranking, onClick: () -> Unit) {
    ResonotePlainAction(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.resonoteHero(ResonoteHeroKeys.ranking(ranking.id)).size(60.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.BarChart,
                    contentDescription = stringResource(
                        R.string.feature_discover_impl_discover_ranking_artwork,
                        ranking.title,
                    ),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!ranking.coverUrl.isNullOrBlank()) {
                    ResonoteRemoteArtwork(
                        model = ranking.coverUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        fallback = {},
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Text(
                ranking.title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun AlbumCard(album: Album, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ResonotePlainAction(onClick = onClick, modifier = modifier) {
        Column {
            ResonoteArtwork(
                state = if (album.coverUrl.isNullOrBlank()) {
                    ResonoteArtworkState.MISSING
                } else {
                    ResonoteArtworkState.LOADED
                },
                contentDescription = stringResource(R.string.feature_discover_impl_discover_album_artwork, album.name),
                modifier = Modifier.resonoteHero(ResonoteHeroKeys.album(album.id)).fillMaxWidth().height(164.dp),
            ) {
                ResonoteRemoteArtwork(
                    model = album.coverUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                album.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                stringResource(
                    R.string.feature_discover_impl_discover_album_metadata,
                    album.artist.orEmpty(),
                    album.songCount,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun LinearFilterLoading(modifier: Modifier = Modifier) {
    val shimmer = rememberResonoteShimmer("discover-filter-skeleton")
    Row(modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(4) {
            Spacer(Modifier.width(72.dp).height(32.dp).resonoteShimmer(shimmer, MaterialTheme.shapes.large))
        }
    }
}

@Composable
internal fun PaneError(failure: ContentFailure, onRetry: () -> Unit) {
    val body = when (failure) {
        ContentFailure.Network -> stringResource(R.string.feature_discover_impl_discover_error_network)
        ContentFailure.AuthenticationRequired -> stringResource(R.string.feature_discover_impl_discover_error_auth)
        else -> stringResource(R.string.feature_discover_impl_discover_error_generic)
    }
    ResonoteErrorState(
        onRetry = onRetry,
        message = body,
        title = stringResource(R.string.feature_discover_impl_discover_error_title),
        retryLabel = stringResource(R.string.feature_discover_impl_discover_retry),
        modifier = Modifier.fillMaxWidth().height(300.dp),
    )
}

@Composable
internal fun PlaylistSkeleton(shimmer: ResonoteShimmer) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        repeat(3) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(2) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Spacer(
                            Modifier.fillMaxWidth().aspectRatio(1f)
                                .resonoteShimmer(shimmer, MaterialTheme.shapes.large),
                        )
                        SkeletonLine(shimmer, 116.dp, 15.dp)
                        SkeletonLine(shimmer, 72.dp, 12.dp)
                    }
                }
            }
        }
    }
}

@Composable
internal fun RankingSkeleton(shimmer: ResonoteShimmer) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.size(60.dp).resonoteShimmer(shimmer, MaterialTheme.shapes.medium))
        Spacer(Modifier.width(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SkeletonLine(shimmer, 164.dp, 16.dp)
            SkeletonLine(shimmer, 96.dp, 11.dp)
        }
    }
}

@Composable
internal fun AlbumGridSkeleton(shimmer: ResonoteShimmer) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        repeat(3) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(2) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Spacer(
                            Modifier.fillMaxWidth().height(164.dp)
                                .resonoteShimmer(shimmer, MaterialTheme.shapes.large),
                        )
                        SkeletonLine(shimmer, 120.dp, 15.dp)
                        SkeletonLine(shimmer, 88.dp, 12.dp)
                    }
                }
            }
        }
    }
}

@Composable
internal fun SongListSkeleton(shimmer: ResonoteShimmer) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SkeletonLine(shimmer, 116.dp, 18.dp)
                SkeletonLine(shimmer, 152.dp, 11.dp)
            }
            SkeletonLine(shimmer, 64.dp, 14.dp)
        }
        repeat(7) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Spacer(Modifier.size(56.dp).resonoteShimmer(shimmer, MaterialTheme.shapes.medium))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SkeletonLine(shimmer, 176.dp, 15.dp)
                    SkeletonLine(shimmer, 108.dp, 12.dp)
                }
            }
        }
    }
}

@Composable
private fun SkeletonLine(shimmer: ResonoteShimmer, width: Dp, height: Dp) {
    Spacer(Modifier.width(width).height(height).resonoteShimmer(shimmer, MaterialTheme.shapes.small))
}

internal fun DiscoverSection.hasContent(state: DiscoverUiState): Boolean = when (this) {
    DiscoverSection.PLAYLISTS -> state.playlists is DiscoverPageState.Content
    DiscoverSection.RANKINGS -> state.rankings is DiscoverLoadState.Content
    DiscoverSection.ALBUMS -> state.albums is DiscoverLoadState.Content
    DiscoverSection.SONGS -> state.songs is DiscoverPageState.Content
}

@Composable
internal fun DiscoverSection.label(): String = when (this) {
    DiscoverSection.PLAYLISTS -> stringResource(R.string.feature_discover_impl_discover_playlists)
    DiscoverSection.RANKINGS -> stringResource(R.string.feature_discover_impl_discover_rankings)
    DiscoverSection.ALBUMS -> stringResource(R.string.feature_discover_impl_discover_albums)
    DiscoverSection.SONGS -> stringResource(R.string.feature_discover_impl_discover_songs)
}

@Composable
internal fun AlbumRegion.label(): String = when (this) {
    AlbumRegion.Chinese -> stringResource(R.string.feature_discover_impl_discover_chinese)
    AlbumRegion.Western -> stringResource(R.string.feature_discover_impl_discover_western)
    AlbumRegion.Japanese -> stringResource(R.string.feature_discover_impl_discover_japanese)
    AlbumRegion.Korean -> stringResource(R.string.feature_discover_impl_discover_korean)
}

internal fun PaddingValues.plusBottom(extra: Dp) = PaddingValues(
    start = calculateLeftPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
    top = calculateTopPadding(),
    end = calculateRightPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
    bottom = calculateBottomPadding() + extra,
)

internal fun Long.compactCount(): String = when {
    this >= 10_000 ->
        CompactDecimalFormat
            .getInstance(Locale.getDefault(), CompactDecimalFormat.CompactStyle.SHORT)
            .format(this)
    else -> toString()
}

internal fun Long.durationLabel(): String {
    val totalSeconds = coerceAtLeast(0) / 1_000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

internal fun AudioQuality.label(): String? = when (this) {
    AudioQuality.Standard -> null
    AudioQuality.HighQuality -> "HQ"
    AudioQuality.HighResolution -> "HI-RES"
    AudioQuality.Lossless -> "LOSSLESS"
}
