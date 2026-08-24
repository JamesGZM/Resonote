package com.resonote.core.media.local

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

interface LocalMediaTreeSource {
    suspend fun scan(treeUri: String): LocalMediaTreeScanResult
}

sealed interface LocalMediaTreeScanResult {
    data class Available(val documentUris: List<String>) : LocalMediaTreeScanResult

    data class Failed(val reason: LocalMediaTreeScanFailure) : LocalMediaTreeScanResult
}

enum class LocalMediaTreeScanFailure {
    InvalidTree,
    PermissionDenied,
    Unavailable,
}

internal class DocumentsLocalMediaTreeSource @Inject constructor(@ApplicationContext context: Context) :
    LocalMediaTreeSource {
    private val contentResolver = context.contentResolver

    override suspend fun scan(treeUri: String): LocalMediaTreeScanResult = withContext(Dispatchers.IO) {
        val uri = runCatching { Uri.parse(treeUri) }.getOrNull()
            ?.takeIf { it.scheme == ContentResolver.SCHEME_CONTENT && DocumentsContract.isTreeUri(it) }
            ?: return@withContext LocalMediaTreeScanResult.Failed(LocalMediaTreeScanFailure.InvalidTree)
        try {
            val pendingDirectories = ArrayDeque<String>()
            val visitedDirectories = mutableSetOf<String>()
            val documents = mutableListOf<String>()
            pendingDirectories += DocumentsContract.getTreeDocumentId(uri)

            while (pendingDirectories.isNotEmpty()) {
                val directoryId = pendingDirectories.removeFirst()
                if (!visitedDirectories.add(directoryId)) continue
                val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(uri, directoryId)
                val cursor = contentResolver.query(childrenUri, Projection, null, null, null)
                    ?: return@withContext LocalMediaTreeScanResult.Failed(LocalMediaTreeScanFailure.Unavailable)
                cursor.use {
                    val idColumn = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val mimeColumn = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                    val nameColumn = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    while (it.moveToNext()) {
                        val documentId = it.getString(idColumn)
                        val mimeType = it.getString(mimeColumn)
                        if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                            pendingDirectories += documentId
                        } else if (isAudioCandidate(mimeType, it.getString(nameColumn))) {
                            documents += DocumentsContract
                                .buildDocumentUriUsingTree(uri, documentId)
                                .toString()
                        }
                    }
                }
            }
            LocalMediaTreeScanResult.Available(documents.distinct())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: SecurityException) {
            LocalMediaTreeScanResult.Failed(LocalMediaTreeScanFailure.PermissionDenied)
        } catch (_: Exception) {
            LocalMediaTreeScanResult.Failed(LocalMediaTreeScanFailure.Unavailable)
        }
    }

    private fun isAudioCandidate(mimeType: String?, displayName: String?): Boolean {
        if (mimeType?.startsWith("audio/", ignoreCase = true) == true) return true
        if (mimeType.isNullOrBlank() || mimeType.equals("application/octet-stream", ignoreCase = true)) return true
        return displayName?.substringAfterLast('.', missingDelimiterValue = "")?.lowercase() in AudioExtensions
    }

    private companion object {
        val Projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        )

        val AudioExtensions = setOf(
            "aac", "alac", "amr", "ape", "flac", "m4a", "mp3", "ogg", "opus", "wav", "wma",
        )
    }
}
