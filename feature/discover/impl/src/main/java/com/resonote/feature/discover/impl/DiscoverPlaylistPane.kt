@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.resonote.feature.discover.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.resonote.core.designsystem.component.ResonoteArtworkState
import com.resonote.core.designsystem.component.ResonoteEmptyState
import com.resonote.core.designsystem.component.ResonoteFilterPill
import com.resonote.core.designsystem.component.ResonoteHeroKeys
import com.resonote.core.designsystem.component.ResonoteLoadMoreEffect
import com.resonote.core.designsystem.component.ResonoteLoadMoreFooter
import com.resonote.core.designsystem.component.ResonoteLoadMoreState
import com.resonote.core.designsystem.component.ResonotePlaylistItem
import com.resonote.core.designsystem.component.ResonotePlaylistMetadata
import com.resonote.core.designsystem.component.rememberResonoteShimmer
import com.resonote.core.model.PlaylistCategory
import com.resonote.core.model.PlaylistSummary

@Composable
internal fun PlaylistPane(
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
    val shimmer = rememberResonoteShimmer("discover-playlists-skeleton")
    val page = state.playlists as? DiscoverPageState.Content
    ResonoteLoadMoreEffect(
        listState = listState,
        itemCount = page?.items?.size ?: 0,
        enabled = page?.let { it.hasMore && !it.isLoadingMore && it.loadMoreFailure == null } == true,
        onLoadMore = onLoadMore,
    )
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
            -> item(key = "loading") { PlaylistSkeleton(shimmer) }
            DiscoverPageState.Empty -> item(key = "empty") {
                ResonoteEmptyState(
                    title = stringResource(R.string.feature_discover_impl_discover_empty_playlists),
                    message = stringResource(R.string.feature_discover_impl_discover_empty_playlists_supporting),
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                )
            }
            is DiscoverPageState.Error -> item(key = "error") { PaneError(page.failure, onRetry) }
            is DiscoverPageState.Content -> {
                itemsIndexed(
                    items = page.items.chunked(2),
                    key = { index, row -> "row-${row.first().id}-$index" },
                ) { index, row ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(
                            start = 16.dp,
                            top = if (index == 0) 16.dp else 8.dp,
                            end = 16.dp,
                            bottom = 8.dp,
                        ),
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
                                heroKey = ResonoteHeroKeys.playlist(playlist.id),
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                if (page.isLoadingMore || page.loadMoreFailure != null) {
                    item(key = "load-more") {
                        ResonoteLoadMoreFooter(
                            state = if (page.isLoadingMore) {
                                ResonoteLoadMoreState.LOADING
                            } else {
                                ResonoteLoadMoreState.ERROR
                            },
                            onRetry = onLoadMore,
                        )
                    }
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
    Column {
        when (categories) {
            DiscoverLoadState.Idle,
            DiscoverLoadState.Loading,
            -> LinearFilterLoading(Modifier.padding(vertical = 8.dp))
            DiscoverLoadState.Empty -> Unit
            is DiscoverLoadState.Error -> TextButton(
                onClick = onRetry,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            ) { Text(stringResource(R.string.feature_discover_impl_discover_categories_unavailable)) }
            is DiscoverLoadState.Content -> {
                LazyRow(
                    modifier = Modifier.selectableGroup().padding(vertical = 4.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item(key = "recommended") {
                        ResonoteFilterPill(
                            label = stringResource(R.string.feature_discover_impl_discover_recommended),
                            selected = selectedParentId == null,
                            onClick = { onSelectParent(null) },
                        )
                    }
                    items(
                        items = categories.value,
                        key = { "parent-${it.tagId}" },
                    ) { category ->
                        ResonoteFilterPill(
                            label = category.name,
                            selected = category.tagId == selectedParentId,
                            onClick = { onSelectParent(category.tagId) },
                        )
                    }
                }
                categories.value.firstOrNull { it.tagId == selectedParentId }
                    ?.children?.takeIf(List<PlaylistCategory>::isNotEmpty)?.let { children ->
                        LazyRow(
                            modifier = Modifier.selectableGroup().padding(vertical = 4.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            itemsIndexed(
                                items = children,
                                key = { index, category -> "child-${category.tagId}-$index" },
                            ) { _, category ->
                                ResonoteFilterPill(
                                    label = category.name,
                                    selected = category.tagId == selectedCategoryId,
                                    onClick = { onSelectCategory(category.tagId) },
                                )
                            }
                        }
                    }
            }
        }
    }
}
