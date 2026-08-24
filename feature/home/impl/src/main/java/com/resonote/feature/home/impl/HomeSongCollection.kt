package com.resonote.feature.home.impl

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.resonote.core.designsystem.component.ResonoteArtworkState
import com.resonote.core.designsystem.component.ResonoteMusicItem

@Composable
internal fun SongCollection(
    songs: List<HomeSongUiModel>,
    playingMediaId: String?,
    onSongClick: (HomeSongUiModel) -> Unit,
    onSongMoreClick: (HomeSongUiModel) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
    ) {
        Column {
            songs.forEach { song ->
                ResonoteMusicItem(
                    title = song.title,
                    supportingText = song.artist,
                    duration = song.duration,
                    qualityLabel = song.qualityLabel,
                    isVip = song.isVip,
                    isPlaying = song.id == playingMediaId,
                    onClick = { onSongClick(song) },
                    onMoreClick = { onSongMoreClick(song) },
                    artworkState = if (song.artworkUrl.isNullOrBlank()) {
                        ResonoteArtworkState.MISSING
                    } else {
                        ResonoteArtworkState.LOADED
                    },
                    artworkUrl = song.artworkUrl,
                )
            }
        }
    }
}
