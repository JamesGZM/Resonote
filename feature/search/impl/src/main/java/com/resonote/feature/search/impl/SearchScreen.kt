package com.resonote.feature.search.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.resonote.core.designsystem.component.ResonoteMusicItem
import com.resonote.core.designsystem.component.ResonoteRemoteArtwork
import com.resonote.core.designsystem.component.ResonoteTextField
import com.resonote.core.model.AudioQuality
import com.resonote.core.model.ComplexSearchResult
import com.resonote.core.model.ContentFailure
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.SearchAlbum
import com.resonote.core.model.SearchArtist
import com.resonote.core.model.SearchMv
import com.resonote.core.model.SearchPlaylist

@Composable
fun SearchRoute(
    initialQuery: String,
    onBack: () -> Unit,
    onRecognitionClick: (() -> Unit)?,
    onSongClick: (OnlineSong) -> Unit,
    onSongMoreClick: ((OnlineSong) -> Unit)?,
    onPlaylistClick: ((String) -> Unit)?,
    onAlbumClick: ((SearchAlbum) -> Unit)?,
    onArtistClick: ((SearchArtist) -> Unit)?,
    onMvClick: ((SearchMv) -> Unit)?,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(initialQuery) {
        if (initialQuery.isNotBlank() && state.query.isBlank()) {
            viewModel.updateQuery(initialQuery)
            viewModel.submit()
        }
    }
    SearchScreen(
        state = state,
        onQueryChange = viewModel::updateQuery,
        onSubmit = viewModel::submit,
        onRetry = viewModel::retry,
        onSelectCategory = viewModel::selectCategory,
        onLoadMore = viewModel::loadMore,
        onRemoveHistory = viewModel::removeHistory,
        onClearHistory = viewModel::clearHistory,
        onBack = onBack,
        onRecognitionClick = onRecognitionClick,
        onSongClick = onSongClick,
        onSongMoreClick = onSongMoreClick,
        onPlaylistClick = onPlaylistClick,
        onAlbumClick = onAlbumClick,
        onArtistClick = onArtistClick,
        onMvClick = onMvClick,
    )
}

@Composable
fun SearchScreen(
    state: SearchUiState,
    onQueryChange: (String) -> Unit,
    onSubmit: (String?) -> Unit,
    onRetry: () -> Unit,
    onSelectCategory: (SearchCategory) -> Unit,
    onLoadMore: () -> Unit,
    onRemoveHistory: (String) -> Unit,
    onClearHistory: () -> Unit,
    onBack: () -> Unit,
    onRecognitionClick: (() -> Unit)?,
    onSongClick: (OnlineSong) -> Unit,
    onSongMoreClick: ((OnlineSong) -> Unit)?,
    onPlaylistClick: ((String) -> Unit)?,
    onAlbumClick: ((SearchAlbum) -> Unit)?,
    onArtistClick: ((SearchArtist) -> Unit)?,
    onMvClick: ((SearchMv) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    Scaffold(modifier = modifier.fillMaxSize(), containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            SearchHeader(
                query = state.query,
                onQueryChange = onQueryChange,
                onBack = onBack,
                onRecognitionClick = onRecognitionClick,
                onSubmit = {
                    keyboard?.hide()
                    onSubmit(null)
                },
            )
            if (state.result !is SearchResultUiState.Idle) {
                SearchCategoryBar(
                    selectedCategory = state.selectedCategory,
                    onSelectCategory = onSelectCategory,
                )
            }
            when (val result = state.result) {
                SearchResultUiState.Idle -> SearchDiscovery(
                    history = state.history,
                    hotKeywords = state.hotKeywords.map { it.keyword },
                    suggestions = state.suggestions,
                    onRemoveHistory = onRemoveHistory,
                    onClearHistory = onClearHistory,
                    onKeywordClick = {
                        keyboard?.hide()
                        onSubmit(it)
                    },
                )
                is SearchResultUiState.Loading -> LoadingState(result.query)
                is SearchResultUiState.Empty -> EmptyState()
                is SearchResultUiState.Error -> ErrorState(result.failure, onRetry)
                is SearchResultUiState.Content -> SearchResults(
                    result = result.value,
                    onSelectCategory = onSelectCategory,
                    onLoadMore = onLoadMore,
                    onSongClick = onSongClick,
                    onSongMoreClick = onSongMoreClick,
                    onPlaylistClick = onPlaylistClick,
                    onAlbumClick = onAlbumClick,
                    onArtistClick = onArtistClick,
                    onMvClick = onMvClick,
                )
            }
        }
    }
}

