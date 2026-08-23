@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.resonote.feature.artist.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.resonote.core.designsystem.component.ResonoteMusicItem
import com.resonote.core.model.OnlineSong

@Composable
internal fun ArtistContent(
    state: ArtistUiState,
    profile: ArtistProfile? = state.profile,
    playingMediaId: String?,
    onSelectSection: (ArtistSongSection) -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onPlayAll: (List<OnlineSong>) -> Unit,
    onSongClick: (OnlineSong) -> Unit,
    onSongMoreClick: ((OnlineSong) -> Unit)?,
    bottomContentPadding: Dp,
    modifier: Modifier = Modifier,
) {
    val page = state.selectedPage()
    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("artist-list"),
        contentPadding = PaddingValues(bottom = bottomContentPadding),
    ) {
        item(key = "profile") { ArtistHeader(profile) }
        item(key = "sections") {
            PrimaryTabRow(
                selectedTabIndex = state.selectedSection.ordinal,
                containerColor = MaterialTheme.colorScheme.background,
            ) {
                ArtistSongSection.entries.forEach { section ->
                    Tab(
                        selected = section == state.selectedSection,
                        onClick = { onSelectSection(section) },
                        text = {
                            Text(
                                when (section) {
                                    ArtistSongSection.POPULAR -> stringResource(
                                        R.string.feature_artist_impl_artist_popular,
                                    )
                                    ArtistSongSection.LATEST -> stringResource(
                                        R.string.feature_artist_impl_artist_latest,
                                    )
                                },
                            )
                        },
                    )
                }
            }
        }
        when (page) {
            ArtistPageUiState.Idle,
            ArtistPageUiState.Loading,
            -> item(key = "loading") { LoadingState() }
            ArtistPageUiState.Empty -> item(key = "empty") {
                MessageState(
                    icon = Icons.Rounded.Person,
                    title = stringResource(R.string.feature_artist_impl_artist_empty_title),
                    body = stringResource(R.string.feature_artist_impl_artist_empty_body),
                )
            }
            is ArtistPageUiState.Error -> item(key = "error") {
                ErrorState(page.failure, onRetry)
            }
            is ArtistPageUiState.Content -> {
                item(key = "play-all") {
                    Button(
                        onClick = { onPlayAll(page.songs) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                        contentPadding = PaddingValues(vertical = 14.dp),
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.feature_artist_impl_artist_play_all))
                    }
                }
                itemsIndexed(
                    items = page.songs,
                    key = { index, song -> "${state.selectedSection}-${song.hash}-$index" },
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
                if (page.hasMore || page.isLoadingMore || page.loadMoreFailure != null) {
                    item(key = "load-more") {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            when {
                                page.isLoadingMore -> CircularProgressIndicator(modifier = Modifier.size(28.dp))
                                page.loadMoreFailure != null -> TextButton(onClick = onLoadMore) {
                                    Text(stringResource(R.string.feature_artist_impl_artist_load_more_retry))
                                }
                                page.hasMore -> TextButton(onClick = onLoadMore) {
                                    Text(stringResource(R.string.feature_artist_impl_artist_load_more))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
