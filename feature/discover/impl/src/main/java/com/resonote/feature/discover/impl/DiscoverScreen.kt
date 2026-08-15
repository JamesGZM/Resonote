@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.resonote.feature.discover.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resonote.core.designsystem.component.ResonoteArtwork
import com.resonote.core.designsystem.component.ResonoteArtworkState
import com.resonote.core.designsystem.component.ResonoteMusicItem
import com.resonote.core.designsystem.component.ResonotePlainAction
import com.resonote.core.designsystem.component.ResonotePlaylistItem
import com.resonote.core.designsystem.component.ResonotePlaylistMetadata
import com.resonote.core.designsystem.component.ResonoteRemoteArtwork
import com.resonote.core.designsystem.component.ResonoteTopAppBar
import com.resonote.core.designsystem.tokens.ResonoteTokens
import com.resonote.core.model.Album
import com.resonote.core.model.AlbumRegion
import com.resonote.core.model.AudioQuality
import com.resonote.core.model.ContentFailure
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.PlaylistCategory
import com.resonote.core.model.PlaylistSummary
import com.resonote.core.model.Ranking

@Composable
fun DiscoverRoute(
    bottomContentPadding: Dp,
    playingMediaId: String?,
    requestedSection: DiscoverSection? = null,
    onRequestedSectionConsumed: () -> Unit = {},
    onPlaylistClick: (PlaylistSummary) -> Unit,
    onRankingClick: (Ranking) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onPlaySongs: (List<OnlineSong>) -> Unit,
    onSongClick: (OnlineSong) -> Unit,
    onSongMoreClick: ((OnlineSong) -> Unit)?,
    viewModel: DiscoverViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(requestedSection) {
        requestedSection?.let {
            viewModel.selectSection(it)
            onRequestedSectionConsumed()
        }
    }
    DiscoverScreen(
        state = state,
        bottomContentPadding = bottomContentPadding,
        playingMediaId = playingMediaId,
        onSelectSection = viewModel::selectSection,
        onSelectPlaylistParent = viewModel::selectPlaylistParent,
        onSelectPlaylistCategory = viewModel::selectPlaylistCategory,
        onSelectAlbumRegion = viewModel::selectAlbumRegion,
        onRetryCategories = viewModel::retryCategories,
        onRetry = viewModel::retryCurrent,
        onLoadMore = viewModel::loadMore,
        onPlaylistClick = onPlaylistClick,
        onRankingClick = onRankingClick,
        onAlbumClick = onAlbumClick,
        onPlaySongs = onPlaySongs,
        onSongClick = onSongClick,
        onSongMoreClick = onSongMoreClick,
    )
}

