@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.resonote.feature.library.impl

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.resonote.core.designsystem.component.ResonoteRemoteArtwork
import com.resonote.core.model.ContentFailure
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.UserPlaylist

@Composable
fun PlaylistPickerSheet(
    state: MyUiState,
    song: OnlineSong,
    onDismiss: () -> Unit,
    onRetryPlaylists: () -> Unit,
    onPlaylistClick: (UserPlaylist) -> Unit,
    onDismissFailure: () -> Unit,
) {
    val addition = (state as? MyUiState.Authenticated)?.playlistAddition
    val submitting = addition is PlaylistAdditionUiState.Submitting
    val currentSubmitting by rememberUpdatedState(submitting)
    val sheetState = rememberModalBottomSheetState(
        confirmValueChange = { target ->
            target != SheetValue.Hidden || !currentSubmitting
        },
    )
    ModalBottomSheet(
        onDismissRequest = { if (!submitting) onDismiss() },
        sheetState = sheetState,
    ) {
        Column(Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.feature_library_impl_playlist_picker_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = song.title,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(12.dp))
            when (state) {
                MyUiState.CheckingAccount -> PickerLoading()
                MyUiState.Anonymous -> PickerMessage(
                    text = stringResource(R.string.feature_library_impl_playlist_picker_login_required),
                )
                is MyUiState.Authenticated -> when (val playlists = state.playlists) {
                    MySectionState.Loading -> PickerLoading()
                    is MySectionState.Failed -> PickerFailure(
                        failure = playlists.failure,
                        onRetry = onRetryPlaylists,
                    )
                    is MySectionState.Available -> {
                        val writable = playlists.value.filter(UserPlaylist::isMine)
                        if (writable.isEmpty()) {
                            PickerMessage(
                                text = stringResource(R.string.feature_library_impl_playlist_picker_empty),
                            )
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(bottom = 24.dp),
                            ) {
                                items(writable, key = UserPlaylist::listId) { playlist ->
                                    val isSubmitting = addition ==
                                        PlaylistAdditionUiState.Submitting(playlist.listId)
                                    val failure = (addition as? PlaylistAdditionUiState.Failed)
                                        ?.takeIf { it.listId == playlist.listId }
                                        ?.failure
                                    PlaylistTargetRow(
                                        playlist = playlist,
                                        submitting = isSubmitting,
                                        enabled = !submitting,
                                        failure = failure,
                                        onClick = {
                                            if (failure != null) onDismissFailure()
                                            onPlaylistClick(playlist)
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistTargetRow(
    playlist: UserPlaylist,
    submitting: Boolean,
    enabled: Boolean,
    failure: ContentFailure?,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = {
            Text(playlist.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.feature_library_impl_playlist_picker_count, playlist.count))
                failure?.let {
                    Text(
                        playlistPickerFailureMessage(it),
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
        leadingContent = {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = if (playlist.isLike) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                },
            ) {
                Icon(
                    imageVector = if (playlist.isLike) Icons.Rounded.Favorite else Icons.Rounded.LibraryMusic,
                    contentDescription = null,
                    modifier = Modifier.padding(12.dp).size(24.dp),
                    tint = if (playlist.isLike) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                if (!playlist.coverUrl.isNullOrBlank()) {
                    ResonoteRemoteArtwork(
                        model = playlist.coverUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        fallback = {},
                    )
                }
            }
        },
        trailingContent = {
            if (submitting) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Rounded.Add, contentDescription = null)
            }
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clickable(enabled = enabled, onClick = onClick),
    )
}

@Composable
private fun PickerLoading() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(40.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun PickerFailure(failure: ContentFailure, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = null)
        Text(playlistPickerFailureMessage(failure), color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = onRetry) {
            Text(stringResource(R.string.feature_library_impl_playlist_picker_retry))
        }
    }
}

@Composable
private fun PickerMessage(text: String) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyLarge,
    )
}

@Composable
private fun playlistPickerFailureMessage(failure: ContentFailure): String = when (failure) {
    ContentFailure.AuthenticationRequired ->
        stringResource(R.string.feature_library_impl_playlist_picker_error_auth)
    ContentFailure.Network -> stringResource(R.string.feature_library_impl_playlist_picker_error_network)
    ContentFailure.ServiceRejected ->
        stringResource(R.string.feature_library_impl_playlist_picker_error_service)
    is ContentFailure.RiskVerificationRequired,
    ContentFailure.RiskBlocked,
    -> stringResource(R.string.feature_library_impl_playlist_picker_error_risk)
    ContentFailure.Protocol -> stringResource(R.string.feature_library_impl_playlist_picker_error_protocol)
}
