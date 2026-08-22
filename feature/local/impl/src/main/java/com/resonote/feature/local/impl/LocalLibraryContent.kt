@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.resonote.feature.local.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.resonote.core.designsystem.component.ResonoteArtworkState
import com.resonote.core.designsystem.component.ResonoteMusicItem
import com.resonote.core.designsystem.component.ResonoteTextField
import com.resonote.core.model.LocalMedia

@Composable
internal fun LocalLibraryCard(media: List<LocalMedia>) {
    val totalBytes = media.sumOf(LocalMedia::sizeBytes)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.62f),
                        ),
                    ),
                )
                .padding(22.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.LibraryMusic, contentDescription = null, modifier = Modifier.size(28.dp))
                    }
                }
                Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                    Text(
                        stringResource(R.string.feature_local_impl_on_device),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        stringResource(R.string.feature_local_impl_summary, media.size, totalBytes.fileSize()),
                        modifier = Modifier.padding(top = 4.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        stringResource(R.string.feature_local_impl_private_copy),
                        modifier = Modifier.padding(top = 4.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
internal fun LocalMusicTools(
    state: LocalMusicUiState,
    onQueryChange: (String) -> Unit,
    onSortChange: (LocalMusicSort) -> Unit,
) {
    var sortExpanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ResonoteTextField(
            value = state.query,
            onValueChange = onQueryChange,
            label = stringResource(R.string.feature_local_impl_search),
            placeholder = stringResource(R.string.feature_local_impl_search_hint),
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingAction = if (state.query.isNotEmpty()) {
                {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Rounded.Close, stringResource(R.string.feature_local_impl_clear_search))
                    }
                }
            } else {
                null
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Box {
            FilterChip(
                selected = state.sort != LocalMusicSort.ImportedNewest,
                onClick = { sortExpanded = true },
                label = { Text(stringResource(R.string.feature_local_impl_sort, state.sort.label())) },
                trailingIcon = { Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null) },
            )
            DropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }) {
                LocalMusicSort.entries.forEach { sort ->
                    DropdownMenuItem(
                        text = { Text(sort.label()) },
                        onClick = {
                            onSortChange(sort)
                            sortExpanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
internal fun LocalMediaRow(
    media: LocalMedia,
    isPlaying: Boolean,
    isDeleting: Boolean,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Box {
        ResonoteMusicItem(
            title = media.title,
            supportingText = media.supportingLabel(),
            duration = media.durationMillis.durationLabel(),
            onClick = onPlay,
            onMoreClick = { menuExpanded = true },
            artworkState = if (media.artworkUri == null) ResonoteArtworkState.MISSING else ResonoteArtworkState.LOADED,
            artwork = {
                AsyncImage(
                    model = media.artworkUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            },
            qualityLabel = media.formatLabel(),
            isPlaying = isPlaying,
            enabled = !isDeleting,
        )
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
            modifier = Modifier.align(Alignment.TopEnd),
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.feature_local_impl_delete)) },
                leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                onClick = {
                    menuExpanded = false
                    onDelete()
                },
            )
        }
        if (isDeleting) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp).size(24.dp),
                strokeWidth = 2.dp,
            )
        }
    }
}
