package com.resonote.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.resonote.core.designsystem.tokens.ResonoteTokens

enum class ResonoteArtworkState {
    LOADED,
    LOADING,
    MISSING,
}

@Composable
fun ResonoteArtwork(
    state: ResonoteArtworkState,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    shape: Shape = ResonoteTokens.artworkShapes.compact,
    artwork: @Composable BoxScope.() -> Unit = {},
) {
    val placeholderColor = if (state == ResonoteArtworkState.LOADED) {
        Color.Transparent
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest
    }
    Box(
        modifier = modifier
            .clip(shape)
            .background(placeholderColor)
            .semantics {
                if (contentDescription != null) this.contentDescription = contentDescription
            },
        contentAlignment = Alignment.Center,
    ) {
        if (state == ResonoteArtworkState.LOADED) {
            artwork()
        } else {
            ArtworkPlaceholderMarks()
        }
    }
}

@Composable
fun ResonoteRemoteArtwork(
    model: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    fallback: @Composable BoxScope.() -> Unit = {
        Box(
            modifier = Modifier.matchParentSize().background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            ArtworkPlaceholderMarks()
        }
    },
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        fallback()
        model?.takeIf(String::isNotBlank)?.let { url ->
            AsyncImage(
                model = url,
                contentDescription = contentDescription,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
private fun ArtworkPlaceholderMarks() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ResonoteTokens.spacing.space2),
    ) {
        Box(
            Modifier
                .width(32.dp)
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.22f)),
        )
        Box(
            Modifier
                .width(20.dp)
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.16f)),
        )
    }
}

@Composable
fun ResonoteQualityBadge(label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.heightIn(min = 20.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = MaterialTheme.shapes.extraSmall,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
fun ResonoteVipBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.heightIn(min = 20.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.extraSmall,
    ) {
        Text(
            text = "VIP",
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
        )
    }
}

@Composable
internal fun DefaultSongArtwork(seed: String) {
    DefaultArtwork(seed) {
        Icon(
            imageVector = Icons.Rounded.Album,
            contentDescription = null,
            modifier = Modifier.size(30.dp),
            tint = ResonoteTokens.systemColors.onMediaCanvas.copy(alpha = 0.9f),
        )
    }
}

@Composable
internal fun DefaultPlaylistArtwork(seed: String) {
    DefaultArtwork(seed) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = ResonoteTokens.systemColors.onMediaCanvas.copy(alpha = 0.9f),
        )
    }
}

@Composable
private fun DefaultArtwork(seed: String, icon: @Composable () -> Unit) {
    val palettes = listOf(
        listOf(Color(0xFF5A061B), Color(0xFFE31353), Color(0xFFFF8DA9)),
        listOf(Color(0xFF042E48), Color(0xFF0879BC), Color(0xFFBBD9F4)),
        listOf(Color(0xFF20164B), Color(0xFF786EDB), Color(0xFFF4A9BC)),
        listOf(Color(0xFF6F2E19), Color(0xFFE38A52), Color(0xFFFFD9A8)),
        listOf(Color(0xFF123D36), Color(0xFF3A8068), Color(0xFFC6D9A8)),
    )
    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.linearGradient(palettes[(seed.hashCode() and Int.MAX_VALUE) % palettes.size]),
        ),
        contentAlignment = Alignment.Center,
    ) {
        icon()
    }
}
