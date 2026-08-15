package com.resonote.core.data

import com.google.common.truth.Truth.assertThat
import com.resonote.core.datastore.PlaybackSessionSnapshotStorage
import com.resonote.core.model.AudioQuality
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DefaultPlaybackSessionRepositoryTest {
    @Test
    fun snapshotRoundTripsEveryOriginAndIgnoresUnknownFields() = runTest {
        val storage = FakeStorage()
        val repository = DefaultPlaybackSessionRepository(storage)
        val entries = PlaybackSessionEntryKind.entries.mapIndexed { index, kind ->
            PlaybackSessionEntry(
                kind = kind,
                mediaId = "id-$index",
                title = "Title $index",
                artist = "Artist",
                albumTitle = "Album",
                artworkUri = "https://cover/$index",
                durationMillis = 180_000,
                isVip = kind == PlaybackSessionEntryKind.Online,
                audioQuality = AudioQuality.Lossless.takeIf { kind == PlaybackSessionEntryKind.Online },
                albumId = "album-id",
                albumAudioId = "audio-id",
                fileId = "file-id",
                previewDurationMillis = 60_000,
                mimeType = "audio/flac",
                extension = "flac",
                sampleRateHz = 96_000,
                bitDepth = 24,
                bitrateBitsPerSecond = 2_400_000,
            )
        }
        val expected = PlaybackSessionSnapshot(entries, currentIndex = 1, positionMillis = 42_000, mode = "Shuffle")

        repository.save(expected)
        storage.state.value = storage.state.value?.dropLast(1) + ",\"future\":true}"

        assertThat(repository.load()).isEqualTo(expected)
    }

    @Test
    fun malformedOrInvalidSnapshotIsIgnoredAndClearRemovesIt() = runTest {
        val storage = FakeStorage("not-json")
        val repository = DefaultPlaybackSessionRepository(storage)

        assertThat(repository.load()).isNull()
        storage.state.value = """{"entries":[],"currentIndex":4}"""
        assertThat(repository.load()).isNull()

        repository.clear()
        assertThat(storage.state.value).isNull()
    }

    private class FakeStorage(initial: String? = null) : PlaybackSessionSnapshotStorage {
        val state = MutableStateFlow(initial)
        override val snapshotJson = state

        override suspend fun write(json: String) {
            state.value = json
        }

        override suspend fun clear() {
            state.value = null
        }
    }
}
