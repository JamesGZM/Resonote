package com.resonote.core.datastore.proto

import com.google.protobuf.CodedInputStream
import com.google.protobuf.CodedOutputStream
import java.io.InputStream
import java.io.OutputStream

/** Lite representation of playback_preferences.proto using the standard protobuf wire format. */
data class PlaybackPreferences(val playbackSpeedPercent: Int = 0, val onlinePlaybackQuality: String = "") {
    fun writeTo(output: OutputStream) {
        CodedOutputStream.newInstance(output).apply {
            if (playbackSpeedPercent != 0) writeUInt32(1, playbackSpeedPercent)
            if (onlinePlaybackQuality.isNotEmpty()) writeString(2, onlinePlaybackQuality)
            flush()
        }
    }

    companion object {
        fun getDefaultInstance(): PlaybackPreferences = PlaybackPreferences()

        fun parseFrom(input: InputStream): PlaybackPreferences {
            val coded = CodedInputStream.newInstance(input)
            var playbackSpeedPercent = 0
            var onlinePlaybackQuality = ""
            while (!coded.isAtEnd) {
                when (val tag = coded.readTag()) {
                    0 -> break
                    8 -> playbackSpeedPercent = coded.readUInt32()
                    18 -> onlinePlaybackQuality = coded.readString()
                    else -> coded.skipField(tag)
                }
            }
            return PlaybackPreferences(playbackSpeedPercent, onlinePlaybackQuality)
        }
    }
}
