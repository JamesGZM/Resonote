package com.resonote.core.datastore.proto

import com.google.protobuf.CodedInputStream
import com.google.protobuf.CodedOutputStream
import java.io.InputStream
import java.io.OutputStream

/** Lite representation of appearance_preferences.proto using the standard protobuf wire format. */
data class AppearancePreferences(
    val themeMode: String = "",
    val dynamicColorEnabled: Boolean = false,
) {
    fun writeTo(output: OutputStream) {
        CodedOutputStream.newInstance(output).apply {
            if (themeMode.isNotEmpty()) writeString(1, themeMode)
            if (dynamicColorEnabled) writeBool(2, dynamicColorEnabled)
            flush()
        }
    }

    companion object {
        fun getDefaultInstance(): AppearancePreferences = AppearancePreferences()

        fun parseFrom(input: InputStream): AppearancePreferences {
            val coded = CodedInputStream.newInstance(input)
            var themeMode = ""
            var dynamicColorEnabled = false
            while (!coded.isAtEnd) {
                when (val tag = coded.readTag()) {
                    0 -> break
                    10 -> themeMode = coded.readStringRequireUtf8()
                    16 -> dynamicColorEnabled = coded.readBool()
                    else -> coded.skipField(tag)
                }
            }
            return AppearancePreferences(themeMode, dynamicColorEnabled)
        }
    }
}
