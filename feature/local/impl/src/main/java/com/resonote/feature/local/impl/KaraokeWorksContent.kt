@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.resonote.feature.local.impl

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.resonote.core.designsystem.component.ResonoteContentPhase
import com.resonote.core.designsystem.component.ResonoteContentStateLayout
import com.resonote.core.designsystem.component.ResonoteDestructiveTextButton
import com.resonote.core.designsystem.component.ResonoteEmptyState
import com.resonote.core.designsystem.component.ResonoteErrorState
import com.resonote.core.designsystem.component.ResonoteRotaryKnob
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
    val equalizerPresets = remember {
        listOf(EqualizerPreset.Flat, EqualizerPreset.Custom) +
            EqualizerPreset.entries.filterNot {
                it == EqualizerPreset.Off || it == EqualizerPreset.Flat || it == EqualizerPreset.Custom
            }
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
                actions = {
                    IconButton(onClick = { onSave(settings) }) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = stringResource(R.string.feature_local_impl_save_mix),
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).testTag("karaoke-mix-editor-list"),
            contentPadding = PaddingValues(top = 12.dp, bottom = bottomContentPadding + 12.dp),
        ) {
            item(key = "project") {
                KaraokeMixProjectHeader(
                    project = project,
                    previewing = previewing,
                    onPreview = { onPreview(settings) },
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
                )
            }
            item(key = "balance-title") {
                KaraokeMixSectionTitle(stringResource(R.string.feature_local_impl_mix_balance_section))
            }
            item(key = "balance-controls") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    ResonoteRotaryKnob(
                        title = stringResource(R.string.feature_local_impl_vocal_gain),
                        valueLabel = settings.vocalGainDb.roundToInt().gainLabel(includeUnit = false),
                        value = settings.vocalGainDb,
                        onValueChange = { settings = settings.copy(vocalGainDb = it) },
                        onValueChangeFinished = {},
                        valueRange = -12f..12f,
                        steps = 23,
                        knobSize = 120.dp,
                        modifier = Modifier.weight(1f).testTag("karaoke-vocal-gain"),
                    )
                    ResonoteRotaryKnob(
                        title = stringResource(R.string.feature_local_impl_accompaniment_gain),
                        valueLabel = settings.accompanimentGainDb.roundToInt().gainLabel(includeUnit = false),
                        value = settings.accompanimentGainDb,
                        onValueChange = { settings = settings.copy(accompanimentGainDb = it) },
                        onValueChangeFinished = {},
                        valueRange = -12f..12f,
                        steps = 23,
                        knobSize = 120.dp,
                        modifier = Modifier.weight(1f).testTag("karaoke-accompaniment-gain"),
                    )
                }
            }
            item(key = "equalizer-title") {
                KaraokeMixSectionTitle(stringResource(R.string.feature_local_impl_eq_section))
            }
            item(key = "equalizer-chart") {
                KaraokeEqualizerResponseChart(
                    lowDb = settings.vocalLowEqDb,
                    midDb = settings.vocalMidEqDb,
                    highDb = settings.vocalHighEqDb,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(132.dp)
                        .padding(start = 20.dp, top = 8.dp, end = 20.dp),
                )
            }
            item(key = "equalizer-presets") {
                KaraokeEqualizerPresetTabs(
                    presets = equalizerPresets,
                    selectedPreset = selectedEqualizerPreset,
                    onPresetChange = { preset ->
                        if (preset == EqualizerPreset.Custom) {
                            customEqualizerEditing = true
                        } else {
                            settings = settings.withEqualizerPreset(preset)
                            customEqualizerEditing = false
                        }
                    },
                    modifier = Modifier.height(52.dp),
                )
            }
            item(key = "equalizer-controls") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    ResonoteRotaryKnob(
                        title = stringResource(R.string.feature_local_impl_eq_low),
                        valueLabel = settings.vocalLowEqDb.roundToInt().gainLabel(includeUnit = false),
                        value = settings.vocalLowEqDb,
                        onValueChange = {
                            settings = settings.copy(vocalLowEqDb = it)
                            customEqualizerEditing = true
                        },
                        onValueChangeFinished = {},
                        valueRange = -12f..12f,
                        steps = 23,
                        modifier = Modifier.weight(1f).testTag("karaoke-eq-low"),
                    )
                    ResonoteRotaryKnob(
                        title = stringResource(R.string.feature_local_impl_eq_mid),
                        valueLabel = settings.vocalMidEqDb.roundToInt().gainLabel(includeUnit = false),
                        value = settings.vocalMidEqDb,
                        onValueChange = {
                            settings = settings.copy(vocalMidEqDb = it)
                            customEqualizerEditing = true
                        },
                        onValueChangeFinished = {},
                        valueRange = -12f..12f,
                        steps = 23,
                        modifier = Modifier.weight(1f).testTag("karaoke-eq-mid"),
                    )
                    ResonoteRotaryKnob(
                        title = stringResource(R.string.feature_local_impl_eq_high),
                        valueLabel = settings.vocalHighEqDb.roundToInt().gainLabel(includeUnit = false),
                        value = settings.vocalHighEqDb,
                        onValueChange = {
                            settings = settings.copy(vocalHighEqDb = it)
                            customEqualizerEditing = true
                        },
                        onValueChangeFinished = {},
                        valueRange = -12f..12f,
                        steps = 23,
                        modifier = Modifier.weight(1f).testTag("karaoke-eq-high"),
                    )
                }
            }
        }
    }
}

