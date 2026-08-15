package com.resonote.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import com.resonote.core.datastore.proto.HomeSnapshot
import com.resonote.core.datastore.proto.PlaybackSessionSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

object HomeSnapshotSerializer : Serializer<HomeSnapshot> {
    override val defaultValue: HomeSnapshot = HomeSnapshot.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): HomeSnapshot =
        runCatching { HomeSnapshot.parseFrom(input) }.getOrDefault(defaultValue)

    override suspend fun writeTo(t: HomeSnapshot, output: OutputStream) = t.writeTo(output)
}

object PlaybackSessionSnapshotSerializer : Serializer<PlaybackSessionSnapshot> {
    override val defaultValue: PlaybackSessionSnapshot = PlaybackSessionSnapshot.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): PlaybackSessionSnapshot =
        runCatching { PlaybackSessionSnapshot.parseFrom(input) }.getOrDefault(defaultValue)

    override suspend fun writeTo(t: PlaybackSessionSnapshot, output: OutputStream) = t.writeTo(output)
}

@Singleton
internal class ProtoHomeSnapshotStorage @Inject constructor(private val store: DataStore<HomeSnapshot>) :
    HomeSnapshotStorage {
    override val snapshotJson: Flow<String?> = store.data.map { it.json.takeIf(String::isNotBlank) }

    override suspend fun write(json: String) {
        require(json.isNotBlank()) { "home snapshot must not be blank" }
        store.updateData { HomeSnapshot(json) }
    }

    override suspend fun clear() {
        store.updateData { HomeSnapshot.getDefaultInstance() }
    }
}

@Singleton
internal class ProtoPlaybackSessionSnapshotStorage @Inject constructor(
    private val store: DataStore<PlaybackSessionSnapshot>,
) : PlaybackSessionSnapshotStorage {
    override val snapshotJson: Flow<String?> = store.data.map { it.json.takeIf(String::isNotBlank) }

    override suspend fun write(json: String) {
        require(json.isNotBlank()) { "playback snapshot must not be blank" }
        store.updateData { PlaybackSessionSnapshot(json) }
    }

    override suspend fun clear() {
        store.updateData { PlaybackSessionSnapshot.getDefaultInstance() }
    }
}
