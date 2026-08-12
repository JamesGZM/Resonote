package com.resonote.core.media.local

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AndroidLocalMediaStoreTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun inspectAndDigestUseReadableContentSource() = runTest {
        val source = FakeSourceGateway(bytes = "resonote".encodeToByteArray())
        val store = store(source)

        val inspection = store.inspect(SOURCE_URI)
        val digest = store.calculateDigest(SOURCE_URI, expectedSizeBytes = 8)

        assertThat(inspection).isEqualTo(LocalMediaStoreResult.Success(INSPECTION))
        assertThat(digest).isEqualTo(
            LocalMediaStoreResult.Success(
                LocalMediaDigest(
                    sizeBytes = 8,
                    sha256 = "09031ef72c039dcfd97053abbea50fa1d256a3985b94c9caaab691891ccae171",
                ),
            ),
        )
    }

    @Test
    fun persistWritesPrivateFilesAndLeavesNoPartialArtifacts() = runTest {
        val bytes = "resonote".encodeToByteArray()
        val source = FakeSourceGateway(bytes)
        val store = store(source, artwork = "cover".encodeToByteArray())
        val digest = store.calculateDigest(SOURCE_URI, bytes.size.toLong()).successValue()

        val result = store.persist(
            LocalMediaPersistRequest(
                sourceUri = SOURCE_URI,
                storageKey = "track/with unsafe characters",
                inspection = INSPECTION,
                expectedDigest = digest,
            ),
        ).successValue()

        val audio = File(result.files.audioPath)
        val artwork = File(requireNotNull(result.files.artworkPath))
        assertThat(audio.readBytes()).isEqualTo(bytes)
        assertThat(artwork.readText()).isEqualTo("cover")
        assertThat(audio.canonicalPath).startsWith(temporaryFolder.root.canonicalPath)
        assertThat(audio.name).doesNotContain("unsafe")
        assertThat(temporaryFolder.root.walkTopDown().filter { it.name.endsWith(".part") }.toList()).isEmpty()
    }

    @Test
    fun persistRejectsChangedSourceAndRollsBackStaging() = runTest {
        val source = FakeSourceGateway("first version".encodeToByteArray())
        val store = store(source)
        val digest = store.calculateDigest(SOURCE_URI, expectedSizeBytes = 13).successValue()
        source.bytes = "changed version".encodeToByteArray()

        val result = store.persist(
            LocalMediaPersistRequest(
                sourceUri = SOURCE_URI,
                storageKey = "changed-source",
                inspection = INSPECTION.copy(reportedSizeBytes = 13),
                expectedDigest = digest,
            ),
        )

        assertThat(result).isEqualTo(LocalMediaStoreResult.Failure(LocalMediaStoreError.SourceChanged))
        assertThat(temporaryFolder.root.walkTopDown().filter(File::isFile).toList()).isEmpty()
    }

    @Test
    fun persistReportsReadFailureAndRollsBackPartialFile() = runTest {
        val bytes = "resonote".encodeToByteArray()
        val source = FakeSourceGateway(bytes)
        val store = store(source)
        val digest = store.calculateDigest(SOURCE_URI, expectedSizeBytes = bytes.size.toLong()).successValue()
        source.openOverride = { FailingInputStream(bytes, failAfterBytes = 4) }

        val result = store.persist(
            LocalMediaPersistRequest(
                sourceUri = SOURCE_URI,
                storageKey = "read-failure",
                inspection = INSPECTION,
                expectedDigest = digest,
            ),
        )

        assertThat(result).isEqualTo(LocalMediaStoreResult.Failure(LocalMediaStoreError.SourceUnavailable))
        assertThat(temporaryFolder.root.walkTopDown().filter(File::isFile).toList()).isEmpty()
    }

    @Test
    fun persistReportsMetadataFailureAndRollsBackPartialFile() = runTest {
        val bytes = "resonote".encodeToByteArray()
        val source = FakeSourceGateway(bytes)
        val probe = FakeMediaProbe(artwork = null).apply { fileFailure = IllegalStateException("bad metadata") }
        val store = store(source, mediaProbe = probe)
        val digest = store.calculateDigest(SOURCE_URI, expectedSizeBytes = bytes.size.toLong()).successValue()

        val result = store.persist(
            LocalMediaPersistRequest(
                sourceUri = SOURCE_URI,
                storageKey = "bad-metadata",
                inspection = INSPECTION,
                expectedDigest = digest,
            ),
        )

        assertThat(result).isEqualTo(LocalMediaStoreResult.Failure(LocalMediaStoreError.MetadataUnavailable))
        assertThat(temporaryFolder.root.walkTopDown().filter(File::isFile).toList()).isEmpty()
    }

    @Test
    fun insufficientStorageStopsBeforeOpeningSource() = runTest {
        val source = FakeSourceGateway("resonote".encodeToByteArray())
        val store = store(source, availableBytes = 1)

        val result = store.persist(
            LocalMediaPersistRequest(
                sourceUri = SOURCE_URI,
                storageKey = "no-space",
                inspection = INSPECTION,
                expectedDigest = LocalMediaDigest(
                    sizeBytes = 8,
                    sha256 = "09031ef72c039dcfd97053abbea50fa1d256a3985b94c9caaab691891ccae171",
                ),
            ),
        )

        assertThat(result).isEqualTo(LocalMediaStoreResult.Failure(LocalMediaStoreError.InsufficientStorage))
        assertThat(source.openCount).isEqualTo(0)
    }

    @Test
    fun removeRefusesFilesOutsideManagedRoot() = runTest {
        val source = FakeSourceGateway("resonote".encodeToByteArray())
        val store = store(source)
        val outside = temporaryFolder.newFile("outside.audio").apply { writeText("keep") }

        val result = store.remove(LocalMediaFiles(audioPath = outside.absolutePath, artworkPath = null))

        assertThat(result).isEqualTo(LocalMediaStoreResult.Failure(LocalMediaStoreError.StorageUnavailable))
        assertThat(outside.readText()).isEqualTo("keep")
    }

    @Test
    fun nonContentUriIsRejectedWithoutTouchingSource() = runTest {
        val source = FakeSourceGateway("resonote".encodeToByteArray())
        val store = store(source)

        assertThat(store.inspect("file:///sdcard/music.mp3"))
            .isEqualTo(LocalMediaStoreResult.Failure(LocalMediaStoreError.InvalidSource))
        assertThat(source.openCount).isEqualTo(0)
    }

    private fun store(
        source: FakeSourceGateway,
        artwork: ByteArray? = null,
        availableBytes: Long = Long.MAX_VALUE,
        mediaProbe: FakeMediaProbe = FakeMediaProbe(artwork),
    ) = AndroidLocalMediaStore(
        privateRoot = File(temporaryFolder.root, "local-media"),
        availableBytes = { availableBytes },
        sourceGateway = source,
        mediaProbe = mediaProbe,
        ioDispatcher = UnconfinedTestDispatcher(),
    )

    private fun <T> LocalMediaStoreResult<T>.successValue(): T =
        (this as LocalMediaStoreResult.Success<T>).value

    private class FakeSourceGateway(
        var bytes: ByteArray,
    ) : LocalMediaSourceGateway {
        var openCount = 0
        var openOverride: (() -> InputStream)? = null

        override fun describe(uri: Uri) = SourceDescription(
            displayName = "Signals.FLAC",
            sizeBytes = bytes.size.toLong(),
            mimeType = "audio/flac",
        )

        override fun open(uri: Uri): InputStream {
            openCount++
            return openOverride?.invoke() ?: ByteArrayInputStream(bytes)
        }
    }

    private class FakeMediaProbe(
        private val artwork: ByteArray?,
    ) : LocalMediaProbe {
        var fileFailure: RuntimeException? = null

        override fun inspect(uri: Uri, displayName: String) = MediaProbeResult(METADATA, artwork = null)

        override fun inspect(file: File, displayName: String): MediaProbeResult {
            fileFailure?.let { throw it }
            return MediaProbeResult(METADATA, artwork)
        }
    }

    private class FailingInputStream(
        private val bytes: ByteArray,
        private val failAfterBytes: Int,
    ) : InputStream() {
        private var position = 0

        override fun read(): Int {
            if (position >= failAfterBytes) throw IOException("source disappeared")
            return if (position >= bytes.size) -1 else bytes[position++].toInt() and 0xff
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            if (position >= failAfterBytes) throw IOException("source disappeared")
            if (position >= bytes.size) return -1
            val count = minOf(length, failAfterBytes - position, bytes.size - position)
            bytes.copyInto(buffer, offset, position, position + count)
            position += count
            return count
        }
    }

    private companion object {
        const val SOURCE_URI = "content://com.resonote.test/music/signals.flac"
        val METADATA = LocalMediaMetadata(
            title = "Night Signals",
            artist = "Resonote Artist",
            albumTitle = "Resonote Sessions",
            durationMillis = 180_000,
            detectedMimeType = "audio/flac",
            sampleRateHz = 96_000,
            bitDepth = 24,
            bitrateBitsPerSecond = 2_304_000,
        )
        val INSPECTION = LocalMediaSourceInspection(
            displayName = "Signals.FLAC",
            reportedSizeBytes = 8,
            declaredMimeType = "audio/flac",
            fileExtension = "flac",
            metadata = METADATA,
        )
    }
}
