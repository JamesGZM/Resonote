package com.resonote.core.network.retrofit

import com.resonote.core.network.ApiProtocolException
import com.resonote.core.network.LibraryNetworkDataSource
import com.resonote.core.network.api.MusicApi
import com.resonote.core.network.api.model.PlaylistCreateRequest
import com.resonote.core.network.api.model.PlaylistFileResource
import com.resonote.core.network.api.model.PlaylistTrackResource
import com.resonote.core.network.api.model.PlaylistTracksAddRequest
import com.resonote.core.network.api.model.PlaylistTracksDeleteRequest
import com.resonote.core.network.api.model.UserPlaylistsRequest
import com.resonote.core.network.model.NetworkPlaylistTrackInput
import com.resonote.core.network.model.NetworkUserPlaylist
import com.resonote.core.network.protocol.DeviceRegistrationCoordinator
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class RealLibraryNetworkDataSource @Inject constructor(
    private val musicApi: MusicApi,
    private val registration: DeviceRegistrationCoordinator,
    private val clock: Clock,
    private val calls: ApiCallExecutor,
    private val responses: ApiResponseVerifier,
) : LibraryNetworkDataSource {
    override suspend fun userPlaylists(page: Int, pageSize: Int): List<NetworkUserPlaylist> {
        require(page > 0) { "page must be positive" }
        require(pageSize in 1..200) { "pageSize must be between 1 and 200" }
        val session = registration.requireAuthenticatedSession()
        val userId = requireNotNull(session.userId)
        val token = requireNotNull(session.token)
        val response = calls.execute {
            musicApi.userPlaylists(
                userId = userId.toLongOrNull() ?: throw missingField(), token = token,
                body = UserPlaylistsRequest(userId, token, 979, 2, page, pageSize),
            )
        }
        responses.requireSuccess(response)
        val raw = response.data?.info ?: throw missingField()
        return raw.mapNotNull { item ->
            val listId = item.listid?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            val globalId = item.globalId?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            val name = item.name?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            if (item.authors != null) return@mapNotNull null
            NetworkUserPlaylist(listId, globalId, name, item.pic?.takeIf(String::isNotBlank), item.count?.coerceAtLeast(0) ?: 0, item.ownerUserId == userId, name == LIKE_PLAYLIST_NAME)
        }.also { if (raw.isNotEmpty() && it.isEmpty()) throw malformedResponse() }
    }

    override suspend fun createPlaylist(name: String): String {
        val normalized = name.trim()
        require(normalized.isNotEmpty()) { "name must not be blank" }
        val session = registration.requireAuthenticatedSession()
        val userId = requireNotNull(session.userId)
        val token = requireNotNull(session.token)
        val response = calls.execute {
            musicApi.createPlaylist(
                lastTime = clock.millis() / 1_000, userId = userId.toLongOrNull() ?: throw missingField(), token = token,
                body = PlaylistCreateRequest(userId, token, 0, normalized, 0, 1, 0, userId, "", 0),
            )
        }
        responses.requireWriteSuccess(response)
        return response.data?.info?.listid?.takeIf(String::isNotBlank) ?: throw missingField()
    }

    override suspend fun addPlaylistTracks(listId: String, tracks: List<NetworkPlaylistTrackInput>) {
        require(listId.isNotBlank()) { "listId must not be blank" }
        require(tracks.isNotEmpty()) { "tracks must not be empty" }
        val session = registration.requireAuthenticatedSession()
        val resources = tracks.map { track ->
            require(track.hash.isNotBlank()) { "track hash must not be blank" }
            PlaylistTrackResource(1, "${track.artist} - ${track.title}".replace(PLAYLIST_SEPARATOR_PATTERN, " "), track.hash, 0, 0, 0, 0, track.albumId?.toLongOrNull() ?: 0, track.albumAudioId?.toLongOrNull() ?: 0)
        }
        val userId = requireNotNull(session.userId)
        val token = requireNotNull(session.token)
        val response = calls.execute {
            musicApi.addPlaylistTracks(
                lastTime = clock.millis() / 1_000, userId = userId.toLongOrNull() ?: throw missingField(), token = token,
                body = PlaylistTracksAddRequest(userId, token, listId, 0, 0, 1, "false;null", resources),
            )
        }
        responses.requireWriteSuccess(response)
    }

    override suspend fun deletePlaylistTracks(listId: String, fileIds: List<String>) {
        require(listId.isNotBlank()) { "listId must not be blank" }
        require(fileIds.isNotEmpty()) { "fileIds must not be empty" }
        val resources = fileIds.map { PlaylistFileResource(it.toLongOrNull()?.takeIf { value -> value > 0 } ?: throw IllegalArgumentException("fileIds must be positive numbers")) }
        val session = registration.requireAuthenticatedSession()
        val response = calls.execute {
            musicApi.deletePlaylistTracks(PlaylistTracksDeleteRequest(listId, requireNotNull(session.userId), resources, 0, requireNotNull(session.token), 0))
        }
        responses.requireWriteSuccess(response)
    }

    private fun missingField() = ApiProtocolException(ApiProtocolException.Reason.MissingRequiredField)
    private fun malformedResponse() = ApiProtocolException(ApiProtocolException.Reason.MalformedResponse)

    private companion object {
        // Provider-defined built-in playlist name used by the fixed Mobile consumer.
        const val LIKE_PLAYLIST_NAME = "我喜欢"
        val PLAYLIST_SEPARATOR_PATTERN = Regex("[,|]")
    }
}
