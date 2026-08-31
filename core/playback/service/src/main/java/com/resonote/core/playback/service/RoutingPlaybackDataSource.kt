@file:androidx.media3.common.util.UnstableApi

package com.resonote.core.playback.service

import android.net.Uri
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import java.io.IOException

internal class RoutingPlaybackDataSource(
    private val downloads: DataSource.Factory,
    private val streaming: DataSource.Factory,
) : DataSource {
    private val listeners = mutableListOf<TransferListener>()
    private var delegate: DataSource? = null

    override fun addTransferListener(transferListener: TransferListener) {
        listeners += transferListener
    }

    @Throws(IOException::class)
    override fun open(dataSpec: DataSpec): Long {
        check(delegate == null) { "DataSource is already open" }
        val selected = if (dataSpec.key?.startsWith(DOWNLOAD_ID_PREFIX) == true) {
            downloads.createDataSource()
        } else {
            streaming.createDataSource()
        }
        listeners.forEach(selected::addTransferListener)
        delegate = selected
        return selected.open(dataSpec)
    }

    @Throws(IOException::class)
    override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
        checkNotNull(delegate) { "DataSource is not open" }.read(buffer, offset, length)

    override fun getUri(): Uri? = delegate?.uri

    override fun getResponseHeaders(): Map<String, List<String>> = delegate?.responseHeaders.orEmpty()

    @Throws(IOException::class)
    override fun close() {
        val selected = delegate
        delegate = null
        selected?.close()
    }
}

internal const val DOWNLOAD_ID_PREFIX = "download:"
