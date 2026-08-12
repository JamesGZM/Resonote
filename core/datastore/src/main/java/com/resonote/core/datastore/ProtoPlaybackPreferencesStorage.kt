package com.resonote.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import com.resonote.core.datastore.proto.PlaybackPreferences
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object PlaybackPreferencesSerializer : Serializer<PlaybackPreferences> {
    override val defaultValue: PlaybackPreferences = PlaybackPreferences.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): PlaybackPreferences = PlaybackPreferences.parseFrom(input)

    override suspend fun writeTo(t: PlaybackPreferences, output: OutputStream) = t.writeTo(output)
}

@Singleton
internal class ProtoPlaybackPreferencesStorage @Inject constructor(
    private val store: DataStore<PlaybackPreferences>,
) : PlaybackPreferencesStorage {
    override val playbackSpeedPercent: Flow<Int> = store.data.map { it.playbackSpeedPercent }

    override suspend fun setPlaybackSpeedPercent(percent: Int) {
        require(percent > 0) { "playback speed percent must be positive" }
        store.updateData { it.copy(playbackSpeedPercent = percent) }
    }
}
