@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.resonote.feature.local.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.resonote.core.designsystem.component.ResonoteButton
import com.resonote.core.designsystem.component.ResonoteContentPhase
import com.resonote.core.designsystem.component.ResonoteContentStateLayout
import com.resonote.core.designsystem.component.ResonoteDestructiveTextButton
import com.resonote.core.designsystem.component.ResonoteEmptyState
import com.resonote.core.designsystem.component.ResonoteErrorState
import com.resonote.core.designsystem.component.ResonoteTonalIconButton
import com.resonote.core.designsystem.component.ResonoteTopAppBar
import com.resonote.core.model.EqualizerPreset
import com.resonote.core.model.KaraokeMixSettings
import com.resonote.core.model.KaraokeProject
import com.resonote.core.model.KaraokeProjectId
import com.resonote.core.model.KaraokeProjectStatus
import java.util.concurrent.TimeUnit
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
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    val phase = when {
        state.karaokeProjectsLoading -> ResonoteContentPhase.LOADING
        state.karaokeProjectsLoadFailed -> ResonoteContentPhase.ERROR
        state.visibleKaraokeProjects.isEmpty() -> ResonoteContentPhase.EMPTY
        else -> ResonoteContentPhase.CONTENT
    }
    ResonoteContentStateLayout(
        phase = phase,
        modifier = modifier,
        empty = {
            ResonoteEmptyState(
                title = stringResource(
                    if (state.query.isBlank()) {
                        R.string.feature_local_impl_karaoke_empty_title
                    } else {
                        R.string.feature_local_impl_no_results_title
                    },
                ),
                message = if (state.query.isBlank()) {
                    stringResource(R.string.feature_local_impl_karaoke_empty_body)
                } else {
                    stringResource(R.string.feature_local_impl_no_results_body, state.query)
                },
                modifier = Modifier.padding(contentPadding),
            )
        },
        error = {
            ResonoteErrorState(
                onRetry = onRetry,
                title = stringResource(R.string.feature_local_impl_karaoke_load_error_title),
                message = stringResource(R.string.feature_local_impl_karaoke_load_error_body),
                modifier = Modifier.padding(contentPadding),
            )
        },
        content = {
            LazyColumn(
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "karaoke-batch-tools") {
                    KaraokeWorksHeader(
                        count = state.visibleKaraokeProjects.size,
                        selectedCount = state.selectedProjectIds.size,
                        allSelected = state.selectedProjectIds.size == state.visibleKaraokeProjects.size,
                        onSelectAll = onSelectAll,
                        onExportSelected = onExportSelected,
                        onDeleteSelected = { confirmDelete = true },
                    )
                }
                items(state.visibleKaraokeProjects, key = { it.id.value }) { project ->
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
        },
    )
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
private fun KaraokeWorksHeader(
    count: Int,
    selectedCount: Int,
    allSelected: Boolean,
    onSelectAll: () -> Unit,
    onExportSelected: () -> Unit,
    onDeleteSelected: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.feature_local_impl_karaoke_count, count),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(R.string.feature_local_impl_karaoke_library_summary),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            TextButton(onClick = onSelectAll) {
                Text(
                    stringResource(
                        if (allSelected) {
                            R.string.feature_local_impl_deselect_all
                        } else {
                            R.string.feature_local_impl_select_all
                        },
                    ),
                )
            }
        }
        if (selectedCount > 0) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.feature_local_impl_selected_count, selectedCount),
                        Modifier.weight(1f).padding(start = 6.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        style = MaterialTheme.typography.labelLarge,
                    )
                    TextButton(onClick = onExportSelected) {
                        Icon(Icons.Rounded.Download, null, Modifier.size(18.dp))
                        Text(stringResource(R.string.feature_local_impl_export), Modifier.padding(start = 4.dp))
                    }
                    IconButton(onClick = onDeleteSelected) {
                        Icon(Icons.Rounded.Delete, stringResource(R.string.feature_local_impl_delete))
                    }
                }
            }
        }
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
        onClick = onEdit,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        } else if (playing) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.32f)
        } else {
            MaterialTheme.colorScheme.surface
        },
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 8.dp, top = 8.dp, end = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(64.dp)) {
                KaraokeProjectArtwork(project)
                Surface(
                    onClick = onToggleSelection,
                    modifier = Modifier.align(Alignment.TopEnd).size(28.dp),
                    shape = RoundedCornerShape(bottomStart = 10.dp, topEnd = 14.dp),
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.scrim.copy(alpha = 0.52f)
                    },
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(
                        if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                        stringResource(
                            if (selected) {
                                R.string.feature_local_impl_deselect_work
                            } else {
                                R.string.feature_local_impl_select_work
                            },
                            project.songTitle,
                        ),
                        Modifier.padding(5.dp),
                    )
                }
            }
            Column(
                Modifier.weight(1f).padding(start = 12.dp, end = 2.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(
                    project.songTitle,
                    color = if (playing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    buildString {
                        append(project.artist ?: stringResource(R.string.feature_local_impl_unknown_artist))
                        append(" · ")
                        append(project.status.label())
                        append(" · ")
                        append(project.durationMillis.asDurationLabel())
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            KaraokeWorkIconButton(
                icon = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                label = stringResource(R.string.feature_local_impl_preview),
                highlighted = playing,
                onClick = onPreview,
            )
            KaraokeWorkIconButton(
                icon = Icons.Rounded.Edit,
                label = stringResource(R.string.feature_local_impl_edit_mix),
                onClick = onEdit,
            )
            KaraokeWorkIconButton(
                icon = Icons.Rounded.Download,
                label = stringResource(R.string.feature_local_impl_export),
                onClick = onExport,
            )
        }
    }
}

@Composable
private fun KaraokeProjectArtwork(project: KaraokeProject) {
    val paletteIndex = project.songTitle.hashCode().ushr(1) % 3
    val containerColor = when (paletteIndex) {
        0 -> MaterialTheme.colorScheme.primaryContainer
        1 -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.tertiaryContainer
    }
    val contentColor = when (paletteIndex) {
        0 -> MaterialTheme.colorScheme.onPrimaryContainer
        1 -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onTertiaryContainer
    }
    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        contentColor = contentColor,
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (project.artworkUri.isNullOrBlank()) {
                Text(
                    project.songTitle.firstArtworkCharacter(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
            } else {
                AsyncImage(
                    project.artworkUri,
                    null,
                    Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}

@Composable
private fun KaraokeWorkIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    highlighted: Boolean = false,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
        Icon(
            icon,
            label,
            Modifier.size(20.dp),
            tint = if (highlighted) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

private fun String.firstArtworkCharacter(): String = trim()
    .firstOrNull { !it.isWhitespace() }
    ?.uppercaseChar()
    ?.toString()
    ?: "K"

@Composable
internal fun KaraokeMixEditorScreen(
    project: KaraokeProject,
    previewing: Boolean,
    onBack: () -> Unit,
    onPreview: (KaraokeMixSettings) -> Unit,
    onSave: (KaraokeMixSettings) -> Unit,
    bottomContentPadding: androidx.compose.ui.unit.Dp,
) {
    var settings by remember(project.id) { mutableStateOf(project.mixSettings) }
    var customEqualizerEditing by remember(project.id) {
        mutableStateOf(project.mixSettings.equalizerPreset == EqualizerPreset.Custom)
    }
    val selectedEqualizerPreset = if (customEqualizerEditing) {
        EqualizerPreset.Custom
    } else {
        settings.equalizerPreset
    }
    Scaffold(
        topBar = {
            ResonoteTopAppBar(
                title = { Text(stringResource(R.string.feature_local_impl_mix_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.feature_local_impl_back))
                    }
                },
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
            ) {
                Row(
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp)
                        .padding(bottom = bottomContentPadding),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ResonoteTonalIconButton(
                        label = stringResource(R.string.feature_local_impl_preview_effect),
                        onClick = { onPreview(settings) },
                        modifier = Modifier.size(52.dp),
                    ) {
                        Icon(if (previewing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null)
                    }
                    ResonoteButton(
                        label = stringResource(R.string.feature_local_impl_save_mix),
                        onClick = { onSave(settings) },
                        modifier = Modifier.weight(1f).height(52.dp),
                        leadingIcon = { Icon(Icons.Rounded.Check, null) },
                    )
                }
            }
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        ) {
            item {
                KaraokeMixProjectHeader(
                    project = project,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }
            item { HorizontalDivider() }
            item {
                MixSection(
                    title = stringResource(R.string.feature_local_impl_mix_balance_section),
                    modifier = Modifier.padding(vertical = 20.dp),
                ) {
                    MixSlider(R.string.feature_local_impl_vocal_gain, settings.vocalGainDb, -12f..12f, "dB") {
                        settings = settings.copy(vocalGainDb = it)
                    }
                    MixSlider(
                        R.string.feature_local_impl_accompaniment_gain,
                        settings.accompanimentGainDb,
                        -12f..12f,
                        "dB",
                    ) { settings = settings.copy(accompanimentGainDb = it) }
                }
            }
            item { HorizontalDivider() }
            item {
                MixSection(
                    title = stringResource(R.string.feature_local_impl_eq_section),
                    modifier = Modifier.padding(top = 20.dp, bottom = 24.dp),
                ) {
                    Text(
                        stringResource(R.string.feature_local_impl_eq_hint),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    EqualizerPreset.entries
                        .filter { it != EqualizerPreset.Off && it != EqualizerPreset.Custom }
                        .chunked(2)
                        .forEach { presets ->
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                presets.forEach { preset ->
                                    KaraokeEqualizerPresetCard(
                                        preset = preset,
                                        selected = selectedEqualizerPreset == preset,
                                        onClick = {
                                            settings = settings.withEqualizerPreset(preset)
                                            customEqualizerEditing = false
                                        },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                if (presets.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                    ) {
                        Column(Modifier.padding(horizontal = 18.dp, vertical = 18.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    stringResource(R.string.feature_local_impl_eq_preset_custom),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    selectedEqualizerPreset.label(),
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                            Text(
                                stringResource(R.string.feature_local_impl_eq_custom_hint),
                                Modifier.padding(top = 4.dp, bottom = 8.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            KaraokeEqualizerBandSlider(
                                label = R.string.feature_local_impl_eq_low,
                                value = settings.vocalLowEqDb,
                                testTag = "karaoke-eq-low",
                                onChange = {
                                    settings = settings.copy(vocalLowEqDb = it)
                                    customEqualizerEditing = true
                                },
                            )
                            KaraokeEqualizerBandSlider(
                                label = R.string.feature_local_impl_eq_mid,
                                value = settings.vocalMidEqDb,
                                testTag = "karaoke-eq-mid",
                                onChange = {
                                    settings = settings.copy(vocalMidEqDb = it)
                                    customEqualizerEditing = true
                                },
                            )
                            KaraokeEqualizerBandSlider(
                                label = R.string.feature_local_impl_eq_high,
                                value = settings.vocalHighEqDb,
                                testTag = "karaoke-eq-high",
                                onChange = {
                                    settings = settings.copy(vocalHighEqDb = it)
                                    customEqualizerEditing = true
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KaraokeEqualizerPresetCard(
    preset: EqualizerPreset,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 76.dp).testTag("karaoke-eq-preset-${preset.name}"),
        shape = RoundedCornerShape(20.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                preset.label(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                preset.gainsLabel(),
                Modifier.padding(top = 4.dp),
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun KaraokeEqualizerBandSlider(label: Int, value: Float, testTag: String, onChange: (Float) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(stringResource(label), style = MaterialTheme.typography.bodyMedium)
        Text(value.roundToInt().gainLabel(), color = MaterialTheme.colorScheme.primary)
    }
    Slider(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth().testTag(testTag),
        valueRange = -12f..12f,
        steps = 23,
    )
}

@Composable
private fun KaraokeMixProjectHeader(project: KaraokeProject, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(64.dp)) { KaraokeProjectArtwork(project) }
        Column(
            Modifier.weight(1f).padding(start = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                project.songTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                project.artist ?: stringResource(R.string.feature_local_impl_unknown_artist),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${project.status.label()} · ${project.durationMillis.asDurationLabel()}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun EqualizerPreset.label(): String = stringResource(
    when (this) {
        EqualizerPreset.Off -> R.string.feature_local_impl_eq_preset_flat
        EqualizerPreset.Flat -> R.string.feature_local_impl_eq_preset_flat
        EqualizerPreset.BassBoost -> R.string.feature_local_impl_eq_preset_bass
        EqualizerPreset.Pop -> R.string.feature_local_impl_eq_preset_pop
        EqualizerPreset.Rock -> R.string.feature_local_impl_eq_preset_rock
        EqualizerPreset.Jazz -> R.string.feature_local_impl_eq_preset_jazz
        EqualizerPreset.Classical -> R.string.feature_local_impl_eq_preset_classical
        EqualizerPreset.Vocal -> R.string.feature_local_impl_eq_preset_vocal
        EqualizerPreset.Custom -> R.string.feature_local_impl_eq_preset_custom
    },
)

private fun EqualizerPreset.gainsLabel(): String = listOf(lowDb, midDb, highDb)
    .joinToString("  ") { it.gainLabel() }

private fun Int.gainLabel(): String = if (this > 0) "+$this dB" else "$this dB"

@Composable
private fun MixSection(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        content()
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
    Slider(
        value = value,
        onValueChange = onChange,
        valueRange = range,
        modifier = Modifier.fillMaxWidth(),
    )
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

private fun Long.asDurationLabel(): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(this)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(this) % 60
    return "%d:%02d".format(minutes, seconds)
}
