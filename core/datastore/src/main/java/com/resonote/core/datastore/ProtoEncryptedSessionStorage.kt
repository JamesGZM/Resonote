package com.resonote.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import com.google.protobuf.ByteString
import com.resonote.core.datastore.proto.EncryptedApiSession
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object EncryptedApiSessionSerializer : Serializer<EncryptedApiSession> {
    override val defaultValue: EncryptedApiSession = EncryptedApiSession.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): EncryptedApiSession = EncryptedApiSession.parseFrom(input)

    override suspend fun writeTo(t: EncryptedApiSession, output: OutputStream) = t.writeTo(output)
}

@Singleton
internal class ProtoEncryptedSessionStorage @Inject constructor(
    private val store: DataStore<EncryptedApiSession>,
) : EncryptedSessionStorage {
    override val data: Flow<EncryptedSessionEnvelope?> =
        store.data.map { value ->
            value.takeIf { it.schemaVersion > 0 && !it.iv.isEmpty && !it.ciphertext.isEmpty }?.let {
                EncryptedSessionEnvelope(it.schemaVersion, it.iv.toByteArray(), it.ciphertext.toByteArray())
            }
        }

    override suspend fun write(envelope: EncryptedSessionEnvelope) {
        store.updateData {
            EncryptedApiSession(
                schemaVersion = envelope.schemaVersion,
                iv = ByteString.copyFrom(envelope.iv),
                ciphertext = ByteString.copyFrom(envelope.ciphertext),
            )
        }
    }

    override suspend fun clear() {
        store.updateData { EncryptedApiSession.getDefaultInstance() }
    }
}
