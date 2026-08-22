package com.resonote.feature.search.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resonote.core.designsystem.component.ResonoteFilterPill
import com.resonote.core.designsystem.tokens.ResonoteTokens
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.SearchAlbum
import com.resonote.core.model.SearchArtist
import com.resonote.core.model.SearchMv

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
internal fun SearchCategoryBar(selectedCategory: SearchCategory, onSelectCategory: (SearchCategory) -> Unit) {
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