@Composable
fun DiscoverScreen(
    state: DiscoverUiState,
    bottomContentPadding: Dp,
    playingMediaId: String?,
    onSelectSection: (DiscoverSection) -> Unit,
    onSelectPlaylistParent: (Int?) -> Unit,
    onSelectPlaylistCategory: (Int) -> Unit,
    onSelectAlbumRegion: (AlbumRegion?) -> Unit,
    onRetryCategories: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onPlaylistClick: (PlaylistSummary) -> Unit,
    onRankingClick: (Ranking) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onPlaySongs: (List<OnlineSong>) -> Unit,
    onSongClick: (OnlineSong) -> Unit,
    onSongMoreClick: ((OnlineSong) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val stateHolder = rememberSaveableStateHolder()
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                ResonoteTopAppBar(title = { Text(stringResource(R.string.feature_discover_impl_discover_title)) })
                PrimaryScrollableTabRow(
                    selectedTabIndex = state.selectedSection.ordinal,
                    edgePadding = 12.dp,
                    containerColor = MaterialTheme.colorScheme.background,
                ) {
                    DiscoverSection.entries.forEach { section ->
                        Tab(
                            selected = section == state.selectedSection,
                            onClick = { onSelectSection(section) },
                            text = { Text(section.label()) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            stateHolder.SaveableStateProvider(state.selectedSection.name) {
                when (state.selectedSection) {
                    DiscoverSection.PLAYLISTS -> PlaylistPane(
                        state = state,
                        bottomContentPadding = bottomContentPadding,
                        onSelectParent = onSelectPlaylistParent,
                        onSelectCategory = onSelectPlaylistCategory,
                        onRetryCategories = onRetryCategories,
                        onRetry = onRetry,
                        onLoadMore = onLoadMore,
                        onPlaylistClick = onPlaylistClick,
                    )
                    DiscoverSection.RANKINGS -> RankingPane(
                        rankings = state.rankings,
                        bottomContentPadding = bottomContentPadding,
                        onRetry = onRetry,
                        onRankingClick = onRankingClick,
                    )
                    DiscoverSection.ALBUMS -> AlbumPane(
                        albums = state.albums,
                        selectedRegion = state.selectedAlbumRegion,
                        bottomContentPadding = bottomContentPadding,
                        onSelectRegion = onSelectAlbumRegion,
                        onRetry = onRetry,
                        onAlbumClick = onAlbumClick,
                    )
                    DiscoverSection.SONGS -> SongPane(
                        songs = state.songs,
                        playingMediaId = playingMediaId,
                        bottomContentPadding = bottomContentPadding,
                        onRetry = onRetry,
                        onLoadMore = onLoadMore,
                        onPlaySongs = onPlaySongs,
                        onSongClick = onSongClick,
                        onSongMoreClick = onSongMoreClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistPane(
    state: DiscoverUiState,
    bottomContentPadding: Dp,
    onSelectParent: (Int?) -> Unit,
    onSelectCategory: (Int) -> Unit,
    onRetryCategories: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onPlaylistClick: (PlaylistSummary) -> Unit,
) {
    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().testTag("discover-playlists"),
        contentPadding = PaddingValues(bottom = bottomContentPadding),
    ) {
        item(key = "category-filters") {
            PlaylistFilters(
                categories = state.categories,
                selectedParentId = state.selectedParentCategoryId,
                selectedCategoryId = state.selectedPlaylistCategoryId,
                onSelectParent = onSelectParent,
                onSelectCategory = onSelectCategory,
                onRetry = onRetryCategories,
            )
        }
        when (val page = state.playlists) {
            DiscoverPageState.Idle,
            DiscoverPageState.Loading,
            -> item(key = "loading") { PaneLoading() }
            DiscoverPageState.Empty -> item(key = "empty") {
                PaneMessage(
                    Icons.Rounded.MusicNote,
                    stringResource(R.string.feature_discover_impl_discover_empty_playlists),
                )
            }
            is DiscoverPageState.Error -> item(key = "error") { PaneError(page.failure, onRetry) }
            is DiscoverPageState.Content -> {
                itemsIndexed(
                    items = page.items.chunked(2),
                    key = { index, row -> "row-${row.first().id}-$index" },
                ) { _, row ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        row.forEach { playlist ->
                            ResonotePlaylistItem(
                                metadata = ResonotePlaylistMetadata(
                                    title = playlist.title,
                                    playCount = playlist.playCount?.compactCount(),
                                ),
                                onClick = { onPlaylistClick(playlist) },
                                modifier = Modifier.weight(1f),
                                artworkState = if (playlist.coverUrl.isNullOrBlank()) {
                                    ResonoteArtworkState.MISSING
                                } else {
                                    ResonoteArtworkState.LOADED
                                },
                                artworkUrl = playlist.coverUrl,
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                if (page.hasMore || page.isLoadingMore || page.loadMoreFailure != null) {
                    item(key = "load-more") { PageFooter(page.isLoadingMore, page.loadMoreFailure, onLoadMore) }
                }
            }
        }
    }
}

@Composable
private fun PlaylistFilters(
    categories: DiscoverLoadState<List<PlaylistCategory>>,
    selectedParentId: Int?,
    selectedCategoryId: Int,
    onSelectParent: (Int?) -> Unit,
    onSelectCategory: (Int) -> Unit,
    onRetry: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 12.dp)) {
        when (categories) {
            DiscoverLoadState.Idle,
            DiscoverLoadState.Loading,
            -> LinearFilterLoading()
            DiscoverLoadState.Empty -> Unit
            is DiscoverLoadState.Error -> TextButton(
                onClick = onRetry,
                modifier = Modifier.padding(horizontal = 8.dp),
            ) { Text(stringResource(R.string.feature_discover_impl_discover_categories_unavailable)) }
            is DiscoverLoadState.Content -> {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item(key = "recommended") {
                        FilterChip(
                            selected = selectedParentId == null,
                            onClick = { onSelectParent(null) },
                            label = { Text(stringResource(R.string.feature_discover_impl_discover_recommended)) },
                        )
                    }
                    itemsIndexed(
                        items = categories.value,
                        key = { index, category -> "parent-${category.tagId}-$index" },
                    ) { _, category ->
                        FilterChip(
                            selected = category.tagId == selectedParentId,
                            onClick = { onSelectParent(category.tagId) },
                            label = { Text(category.name) },
                        )
                    }
                }
                categories.value.firstOrNull { it.tagId == selectedParentId }
                    ?.children?.takeIf(List<PlaylistCategory>::isNotEmpty)?.let { children ->
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            itemsIndexed(
                                items = children,
                                key = { index, category -> "child-${category.tagId}-$index" },
                            ) { _, category ->
                                FilterChip(
                                    selected = category.tagId == selectedCategoryId,
                                    onClick = { onSelectCategory(category.tagId) },
                                    label = { Text(category.name) },
                                )
                            }
                        }
                    }
            }
        }
    }
}

@Composable
private fun RankingPane(
    rankings: DiscoverLoadState<List<Ranking>>,
    bottomContentPadding: Dp,
    onRetry: () -> Unit,
    onRankingClick: (Ranking) -> Unit,
) {
    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().testTag("discover-rankings"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp).plusBottom(bottomContentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when (rankings) {
            DiscoverLoadState.Idle,
            DiscoverLoadState.Loading,
            -> item { PaneLoading() }
            DiscoverLoadState.Empty -> item {
                PaneMessage(
                    Icons.Rounded.BarChart,
                    stringResource(R.string.feature_discover_impl_discover_empty_rankings),
                )
            }
            is DiscoverLoadState.Error -> item { PaneError(rankings.failure, onRetry) }
            is DiscoverLoadState.Content -> itemsIndexed(
                items = rankings.value,
                key = { index, item -> "${item.id}-$index" },
            ) { index, ranking ->
                RankingCard(index + 1, ranking, onClick = { onRankingClick(ranking) })
            }
        }
    }
}

@Composable
private fun AlbumPane(
    albums: DiscoverLoadState<List<Album>>,
    selectedRegion: AlbumRegion?,
    bottomContentPadding: Dp,
    onSelectRegion: (AlbumRegion?) -> Unit,
    onRetry: () -> Unit,
    onAlbumClick: (Album) -> Unit,
) {
    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().testTag("discover-albums"),
        contentPadding = PaddingValues(bottom = bottomContentPadding),
    ) {
        item(key = "regions") {
            LazyRow(
                modifier = Modifier.padding(vertical = 12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "all") {
                    FilterChip(
                        selected = selectedRegion == null,
                        onClick = { onSelectRegion(null) },
                        label = { Text(stringResource(R.string.feature_discover_impl_discover_recommended)) },
                    )
                }
                items(AlbumRegion.entries, key = { it.name }) { region ->
                    FilterChip(
                        selected = selectedRegion == region,
                        onClick = { onSelectRegion(region) },
                        label = { Text(region.label()) },
                    )
                }
            }
        }
        when (albums) {
            DiscoverLoadState.Idle,
            DiscoverLoadState.Loading,
            -> item { PaneLoading() }
            DiscoverLoadState.Empty -> item {
                PaneMessage(Icons.Rounded.Album, stringResource(R.string.feature_discover_impl_discover_empty_albums))
            }
            is DiscoverLoadState.Error -> item { PaneError(albums.failure, onRetry) }
            is DiscoverLoadState.Content -> {
                val filtered = albums.value.filter { selectedRegion == null || it.region == selectedRegion }
                if (filtered.isEmpty()) {
                    item {
                        PaneMessage(
                            Icons.Rounded.Album,
                            stringResource(R.string.feature_discover_impl_discover_empty_albums),
                        )
                    }
                } else {
                    itemsIndexed(
                        items = filtered.chunked(2),
                        key = { index, row -> "album-row-${row.first().id}-$index" },
                    ) { _, row ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            row.forEach { album -> AlbumCard(album, { onAlbumClick(album) }, Modifier.weight(1f)) }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SongPane(
    songs: DiscoverPageState<OnlineSong>,
    playingMediaId: String?,
    bottomContentPadding: Dp,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onPlaySongs: (List<OnlineSong>) -> Unit,
    onSongClick: (OnlineSong) -> Unit,
    onSongMoreClick: ((OnlineSong) -> Unit)?,
) {
    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().testTag("discover-songs"),
        contentPadding = PaddingValues(bottom = bottomContentPadding),
    ) {
        when (songs) {
            DiscoverPageState.Idle,
            DiscoverPageState.Loading,
            -> item { PaneLoading() }
            DiscoverPageState.Empty -> item {
                PaneMessage(
                    Icons.Rounded.MusicNote,
                    stringResource(R.string.feature_discover_impl_discover_empty_songs),
                )
            }
            is DiscoverPageState.Error -> item { PaneError(songs.failure, onRetry) }
            is DiscoverPageState.Content -> {
                item(key = "play-all") {
                    Button(
                        onClick = { onPlaySongs(songs.items) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                        contentPadding = PaddingValues(vertical = 14.dp),
                    ) {
                        Icon(Icons.Rounded.PlayArrow, null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.feature_discover_impl_discover_play_all))
                    }
                }
                itemsIndexed(
                    items = songs.items,
                    key = { index, song -> "${song.hash}-$index" },
                ) { _, song ->
                    ResonoteMusicItem(
                        title = song.title,
                        supportingText = song.artist.orEmpty(),
                        duration = song.durationMillis.durationLabel(),
                        qualityLabel = song.quality.label(),
                        isVip = song.vip,
                        isPlaying = song.hash == playingMediaId,
                        artworkUrl = song.coverUrl,
                        onClick = { onSongClick(song) },
                        onMoreClick = onSongMoreClick?.let { callback -> { callback(song) } },
                    )
                }
                if (songs.hasMore || songs.isLoadingMore || songs.loadMoreFailure != null) {
                    item(key = "load-more") { PageFooter(songs.isLoadingMore, songs.loadMoreFailure, onLoadMore) }
                }
            }
        }
    }
}

@Composable
private fun RankingCard(position: Int, ranking: Ranking, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(72.dp).clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                if (!ranking.coverUrl.isNullOrBlank()) {
                    ResonoteRemoteArtwork(
                        model = ranking.coverUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        fallback = {},
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.52f),
                    contentColor = ResonoteTokens.systemColors.onScrim,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(
                        position.toString().padStart(2, '0'),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Text(
                ranking.title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                Icons.Rounded.BarChart,
                stringResource(R.string.feature_discover_impl_discover_ranking_artwork, ranking.title),
            )
        }
    }
}

@Composable
private fun AlbumCard(album: Album, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ResonotePlainAction(onClick = onClick, modifier = modifier) {
        Column {
            ResonoteArtwork(
                state = if (album.coverUrl.isNullOrBlank()) {
                    ResonoteArtworkState.MISSING
                } else {
                    ResonoteArtworkState.LOADED
                },
                contentDescription = stringResource(R.string.feature_discover_impl_discover_album_artwork, album.name),
                modifier = Modifier.fillMaxWidth().height(164.dp),
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
private fun PaneLoading() {
    Box(Modifier.fillMaxWidth().height(280.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
private fun LinearFilterLoading() {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(4) {
            Box(
                Modifier.width(
                    72.dp,
                ).height(
                    32.dp,
                ).clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surfaceContainerHigh),
            )
        }
    }
}

@Composable
private fun PaneError(failure: ContentFailure, onRetry: () -> Unit) {
    val body = when (failure) {
        ContentFailure.Network -> stringResource(R.string.feature_discover_impl_discover_error_network)
        ContentFailure.AuthenticationRequired -> stringResource(R.string.feature_discover_impl_discover_error_auth)
        else -> stringResource(R.string.feature_discover_impl_discover_error_generic)
    }
    PaneMessage(
        icon = Icons.Rounded.CloudOff,
        text = body,
        title = stringResource(R.string.feature_discover_impl_discover_error_title),
        action = { Button(onClick = onRetry) { Text(stringResource(R.string.feature_discover_impl_discover_retry)) } },
    )
}

@Composable
private fun PaneMessage(
    icon: ImageVector,
    text: String,
    title: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Box(Modifier.fillMaxWidth().height(300.dp).padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            title?.let { Text(it, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }
            Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            action?.invoke()
        }
    }
}

@Composable
private fun PageFooter(isLoading: Boolean, failure: ContentFailure?, onLoadMore: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
        when {
            isLoading -> CircularProgressIndicator(Modifier.size(28.dp))
            failure != null -> TextButton(onClick = onLoadMore) {
                Text(stringResource(R.string.feature_discover_impl_discover_load_more_retry))
            }
            else -> TextButton(onClick = onLoadMore) {
                Text(stringResource(R.string.feature_discover_impl_discover_load_more))
            }
        }
    }
}

@Composable
private fun DiscoverSection.label(): String = when (this) {
    DiscoverSection.PLAYLISTS -> stringResource(R.string.feature_discover_impl_discover_playlists)
    DiscoverSection.RANKINGS -> stringResource(R.string.feature_discover_impl_discover_rankings)
    DiscoverSection.ALBUMS -> stringResource(R.string.feature_discover_impl_discover_albums)
    DiscoverSection.SONGS -> stringResource(R.string.feature_discover_impl_discover_songs)
}

@Composable
private fun AlbumRegion.label(): String = when (this) {
    AlbumRegion.Chinese -> stringResource(R.string.feature_discover_impl_discover_chinese)
    AlbumRegion.Western -> stringResource(R.string.feature_discover_impl_discover_western)
    AlbumRegion.Japanese -> stringResource(R.string.feature_discover_impl_discover_japanese)
    AlbumRegion.Korean -> stringResource(R.string.feature_discover_impl_discover_korean)
}

private fun PaddingValues.plusBottom(extra: Dp) = PaddingValues(
    start = calculateLeftPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
    top = calculateTopPadding(),
    end = calculateRightPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
    bottom = calculateBottomPadding() + extra,
)

private fun Long.compactCount(): String = when {
    this >= 100_000_000 -> "%.1f亿".format(this / 100_000_000.0)
    this >= 10_000 -> "%.1f万".format(this / 10_000.0)
    else -> toString()
}

private fun Long.durationLabel(): String {
    val totalSeconds = coerceAtLeast(0) / 1_000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

private fun AudioQuality.label(): String? = when (this) {
    AudioQuality.Standard -> null
    AudioQuality.HighQuality -> "HQ"
    AudioQuality.HighResolution -> "HI-RES"
    AudioQuality.Lossless -> "LOSSLESS"
}
