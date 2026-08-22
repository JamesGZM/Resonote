package com.resonote.feature.search.impl

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resonote.core.designsystem.component.ResonoteArtistItem
import com.resonote.core.designsystem.component.ResonoteArtistMetadata
import com.resonote.core.designsystem.component.ResonoteArtworkState
import com.resonote.core.designsystem.component.ResonoteFilterPill
import com.resonote.core.designsystem.component.ResonoteMediaCardItem
import com.resonote.core.designsystem.component.ResonoteMediaCardMetadata
import com.resonote.core.designsystem.component.ResonoteMusicItem
import com.resonote.core.designsystem.component.ResonotePlainAction
import com.resonote.core.designsystem.component.ResonoteSectionHeader
import com.resonote.core.designsystem.component.ResonoteVideoItem
import com.resonote.core.designsystem.component.ResonoteVideoMetadata
import com.resonote.core.designsystem.tokens.ResonoteTokens
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
    sessionId: Long,
    initialQuery: String,
    playingMediaId: String?,
    onBack: () -> Unit,
    onRecognitionClick: (() -> Unit)?,
    onSongClick: (OnlineSong) -> Unit,
    onSongMoreClick: ((OnlineSong) -> Unit)?,
    onPlaylistClick: ((String) -> Unit)?,
    onAlbumClick: ((SearchAlbum) -> Unit)?,
    onArtistClick: ((SearchArtist) -> Unit)?,
    onMvClick: ((SearchMv) -> Unit)?,
    bottomContentPadding: Dp = 32.dp,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(sessionId, initialQuery) { viewModel.initialize(sessionId, initialQuery) }
    SearchScreen(
        state = state,
        playingMediaId = playingMediaId,
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
        bottomContentPadding = bottomContentPadding,
    )
}

@Composable
fun SearchScreen(
    state: SearchUiState,
    playingMediaId: String?,
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
    bottomContentPadding: Dp = 32.dp,
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
                    bottomContentPadding = bottomContentPadding,
                )
                is SearchResultUiState.Loading -> LoadingState(result.query)
                is SearchResultUiState.Empty -> EmptyState()
                is SearchResultUiState.Error -> ErrorState(result.failure, onRetry)
                is SearchResultUiState.Content -> SearchResults(
                    result = result.value,
                    category = result.category,
                    playingMediaId = playingMediaId,
                    onSelectCategory = onSelectCategory,
                    onLoadMore = onLoadMore,
                    onSongClick = onSongClick,
                    onSongMoreClick = onSongMoreClick,
                    onPlaylistClick = onPlaylistClick,
                    onAlbumClick = onAlbumClick,
                    onArtistClick = onArtistClick,
                    onMvClick = onMvClick,
                    bottomContentPadding = bottomContentPadding,
                )
            }
        }
    }
}

@Composable
private fun SearchCategoryBar(selectedCategory: SearchCategory, onSelectCategory: (SearchCategory) -> Unit) {
    LazyRow(
        modifier = Modifier.selectableGroup().padding(vertical = 4.dp),
        contentPadding = PaddingValues(horizontal = ResonoteTokens.spacing.space4),
        horizontalArrangement = Arrangement.spacedBy(ResonoteTokens.spacing.space2),
    ) {
        itemsIndexed(
            items = SearchCategory.entries,
            key = { _, category -> category.name },
        ) { _, category ->
            ResonoteFilterPill(
                label = category.label(),
                selected = category == selectedCategory,
                onClick = { onSelectCategory(category) },
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = ResonoteTokens.spacing.space2,
                end = ResonoteTokens.spacing.space4,
                top = 6.dp,
                bottom = 6.dp,
            ),
        horizontalArrangement = Arrangement.spacedBy(ResonoteTokens.spacing.space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.feature_search_impl_search_back),
                modifier = Modifier.size(20.dp),
            )
        }
        SearchInputBar(
            value = query,
            onValueChange = onQueryChange,
            placeholder = stringResource(R.string.feature_search_impl_search_hint),
            clearLabel = stringResource(R.string.feature_search_impl_search_clear),
            recognitionLabel = stringResource(R.string.feature_search_impl_search_recognition),
            onRecognitionClick = onRecognitionClick,
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
        )
    }
}

