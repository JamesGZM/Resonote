package com.resonote.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.resonote.core.designsystem.component.ResonoteArtworkState
import com.resonote.core.designsystem.component.ResonoteMusicItem
import com.resonote.core.designsystem.component.ResonotePlaylistItem
import com.resonote.core.designsystem.component.ResonotePlaylistMetadata
import com.resonote.core.designsystem.tokens.ResonoteTokens

@Composable
internal fun MusicComponentsCatalog() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.shapes.extraLarge)
            .padding(vertical = 8.dp),
    ) {
        ResonoteMusicItem(
            title = "那些年我们一起听过的歌",
            supportingText = "陈粒",
            duration = "5:41",
            qualityLabel = "LOSSLESS",
            isVip = true,
            onClick = {},
            onMoreClick = {},
            artwork = { CatalogArtwork(listOf(Color(0xFF20164B), Color(0xFFF4A9BC))) },
        )
        ResonoteMusicItem(
            title = "静默轨道",
            supportingText = "Resonote Ensemble",
            duration = "4:12",
            qualityLabel = "HI-RES",
            isVip = true,
            isPlaying = true,
            onClick = {},
            onMoreClick = {},
            artwork = { CatalogArtwork(listOf(Color(0xFF5A061B), Color(0xFFFF8DA9))) },
        )
        ResonoteMusicItem(
            title = "Loading",
            supportingText = "Loading",
            duration = "--:--",
            artworkState = ResonoteArtworkState.LOADING,
            onClick = {},
            onMoreClick = {},
        )
        ResonoteMusicItem(
            title = "未收录封面",
            supportingText = "Resonote",
            duration = "3:36",
            artworkState = ResonoteArtworkState.MISSING,
            onClick = {},
            onMoreClick = {},
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = ResonoteTokens.spacing.space4),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ResonotePlaylistItem(
            metadata = ResonotePlaylistMetadata("深夜独白：安静的陪伴", "148.7万"),
            onClick = {},
            modifier = Modifier.weight(1f),
            artwork = { CatalogArtwork(listOf(Color(0xFF20164B), Color(0xFF786EDB))) },
        )
        ResonotePlaylistItem(
            metadata = ResonotePlaylistMetadata("未收录封面"),
            onClick = {},
            modifier = Modifier.weight(1f),
            artworkState = ResonoteArtworkState.MISSING,
        )
    }
}

@Composable
private fun CatalogArtwork(colors: List<Color>) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(colors)),
    )
}
