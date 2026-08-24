package com.resonote.core.network

import com.resonote.core.network.model.NetworkAppRelease

interface AppUpdateNetworkDataSource {
    suspend fun latestRelease(): NetworkAppRelease
}
