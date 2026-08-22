@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.resonote.feature.history.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.resonote.core.designsystem.component.ResonoteButton
import com.resonote.feature.history.api.HistoryTab

@Composable
internal fun ArchiveCard(
    tab: HistoryTab,
    count: Int,
    canPlay: Boolean,
    onPlayAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = if (tab == HistoryTab.Online) {
        listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.surfaceContainerLow)
    } else {
        listOf(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.surfaceContainerLow)
    }
    Card(
        modifier = modifier.fillMaxWidth().testTag("history-archive"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().background(Brush.linearGradient(colors)).padding(22.dp),
        ) {
            Text(
                stringResource(R.string.feature_history_impl_archive_label),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(
                    if (tab == HistoryTab.Online) {
                        R.string.feature_history_impl_online_archive
                    } else {
                        R.string.feature_history_impl_device_archive
                    },
                ),
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(
                    if (tab == HistoryTab.Online) {
                        R.string.feature_history_impl_online_archive_body
                    } else {
                        R.string.feature_history_impl_device_archive_body
                    },
                ),
                modifier = Modifier.padding(top = 6.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.feature_history_impl_track_count, count),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                ResonoteButton(
                    label = stringResource(R.string.feature_history_impl_play_all),
                    onClick = onPlayAll,
                    enabled = canPlay,
                    leadingIcon = { Icon(Icons.Rounded.PlayArrow, contentDescription = null) },
                )
            }
        }
    }
}
