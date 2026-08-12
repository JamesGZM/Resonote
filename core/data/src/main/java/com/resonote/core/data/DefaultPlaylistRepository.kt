package com.resonote.core.data

import com.resonote.core.model.CollectionLoadResult
import com.resonote.core.model.PlaylistDetails
import com.resonote.core.model.PlaylistPage
import com.resonote.core.network.ApiNetworkDataSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultPlaylistRepository @Inject constructor(
    private val network: ApiNetworkDataSource,
    private val riskChallenges: RiskChallengeRegistry,
) : PlaylistRepository {
    override suspend fun loadPlaylist(
        globalCollectionId: String,
        page: Int,
        pageSize: Int,
    ): CollectionLoadResult<PlaylistPage> {
        require(globalCollectionId.isNotBlank()) { "globalCollectionId must not be blank" }
        validateCollectionPage(page, pageSize)
        return loadCollection(riskChallenges) {
            val result = network.playlistSongs(globalCollectionId, page, pageSize)
            PlaylistPage(
                details =
                    result.info?.let {
                        PlaylistDetails(
                            id = it.id,
                            title = it.title,
                            description = it.description,
                            coverUrl = it.coverUrl?.replace("{size}", "480"),
                            songCount = it.songCount,
                        )
                    },
                songs = result.songs.map { it.toOnlineSong() },
                page = page,
                hasMore = result.hasMore,
            )
        }
    }
}
