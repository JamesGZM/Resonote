package com.resonote.core.playback.service

import com.resonote.core.model.AudioQuality
import com.resonote.core.model.OnlinePlaybackQuality
import com.resonote.core.model.OnlineSong
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

internal data class MusicDownloadMetadata(
    val song: OnlineSong,
    val quality: OnlinePlaybackQuality,
    val extension: String?,
)

internal object MusicDownloadMetadataCodec {
    fun encode(metadata: MusicDownloadMetadata): ByteArray = ByteArrayOutputStream().use { bytes ->
        DataOutputStream(bytes).use { output ->
            output.writeInt(VERSION)
            output.writeUTF(metadata.song.hash)
            output.writeUTF(metadata.song.title)
            output.writeNullableString(metadata.song.artist)
            output.writeNullableString(metadata.song.coverUrl)
            output.writeNullableString(metadata.song.albumId)
            output.writeNullableString(metadata.song.albumAudioId)
            output.writeLong(metadata.song.durationMillis)
            output.writeUTF(metadata.song.quality.name)
            output.writeBoolean(metadata.song.vip)
            output.writeNullableString(metadata.song.albumTitle)
            output.writeNullableString(metadata.song.fileId)
            output.writeNullableLong(metadata.song.previewDurationMillis)
            output.writeUTF(metadata.quality.name)
            output.writeNullableString(metadata.extension)
        }
        bytes.toByteArray()
    }

    fun decode(bytes: ByteArray): MusicDownloadMetadata? = runCatching {
        DataInputStream(ByteArrayInputStream(bytes)).use { input ->
            check(input.readInt() == VERSION)
            val song = OnlineSong(
                hash = input.readUTF(),
                title = input.readUTF(),
                artist = input.readNullableString(),
                coverUrl = input.readNullableString(),
                albumId = input.readNullableString(),
                albumAudioId = input.readNullableString(),
                durationMillis = input.readLong(),
                quality = input.readUTF().let { value ->
                    AudioQuality.entries.firstOrNull { it.name == value } ?: AudioQuality.Standard
                },
                vip = input.readBoolean(),
                albumTitle = input.readNullableString(),
                fileId = input.readNullableString(),
                previewDurationMillis = input.readNullableLong(),
            )
            MusicDownloadMetadata(
                song = song,
                quality = input.readUTF().let { value ->
                    OnlinePlaybackQuality.entries.firstOrNull { it.name == value }
                        ?: OnlinePlaybackQuality.Standard
                },
                extension = input.readNullableString(),
            )
        }
    }.getOrNull()

    private fun DataOutputStream.writeNullableString(value: String?) {
        writeBoolean(value != null)
        if (value != null) writeUTF(value)
    }

    private fun DataInputStream.readNullableString(): String? = if (readBoolean()) readUTF() else null

    private fun DataOutputStream.writeNullableLong(value: Long?) {
        writeBoolean(value != null)
        if (value != null) writeLong(value)
    }

    private fun DataInputStream.readNullableLong(): Long? = if (readBoolean()) readLong() else null

    private const val VERSION = 1
}