@Composable
private fun SearchCategoryBar(
    selectedCategory: SearchCategory,
    onSelectCategory: (SearchCategory) -> Unit,
) {
    PrimaryScrollableTabRow(
        selectedTabIndex = selectedCategory.ordinal,
        edgePadding = 12.dp,
        divider = {},
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        SearchCategory.entries.forEach { category ->
            Tab(
                selected = selectedCategory == category,
                onClick = { onSelectCategory(category) },
                text = { Text(category.label()) },
            )
        }
    }
}

@Composable
private fun SearchCategory.label(): String = stringResource(
    when (this) {
        SearchCategory.ALL -> R.string.feature_search_impl_search_all
        SearchCategory.SONGS -> R.string.feature_search_impl_search_songs
        SearchCategory.PLAYLISTS -> R.string.feature_search_impl_search_playlists
        SearchCategory.ALBUMS -> R.string.feature_search_impl_search_albums
        SearchCategory.MVS -> R.string.feature_search_impl_search_mvs
        SearchCategory.ARTISTS -> R.string.feature_search_impl_search_artists
    },
)

@Composable
private fun SearchHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onRecognitionClick: (() -> Unit)?,
    onSubmit: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.feature_search_impl_search_back))
        }
        ResonoteTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            label = if (query.isBlank()) stringResource(R.string.feature_search_impl_search_hint) else "",
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = if (query.isBlank()) null else ({
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Rounded.Clear, stringResource(R.string.feature_search_impl_search_clear))
                }
            }),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
        )
        IconButton(onClick = { onRecognitionClick?.invoke() }, enabled = onRecognitionClick != null) {
            Icon(Icons.Rounded.Mic, stringResource(R.string.feature_search_impl_search_recognition))
        }
    }
}

