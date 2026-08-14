package com.resonote.core.media.local

import android.content.ContentResolver
import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream

internal data class SourceDescription(val displayName: String, val sizeBytes: Long?, val mimeType: String?)

internal interface LocalMediaSourceGateway {
    fun describe(uri: Uri): SourceDescription

    fun open(uri: Uri): InputStream
}

internal class ContentResolverSourceGateway(private val resolver: ContentResolver) : LocalMediaSourceGateway {
    override fun describe(uri: Uri): SourceDescription {
        var displayName = uri.lastPathSegment?.substringAfterLast('/')?.takeIf(String::isNotBlank)
            ?: DEFAULT_DISPLAY_NAME
        var sizeBytes: Long? = null
        resolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    .takeIf { it >= 0 && !cursor.isNull(it) }
                    ?.let(cursor::getString)
                    ?.trim()
                    ?.takeIf(String::isNotEmpty)
                    ?.let { displayName = it }
                cursor.getColumnIndex(OpenableColumns.SIZE)
                    .takeIf { it >= 0 && !cursor.isNull(it) }
                    ?.let(cursor::getLong)
                    ?.takeIf { it >= 0 }
                    ?.let { sizeBytes = it }
            }
        }
        return SourceDescription(
            displayName = displayName,
            sizeBytes = sizeBytes,
            mimeType = resolver.getType(uri)?.trim()?.takeIf(String::isNotEmpty),
        )
    }

    override fun open(uri: Uri): InputStream = resolver.openInputStream(uri)
        ?: throw FileNotFoundException("Content provider returned no stream")

    private companion object {
        const val DEFAULT_DISPLAY_NAME = "导入的音乐"
    }
}

internal data class MediaProbeResult(val metadata: LocalMediaMetadata, val artwork: ByteArray?)

internal interface LocalMediaProbe {
    fun inspect(uri: Uri, displayName: String): MediaProbeResult

    fun inspect(file: File, displayName: String): MediaProbeResult
}

internal class PlatformLocalMediaProbe(private val context: Context) : LocalMediaProbe {
    override fun inspect(uri: Uri, displayName: String): MediaProbeResult = inspectMedia(
        displayName = displayName,
        includeArtwork = false,
        configureExtractor = { setDataSource(context, uri, emptyMap()) },
        configureRetriever = { setDataSource(context, uri) },
    )

    override fun inspect(file: File, displayName: String): MediaProbeResult = inspectMedia(
        displayName = displayName,
        includeArtwork = true,
        configureExtractor = { setDataSource(file.absolutePath) },
        configureRetriever = { setDataSource(file.absolutePath) },
    )

    private fun inspectMedia(
        displayName: String,
        includeArtwork: Boolean,
        configureExtractor: MediaExtractor.() -> Unit,
        configureRetriever: MediaMetadataRetriever.() -> Unit,
    ): MediaProbeResult {
        val extractor = MediaExtractor()
        val format = try {
            extractor.configureExtractor()
            (0 until extractor.trackCount)
                .map(extractor::getTrackFormat)
                .firstOrNull { it.string(MediaFormat.KEY_MIME)?.startsWith(AUDIO_MIME_PREFIX) == true }
                ?: throw UnsupportedMediaException()
        } finally {
            extractor.release()
        }
        if (MediaCodecList(MediaCodecList.ALL_CODECS).findDecoderForFormat(format) == null) {
            throw UnsupportedMediaException()
        }

        val retriever = MediaMetadataRetriever()
        return try {
            retriever.configureRetriever()
            val fallbackTitle = displayName.substringBeforeLast('.').trim().ifEmpty { displayName }
            MediaProbeResult(
                metadata = LocalMediaMetadata(
                    title = retriever.text(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: fallbackTitle,
                    artist = retriever.text(MediaMetadataRetriever.METADATA_KEY_ARTIST),
                    albumTitle = retriever.text(MediaMetadataRetriever.METADATA_KEY_ALBUM),
                    durationMillis = retriever.long(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        ?: format.long(MediaFormat.KEY_DURATION)?.div(MICROS_PER_MILLISECOND)
                        ?: 0,
                    detectedMimeType = format.string(MediaFormat.KEY_MIME) ?: AUDIO_WILDCARD_MIME,
                    sampleRateHz = format.int(MediaFormat.KEY_SAMPLE_RATE)?.positiveOrNull(),
                    bitDepth = format.bitDepth(),
                    bitrateBitsPerSecond = format.int(MediaFormat.KEY_BIT_RATE)?.positiveOrNull(),
                ),
                artwork = if (includeArtwork) {
                    retriever.embeddedPicture?.takeIf { it.size <= MAX_ARTWORK_BYTES }
                } else {
                    null
                },
            )
        } finally {
            retriever.release()
        }
    }

    private fun MediaMetadataRetriever.text(key: Int): String? =
        extractMetadata(key)?.trim()?.takeIf(String::isNotEmpty)

    private fun MediaMetadataRetriever.long(key: Int): Long? = text(key)?.toLongOrNull()?.takeIf { it >= 0 }

    private fun MediaFormat.string(key: String): String? =
        if (containsKey(key)) getString(key)?.trim()?.takeIf(String::isNotEmpty) else null

    private fun MediaFormat.int(key: String): Int? = if (containsKey(key)) {
        runCatching {
            getInteger(key)
        }.getOrNull()
    } else {
        null
    }

    private fun MediaFormat.long(key: String): Long? = if (containsKey(key)) {
        runCatching {
            getLong(key)
        }.getOrNull()
    } else {
        null
    }

    private fun MediaFormat.bitDepth(): Int? {
        int(BITS_PER_SAMPLE_KEY)?.positiveOrNull()?.let { return it }
        return when (int(MediaFormat.KEY_PCM_ENCODING)) {
            AudioFormat.ENCODING_PCM_8BIT -> 8
            AudioFormat.ENCODING_PCM_16BIT -> 16
            AudioFormat.ENCODING_PCM_24BIT_PACKED -> 24
            AudioFormat.ENCODING_PCM_32BIT, AudioFormat.ENCODING_PCM_FLOAT -> 32
            else -> null
        }
    }

    private fun Int.positiveOrNull(): Int? = takeIf { it > 0 }

    private companion object {
        const val AUDIO_MIME_PREFIX = "audio/"
        const val AUDIO_WILDCARD_MIME = "audio/*"
        const val BITS_PER_SAMPLE_KEY = "bits-per-sample"
        const val MICROS_PER_MILLISECOND = 1_000
        const val MAX_ARTWORK_BYTES = 16 * 1024 * 1024
    }
}

internal class UnsupportedMediaException : Exception()
