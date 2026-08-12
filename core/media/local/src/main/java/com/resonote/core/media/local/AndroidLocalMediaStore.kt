package com.resonote.core.media.local

import android.content.Context
import android.net.Uri
import android.os.StatFs
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

internal class AndroidLocalMediaStore internal constructor(
    private val privateRoot: File,
    private val availableBytes: () -> Long,
    private val sourceGateway: LocalMediaSourceGateway,
    private val mediaProbe: LocalMediaProbe,
    private val ioDispatcher: CoroutineDispatcher,
) : LocalMediaStore {
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : this(
        privateRoot = File(context.filesDir, ROOT_DIRECTORY),
        availableBytes = { StatFs(context.filesDir.absolutePath).availableBytes },
        sourceGateway = ContentResolverSourceGateway(context.contentResolver),
        mediaProbe = PlatformLocalMediaProbe(context),
        ioDispatcher = Dispatchers.IO,
    )

    override suspend fun recover(retainedFiles: Set<LocalMediaFiles>): LocalMediaStoreResult<Unit> =
        withContext(ioDispatcher) {
            val stagingDirectory = File(privateRoot, STAGING_DIRECTORY)
            val audioDirectory = File(privateRoot, AUDIO_DIRECTORY)
            val artworkDirectory = File(privateRoot, ARTWORK_DIRECTORY)
            try {
                val retainedPaths = retainedFiles
                    .flatMap { files -> listOfNotNull(files.audioPath, files.artworkPath) }
                    .map { path -> File(path).canonicalPath }
                    .toSet()
                val stagingEntries = stagingDirectory.managedEntries()
                    ?: return@withContext LocalMediaStoreResult.Failure(LocalMediaStoreError.StorageUnavailable)
                val audioEntries = audioDirectory.managedEntries()
                    ?: return@withContext LocalMediaStoreResult.Failure(LocalMediaStoreError.StorageUnavailable)
                val artworkEntries = artworkDirectory.managedEntries()
                    ?: return@withContext LocalMediaStoreResult.Failure(LocalMediaStoreError.StorageUnavailable)
                val stagingRecovered = stagingEntries.all { entry ->
                    entry.name.endsWith(PARTIAL_FILE_SUFFIX) && entry.isFile && entry.delete()
                }
                val committedRecovered = (audioEntries + artworkEntries)
                    .filterNot { it.canonicalPath in retainedPaths }
                    .all { entry -> entry.isFile && entry.delete() }
                if (stagingRecovered && committedRecovered) {
                    LocalMediaStoreResult.Success(Unit)
                } else {
                    LocalMediaStoreResult.Failure(LocalMediaStoreError.StorageUnavailable)
                }
            } catch (_: SecurityException) {
                LocalMediaStoreResult.Failure(LocalMediaStoreError.StorageUnavailable)
            } catch (_: IOException) {
                LocalMediaStoreResult.Failure(LocalMediaStoreError.StorageUnavailable)
            }
        }

    override suspend fun inspect(sourceUri: String): LocalMediaStoreResult<LocalMediaSourceInspection> =
        withContext(ioDispatcher) {
            val uri = sourceUri.toContentUri()
                ?: return@withContext LocalMediaStoreResult.Failure(LocalMediaStoreError.InvalidSource)
            try {
                val source = sourceGateway.describe(uri)
                if (source.sizeBytes == 0L) {
                    return@withContext LocalMediaStoreResult.Failure(LocalMediaStoreError.EmptyFile)
                }
                val probe = mediaProbe.inspect(uri, source.displayName)
                LocalMediaStoreResult.Success(
                    LocalMediaSourceInspection(
                        displayName = source.displayName,
                        reportedSizeBytes = source.sizeBytes,
                        declaredMimeType = source.mimeType,
                        fileExtension = source.displayName.safeExtension(),
                        metadata = probe.metadata,
                    ),
                )
            } catch (_: SecurityException) {
                LocalMediaStoreResult.Failure(LocalMediaStoreError.PermissionDenied)
            } catch (_: UnsupportedMediaException) {
                LocalMediaStoreResult.Failure(LocalMediaStoreError.UnsupportedFormat)
            } catch (_: FileNotFoundException) {
                LocalMediaStoreResult.Failure(LocalMediaStoreError.SourceUnavailable)
            } catch (_: IOException) {
                LocalMediaStoreResult.Failure(LocalMediaStoreError.SourceUnavailable)
            } catch (_: RuntimeException) {
                LocalMediaStoreResult.Failure(LocalMediaStoreError.MetadataUnavailable)
            }
        }

    override suspend fun calculateDigest(
        sourceUri: String,
        expectedSizeBytes: Long?,
    ): LocalMediaStoreResult<LocalMediaDigest> = withContext(ioDispatcher) {
        val uri = sourceUri.toContentUri()
            ?: return@withContext LocalMediaStoreResult.Failure(LocalMediaStoreError.InvalidSource)
        try {
            val digest = sourceGateway.open(uri).use { input -> digest(input) }
            when {
                digest.sizeBytes == 0L -> LocalMediaStoreResult.Failure(LocalMediaStoreError.EmptyFile)
                expectedSizeBytes != null && digest.sizeBytes != expectedSizeBytes ->
                    LocalMediaStoreResult.Failure(LocalMediaStoreError.SourceChanged)
                else -> LocalMediaStoreResult.Success(digest)
            }
        } catch (_: SecurityException) {
            LocalMediaStoreResult.Failure(LocalMediaStoreError.PermissionDenied)
        } catch (_: FileNotFoundException) {
            LocalMediaStoreResult.Failure(LocalMediaStoreError.SourceUnavailable)
        } catch (_: IOException) {
            LocalMediaStoreResult.Failure(LocalMediaStoreError.HashFailed)
        }
    }

    override suspend fun persist(request: LocalMediaPersistRequest): LocalMediaStoreResult<StoredLocalMedia> =
        withContext(ioDispatcher) {
            val uri = request.sourceUri.toContentUri()
                ?: return@withContext LocalMediaStoreResult.Failure(LocalMediaStoreError.InvalidSource)
            if (!request.expectedDigest.sha256.isSha256() || request.expectedDigest.sizeBytes <= 0) {
                return@withContext LocalMediaStoreResult.Failure(LocalMediaStoreError.HashFailed)
            }
            val freeBytes = try {
                availableBytes()
            } catch (_: RuntimeException) {
                return@withContext LocalMediaStoreResult.Failure(LocalMediaStoreError.StorageUnavailable)
            }
            if (freeBytes <= SPACE_RESERVE_BYTES || request.expectedDigest.sizeBytes > freeBytes - SPACE_RESERVE_BYTES) {
                return@withContext LocalMediaStoreResult.Failure(LocalMediaStoreError.InsufficientStorage)
            }

            val stagingDirectory = File(privateRoot, STAGING_DIRECTORY)
            val audioDirectory = File(privateRoot, AUDIO_DIRECTORY)
            val artworkDirectory = File(privateRoot, ARTWORK_DIRECTORY)
            val storageName = request.storageKey.safeStorageName()
            val extension = request.inspection.fileExtension?.safeExtensionPart() ?: DEFAULT_AUDIO_EXTENSION
            val partialAudio = File(stagingDirectory, "$storageName.audio.part")
            val finalAudio = File(audioDirectory, "$storageName.$extension")
            val partialArtwork = File(stagingDirectory, "$storageName.artwork.part")
            val finalArtwork = File(artworkDirectory, "$storageName.image")
            var committedArtwork = false
            try {
                if (!stagingDirectory.mkdirs() && !stagingDirectory.isDirectory) throw IOException()
                if (!audioDirectory.mkdirs() && !audioDirectory.isDirectory) throw IOException()
                if (partialAudio.exists() || finalAudio.exists()) throw IOException()

                val copiedDigest = copyAndDigest(uri, partialAudio)
                if (copiedDigest != request.expectedDigest) {
                    return@withContext LocalMediaStoreResult.Failure(LocalMediaStoreError.SourceChanged)
                }
                val probe = mediaProbe.inspect(partialAudio, request.inspection.displayName)
                probe.artwork?.let { artwork ->
                    if (!artworkDirectory.mkdirs() && !artworkDirectory.isDirectory) throw IOException()
                    writeSynced(partialArtwork, artwork)
                    atomicMove(partialArtwork, finalArtwork)
                    committedArtwork = true
                }
                atomicMove(partialAudio, finalAudio)
                LocalMediaStoreResult.Success(
                    StoredLocalMedia(
                        files = LocalMediaFiles(
                            audioPath = finalAudio.absolutePath,
                            artworkPath = finalArtwork.takeIf { committedArtwork }?.absolutePath,
                        ),
                        displayName = request.inspection.displayName,
                        fileExtension = request.inspection.fileExtension,
                        digest = copiedDigest,
                        metadata = probe.metadata,
                    ),
                )
            } catch (_: SecurityException) {
                LocalMediaStoreResult.Failure(LocalMediaStoreError.PermissionDenied)
            } catch (_: UnsupportedMediaException) {
                LocalMediaStoreResult.Failure(LocalMediaStoreError.UnsupportedFormat)
            } catch (_: RuntimeException) {
                LocalMediaStoreResult.Failure(LocalMediaStoreError.MetadataUnavailable)
            } catch (_: SourceReadException) {
                LocalMediaStoreResult.Failure(LocalMediaStoreError.SourceUnavailable)
            } catch (_: IOException) {
                LocalMediaStoreResult.Failure(LocalMediaStoreError.StorageUnavailable)
            } finally {
                partialAudio.delete()
                partialArtwork.delete()
                if (!finalAudio.isFile && committedArtwork) finalArtwork.delete()
            }
        }

    override suspend fun remove(files: LocalMediaFiles): LocalMediaStoreResult<Unit> = withContext(ioDispatcher) {
        val audio = File(files.audioPath)
        val artwork = files.artworkPath?.let(::File)
        try {
            if (!audio.isManagedFile() || artwork?.isManagedFile() == false) {
                return@withContext LocalMediaStoreResult.Failure(LocalMediaStoreError.StorageUnavailable)
            }
            val audioRemoved = !audio.exists() || audio.delete()
            val artworkRemoved = artwork == null || !artwork.exists() || artwork.delete()
            if (audioRemoved && artworkRemoved) {
                LocalMediaStoreResult.Success(Unit)
            } else {
                LocalMediaStoreResult.Failure(LocalMediaStoreError.StorageUnavailable)
            }
        } catch (_: SecurityException) {
            LocalMediaStoreResult.Failure(LocalMediaStoreError.StorageUnavailable)
        } catch (_: IOException) {
            LocalMediaStoreResult.Failure(LocalMediaStoreError.StorageUnavailable)
        }
    }

    private suspend fun digest(input: java.io.InputStream): LocalMediaDigest {
        val messageDigest = MessageDigest.getInstance(SHA_256)
        val buffer = ByteArray(BUFFER_SIZE)
        var sizeBytes = 0L
        while (true) {
            coroutineContext.ensureActive()
            val count = input.read(buffer)
            if (count < 0) break
            if (count == 0) continue
            messageDigest.update(buffer, 0, count)
            sizeBytes += count
        }
        return LocalMediaDigest(sizeBytes, messageDigest.digest().toHex())
    }

    private suspend fun copyAndDigest(uri: Uri, destination: File): LocalMediaDigest {
        val input = try {
            sourceGateway.open(uri)
        } catch (error: IOException) {
            throw SourceReadException(error)
        }
        input.use {
            FileOutputStream(destination).use { output ->
                val messageDigest = MessageDigest.getInstance(SHA_256)
                val buffer = ByteArray(BUFFER_SIZE)
                var sizeBytes = 0L
                while (true) {
                    coroutineContext.ensureActive()
                    val count = try {
                        input.read(buffer)
                    } catch (error: IOException) {
                        throw SourceReadException(error)
                    }
                    if (count < 0) break
                    if (count == 0) continue
                    output.write(buffer, 0, count)
                    messageDigest.update(buffer, 0, count)
                    sizeBytes += count
                }
                output.fd.sync()
                return LocalMediaDigest(sizeBytes, messageDigest.digest().toHex())
            }
        }
    }

    private fun writeSynced(destination: File, bytes: ByteArray) {
        FileOutputStream(destination).use { output ->
            output.write(bytes)
            output.fd.sync()
        }
    }

    private fun atomicMove(source: File, destination: File) {
        try {
            Files.move(source.toPath(), destination.toPath(), StandardCopyOption.ATOMIC_MOVE)
        } catch (_: AtomicMoveNotSupportedException) {
            if (!source.renameTo(destination)) throw IOException("Atomic move unavailable")
        }
    }

    private fun String.toContentUri(): Uri? = runCatching { Uri.parse(this) }
        .getOrNull()
        ?.takeIf { it.scheme == ContentResolverScheme && !it.authority.isNullOrBlank() }

    private fun String.safeExtension(): String? = substringAfterLast('.', "")
        .lowercase()
        .safeExtensionPart()

    private fun String.safeExtensionPart(): String? = takeIf { matches(SAFE_EXTENSION) }

    private fun String.safeStorageName(): String = MessageDigest.getInstance(SHA_256)
        .digest(toByteArray(Charsets.UTF_8))
        .toHex()
        .take(STORAGE_NAME_LENGTH)

    private fun ByteArray.toHex(): String = joinToString(separator = "") { byte -> "%02x".format(byte) }

    private fun String.isSha256(): Boolean = matches(SHA_256_HEX)

    private fun File.isManagedFile(): Boolean {
        val rootPath = privateRoot.canonicalFile.toPath()
        val path = canonicalFile.toPath()
        return path.startsWith(rootPath) && path != rootPath
    }

    private fun File.managedEntries(): List<File>? = when {
        !exists() -> emptyList()
        !isDirectory -> null
        else -> listFiles()?.toList()
    }

    private class SourceReadException(cause: IOException) : IOException(cause)

    private companion object {
        const val ContentResolverScheme = "content"
        const val ROOT_DIRECTORY = "local-media"
        const val STAGING_DIRECTORY = ".staging"
        const val AUDIO_DIRECTORY = "audio"
        const val ARTWORK_DIRECTORY = "artwork"
        const val DEFAULT_AUDIO_EXTENSION = "audio"
        const val PARTIAL_FILE_SUFFIX = ".part"
        const val SHA_256 = "SHA-256"
        const val STORAGE_NAME_LENGTH = 32
        const val BUFFER_SIZE = 64 * 1024
        const val SPACE_RESERVE_BYTES = 16L * 1024 * 1024
        val SAFE_EXTENSION = Regex("[a-z0-9]{1,10}")
        val SHA_256_HEX = Regex("[0-9a-f]{64}")
    }
}
