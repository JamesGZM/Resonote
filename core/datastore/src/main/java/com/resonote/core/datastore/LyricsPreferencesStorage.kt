package com.resonote.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import com.resonote.core.datastore.proto.LyricsPreferences
import kotlinx.coroutines.flow.Flow
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

interface LyricsPreferencesStorage {
    val values: Flow<LyricsPreferences>
    suspend fun update(value: LyricsPreferences)
    suspend fun reset()
}

object LyricsPreferencesSerializer : Serializer<LyricsPreferences> {
    override val defaultValue = LyricsPreferences.getDefaultInstance()
    override suspend fun readFrom(input: InputStream) = LyricsPreferences.parseFrom(input)
    override suspend fun writeTo(t: LyricsPreferences, output: OutputStream) = t.writeTo(output)
}

@Singleton
internal class ProtoLyricsPreferencesStorage @Inject constructor(private val store: DataStore<LyricsPreferences>) :
    LyricsPreferencesStorage {
    override val values = store.data
    override suspend fun update(value: LyricsPreferences) {
        store.updateData { value }
    }
    override suspend fun reset() {
        store.updateData { LyricsPreferences.getDefaultInstance() }
    }
}
