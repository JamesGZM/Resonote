package com.resonote.core.datastore.proto

import com.google.protobuf.CodedInputStream
import com.google.protobuf.CodedOutputStream
import java.io.InputStream
import java.io.OutputStream

/** Lite representation of playback_session_snapshot.proto using the standard protobuf wire format. */
data class PlaybackSessionSnapshot(val json: String = "") {
    fun writeTo(output: OutputStream) {
        CodedOutputStream.newInstance(output).apply {
            if (json.isNotEmpty()) writeString(1, json)
            flush()
        }
    }

    companion object {
        fun getDefaultInstance(): PlaybackSessionSnapshot = PlaybackSessionSnapshot()

        fun parseFrom(input: InputStream): PlaybackSessionSnapshot {
            val coded = CodedInputStream.newInstance(input)
            var json = ""
            while (!coded.isAtEnd) {
                when (val tag = coded.readTag()) {
                    0 -> break
                    10 -> json = coded.readStringRequireUtf8()
                    else -> coded.skipField(tag)
                }
            }
            return PlaybackSessionSnapshot(json)
        }
    }
}
