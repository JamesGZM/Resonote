package com.resonote.core.datastore.proto

import com.google.protobuf.CodedInputStream
import com.google.protobuf.CodedOutputStream
import java.io.InputStream
import java.io.OutputStream

data class LyricsPreferences(
    val supplementalText: String = "",
    val displayMode: String = "",
    val highlightMode: String = "",
    val textAlignment: String = "",
    val fontSize: String = "",
    val backgroundMode: String = "",
    val translationEnabled: Boolean = false,
    val transliterationEnabled: Boolean = false,
    val supplementalTextFlagsSet: Boolean = false,
    val desktopLyricsEnabled: Boolean = false,
    val desktopLyricsDisplayMode: String = "",
    val desktopLyricsFontSize: String = "",
    val desktopLyricsAutoHideWhenPaused: Boolean = false,
    val desktopLyricsLocked: Boolean = false,
    val desktopLyricsPositionX: Int = 0,
    val desktopLyricsPositionY: Int = 0,
    val desktopLyricsPositionSet: Boolean = false,
    val desktopLyricsFlagsSet: Boolean = false,
    val desktopLyricsControlsTimeout: String = "",
    val desktopLyricsSurfaceOpacity: Int = 0,
    val desktopLyricsSurfaceOpacitySet: Boolean = false,
) {
    fun writeTo(output: OutputStream) {
        CodedOutputStream.newInstance(output).apply {
            if (supplementalText.isNotEmpty()) writeString(1, supplementalText)
            if (displayMode.isNotEmpty()) writeString(2, displayMode)
            if (highlightMode.isNotEmpty()) writeString(3, highlightMode)
            if (textAlignment.isNotEmpty()) writeString(4, textAlignment)
            if (fontSize.isNotEmpty()) writeString(5, fontSize)
            if (backgroundMode.isNotEmpty()) writeString(6, backgroundMode)
            if (translationEnabled) writeBool(7, true)
            if (transliterationEnabled) writeBool(8, true)
            if (supplementalTextFlagsSet) writeBool(9, true)
            if (desktopLyricsEnabled) writeBool(10, true)
            if (desktopLyricsDisplayMode.isNotEmpty()) writeString(11, desktopLyricsDisplayMode)
            if (desktopLyricsFontSize.isNotEmpty()) writeString(12, desktopLyricsFontSize)
            if (desktopLyricsAutoHideWhenPaused) writeBool(13, true)
            if (desktopLyricsLocked) writeBool(14, true)
            if (desktopLyricsPositionX != 0) writeSInt32(15, desktopLyricsPositionX)
            if (desktopLyricsPositionY != 0) writeSInt32(16, desktopLyricsPositionY)
            if (desktopLyricsPositionSet) writeBool(17, true)
            if (desktopLyricsFlagsSet) writeBool(18, true)
            if (desktopLyricsControlsTimeout.isNotEmpty()) writeString(19, desktopLyricsControlsTimeout)
            if (desktopLyricsSurfaceOpacity != 0) writeInt32(20, desktopLyricsSurfaceOpacity)
            if (desktopLyricsSurfaceOpacitySet) writeBool(21, true)
            flush()
        }
    }

    companion object {
        fun getDefaultInstance() = LyricsPreferences()

        fun parseFrom(input: InputStream): LyricsPreferences {
            val coded = CodedInputStream.newInstance(input)
            var supplementalText = ""
            var displayMode = ""
            var highlightMode = ""
            var textAlignment = ""
            var fontSize = ""
            var backgroundMode = ""
            var translationEnabled = false
            var transliterationEnabled = false
            var supplementalTextFlagsSet = false
            var desktopLyricsEnabled = false
            var desktopLyricsDisplayMode = ""
            var desktopLyricsFontSize = ""
            var desktopLyricsAutoHideWhenPaused = false
            var desktopLyricsLocked = false
            var desktopLyricsPositionX = 0
            var desktopLyricsPositionY = 0
            var desktopLyricsPositionSet = false
            var desktopLyricsFlagsSet = false
            var desktopLyricsControlsTimeout = ""
            var desktopLyricsSurfaceOpacity = 0
            var desktopLyricsSurfaceOpacitySet = false
            while (!coded.isAtEnd) {
                when (val tag = coded.readTag()) {
                    0 -> break
                    10 -> supplementalText = coded.readStringRequireUtf8()
                    18 -> displayMode = coded.readStringRequireUtf8()
                    26 -> highlightMode = coded.readStringRequireUtf8()
                    34 -> textAlignment = coded.readStringRequireUtf8()
                    42 -> fontSize = coded.readStringRequireUtf8()
                    50 -> backgroundMode = coded.readStringRequireUtf8()
                    56 -> translationEnabled = coded.readBool()
                    64 -> transliterationEnabled = coded.readBool()
                    72 -> supplementalTextFlagsSet = coded.readBool()
                    80 -> desktopLyricsEnabled = coded.readBool()
                    90 -> desktopLyricsDisplayMode = coded.readStringRequireUtf8()
                    98 -> desktopLyricsFontSize = coded.readStringRequireUtf8()
                    104 -> desktopLyricsAutoHideWhenPaused = coded.readBool()
                    112 -> desktopLyricsLocked = coded.readBool()
                    120 -> desktopLyricsPositionX = coded.readSInt32()
                    128 -> desktopLyricsPositionY = coded.readSInt32()
                    136 -> desktopLyricsPositionSet = coded.readBool()
                    144 -> desktopLyricsFlagsSet = coded.readBool()
                    154 -> desktopLyricsControlsTimeout = coded.readStringRequireUtf8()
                    160 -> desktopLyricsSurfaceOpacity = coded.readInt32()
                    168 -> desktopLyricsSurfaceOpacitySet = coded.readBool()
                    else -> coded.skipField(tag)
                }
            }
            return LyricsPreferences(
                supplementalText = supplementalText,
                displayMode = displayMode,
                highlightMode = highlightMode,
                textAlignment = textAlignment,
                fontSize = fontSize,
                backgroundMode = backgroundMode,
                translationEnabled = translationEnabled,
                transliterationEnabled = transliterationEnabled,
                supplementalTextFlagsSet = supplementalTextFlagsSet,
                desktopLyricsEnabled = desktopLyricsEnabled,
                desktopLyricsDisplayMode = desktopLyricsDisplayMode,
                desktopLyricsFontSize = desktopLyricsFontSize,
                desktopLyricsAutoHideWhenPaused = desktopLyricsAutoHideWhenPaused,
                desktopLyricsLocked = desktopLyricsLocked,
                desktopLyricsPositionX = desktopLyricsPositionX,
                desktopLyricsPositionY = desktopLyricsPositionY,
                desktopLyricsPositionSet = desktopLyricsPositionSet,
                desktopLyricsFlagsSet = desktopLyricsFlagsSet,
                desktopLyricsControlsTimeout = desktopLyricsControlsTimeout,
                desktopLyricsSurfaceOpacity = desktopLyricsSurfaceOpacity,
                desktopLyricsSurfaceOpacitySet = desktopLyricsSurfaceOpacitySet,
            )
        }
    }
}