@Composable
private fun SearchDiscovery(
    history: List<String>,
    hotKeywords: List<String>,
    suggestions: List<String>,
    onRemoveHistory: (String) -> Unit,
    onClearHistory: () -> Unit,
    onKeywordClick: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        if (suggestions.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.feature_search_impl_search_suggestions), Icons.Rounded.Search) }
            itemsIndexed(suggestions, key = { index, value -> "suggestion-$value-$index" }) { _, suggestion ->
                ListItem(
                    headlineContent = { Text(suggestion) },
                    leadingContent = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingContent = { Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null) },
                    modifier = Modifier.clickable { onKeywordClick(suggestion) },
                )
            }
        } else {
            if (history.isNotEmpty()) {
                item {
                    SectionTitle(
                        title = stringResource(R.string.feature_search_impl_search_history),
                        icon = Icons.Rounded.History,
                        actionLabel = stringResource(R.string.feature_search_impl_search_history_clear),
                        onAction = onClearHistory,
                    )
                }
                itemsIndexed(history, key = { index, value -> "history-$value-$index" }) { _, query ->
                    ListItem(
                        headlineContent = { Text(query) },
                        leadingContent = { Icon(Icons.Rounded.History, contentDescription = null) },
                        trailingContent = {
                            IconButton(onClick = { onRemoveHistory(query) }) {
                                Icon(
                                    Icons.Rounded.DeleteOutline,
                                    stringResource(R.string.feature_search_impl_search_history_remove, query),
                                )
                            }
                        },
                        modifier = Modifier.clickable { onKeywordClick(query) },
                    )
                }
            }
            if (hotKeywords.isNotEmpty()) {
                item { SectionTitle(stringResource(R.string.feature_search_impl_search_hot), Icons.Rounded.GraphicEq) }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        itemsIndexed(hotKeywords, key = { index, value -> "hot-$value-$index" }) { _, keyword ->
                            AssistChip(onClick = { onKeywordClick(keyword) }, label = { Text(keyword) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingState(query: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator()
            Text(stringResource(R.string.feature_search_impl_search_loading, query), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EmptyState() = MessageState(
    icon = Icons.Rounded.Search,
    title = stringResource(R.string.feature_search_impl_search_empty_title),
    body = stringResource(R.string.feature_search_impl_search_empty_body),
)

@Composable
private fun ErrorState(failure: ContentFailure, onRetry: () -> Unit) {
    val body = when (failure) {
        ContentFailure.Network -> stringResource(R.string.feature_search_impl_search_error_network)
        ContentFailure.AuthenticationRequired -> stringResource(R.string.feature_search_impl_search_error_auth)
        else -> stringResource(R.string.feature_search_impl_search_error_generic)
    }
    MessageState(
        icon = Icons.Rounded.Search,
        title = stringResource(R.string.feature_search_impl_search_error_title),
        body = body,
        action = { Button(onClick = onRetry) { Text(stringResource(R.string.feature_search_impl_search_retry)) } },
    )
}

@Composable
private fun MessageState(
    icon: ImageVector,
    title: String,
    body: String,
    action: (@Composable () -> Unit)? = null,
) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                Icon(icon, contentDescription = null, modifier = Modifier.padding(20.dp).size(32.dp))
            }
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
            action?.invoke()
        }
    }
}

@Composable
private fun SearchResults(
    result: SearchContentUiState,
    onSelectCategory: (SearchCategory) -> Unit,
    onLoadMore: () -> Unit,
    onSongClick: (OnlineSong) -> Unit,
    onSongMoreClick: ((OnlineSong) -> Unit)?,
    onPlaylistClick: ((String) -> Unit)?,
    onAlbumClick: ((SearchAlbum) -> Unit)?,
    onArtistClick: ((SearchArtist) -> Unit)?,
    onMvClick: ((SearchMv) -> Unit)?,
) {
    when (result) {
        is SearchContentUiState.Aggregate -> AggregateSearchResults(
            result = result.value,
            onSelectCategory = onSelectCategory,
            onSongClick = onSongClick,
            onSongMoreClick = onSongMoreClick,
            onPlaylistClick = onPlaylistClick,
            onAlbumClick = onAlbumClick,
            onArtistClick = onArtistClick,
            onMvClick = onMvClick,
        )
        is SearchContentUiState.Page -> PagedSearchResults(
            page = result,
            onLoadMore = onLoadMore,
            onSongClick = onSongClick,
            onSongMoreClick = onSongMoreClick,
            onPlaylistClick = onPlaylistClick,
            onAlbumClick = onAlbumClick,
            onArtistClick = onArtistClick,
            onMvClick = onMvClick,
        )
    }
}

@Composable
private fun AggregateSearchResults(
    result: ComplexSearchResult,
    onSelectCategory: (SearchCategory) -> Unit,
    onSongClick: (OnlineSong) -> Unit,
    onSongMoreClick: ((OnlineSong) -> Unit)?,
    onPlaylistClick: ((String) -> Unit)?,
    onAlbumClick: ((SearchAlbum) -> Unit)?,
    onArtistClick: ((SearchArtist) -> Unit)?,
    onMvClick: ((SearchMv) -> Unit)?,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        if (result.songs.isNotEmpty()) {
            item {
                SectionTitle(
                    stringResource(R.string.feature_search_impl_search_songs),
                    Icons.Rounded.MusicNote,
                    stringResource(R.string.feature_search_impl_search_see_all),
                ) { onSelectCategory(SearchCategory.SONGS) }
            }
            itemsIndexed(result.songs, key = { index, song -> "song-${song.hash}-$index" }) { _, song ->
                ResonoteMusicItem(
                    title = song.title,
                    supportingText = song.artist.orEmpty(),
                    duration = song.durationMillis.durationLabel(),
                    qualityLabel = song.quality.label(),
                    isVip = song.vip,
                    artworkUrl = song.coverUrl,
                    onClick = { onSongClick(song) },
                    onMoreClick = onSongMoreClick?.let { callback -> { callback(song) } },
                )
            }
        }
        if (result.artists.isNotEmpty()) {
            item {
                SectionTitle(
                    stringResource(R.string.feature_search_impl_search_artists),
                    Icons.Rounded.Person,
                    stringResource(R.string.feature_search_impl_search_see_all),
                ) { onSelectCategory(SearchCategory.ARTISTS) }
            }
            itemsIndexed(result.artists, key = { index, artist -> "artist-${artist.id}-$index" }) { _, artist ->
                EntityRow(
                    title = artist.name,
                    supporting = stringResource(R.string.feature_search_impl_search_artist_metadata, artist.songCount, artist.albumCount),
                    icon = Icons.Rounded.Person,
                    artworkUrl = artist.avatarUrl,
                    onClick = onArtistClick?.let { callback -> { callback(artist) } },
                )
            }
        }
        if (result.albums.isNotEmpty()) {
            item {
                SectionTitle(
                    stringResource(R.string.feature_search_impl_search_albums),
                    Icons.Rounded.Album,
                    stringResource(R.string.feature_search_impl_search_see_all),
                ) { onSelectCategory(SearchCategory.ALBUMS) }
            }
            itemsIndexed(result.albums, key = { index, album -> "album-${album.id}-$index" }) { _, album ->
                AlbumRow(album, onClick = onAlbumClick?.let { callback -> { callback(album) } })
            }
        }
        if (result.playlists.isNotEmpty()) {
            item {
                SectionTitle(
                    stringResource(R.string.feature_search_impl_search_playlists),
                    Icons.AutoMirrored.Rounded.PlaylistPlay,
                    stringResource(R.string.feature_search_impl_search_see_all),
                ) { onSelectCategory(SearchCategory.PLAYLISTS) }
            }
            itemsIndexed(
                items = result.playlists,
                key = { index, playlist -> "playlist-${playlist.id}-$index" },
            ) { _, playlist ->
                PlaylistRow(playlist, onClick = onPlaylistClick?.let { callback -> { callback(playlist.id) } })
            }
        }
        if (result.mvs.isNotEmpty()) {
            item {
                SectionTitle(
                    stringResource(R.string.feature_search_impl_search_mvs),
                    Icons.Rounded.VideoLibrary,
                    stringResource(R.string.feature_search_impl_search_see_all),
                ) { onSelectCategory(SearchCategory.MVS) }
            }
            itemsIndexed(result.mvs, key = { index, mv -> "mv-${mv.hash}-$index" }) { _, mv ->
                MvRow(mv, onClick = onMvClick?.let { callback -> { callback(mv) } })
            }
        }
    }
}

@Composable
private fun PagedSearchResults(
    page: SearchContentUiState.Page,
    onLoadMore: () -> Unit,
    onSongClick: (OnlineSong) -> Unit,
    onSongMoreClick: ((OnlineSong) -> Unit)?,
    onPlaylistClick: ((String) -> Unit)?,
    onAlbumClick: ((SearchAlbum) -> Unit)?,
    onArtistClick: ((SearchArtist) -> Unit)?,
    onMvClick: ((SearchMv) -> Unit)?,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        itemsIndexed(page.items, key = { index, item -> "page-${item.stableId}-$index" }) { _, item ->
            when (item) {
                is SearchResultItem.Song -> ResonoteMusicItem(
                    title = item.value.title,
                    supportingText = item.value.artist.orEmpty(),
                    duration = item.value.durationMillis.durationLabel(),
                    qualityLabel = item.value.quality.label(),
                    isVip = item.value.vip,
                    artworkUrl = item.value.coverUrl,
                    onClick = { onSongClick(item.value) },
                    onMoreClick = onSongMoreClick?.let { callback -> { callback(item.value) } },
                )
                is SearchResultItem.Playlist -> PlaylistRow(
                    item.value,
                    onPlaylistClick?.let { callback -> { callback(item.value.id) } },
                )
                is SearchResultItem.Album -> AlbumRow(
                    item.value,
                    onAlbumClick?.let { callback -> { callback(item.value) } },
                )
                is SearchResultItem.Artist -> EntityRow(
                    title = item.value.name,
                    supporting = stringResource(
                        R.string.feature_search_impl_search_artist_metadata,
                        item.value.songCount,
                        item.value.albumCount,
                    ),
                    icon = Icons.Rounded.Person,
                    artworkUrl = item.value.avatarUrl,
                    onClick = onArtistClick?.let { callback -> { callback(item.value) } },
                )
                is SearchResultItem.Mv -> MvRow(
                    item.value,
                    onMvClick?.let { callback -> { callback(item.value) } },
                )
            }
        }
        if (page.hasMore || page.isLoadingMore || page.loadMoreFailure != null) {
            item(key = "load-more") {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        page.isLoadingMore -> CircularProgressIndicator(modifier = Modifier.size(28.dp))
                        page.loadMoreFailure != null -> TextButton(onClick = onLoadMore) {
                            Text(stringResource(R.string.feature_search_impl_search_load_more_retry))
                        }
                        page.hasMore -> TextButton(onClick = onLoadMore) {
                            Text(stringResource(R.string.feature_search_impl_search_load_more))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(
    title: String,
    icon: ImageVector,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
            Icon(icon, contentDescription = null, modifier = Modifier.padding(8.dp).size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(
            title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
private fun AlbumRow(album: SearchAlbum, onClick: (() -> Unit)?) = EntityRow(
    title = album.name,
    supporting = stringResource(R.string.feature_search_impl_search_album_metadata, album.artist.orEmpty(), album.songCount),
    icon = Icons.Rounded.Album,
    artworkUrl = album.coverUrl,
    onClick = onClick,
)

@Composable
private fun PlaylistRow(playlist: SearchPlaylist, onClick: (() -> Unit)?) = EntityRow(
    title = playlist.name,
    supporting = stringResource(R.string.feature_search_impl_search_playlist_metadata, playlist.creator.orEmpty(), playlist.songCount),
    icon = Icons.AutoMirrored.Rounded.PlaylistPlay,
    artworkUrl = playlist.coverUrl,
    onClick = onClick,
)

@Composable
private fun MvRow(mv: SearchMv, onClick: (() -> Unit)?) {
    ListItem(
        headlineContent = {
            Text(mv.name, maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
        },
        supportingContent = {
            Text(
                mv.singer.orEmpty(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        leadingContent = {
            Box(
                modifier = Modifier.width(112.dp).height(63.dp)
                    .background(entityGradient(mv.name), MaterialTheme.shapes.medium),
                contentAlignment = Alignment.Center,
            ) {
                if (!mv.coverUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = mv.coverUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
                Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.52f), contentColor = Color.White) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.padding(6.dp).size(18.dp))
                }
                if (mv.durationMillis > 0) {
                    Surface(
                        modifier = Modifier.align(Alignment.BottomEnd).padding(5.dp),
                        shape = MaterialTheme.shapes.extraSmall,
                        color = Color.Black.copy(alpha = 0.66f),
                        contentColor = Color.White,
                    ) {
                        Text(
                            stringResource(
                                R.string.feature_search_impl_search_mv_duration,
                                mv.durationMillis / 60_000,
                                mv.durationMillis / 1_000 % 60,
                            ),
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        },
        trailingContent = onClick?.let {
            { Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null) }
        },
        modifier = if (onClick == null) Modifier else Modifier.clickable(onClick = onClick),
    )
    HorizontalDivider(modifier = Modifier.padding(start = 144.dp), color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun EntityRow(
    title: String,
    supporting: String,
    icon: ImageVector,
    artworkUrl: String? = null,
    onClick: (() -> Unit)?,
) {
    ListItem(
        headlineContent = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text(supporting, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingContent = {
            Box(
                modifier = Modifier.size(56.dp).background(entityGradient(title), MaterialTheme.shapes.medium),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = Color.White)
                if (!artworkUrl.isNullOrBlank()) {
                    ResonoteRemoteArtwork(
                        model = artworkUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        fallback = {},
                    )
                }
            }
        },
        trailingContent = onClick?.let {
            { Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null) }
        },
        modifier = if (onClick == null) Modifier else Modifier.clickable(onClick = onClick),
    )
    HorizontalDivider(modifier = Modifier.padding(start = 88.dp), color = MaterialTheme.colorScheme.outlineVariant)
}

private fun entityGradient(seed: String): Brush {
    val palettes = listOf(
        listOf(Color(0xFF5A061B), Color(0xFFE31353)),
        listOf(Color(0xFF042E48), Color(0xFF0879BC)),
        listOf(Color(0xFF20164B), Color(0xFF786EDB)),
        listOf(Color(0xFF123D36), Color(0xFF3A8068)),
    )
    return Brush.linearGradient(palettes[(seed.hashCode() and Int.MAX_VALUE) % palettes.size])
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
