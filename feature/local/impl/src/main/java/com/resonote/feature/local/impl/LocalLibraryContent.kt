@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.resonote.feature.local.impl

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.resonote.core.designsystem.component.ResonoteArtworkState
import com.resonote.core.designsystem.component.ResonoteIconButton
import com.resonote.core.designsystem.component.ResonoteMusicItem
import com.resonote.core.designsystem.component.ResonotePlainAction
import com.resonote.core.designsystem.tokens.ResonoteTokens
import com.resonote.core.model.LocalMedia

@Composable
internal fun LocalLibrarySummary(
    media: List<LocalMedia>,
    importEnabled: Boolean,
    onPickFiles: () -> Unit,
    onPickDirectory: () -> Unit,
) {
    val totalBytes = media.sumOf(LocalMedia::sizeBytes)
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                stringResource(R.string.feature_local_impl_summary, media.size, totalBytes.fileSize()),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                stringResource(R.string.feature_local_impl_private_copy),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        IconButton(onClick = onPickDirectory, enabled = importEnabled) {
            Icon(Icons.Rounded.FolderOpen, stringResource(R.string.feature_local_impl_import_directory))
        }
        IconButton(onClick = onPickFiles, enabled = importEnabled) {
            Icon(Icons.Rounded.Add, stringResource(R.string.feature_local_impl_import))
        }
    }
}

@Composable
internal fun LocalMusicTools(state: LocalMusicUiState, onSortChange: (LocalMusicSort) -> Unit, onPlayAll: () -> Unit) {
    var sortExpanded by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
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
        Spacer(Modifier.weight(1f))
        ResonotePlainAction(onClick = onPlayAll, enabled = state.visibleMedia.isNotEmpty()) {
            Text(
                text = stringResource(R.string.feature_local_impl_play_all),
                modifier = Modifier.padding(horizontal = 8.dp),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
internal fun LocalSearchInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    clearLabel: String,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        modifier = modifier.height(44.dp),
        shape = ResonoteTokens.shapes.full,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            interactionSource = interactionSource,
            singleLine = true,
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 14.dp),
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
                        Spacer(Modifier.width(12.dp))
                    }
                }
            },
        )
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
            onMoreClick = null,
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
            trailingAction = {
                Box {
                    ResonoteIconButton(
                        label = stringResource(R.string.feature_local_impl_more_actions, media.title),
                        onClick = { menuExpanded = true },
                        enabled = !isDeleting,
                        icon = {
                            Icon(
                                Icons.Rounded.MoreVert,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
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
                }
            },
        )
        if (isDeleting) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp).size(24.dp),
                strokeWidth = 2.dp,
            )
        }
    }
}
