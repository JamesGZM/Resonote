package com.resonote.core.datastore.proto

import com.google.protobuf.ByteString
import com.google.protobuf.CodedInputStream
import com.google.protobuf.CodedOutputStream
import java.io.InputStream
import java.io.OutputStream

/** Lite representation of encrypted_api_session.proto using the standard protobuf wire format. */
data class EncryptedApiSession(
    val schemaVersion: Int = 0,
    val iv: ByteString = ByteString.EMPTY,
    val ciphertext: ByteString = ByteString.EMPTY,
) {
    fun writeTo(output: OutputStream) {
        CodedOutputStream.newInstance(output).apply {
            if (schemaVersion != 0) writeInt32(1, schemaVersion)
            if (!iv.isEmpty) writeBytes(2, iv)
            if (!ciphertext.isEmpty) writeBytes(3, ciphertext)
            flush()
        }
    }

    companion object {
        fun getDefaultInstance(): EncryptedApiSession = EncryptedApiSession()

        fun parseFrom(input: InputStream): EncryptedApiSession {
            val coded = CodedInputStream.newInstance(input)
            var version = 0
            var iv = ByteString.EMPTY
            var ciphertext = ByteString.EMPTY
            while (!coded.isAtEnd) {
                when (val tag = coded.readTag()) {
                    0 -> break
                    8 -> version = coded.readInt32()
                    18 -> iv = coded.readBytes()
                    26 -> ciphertext = coded.readBytes()
                    else -> coded.skipField(tag)
                }
            }
            return EncryptedApiSession(version, iv, ciphertext)
        }
    }
}
