package com.resonote.core.media.local

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DocumentsLocalMediaTreeSourceTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private lateinit var provider: TreeProvider
    private lateinit var source: DocumentsLocalMediaTreeSource

    @Before
    fun setUp() {
        provider = TreeProvider().also {
            it.attachInfo(context, ProviderInfo().apply { authority = AUTHORITY })
            ShadowContentResolver.registerProviderInternal(AUTHORITY, it)
        }
        source = DocumentsLocalMediaTreeSource(context)
    }

    @After
    fun tearDown() = ShadowContentResolver.reset()

    @Test
    fun recursivelyEnumeratesFilesAndIgnoresDirectoryCycles() = runTest {
        provider.children = mapOf(
            "root" to listOf(
                Document("album", DocumentsContract.Document.MIME_TYPE_DIR),
                Document("root-song", "audio/flac", "root.flac"),
                Document("cover", "image/jpeg", "cover.jpg"),
            ),
            "album" to listOf(
                Document("nested-song", "application/octet-stream", "nested.bin"),
                Document("mislabelled-song", "application/binary", "track.opus"),
                Document("root", DocumentsContract.Document.MIME_TYPE_DIR),
            ),
        )

        val result = source.scan("content://$AUTHORITY/tree/root")

        assertThat(result).isEqualTo(
            LocalMediaTreeScanResult.Available(
                listOf(
                    "content://$AUTHORITY/tree/root/document/root-song",
                    "content://$AUTHORITY/tree/root/document/nested-song",
                    "content://$AUTHORITY/tree/root/document/mislabelled-song",
                ),
            ),
        )
        assertThat(provider.queriedDirectoryIds).containsExactly("root", "album").inOrder()
    }

    @Test
    fun permissionFailureRemainsTyped() = runTest {
        provider.denyQueries = true

        assertThat(source.scan("content://$AUTHORITY/tree/root")).isEqualTo(
            LocalMediaTreeScanResult.Failed(LocalMediaTreeScanFailure.PermissionDenied),
        )
    }

    @Test
    fun nonTreeUriIsRejectedBeforeProviderAccess() = runTest {
        assertThat(source.scan("content://$AUTHORITY/document/song")).isEqualTo(
            LocalMediaTreeScanResult.Failed(LocalMediaTreeScanFailure.InvalidTree),
        )
        assertThat(provider.queriedDirectoryIds).isEmpty()
    }

    private class TreeProvider : ContentProvider() {
        var children: Map<String, List<Document>> = emptyMap()
        var denyQueries = false
        val queriedDirectoryIds = mutableListOf<String>()

        override fun onCreate(): Boolean = true

        override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?,
        ): Cursor {
            if (denyQueries) throw SecurityException("denied")
            val directoryId = DocumentsContract.getDocumentId(uri)
            queriedDirectoryIds += directoryId
            return MatrixCursor(
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                ),
            ).apply {
                children[directoryId].orEmpty().forEach { document ->
                    addRow(arrayOf(document.id, document.mimeType, document.displayName))
                }
            }
        }

        override fun getType(uri: Uri): String? = null
        override fun insert(uri: Uri, values: ContentValues?): Uri? = null
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
        override fun update(
            uri: Uri,
            values: ContentValues?,
            selection: String?,
            selectionArgs: Array<out String>?,
        ): Int = 0

        override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? = null
        override fun call(method: String, arg: String?, extras: Bundle?): Bundle? = null
        override fun query(
            uri: Uri,
            projection: Array<out String>?,
            queryArgs: Bundle?,
            cancellationSignal: CancellationSignal?,
        ): Cursor = query(uri, projection, null, null, null)
    }

    private data class Document(val id: String, val mimeType: String, val displayName: String = id)

    private companion object {
        const val AUTHORITY = "com.resonote.test.documents"
    }
}