@Composable
private fun KaraokeMixSectionTitle(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 4.dp),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun KaraokeEqualizerResponseChart(lowDb: Float, midDb: Float, highDb: Float, modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val guide = MaterialTheme.colorScheme.outlineVariant
    val labels = listOf(
        stringResource(R.string.feature_local_impl_eq_low) to lowDb,
        stringResource(R.string.feature_local_impl_eq_mid) to midDb,
        stringResource(R.string.feature_local_impl_eq_high) to highDb,
    )

    Column(modifier.testTag("karaoke-eq-response-chart")) {
        Row(Modifier.fillMaxWidth().height(40.dp)) {
            labels.forEach { (label, value) ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = value.roundToInt().gainLabel(),
                        color = primary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = label,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
        Canvas(Modifier.fillMaxWidth().weight(1f)) {
            val lowX = size.width / 6f
            val midX = size.width / 2f
            val highX = size.width * 5f / 6f
            val zeroY = size.height * 0.58f
            val amplitude = size.height * 0.34f
            fun yFor(value: Float) = zeroY - (value.coerceIn(-12f, 12f) / 12f) * amplitude

            val lowY = yFor(lowDb)
            val midY = yFor(midDb)
            val highY = yFor(highDb)
            val dash = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 5.dp.toPx()))
            listOf(lowX, midX, highX).forEach { x ->
                drawLine(
                    color = guide.copy(alpha = 0.7f),
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dash,
                )
            }
            drawLine(
                color = guide,
                start = Offset(0f, zeroY),
                end = Offset(size.width, zeroY),
                strokeWidth = 1.dp.toPx(),
            )
            val curve = karaokeEqualizerCurve(size.width, lowX, midX, highX, lowY, midY, highY)
            val fill = karaokeEqualizerCurve(size.width, lowX, midX, highX, lowY, midY, highY).apply {
                lineTo(size.width, zeroY)
                lineTo(0f, zeroY)
                close()
            }
            drawPath(fill, primary.copy(alpha = 0.08f))
            drawPath(
                path = curve,
                color = primary,
                style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round),
            )
        }
    }
}

@Composable
private fun KaraokeEqualizerPresetTabs(
    presets: List<EqualizerPreset>,
    selectedPreset: EqualizerPreset,
    onPresetChange: (EqualizerPreset) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth().testTag("karaoke-eq-presets"),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
    ) {
        items(presets, key = EqualizerPreset::name) { preset ->
            val selected = preset == selectedPreset
            Surface(
                onClick = { onPresetChange(preset) },
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .height(40.dp)
                    .testTag("karaoke-eq-preset-${preset.name}")
                    .semantics {
                        this.selected = selected
                        role = Role.Tab
                    },
                shape = RoundedCornerShape(20.dp),
                color = if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.background
                },
                contentColor = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 9.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = preset.label(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

private fun karaokeEqualizerCurve(
    width: Float,
    lowX: Float,
    midX: Float,
    highX: Float,
    lowY: Float,
    midY: Float,
    highY: Float,
): Path = Path().apply {
    moveTo(0f, lowY)
    lineTo(lowX, lowY)
    cubicTo(lowX + width * 0.12f, lowY, midX - width * 0.14f, midY, midX, midY)
    cubicTo(midX + width * 0.14f, midY, highX - width * 0.12f, highY, highX, highY)
    lineTo(width, highY)
}

@Composable
private fun KaraokeMixProjectHeader(
    project: KaraokeProject,
    previewing: Boolean,
    onPreview: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
        ResonoteTonalIconButton(
            label = stringResource(R.string.feature_local_impl_preview_effect),
            onClick = onPreview,
            modifier = Modifier.size(44.dp),
        ) {
            Icon(if (previewing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null)
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

private fun Int.gainLabel(includeUnit: Boolean = true): String {
    val signed = if (this > 0) "+$this" else "$this"
    return if (includeUnit) "$signed dB" else signed
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
