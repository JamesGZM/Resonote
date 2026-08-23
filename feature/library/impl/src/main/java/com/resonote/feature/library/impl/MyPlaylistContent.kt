package com.resonote.feature.library.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.resonote.core.designsystem.component.ResonoteArtworkState
import com.resonote.core.designsystem.component.ResonoteContentPhase
import com.resonote.core.designsystem.component.ResonoteContentStateLayout
import com.resonote.core.designsystem.component.ResonoteEmptyState
import com.resonote.core.designsystem.component.ResonoteErrorState
import com.resonote.core.designsystem.component.ResonoteFilterPill
import com.resonote.core.designsystem.component.ResonoteHeroKeys
import com.resonote.core.designsystem.component.ResonoteIconButton
import com.resonote.core.designsystem.component.ResonotePlaylistItem
import com.resonote.core.designsystem.component.ResonotePlaylistMetadata
import com.resonote.core.model.UserPlaylist

@Composable
internal fun PlaylistSection(
    state: MySectionState<List<UserPlaylist>>,
    selectedGroup: Int,
    onSelectGroup: (Int) -> Unit,
    onPlaylistClick: (UserPlaylist) -> Unit,
    onCreatePlaylistClick: () -> Unit,
    onRetry: () -> Unit,
    createEnabled: Boolean,
) {
    val playlists = (state as? MySectionState.Available)?.value.orEmpty()
    val created = playlists.filter { it.isMine && !it.isLike }
    val collected = playlists.filterNot { it.isMine }
    val visible = if (selectedGroup == 0) created else collected
    val phase = when (state) {
        MySectionState.Loading -> ResonoteContentPhase.LOADING
        is MySectionState.Failed -> ResonoteContentPhase.ERROR
        is MySectionState.Available -> if (visible.isEmpty()) {
            ResonoteContentPhase.EMPTY
        } else {
            ResonoteContentPhase.CONTENT
        }
    }
    val stateModifier = if (phase == ResonoteContentPhase.CONTENT) {
        Modifier.fillMaxWidth()
    } else {
        Modifier.fillMaxWidth().heightIn(min = 236.dp)
    }
    ResonoteContentStateLayout(
        phase = phase,
        modifier = stateModifier.testTag("my-playlist-state"),
        loading = { PlaylistSkeleton() },
        empty = {
            Column {
                PlaylistHeader(
                    createdCount = created.size,
                    collectedCount = collected.size,
                    selectedGroup = selectedGroup,
                    onSelectGroup = onSelectGroup,
                    onCreatePlaylistClick = onCreatePlaylistClick,
                    createEnabled = createEnabled,
                )
                ResonoteEmptyState(
                    title = stringResource(
                        if (selectedGroup == 0) {
                            R.string.feature_library_impl_my_created_empty_title
                        } else {
                            R.string.feature_library_impl_my_collected_empty_title
                        },
                    ),
                    message = stringResource(
                        if (selectedGroup == 0) {
                            R.string.feature_library_impl_my_created_empty
                        } else {
                            R.string.feature_library_impl_my_collected_empty
                        },
                    ),
                    modifier = Modifier.height(220.dp),
                )
            }
        },
        error = {
            ResonoteErrorState(
                onRetry = onRetry,
                title = stringResource(R.string.feature_library_impl_my_playlists_error),
                message = (state as MySectionState.Failed).failure.message(),
                modifier = Modifier.testTag("my-playlists-error"),
            )
        },
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PlaylistHeader(
                    createdCount = created.size,
                    collectedCount = collected.size,
                    selectedGroup = selectedGroup,
                    onSelectGroup = onSelectGroup,
                    onCreatePlaylistClick = onCreatePlaylistClick,
                    createEnabled = createEnabled,
                )
                visible.chunked(2).forEach { row ->
                    PlaylistRow(row, onPlaylistClick)
                }
            }
        },
    )
}

@Composable
private fun PlaylistHeader(
    createdCount: Int,
    collectedCount: Int,
    selectedGroup: Int,
    onSelectGroup: (Int) -> Unit,
    onCreatePlaylistClick: () -> Unit,
    createEnabled: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(40.dp).testTag("my-playlist-header"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.feature_library_impl_my_playlists),
                modifier = Modifier.weight(1f).testTag("my-playlist-title"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            ResonoteIconButton(
                label = stringResource(R.string.feature_library_impl_create_playlist_action),
                onClick = onCreatePlaylistClick,
                modifier = Modifier.offset(x = 12.dp).testTag("my-create-playlist"),
                enabled = createEnabled,
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.PlaylistAdd,
                    contentDescription = null,
                    modifier = Modifier.testTag("my-create-playlist-icon"),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ResonoteFilterPill(
                stringResource(R.string.feature_library_impl_my_created_filter, createdCount),
                selectedGroup == 0,
                { onSelectGroup(0) },
            )
            ResonoteFilterPill(
                stringResource(R.string.feature_library_impl_my_collected_filter, collectedCount),
                selectedGroup == 1,
                { onSelectGroup(1) },
            )
        }
    }
}

@Composable
private fun PlaylistRow(row: List<UserPlaylist>, onPlaylistClick: (UserPlaylist) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        row.forEach { playlist ->
            ResonotePlaylistItem(
                metadata = ResonotePlaylistMetadata(
                    title = playlist.name,
                    supportingText = stringResource(
                        R.string.feature_library_impl_my_song_count,
                        playlist.count,
                    ),
                ),
                onClick = { onPlaylistClick(playlist) },
                modifier = Modifier.weight(1f).testTag("my-playlist-${playlist.globalId}"),
                artworkState = ResonoteArtworkState.LOADED,
                artworkUrl = playlist.coverUrl,
                heroKey = ResonoteHeroKeys.playlist(playlist.globalId),
            )
        }
        if (row.size == 1) Spacer(Modifier.weight(1f))
    }
}
