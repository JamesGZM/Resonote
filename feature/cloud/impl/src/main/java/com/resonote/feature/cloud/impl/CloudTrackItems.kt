@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.resonote.feature.cloud.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.resonote.core.model.CloudTrack

@Composable
internal fun CloudTrackRow(
    track: CloudTrack,
    isPlaying: Boolean,
    isResolving: Boolean,
    onPlay: () -> Unit,
    onAppend: () -> Unit,
) {
    Card(
        onClick = onPlay,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor =
            if (isPlaying) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
        shape = MaterialTheme.shapes.large,
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            CloudArtwork(track, Modifier.size(56.dp))
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    track.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOfNotNull(track.artist, track.album)
                        .filter(String::isNotBlank)
                        .joinToString(" · ")
                        .ifBlank {
                            stringResource(R.string.feature_cloud_impl_cloud_unknown_artist)
                        },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    track.durationMillis.durationLabel(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            if (isResolving) {
                CircularProgressIndicator(modifier = Modifier.padding(12.dp).size(22.dp), strokeWidth = 2.dp)
            } else {
                IconButton(onClick = onAppend) {
                    Icon(Icons.Rounded.Add, stringResource(R.string.feature_cloud_impl_cloud_append_track, track.title))
                }
            }
        }
    }
}

@Composable
internal fun CloudTrackGridCard(
    track: CloudTrack,
    isPlaying: Boolean,
    isResolving: Boolean,
    onPlay: () -> Unit,
    onAppend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onPlay,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor =
            if (isPlaying) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column {
            CloudArtwork(track, Modifier.fillMaxWidth().aspectRatio(1f))
            Column(modifier = Modifier.padding(start = 12.dp, top = 10.dp, end = 6.dp, bottom = 8.dp)) {
                Text(
                    track.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    track.artist?.takeIf(String::isNotBlank)
                        ?: stringResource(R.string.feature_cloud_impl_cloud_unknown_artist),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        track.durationMillis.durationLabel(),
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                    )
                    if (isResolving) {
                        CircularProgressIndicator(modifier = Modifier.padding(10.dp).size(20.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = onAppend) {
                            Icon(
                                Icons.Rounded.Add,
                                stringResource(R.string.feature_cloud_impl_cloud_append_track, track.title),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun CloudArtwork(track: CloudTrack, modifier: Modifier) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(
                Brush.linearGradient(
                    listOf(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.primary),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Rounded.Cloud,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondary,
            modifier = Modifier.size(25.dp),
        )
        track.coverUrl?.let {
            AsyncImage(
                model = it,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
