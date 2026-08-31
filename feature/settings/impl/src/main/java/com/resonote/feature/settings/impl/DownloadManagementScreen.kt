@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.resonote.feature.settings.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resonote.core.designsystem.component.ResonoteEmptyState
import com.resonote.core.designsystem.component.ResonoteRemoteArtwork
import com.resonote.core.designsystem.component.ResonoteTopAppBar
import com.resonote.core.playback.MusicDownload
import com.resonote.core.playback.MusicDownloadController
import com.resonote.core.playback.MusicDownloadState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class DownloadManagementViewModel @Inject constructor(private val controller: MusicDownloadController) : ViewModel() {
    val downloads: StateFlow<List<MusicDownload>> = controller.downloads

    fun pause(id: String) = controller.pause(id)
    fun resume(id: String) = controller.resume(id)
    fun retry(id: String) = controller.retry(id)
    fun remove(id: String) = controller.remove(id)
    fun pauseAll() = controller.pauseAll()
    fun resumeAll() = controller.resumeAll()
}

@Composable
fun DownloadManagementRoute(
    onBack: () -> Unit,
    bottomContentPadding: Dp = 32.dp,
    viewModel: DownloadManagementViewModel = hiltViewModel(),
) {
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    DownloadManagementScreen(
        downloads = downloads,
        onBack = onBack,
        onPause = viewModel::pause,
        onResume = viewModel::resume,
        onRetry = viewModel::retry,
        onRemove = viewModel::remove,
        onPauseAll = viewModel::pauseAll,
        onResumeAll = viewModel::resumeAll,
        bottomContentPadding = bottomContentPadding,
    )
}

@Composable
internal fun DownloadManagementScreen(
    downloads: List<MusicDownload>,
    onBack: () -> Unit,
    onPause: (String) -> Unit,
    onResume: (String) -> Unit,
    onRetry: (String) -> Unit,
    onRemove: (String) -> Unit,
    onPauseAll: () -> Unit,
    onResumeAll: () -> Unit,
    modifier: Modifier = Modifier,
    bottomContentPadding: Dp = 32.dp,
) {
    var pendingRemoval by remember { mutableStateOf<MusicDownload?>(null) }
    val activeCount = downloads.count(MusicDownload::isActive)
    val failedCount = downloads.count { it.state == MusicDownloadState.Failed }
    val completedCount = downloads.count { it.state == MusicDownloadState.Completed }
    val hasProgressing = downloads.any {
        it.state == MusicDownloadState.Preparing ||
            it.state == MusicDownloadState.Queued ||
            it.state == MusicDownloadState.Downloading
    }
    val hasPaused = downloads.any { it.state == MusicDownloadState.Paused }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ResonoteTopAppBar(
                title = { Text(stringResource(R.string.feature_settings_impl_download_management)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            stringResource(R.string.feature_settings_impl_back),
                        )
                    }
                },
            )
        },
    ) { scaffoldPadding ->
        if (downloads.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(scaffoldPadding).padding(bottom = bottomContentPadding),
                contentAlignment = Alignment.Center,
            ) {
                ResonoteEmptyState(
                    title = stringResource(R.string.feature_settings_impl_download_empty_title),
                    message = stringResource(R.string.feature_settings_impl_download_empty_body),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(scaffoldPadding).testTag("download-management-list"),
                contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = bottomContentPadding),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "summary") {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    stringResource(
                                        R.string.feature_settings_impl_download_summary,
                                        activeCount,
                                        completedCount,
                                        failedCount,
                                    ),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    stringResource(R.string.feature_settings_impl_download_private_storage),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            if (hasProgressing) {
                                TextButton(onClick = onPauseAll) {
                                    Text(stringResource(R.string.feature_settings_impl_download_pause_all))
                                }
                            } else if (hasPaused) {
                                TextButton(onClick = onResumeAll) {
                                    Text(stringResource(R.string.feature_settings_impl_download_resume_all))
                                }
                            }
                        }
                    }
                }
                items(downloads, key = MusicDownload::id) { download ->
                    DownloadRow(
                        download = download,
                        onPause = { onPause(download.id) },
                        onResume = { onResume(download.id) },
                        onRetry = { onRetry(download.id) },
                        onRemove = { pendingRemoval = download },
                    )
                }
            }
        }
    }

    pendingRemoval?.let { download ->
        AlertDialog(
            onDismissRequest = { pendingRemoval = null },
            title = { Text(stringResource(R.string.feature_settings_impl_download_remove_title)) },
            text = { Text(stringResource(R.string.feature_settings_impl_download_remove_body, download.song.title)) },
            dismissButton = {
                TextButton(onClick = { pendingRemoval = null }) {
                    Text(stringResource(R.string.feature_settings_impl_cancel))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingRemoval = null
                        onRemove(download.id)
                    },
                ) {
                    Text(stringResource(R.string.feature_settings_impl_download_remove))
                }
            },
        )
    }
}

@Composable
private fun DownloadRow(
    download: MusicDownload,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRetry: () -> Unit,
    onRemove: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ResonoteRemoteArtwork(
                    model = download.song.coverUrl,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        download.song.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        download.statusLabel(),
                        color = if (download.state == MusicDownloadState.Failed) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                when (download.state) {
                    MusicDownloadState.Preparing -> CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    MusicDownloadState.Queued,
                    MusicDownloadState.Downloading,
                    -> IconButton(onClick = onPause) {
                        Icon(Icons.Rounded.Pause, stringResource(R.string.feature_settings_impl_download_pause))
                    }
                    MusicDownloadState.Paused -> IconButton(onClick = onResume) {
                        Icon(Icons.Rounded.PlayArrow, stringResource(R.string.feature_settings_impl_download_resume))
                    }
                    MusicDownloadState.Failed -> IconButton(onClick = onRetry) {
                        Icon(Icons.Rounded.Refresh, stringResource(R.string.feature_settings_impl_retry))
                    }
                    MusicDownloadState.Completed,
                    MusicDownloadState.Removing,
                    -> Unit
                }
                IconButton(onClick = onRemove, enabled = download.state != MusicDownloadState.Removing) {
                    Icon(Icons.Rounded.DeleteOutline, stringResource(R.string.feature_settings_impl_download_remove))
                }
            }
            if (download.state == MusicDownloadState.Downloading) {
                val progress = download.progressPercent
                if (progress == null) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                } else {
                    LinearProgressIndicator(
                        progress = { (progress / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun MusicDownload.statusLabel(): String = when (state) {
    MusicDownloadState.Preparing -> stringResource(R.string.feature_settings_impl_download_preparing)
    MusicDownloadState.Queued -> stringResource(R.string.feature_settings_impl_download_queued)
    MusicDownloadState.Downloading -> progressPercent?.let {
        stringResource(R.string.feature_settings_impl_download_progress, it.toInt())
    } ?: stringResource(R.string.feature_settings_impl_download_downloading)
    MusicDownloadState.Paused -> stringResource(R.string.feature_settings_impl_download_paused)
    MusicDownloadState.Completed -> stringResource(R.string.feature_settings_impl_download_completed)
    MusicDownloadState.Failed -> stringResource(R.string.feature_settings_impl_download_failed)
    MusicDownloadState.Removing -> stringResource(R.string.feature_settings_impl_download_removing)
}
