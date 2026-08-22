package com.resonote.feature.library.impl

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.resonote.core.model.UserPlaylist

@Composable
internal fun QuickEntries(
    likedPlaylist: UserPlaylist?,
    likedRequiresLogin: Boolean,
    onLikedClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onCloudClick: () -> Unit,
    onLocalMusicClick: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().height(76.dp).padding(horizontal = 4.dp)) {
        QuickEntry(
            icon = { Icon(Icons.Rounded.Favorite, null) },
            label = stringResource(R.string.feature_library_impl_my_liked),
            onClick = onLikedClick,
            iconColor = MaterialTheme.colorScheme.primary,
            enabled = likedRequiresLogin || likedPlaylist != null,
            modifier = Modifier.weight(1f).testTag("my-liked"),
            iconTestTag = "my-liked-icon",
            horizontalBias = -1f,
        )
        QuickEntry(
            icon = { Icon(Icons.Rounded.History, null) },
            label = stringResource(R.string.feature_library_impl_recent_playback),
            onClick = onHistoryClick,
            iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f).testTag("my-history"),
            iconTestTag = "my-history-icon",
            horizontalBias = -1f / 3f,
        )
        QuickEntry(
            icon = { Icon(Icons.Rounded.Cloud, null) },
            label = stringResource(R.string.feature_library_impl_my_cloud),
            onClick = onCloudClick,
            iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f).testTag("my-cloud"),
            iconTestTag = "my-cloud-icon",
            horizontalBias = 1f / 3f,
        )
        QuickEntry(
            icon = { Icon(Icons.Rounded.LibraryMusic, null) },
            label = stringResource(R.string.feature_library_impl_local_music),
            onClick = onLocalMusicClick,
            iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f).testTag("my-local-music"),
            iconTestTag = "my-local-music-icon",
            horizontalBias = 1f,
        )
    }
}

@Composable
private fun QuickEntry(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit,
    iconColor: Color,
    modifier: Modifier,
    iconTestTag: String? = null,
    horizontalBias: Float,
    enabled: Boolean = true,
) {
    var labelWidth by remember(label) { mutableIntStateOf(0) }
    val iconWidth = with(LocalDensity.current) { 44.dp.roundToPx() }
    val edgeLabelOffset = ((labelWidth - iconWidth).coerceAtLeast(0) + 1) / 2
    Surface(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        color = Color.Transparent,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = BiasAlignment(horizontalBias, 0f),
        ) {
            Column(
                modifier = Modifier.width(44.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Surface(
                    modifier = Modifier
                        .size(44.dp)
                        .then(if (iconTestTag == null) Modifier else Modifier.testTag(iconTestTag)),
                    shape = CircleShape,
                    color = if (iconColor == MaterialTheme.colorScheme.primary) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    contentColor = if (enabled) {
                        iconColor
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    },
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) { icon() }
                    }
                }
                Text(
                    label,
                    Modifier
                        .padding(top = 8.dp)
                        .wrapContentWidth(unbounded = true)
                        .offset {
                            IntOffset(
                                x = when {
                                    horizontalBias <= -1f -> edgeLabelOffset
                                    horizontalBias >= 1f -> -edgeLabelOffset
                                    else -> 0
                                },
                                y = 0,
                            )
                        },
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false,
                    onTextLayout = { labelWidth = it.size.width },
                )
            }
        }
    }
}
