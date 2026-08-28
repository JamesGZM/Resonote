package com.resonote.core.karaoke.service

import com.google.common.truth.Truth.assertThat
import com.resonote.core.data.KaraokeRenderBackingSegment
import com.resonote.core.data.KaraokeRenderInput
import com.resonote.core.data.KaraokeRenderSegment
import com.resonote.core.model.KaraokeAssetId
import com.resonote.core.model.KaraokeBackingSegment
import com.resonote.core.model.KaraokeMixSettings
import com.resonote.core.model.KaraokeProject
import com.resonote.core.model.KaraokeProjectId
import com.resonote.core.model.KaraokeProjectStatus
import com.resonote.core.model.KaraokeRecordingSegment
import com.resonote.core.model.KaraokeSourceMode
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
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
            KaraokeBackingClip("original", 60_000, 10_000, 30_000),
            KaraokeBackingClip("accompaniment", 60_000, 30_000, 60_000),
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
            KaraokeBackingClip("accompaniment", 60_000, 5_000, 60_000),
        )
    }

    @Test
    fun compositionProvidesSourceDurationsForBackingAndVocalItems() {
        val input = renderInput(
            trimStartMillis = 0,
            segments = listOf(backing("original", KaraokeSourceMode.Original, 0)),
            recordings = listOf(
                KaraokeRenderSegment(
                    path = "vocal",
                    segment = KaraokeRecordingSegment(
                        id = "vocal",
                        projectId = PROJECT_ID,
                        timelineStartMillis = 1_000,
                        durationMillis = 2_500,
                        sampleRateHz = 48_000,
                        channelCount = 1,
                        peakAmplitude = 1,
                        nonSilent = true,
                    ),
                ),
            ),
        )

        val composition = KaraokeCompositionFactory.create(input)

        assertThat(composition.sequences[0].editedMediaItems.single().durationUs).isEqualTo(60_000_000L)
        assertThat(composition.sequences[1].editedMediaItems.last().durationUs).isEqualTo(2_500_000L)
    }

    private fun renderInput(
        trimStartMillis: Long,
        segments: List<KaraokeRenderBackingSegment>,
        recordings: List<KaraokeRenderSegment> = emptyList(),
    ) = KaraokeRenderInput(
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
        segments = recordings,
    )

    private fun backing(path: String, sourceMode: KaraokeSourceMode, timelineStartMillis: Long) =
        KaraokeRenderBackingSegment(
            path = path,
            durationMillis = 60_000,
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
