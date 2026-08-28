package com.resonote.core.karaoke.service

import com.google.common.truth.Truth.assertThat
import com.resonote.core.data.KaraokeRenderBackingSegment
import com.resonote.core.data.KaraokeRenderInput
import com.resonote.core.model.KaraokeAssetId
import com.resonote.core.model.KaraokeBackingSegment
import com.resonote.core.model.KaraokeMixSettings
import com.resonote.core.model.KaraokeProject
import com.resonote.core.model.KaraokeProjectId
import com.resonote.core.model.KaraokeProjectStatus
import com.resonote.core.model.KaraokeSourceMode
import org.junit.Test

class KaraokeCompositionFactoryTest {
    @Test
    fun backingClipsRespectTrimAndSourceSwitchBoundaries() {
        val input = renderInput(
            trimStartMillis = 10_000,
            segments = listOf(
                backing("original", KaraokeSourceMode.Original, 0),
                backing("accompaniment", KaraokeSourceMode.Accompaniment, 30_000),
            ),
        )

        assertThat(backingClips(input)).containsExactly(
            KaraokeBackingClip("original", 10_000, 30_000),
            KaraokeBackingClip("accompaniment", 30_000, 60_000),
        ).inOrder()
    }

    @Test
    fun backingClipsUseLastSourceWhenSwitchesSharePosition() {
        val input = renderInput(
            trimStartMillis = 5_000,
            segments = listOf(
                backing("original", KaraokeSourceMode.Original, 5_000),
                backing("accompaniment", KaraokeSourceMode.Accompaniment, 5_000),
            ),
        )

        assertThat(backingClips(input)).containsExactly(
            KaraokeBackingClip("accompaniment", 5_000, 60_000),
        )
    }

    private fun renderInput(trimStartMillis: Long, segments: List<KaraokeRenderBackingSegment>) = KaraokeRenderInput(
        project = KaraokeProject(
            id = PROJECT_ID,
            songHash = "song",
            songTitle = "Song",
            artist = "Artist",
            artworkUri = null,
            sourceMode = KaraokeSourceMode.Mixed,
            trimStartMillis = trimStartMillis,
            status = KaraokeProjectStatus.Draft,
            mixSettings = KaraokeMixSettings(),
            durationMillis = 60_000,
            createdAtEpochMillis = 0,
            updatedAtEpochMillis = 0,
            exportedContentUri = null,
        ),
        backingSegments = segments,
        segments = emptyList(),
    )

    private fun backing(path: String, sourceMode: KaraokeSourceMode, timelineStartMillis: Long) =
        KaraokeRenderBackingSegment(
            path = path,
            segment = KaraokeBackingSegment(
                id = "$path-$timelineStartMillis",
                projectId = PROJECT_ID,
                assetId = KaraokeAssetId(path),
                sourceMode = sourceMode,
                timelineStartMillis = timelineStartMillis,
            ),
        )

    private companion object {
        val PROJECT_ID = KaraokeProjectId("project")
    }
}
