package com.resonote.core.datastore.proto

import com.google.protobuf.CodedInputStream
import com.google.protobuf.CodedOutputStream
import java.io.InputStream
import java.io.OutputStream

data class VipPreferences(val automaticDisabled: Boolean = false) {
    fun writeTo(output: OutputStream) {
        CodedOutputStream.newInstance(output).apply {
            writeBool(1, automaticDisabled)
            flush()
        }
    }
    companion object {
        fun parseFrom(input: InputStream): VipPreferences {
            val coded = CodedInputStream.newInstance(input)
            var disabled = false
            while (!coded.isAtEnd) {
                when (val tag = coded.readTag()) {
                    0 -> break
                    8 -> disabled = coded.readBool()
                    else -> coded.skipField(tag)
                }
            }
            return VipPreferences(disabled)
        }
    }
}