@Composable
private fun SearchInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    clearLabel: String,
    recognitionLabel: String,
    onRecognitionClick: (() -> Unit)?,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        modifier = modifier.height(40.dp),
        shape = ResonoteTokens.shapes.full,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxSize(),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            interactionSource = interactionSource,
            singleLine = true,
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier.fillMaxSize().padding(start = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(ResonoteTokens.spacing.space2))
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        innerTextField()
                    }
                    if (value.isNotEmpty()) {
                        IconButton(onClick = { onValueChange("") }, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Rounded.Clear, clearLabel, modifier = Modifier.size(18.dp))
                        }
                    } else {
                        IconButton(
                            onClick = { onRecognitionClick?.invoke() },
                            enabled = onRecognitionClick != null,
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(
                                Icons.Rounded.Mic,
                                recognitionLabel,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun SearchSectionHeader(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(horizontal = ResonoteTokens.spacing.space4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(ResonoteTokens.spacing.space2))
        Text(
            title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        trailingContent?.invoke()
    }
}

@Composable
private fun SearchHeaderTextAction(label: String, onClick: () -> Unit) {
    ResonotePlainAction(
        onClick = onClick,
        modifier = Modifier.size(48.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
            Text(
                label,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
            )
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
    bottomContentPadding: Dp,
) {
    var isEditingHistory by rememberSaveable { mutableStateOf(false) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = bottomContentPadding),
    ) {
        if (suggestions.isNotEmpty()) {
            item {
                SearchSectionHeader(
                    stringResource(R.string.feature_search_impl_search_suggestions),
                    Icons.Rounded.Search,
                )
            }
            itemsIndexed(suggestions, key = { index, value -> "suggestion-$value-$index" }) { _, suggestion ->
                SearchSuggestionRow(
                    label = suggestion,
                    icon = Icons.Rounded.Search,
                    trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward,
                    onClick = { onKeywordClick(suggestion) },
                )
            }
        } else {
            if (history.isNotEmpty()) {
                item {
                    SearchSectionHeader(
                        title = stringResource(R.string.feature_search_impl_search_history),
                        icon = Icons.Rounded.History,
                        trailingContent = {
                            if (isEditingHistory) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    SearchHeaderTextAction(
                                        label = stringResource(R.string.feature_search_impl_search_history_clear),
                                        onClick = {
                                            isEditingHistory = false
                                            onClearHistory()
                                        },
                                    )
                                    SearchHeaderTextAction(
                                        label = stringResource(R.string.feature_search_impl_search_history_done),
                                        onClick = { isEditingHistory = false },
                                    )
                                }
                            } else {
                                SearchHeaderTextAction(
                                    label = stringResource(R.string.feature_search_impl_search_history_edit),
                                    onClick = { isEditingHistory = true },
                                )
                            }
                        },
                    )
                }
                item {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = ResonoteTokens.spacing.space4),
                        horizontalArrangement = Arrangement.spacedBy(ResonoteTokens.spacing.space2),
                        verticalArrangement = Arrangement.spacedBy(ResonoteTokens.spacing.space2),
                    ) {
                        history.forEach { query ->
                            HistoryKeywordChip(
                                query = query,
                                editing = isEditingHistory,
                                removeLabel = stringResource(
                                    R.string.feature_search_impl_search_history_remove,
                                    query,
                                ),
                                onClick = { onKeywordClick(query) },
                                onRemove = { onRemoveHistory(query) },
                            )
                        }
                    }
                }
            }
            if (hotKeywords.isNotEmpty()) {
                item {
                    SearchSectionHeader(
                        stringResource(R.string.feature_search_impl_search_hot),
                        Icons.Rounded.GraphicEq,
                        modifier = if (history.isNotEmpty()) {
                            Modifier.padding(top = ResonoteTokens.spacing.space3)
                        } else {
                            Modifier
                        },
                    )
                }
                item {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = ResonoteTokens.spacing.space4),
                        horizontalArrangement = Arrangement.spacedBy(ResonoteTokens.spacing.space2),
                        verticalArrangement = Arrangement.spacedBy(ResonoteTokens.spacing.space2),
                    ) {
                        hotKeywords.forEachIndexed { index, keyword ->
                            HotKeywordChip(
                                rank = index + 1,
                                keyword = keyword,
                                onClick = { onKeywordClick(keyword) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryKeywordChip(
    query: String,
    editing: Boolean,
    removeLabel: String,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    Surface(
        onClick = if (editing) onRemove else onClick,
        modifier = Modifier
            .height(36.dp)
            .then(if (editing) Modifier.semantics { contentDescription = removeLabel } else Modifier),
        shape = ResonoteTokens.shapes.full,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(ResonoteTokens.borders.hairline, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(start = 13.dp, end = if (editing) 9.dp else 13.dp),
            horizontalArrangement = Arrangement.spacedBy(ResonoteTokens.spacing.space2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(query, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            if (editing) {
                Icon(
                    Icons.Rounded.Clear,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun SearchSuggestionRow(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    trailingIcon: ImageVector? = null,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable(onClick = onClick)
            .padding(start = ResonoteTokens.spacing.space4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(ResonoteTokens.spacing.space3))
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        when {
            trailingContent != null -> trailingContent()
            trailingIcon != null -> Box(
                modifier = Modifier.size(ResonoteTokens.touchTargets.minimum),
                contentAlignment = Alignment.Center,
            ) {
                Icon(trailingIcon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun HotKeywordChip(rank: Int, keyword: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.height(36.dp),
        shape = ResonoteTokens.shapes.full,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(ResonoteTokens.borders.hairline, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = rank.toString(),
                color = if (rank <=
                    3
                ) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(keyword, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun LoadingState(query: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator()
            Text(
                stringResource(R.string.feature_search_impl_search_loading, query),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
private fun MessageState(icon: ImageVector, title: String, body: String, action: (@Composable () -> Unit)? = null) {
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
    category: SearchCategory,
    playingMediaId: String?,
    onSelectCategory: (SearchCategory) -> Unit,
    onLoadMore: () -> Unit,
    onSongClick: (OnlineSong) -> Unit,
    onSongMoreClick: ((OnlineSong) -> Unit)?,
    onPlaylistClick: ((String) -> Unit)?,
    onAlbumClick: ((SearchAlbum) -> Unit)?,
    onArtistClick: ((SearchArtist) -> Unit)?,
    onMvClick: ((SearchMv) -> Unit)?,
    bottomContentPadding: Dp,
) {
    when (result) {
        is SearchContentUiState.Aggregate -> AggregateSearchResults(
            result = result.value,
            playingMediaId = playingMediaId,
            onSelectCategory = onSelectCategory,
            onSongClick = onSongClick,
            onSongMoreClick = onSongMoreClick,
            onPlaylistClick = onPlaylistClick,
            onAlbumClick = onAlbumClick,
            onArtistClick = onArtistClick,
            onMvClick = onMvClick,
            bottomContentPadding = bottomContentPadding,
        )
        is SearchContentUiState.Page -> PagedSearchResults(
            page = result,
            category = category,
            playingMediaId = playingMediaId,
            onLoadMore = onLoadMore,
            onSongClick = onSongClick,
            onSongMoreClick = onSongMoreClick,
            onPlaylistClick = onPlaylistClick,
            onAlbumClick = onAlbumClick,
            onArtistClick = onArtistClick,
            onMvClick = onMvClick,
            bottomContentPadding = bottomContentPadding,
        )
    }
}

@Composable
private fun AggregateSearchResults(
    result: ComplexSearchResult,
    playingMediaId: String?,
    onSelectCategory: (SearchCategory) -> Unit,
    onSongClick: (OnlineSong) -> Unit,
    onSongMoreClick: ((OnlineSong) -> Unit)?,
    onPlaylistClick: ((String) -> Unit)?,
    onAlbumClick: ((SearchAlbum) -> Unit)?,
    onArtistClick: ((SearchArtist) -> Unit)?,
    onMvClick: ((SearchMv) -> Unit)?,
    bottomContentPadding: Dp,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("search-aggregate"),
        contentPadding = PaddingValues(top = 8.dp, bottom = bottomContentPadding),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        if (result.artists.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SearchResultSectionHeader(
                        title = stringResource(R.string.feature_search_impl_search_artists),
                        onClick = { onSelectCategory(SearchCategory.ARTISTS) },
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        itemsIndexed(
                            result.artists,
                            key = { index, artist -> "artist-${artist.id}-$index" },
                        ) { _, artist ->
                            SearchArtistItem(
                                artist = artist,
                                modifier = Modifier.width(104.dp),
                                onClick = onArtistClick?.let { callback -> { callback(artist) } },
                            )
                        }
                    }
                }
            }
        }
        if (result.songs.isNotEmpty()) {
            item {
                SearchResultSectionHeader(
                    title = stringResource(R.string.feature_search_impl_search_songs),
                    total = result.songsTotal,
                    onClick = { onSelectCategory(SearchCategory.SONGS) },
                )
            }
            itemsIndexed(result.songs, key = { index, song -> "song-${song.hash}-$index" }) { _, song ->
                SearchSongItem(
                    song = song,
                    playingMediaId = playingMediaId,
                    onClick = { onSongClick(song) },
                    onMoreClick = onSongMoreClick?.let { callback -> { callback(song) } },
                )
            }
        }
        if (result.playlists.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SearchResultSectionHeader(
                        title = stringResource(R.string.feature_search_impl_search_playlists),
                        total = result.playlistsTotal,
                        onClick = { onSelectCategory(SearchCategory.PLAYLISTS) },
                    )
                    result.playlists.chunked(2).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            row.forEach { playlist ->
                                SearchPlaylistItem(
                                    playlist = playlist,
                                    modifier = Modifier.weight(1f),
                                    onClick = onPlaylistClick?.let { callback -> { callback(playlist.id) } },
                                )
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        if (result.albums.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SearchResultSectionHeader(
                        title = stringResource(R.string.feature_search_impl_search_albums),
                        total = result.albumsTotal,
                        onClick = { onSelectCategory(SearchCategory.ALBUMS) },
                    )
                    result.albums.chunked(2).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            row.forEach { album ->
                                SearchAlbumItem(
                                    album = album,
                                    modifier = Modifier.weight(1f),
                                    onClick = onAlbumClick?.let { callback -> { callback(album) } },
                                )
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        if (result.mvs.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SearchResultSectionHeader(
                        title = stringResource(R.string.feature_search_impl_search_mvs),
                        total = result.mvsTotal,
                        onClick = { onSelectCategory(SearchCategory.MVS) },
                    )
                    result.mvs.chunked(2).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            row.forEach { mv ->
                                SearchMvItem(
                                    mv = mv,
                                    modifier = Modifier.weight(1f),
                                    onClick = onMvClick?.let { callback -> { callback(mv) } },
                                )
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PagedSearchResults(
    page: SearchContentUiState.Page,
    category: SearchCategory,
    playingMediaId: String?,
    onLoadMore: () -> Unit,
    onSongClick: (OnlineSong) -> Unit,
    onSongMoreClick: ((OnlineSong) -> Unit)?,
    onPlaylistClick: ((String) -> Unit)?,
    onAlbumClick: ((SearchAlbum) -> Unit)?,
    onArtistClick: ((SearchArtist) -> Unit)?,
    onMvClick: ((SearchMv) -> Unit)?,
    bottomContentPadding: Dp,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = bottomContentPadding),
    ) {
        when (category) {
            SearchCategory.ALL -> Unit
            SearchCategory.SONGS -> {
                val songs = page.items.filterIsInstance<SearchResultItem.Song>()
                itemsIndexed(songs, key = { index, item -> "song-${item.stableId}-$index" }) { _, item ->
                    SearchSongItem(
                        song = item.value,
                        playingMediaId = playingMediaId,
                        onClick = { onSongClick(item.value) },
                        onMoreClick = onSongMoreClick?.let { callback -> { callback(item.value) } },
                    )
                }
            }
            SearchCategory.PLAYLISTS -> {
                val rows = page.items.filterIsInstance<SearchResultItem.Playlist>().chunked(2)
                itemsIndexed(rows, key = { index, row -> "playlist-${row.first().stableId}-$index" }) { _, row ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        row.forEach { item ->
                            SearchPlaylistItem(
                                playlist = item.value,
                                modifier = Modifier.weight(1f),
                                onClick = onPlaylistClick?.let { callback -> { callback(item.value.id) } },
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            SearchCategory.ALBUMS -> {
                val rows = page.items.filterIsInstance<SearchResultItem.Album>().chunked(2)
                itemsIndexed(rows, key = { index, row -> "album-${row.first().stableId}-$index" }) { _, row ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        row.forEach { item ->
                            SearchAlbumItem(
                                album = item.value,
                                modifier = Modifier.weight(1f),
                                onClick = onAlbumClick?.let { callback -> { callback(item.value) } },
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            SearchCategory.MVS -> {
                val rows = page.items.filterIsInstance<SearchResultItem.Mv>().chunked(2)
                itemsIndexed(rows, key = { index, row -> "mv-${row.first().stableId}-$index" }) { _, row ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        row.forEach { item ->
                            SearchMvItem(
                                mv = item.value,
                                modifier = Modifier.weight(1f),
                                onClick = onMvClick?.let { callback -> { callback(item.value) } },
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            SearchCategory.ARTISTS -> {
                val rows = page.items.filterIsInstance<SearchResultItem.Artist>().chunked(3)
                itemsIndexed(rows, key = { index, row -> "artist-${row.first().stableId}-$index" }) { _, row ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        row.forEach { item ->
                            SearchArtistItem(
                                artist = item.value,
                                modifier = Modifier.weight(1f),
                                onClick = onArtistClick?.let { callback -> { callback(item.value) } },
                            )
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
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
private fun SearchResultSectionHeader(title: String, total: Int? = null, onClick: () -> Unit) {
    ResonoteSectionHeader(
        title = title,
        supportingText = total?.let {
            stringResource(R.string.feature_search_impl_search_result_count, it)
        } ?: stringResource(R.string.feature_search_impl_search_section_supporting),
        modifier = Modifier.padding(horizontal = 16.dp),
        trailingContent = {
            ResonotePlainAction(onClick = onClick) {
                Text(
                    text = stringResource(R.string.feature_search_impl_search_see_all),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        },
    )
}

@Composable
private fun SearchSongItem(
    song: OnlineSong,
    playingMediaId: String?,
    onClick: () -> Unit,
    onMoreClick: (() -> Unit)?,
) = ResonoteMusicItem(
    title = song.title,
    supportingText = song.artist.orEmpty(),
    modifier = Modifier.padding(horizontal = 8.dp),
    duration = song.durationMillis.durationLabel(),
    qualityLabel = song.quality.label(),
    isVip = song.vip,
    isPlaying = song.hash == playingMediaId,
    artworkUrl = song.coverUrl,
    onClick = onClick,
    onMoreClick = onMoreClick,
)

@Composable
private fun SearchPlaylistItem(playlist: SearchPlaylist, modifier: Modifier, onClick: (() -> Unit)?) {
    ResonoteMediaCardItem(
        metadata = ResonoteMediaCardMetadata(
            title = playlist.name,
            playCount = playlist.playCount.takeIf { it > 0 }?.compactCount(),
            supportingText = listOfNotNull(
                playlist.creator?.takeIf(String::isNotBlank),
                stringResource(R.string.feature_search_impl_search_song_count, playlist.songCount),
            ).joinToString(" · "),
        ),
        artworkContentDescription = stringResource(
            R.string.feature_search_impl_search_playlist_artwork,
            playlist.name,
        ),
        onClick = { onClick?.invoke() },
        modifier = modifier,
        artworkState = if (playlist.coverUrl.isNullOrBlank()) {
            ResonoteArtworkState.MISSING
        } else {
            ResonoteArtworkState.LOADED
        },
        artworkUrl = playlist.coverUrl,
        enabled = onClick != null,
    )
}

@Composable
private fun SearchAlbumItem(album: SearchAlbum, modifier: Modifier, onClick: (() -> Unit)?) {
    ResonoteMediaCardItem(
        metadata = ResonoteMediaCardMetadata(
            title = album.name,
            supportingText = listOfNotNull(
                album.artist?.takeIf(String::isNotBlank),
                album.publishDate.takeIf(String::isNotBlank),
            ).joinToString(" · ").ifBlank {
                stringResource(R.string.feature_search_impl_search_song_count, album.songCount)
            },
        ),
        artworkContentDescription = stringResource(
            R.string.feature_search_impl_search_album_artwork,
            album.name,
        ),
        onClick = { onClick?.invoke() },
        modifier = modifier,
        artworkState = if (album.coverUrl.isNullOrBlank()) {
            ResonoteArtworkState.MISSING
        } else {
            ResonoteArtworkState.LOADED
        },
        artworkUrl = album.coverUrl,
        enabled = onClick != null,
    )
}

@Composable
private fun SearchArtistItem(artist: SearchArtist, modifier: Modifier, onClick: (() -> Unit)?) {
    ResonoteArtistItem(
        metadata = ResonoteArtistMetadata(
            title = artist.name,
            supportingText = stringResource(
                R.string.feature_search_impl_search_artist_metadata,
                artist.songCount,
                artist.albumCount,
            ),
        ),
        onClick = { onClick?.invoke() },
        modifier = modifier,
        artworkState = if (artist.avatarUrl.isNullOrBlank()) {
            ResonoteArtworkState.MISSING
        } else {
            ResonoteArtworkState.LOADED
        },
        artworkUrl = artist.avatarUrl,
        enabled = onClick != null,
    )
}

@Composable
private fun SearchMvItem(mv: SearchMv, modifier: Modifier, onClick: (() -> Unit)?) {
    ResonoteVideoItem(
        metadata = ResonoteVideoMetadata(
            title = mv.name,
            supportingText = mv.singer,
            duration = mv.durationMillis.takeIf { it > 0 }?.durationLabel(),
        ),
        onClick = { onClick?.invoke() },
        modifier = modifier,
        artworkState = if (mv.coverUrl.isNullOrBlank()) ResonoteArtworkState.MISSING else ResonoteArtworkState.LOADED,
        artworkUrl = mv.coverUrl,
        enabled = onClick != null,
    )
}

private fun Long.compactCount(): String = when {
    this >= 100_000_000 -> compactUnit(100_000_000, "亿")
    this >= 10_000 -> compactUnit(10_000, "万")
    else -> toString()
}

private fun Long.compactUnit(divisor: Long, suffix: String): String {
    val number = if (this % divisor == 0L) {
        "%.0f".format(this / divisor.toDouble())
    } else {
        "%.1f".format(this / divisor.toDouble())
    }
    return "$number$suffix"
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
