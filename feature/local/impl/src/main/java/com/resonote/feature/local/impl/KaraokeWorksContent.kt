@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.resonote.feature.local.impl

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.resonote.core.designsystem.component.ResonoteDestructiveTextButton
import com.resonote.core.model.KaraokeMixSettings
import com.resonote.core.model.KaraokeProject
import com.resonote.core.model.KaraokeProjectId
import com.resonote.core.model.KaraokeProjectStatus
import kotlin.math.roundToInt

@Composable
internal fun KaraokeWorksContent(
    state: LocalMusicUiState,
    contentPadding: PaddingValues,
    onToggleSelection: (KaraokeProjectId) -> Unit,
    onSelectAll: () -> Unit,
    onDeleteSelected: () -> Unit,
    onExportSelected: () -> Unit,
    onExport: (KaraokeProjectId) -> Unit,
    onPreview: (KaraokeProjectId) -> Unit,
    onEdit: (KaraokeProjectId) -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (state.karaokeProjects.isEmpty()) {
            item {
                Column(
                    Modifier.fillParentMaxHeight(0.55f).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Rounded.Mic, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(
                        stringResource(R.string.feature_local_impl_karaoke_empty_title),
                        Modifier.padding(top = 16.dp),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        stringResource(R.string.feature_local_impl_karaoke_empty_body),
                        Modifier.padding(top = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            item(key = "karaoke-batch-tools") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.feature_local_impl_karaoke_count, state.karaokeProjects.size),
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        TextButton(onClick = onSelectAll) {
                            Text(
                                stringResource(
                                    if (state.selectedProjectIds.size == state.karaokeProjects.size) {
                                        R.string.feature_local_impl_deselect_all
                                    } else {
                                        R.string.feature_local_impl_select_all
                                    },
                                ),
                            )
                        }
                    }
                    if (state.selectedProjectIds.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FilledTonalButton(onClick = onExportSelected, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Rounded.Download, null)
                                Text(
                                    stringResource(
                                        R.string.feature_local_impl_export_selected,
                                        state.selectedProjectIds.size,
                                    ),
                                    Modifier.padding(start = 8.dp),
                                )
                            }
                            OutlinedButton(onClick = { confirmDelete = true }) {
                                Icon(Icons.Rounded.Delete, stringResource(R.string.feature_local_impl_delete))
                            }
                        }
                    }
                }
            }
            items(state.karaokeProjects, key = { it.id.value }) { project ->
                KaraokeWorkRow(
                    project = project,
                    selected = project.id in state.selectedProjectIds,
                    playing = state.preview.projectId == project.id && state.preview.isPlaying,
                    onToggleSelection = { onToggleSelection(project.id) },
                    onPreview = { onPreview(project.id) },
                    onEdit = { onEdit(project.id) },
                    onExport = { onExport(project.id) },
                )
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            icon = { Icon(Icons.Rounded.Delete, null) },
            title = { Text(stringResource(R.string.feature_local_impl_delete_works_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.feature_local_impl_delete_works_body,
                        state.selectedProjectIds.size,
                    ),
                )
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.feature_local_impl_cancel))
                }
            },
            confirmButton = {
                ResonoteDestructiveTextButton(
                    label = stringResource(R.string.feature_local_impl_delete_confirm),
                    onClick = {
                        confirmDelete = false
                        onDeleteSelected()
                    },
                )
            },
        )
    }
}

@Composable
private fun KaraokeWorkRow(
    project: KaraokeProject,
    selected: Boolean,
    playing: Boolean,
    onToggleSelection: () -> Unit,
    onPreview: () -> Unit,
    onEdit: () -> Unit,
    onExport: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
    ) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onEdit).padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = selected, onCheckedChange = { onToggleSelection() })
            AsyncImage(
                project.artworkUri,
                null,
                Modifier.size(58.dp).padding(2.dp),
                contentScale = ContentScale.Crop,
            )
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    project.songTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOfNotNull(project.artist, project.status.label()).joinToString(" · "),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                )
            }
            IconButton(onClick = onPreview) {
                Icon(
                    if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    stringResource(R.string.feature_local_impl_preview),
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Rounded.Edit, stringResource(R.string.feature_local_impl_edit_mix))
            }
            IconButton(onClick = onExport) {
                Icon(Icons.Rounded.Download, stringResource(R.string.feature_local_impl_export))
            }
        }
    }
}

@Composable
internal fun KaraokeMixEditorSheet(
    project: KaraokeProject,
    onDismiss: () -> Unit,
    onSave: (KaraokeMixSettings) -> Unit,
) {
    var settings by remember(project.id) { mutableStateOf(project.mixSettings) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                stringResource(R.string.feature_local_impl_mix_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                project.songTitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            MixSlider(R.string.feature_local_impl_vocal_gain, settings.vocalGainDb, -12f..12f, "dB") {
                settings = settings.copy(vocalGainDb = it)
            }
            MixSlider(
                R.string.feature_local_impl_accompaniment_gain,
                settings.accompanimentGainDb,
                -12f..12f,
                "dB",
            ) { settings = settings.copy(accompanimentGainDb = it) }
            MixSlider(R.string.feature_local_impl_eq_low, settings.vocalLowEqDb, -12f..12f, "dB") {
                settings = settings.copy(vocalLowEqDb = it)
            }
            MixSlider(R.string.feature_local_impl_eq_mid, settings.vocalMidEqDb, -12f..12f, "dB") {
                settings = settings.copy(vocalMidEqDb = it)
            }
            MixSlider(R.string.feature_local_impl_eq_high, settings.vocalHighEqDb, -12f..12f, "dB") {
                settings = settings.copy(vocalHighEqDb = it)
            }
            MixSlider(
                R.string.feature_local_impl_vocal_offset,
                settings.vocalOffsetMillis.toFloat(),
                -200f..200f,
                "ms",
            ) { settings = settings.copy(vocalOffsetMillis = it.roundToInt()) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.feature_local_impl_cancel)) }
                Button(onClick = { onSave(settings) }) {
                    Text(stringResource(R.string.feature_local_impl_save_mix))
                }
            }
        }
    }
}

@Composable
private fun MixSlider(
    label: Int,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    unit: String,
    onChange: (Float) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(label), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(
            "${if (value > 0) "+" else ""}${value.roundToInt()} $unit",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge,
        )
    }
    Slider(value = value, onValueChange = onChange, valueRange = range)
}

@Composable
private fun KaraokeProjectStatus.label(): String = stringResource(
    when (this) {
        KaraokeProjectStatus.Draft -> R.string.feature_local_impl_status_draft
        KaraokeProjectStatus.Edited -> R.string.feature_local_impl_status_edited
        KaraokeProjectStatus.Exporting -> R.string.feature_local_impl_status_exporting
        KaraokeProjectStatus.Exported -> R.string.feature_local_impl_status_exported
        KaraokeProjectStatus.ExportFailed -> R.string.feature_local_impl_status_export_failed
    },
)
