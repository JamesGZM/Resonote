package com.resonote.core.datastore.proto

import com.google.protobuf.CodedInputStream
import com.google.protobuf.CodedOutputStream
import java.io.InputStream
import java.io.OutputStream

/** Lite representation of playback_preferences.proto using the standard protobuf wire format. */
data class PlaybackPreferences(
    val playbackSpeedPercent: Int = 0,
    val onlinePlaybackQuality: String = "",
    val playbackMode: String = "",
    val gaplessEnabled: Boolean = false,
    val crossfadeDuration: String = "",
    val loudnessNormalizationEnabled: Boolean = false,
    val audioFocusPolicy: String = "",
    val gaplessConfigured: Boolean = false,
    val equalizerEnabled: Boolean = false,
    val equalizerLowDb: Int = 0,
    val equalizerMidDb: Int = 0,
    val equalizerHighDb: Int = 0,
    val equalizerCustom: Boolean = false,
) {
    fun writeTo(output: OutputStream) {
        CodedOutputStream.newInstance(output).apply {
            if (playbackSpeedPercent != 0) writeUInt32(1, playbackSpeedPercent)
            if (onlinePlaybackQuality.isNotEmpty()) writeString(2, onlinePlaybackQuality)
            if (playbackMode.isNotEmpty()) writeString(3, playbackMode)
            if (gaplessEnabled) writeBool(4, true)
            if (crossfadeDuration.isNotEmpty()) writeString(5, crossfadeDuration)
            if (loudnessNormalizationEnabled) writeBool(6, true)
            if (audioFocusPolicy.isNotEmpty()) writeString(7, audioFocusPolicy)
            if (gaplessConfigured) writeBool(8, true)
            if (equalizerEnabled) writeBool(9, true)
            if (equalizerLowDb != 0) writeSInt32(10, equalizerLowDb)
            if (equalizerMidDb != 0) writeSInt32(11, equalizerMidDb)
            if (equalizerHighDb != 0) writeSInt32(12, equalizerHighDb)
            if (equalizerCustom) writeBool(13, true)
            flush()
        }
    }

    companion object {
        fun getDefaultInstance(): PlaybackPreferences = PlaybackPreferences()

        fun parseFrom(input: InputStream): PlaybackPreferences {
            val coded = CodedInputStream.newInstance(input)
            var playbackSpeedPercent = 0
            var onlinePlaybackQuality = ""
            var playbackMode = ""
            var gaplessEnabled = false
            var crossfadeDuration = ""
            var loudnessNormalizationEnabled = false
            var audioFocusPolicy = ""
            var gaplessConfigured = false
            var equalizerEnabled = false
            var equalizerLowDb = 0
            var equalizerMidDb = 0
            var equalizerHighDb = 0
            var equalizerCustom = false
            while (!coded.isAtEnd) {
                when (val tag = coded.readTag()) {
                    0 -> break
                    8 -> playbackSpeedPercent = coded.readUInt32()
                    18 -> onlinePlaybackQuality = coded.readString()
                    26 -> playbackMode = coded.readStringRequireUtf8()
                    32 -> gaplessEnabled = coded.readBool()
                    42 -> crossfadeDuration = coded.readStringRequireUtf8()
                    48 -> loudnessNormalizationEnabled = coded.readBool()
                    58 -> audioFocusPolicy = coded.readStringRequireUtf8()
                    64 -> gaplessConfigured = coded.readBool()
                    72 -> equalizerEnabled = coded.readBool()
                    80 -> equalizerLowDb = coded.readSInt32()
                    88 -> equalizerMidDb = coded.readSInt32()
                    96 -> equalizerHighDb = coded.readSInt32()
                    104 -> equalizerCustom = coded.readBool()
                    else -> coded.skipField(tag)
                }
            }
            return PlaybackPreferences(
                playbackSpeedPercent,
                onlinePlaybackQuality,
                playbackMode,
                gaplessEnabled,
                crossfadeDuration,
                loudnessNormalizationEnabled,
                audioFocusPolicy,
                gaplessConfigured,
                equalizerEnabled,
                equalizerLowDb,
                equalizerMidDb,
                equalizerHighDb,
                equalizerCustom,
            )
        }
    }
}
