package com.resonote.core.karaoke.service

import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.audio.DefaultGainProvider
import androidx.media3.common.audio.GainProcessor
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import com.resonote.core.data.KaraokeRenderInput
import java.io.File
import kotlin.math.pow

internal object KaraokeCompositionFactory {
    fun create(input: KaraokeRenderInput): Composition {
        val sequences = mutableListOf<EditedMediaItemSequence>()
        val backingBuilder = EditedMediaItemSequence.Builder(setOf(C.TRACK_TYPE_AUDIO))
        backingClips(input).forEach { clip ->
            val mediaItem = MediaItem.Builder()
                .setUri(File(clip.path).toUri())
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(clip.startMillis)
                        .setEndPositionMs(clip.endMillis)
                        .build(),
                )
                .build()
            backingBuilder.addItem(
                EditedMediaItem.Builder(mediaItem)
                    .setRemoveVideo(true)
                    .setEffects(
                        Effects(
                            listOf(gainProcessor(input.project.mixSettings.accompanimentGainDb)),
                            emptyList(),
                        ),
                    )
                    .build(),
            )
        }
        sequences += backingBuilder.build()
        input.segments.forEach { renderSegment ->
            val adjustedStart = renderSegment.segment.timelineStartMillis +
                input.project.mixSettings.vocalOffsetMillis - input.project.trimStartMillis
            val clipStart = (-adjustedStart).coerceAtLeast(0L)
            val mediaItem = MediaItem.Builder()
                .setUri(File(renderSegment.path).toUri())
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder().setStartPositionMs(clipStart).build(),
                )
                .build()
            val vocalEffects = Effects(
                listOf(
                    gainProcessor(input.project.mixSettings.vocalGainDb),
                    KaraokeEqAudioProcessor(
                        input.project.mixSettings.vocalLowEqDb,
                        input.project.mixSettings.vocalMidEqDb,
                        input.project.mixSettings.vocalHighEqDb,
                    ),
                ),
                emptyList(),
            )
            val builder = EditedMediaItemSequence.Builder(setOf(C.TRACK_TYPE_AUDIO))
            val gapUs = adjustedStart.coerceAtLeast(0L) * 1_000L
            if (gapUs > 0) builder.addGap(gapUs)
            builder.addItem(
                EditedMediaItem.Builder(mediaItem)
                    .setRemoveVideo(true)
                    .setEffects(vocalEffects)
                    .build(),
            )
            sequences += builder.build()
        }
        return Composition.Builder(sequences).build()
    }

    private fun gainProcessor(db: Float) = GainProcessor(DefaultGainProvider.Builder(10f.pow(db / 20f)).build())
}

internal data class KaraokeBackingClip(val path: String, val startMillis: Long, val endMillis: Long)

internal fun backingClips(input: KaraokeRenderInput): List<KaraokeBackingClip> =
    input.backingSegments.mapIndexedNotNull { index, backing ->
        val start = maxOf(input.project.trimStartMillis, backing.segment.timelineStartMillis)
        val end = input.backingSegments.getOrNull(index + 1)?.segment?.timelineStartMillis
            ?: input.project.durationMillis
        if (end <= start) {
            null
        } else {
            KaraokeBackingClip(backing.path, start, end)
        }
    }
