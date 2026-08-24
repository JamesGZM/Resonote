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
) {
    fun writeTo(output: OutputStream) {
        CodedOutputStream.newInstance(output).apply {
            if (supplementalText.isNotEmpty()) writeString(1, supplementalText)
            if (displayMode.isNotEmpty()) writeString(2, displayMode)
            if (highlightMode.isNotEmpty()) writeString(3, highlightMode)
            if (textAlignment.isNotEmpty()) writeString(4, textAlignment)
            if (fontSize.isNotEmpty()) writeString(5, fontSize)
            if (backgroundMode.isNotEmpty()) writeString(6, backgroundMode)
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
            while (!coded.isAtEnd) {
                when (val tag = coded.readTag()) {
                    0 -> break
                    10 -> supplementalText = coded.readStringRequireUtf8()
                    18 -> displayMode = coded.readStringRequireUtf8()
                    26 -> highlightMode = coded.readStringRequireUtf8()
                    34 -> textAlignment = coded.readStringRequireUtf8()
                    42 -> fontSize = coded.readStringRequireUtf8()
                    50 -> backgroundMode = coded.readStringRequireUtf8()
                    else -> coded.skipField(tag)
                }
            }
            return LyricsPreferences(
                supplementalText,
                displayMode,
                highlightMode,
                textAlignment,
                fontSize,
                backgroundMode,
            )
        }
    }
}
