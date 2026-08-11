package com.resonote.core.network

import com.resonote.core.network.model.NetworkSearchPage

interface ApiNetworkDataSource {
    suspend fun searchSongs(
        keywords: String,
        page: Int = 1,
        pageSize: Int = 30,
    ): NetworkSearchPage
}
