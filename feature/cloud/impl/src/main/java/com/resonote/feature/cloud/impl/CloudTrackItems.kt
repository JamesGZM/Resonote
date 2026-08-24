@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.resonote.feature.cloud.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.resonote.core.designsystem.component.ResonoteMusicItem
import com.resonote.core.model.CloudTrack

@Composable
internal fun CloudTrackRow(
    track: CloudTrack,
    isPlaying: Boolean,
    isResolving: Boolean,
    onPlay: () -> Unit,
    onAppend: () -> Unit,
) {
    ResonoteMusicItem(
        title = track.title,
        supportingText = listOfNotNull(track.artist, track.album)
            .filter(String::isNotBlank)
            .joinToString(" · ")
            .ifBlank { stringResource(R.string.feature_cloud_impl_cloud_unknown_artist) },
        duration = track.durationMillis.durationLabel(),
        onClick = onPlay,
        onMoreClick = null,
        isPlaying = isPlaying,
        artwork = { CloudArtwork(track, Modifier.fillMaxSize()) },
        trailingAction = {
            if (isResolving) {
                CircularProgressIndicator(modifier = Modifier.padding(12.dp).size(22.dp), strokeWidth = 2.dp)
            } else {
                IconButton(onClick = onAppend) {
                    Icon(Icons.Rounded.Add, stringResource(R.string.feature_cloud_impl_cloud_append_track, track.title))
                }
            }
        },
    )
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
