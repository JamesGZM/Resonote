@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.resonote.feature.cloud.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.resonote.core.model.CloudStorage

@Composable
internal fun CloudVaultCard(storage: CloudStorage?, total: Int) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("cloud-vault"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.secondaryContainer,
                            MaterialTheme.colorScheme.surfaceContainerHigh,
                        ),
                    ),
                )
                .padding(22.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(52.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Cloud, contentDescription = null, modifier = Modifier.size(27.dp))
                        }
                    }
                    Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                        Text(
                            stringResource(R.string.feature_cloud_impl_cloud_vault),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            stringResource(R.string.feature_cloud_impl_cloud_total_tracks, total),
                            modifier = Modifier.padding(top = 3.dp),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                if (storage != null && storage.maxBytes > 0) {
                    val used = storage.usedBytes.coerceIn(0, storage.maxBytes)
                    val available = (storage.maxBytes - used).coerceAtLeast(0)
                    LinearProgressIndicator(
                        progress = { used.toFloat() / storage.maxBytes.toFloat() },
                        modifier = Modifier.fillMaxWidth().padding(top = 22.dp).height(7.dp).clip(CircleShape),
                    )
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 9.dp)) {
                        Text(
                            stringResource(
                                R.string.feature_cloud_impl_cloud_storage_used,
                                used.fileSize(),
                                storage.maxBytes.fileSize(),
                            ),
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            stringResource(R.string.feature_cloud_impl_cloud_storage_available, available.fileSize()),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                } else {
                    Text(
                        stringResource(R.string.feature_cloud_impl_cloud_storage_unknown),
                        modifier = Modifier.padding(top = 18.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
internal fun CloudTools(
    state: CloudUiState,
    onQueryChange: (String) -> Unit,
    onSortChange: (CloudSort) -> Unit,
    onViewModeChange: (CloudViewMode) -> Unit,
) {
    var sortExpanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.feature_cloud_impl_cloud_search_label)) },
            placeholder = { Text(stringResource(R.string.feature_cloud_impl_cloud_search_placeholder)) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = if (state.query.isNotEmpty()) {
                {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Rounded.Close, stringResource(R.string.feature_cloud_impl_cloud_clear_search))
                    }
                }
            } else {
                null
            },
            singleLine = true,
            shape = MaterialTheme.shapes.extraLarge,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box {
                FilterChip(
                    selected = state.sort != CloudSort.UploadOrder,
                    onClick = { sortExpanded = true },
                    label = { Text(stringResource(R.string.feature_cloud_impl_cloud_sort, state.sort.label())) },
                    trailingIcon = { Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null) },
                )
                DropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }) {
                    CloudSort.entries.forEach { sort ->
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
            IconButton(onClick = { onViewModeChange(CloudViewMode.List) }) {
                Icon(
                    Icons.AutoMirrored.Rounded.List,
                    stringResource(R.string.feature_cloud_impl_cloud_list_view),
                    tint =
                    if (state.viewMode == CloudViewMode.List) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            IconButton(onClick = { onViewModeChange(CloudViewMode.Grid) }) {
                Icon(
                    Icons.Rounded.GridView,
                    stringResource(R.string.feature_cloud_impl_cloud_grid_view),
                    tint =
                    if (state.viewMode == CloudViewMode.Grid) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}
