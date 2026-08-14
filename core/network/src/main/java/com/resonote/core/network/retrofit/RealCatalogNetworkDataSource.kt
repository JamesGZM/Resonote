package com.resonote.core.network.retrofit

import com.resonote.core.network.CatalogNetworkDataSource
import com.resonote.core.network.ApiProtocolException
import com.resonote.core.network.api.MusicApi
import com.resonote.core.network.api.model.PlaylistRecommendationsResponse
import com.resonote.core.network.api.model.RecommendedPlaylistsRequest
import com.resonote.core.network.api.model.SpecialRecommendRequest
import com.resonote.core.network.api.model.AlbumSongsRequest
import com.resonote.core.network.api.model.ArtistAudiosRequest
import com.resonote.core.network.api.model.ArtistDetailRequest
import com.resonote.core.network.api.model.BannerRequest
import com.resonote.core.network.api.model.PlaylistTagsRequest
import com.resonote.core.network.api.model.TopAlbumsRequest
import com.resonote.core.network.protocol.DeviceRegistrationCoordinator
import com.resonote.core.network.model.NetworkAlbum
import com.resonote.core.network.model.NetworkAlbumRegion
import com.resonote.core.network.model.NetworkAlbumSongPage
import com.resonote.core.network.model.NetworkArtistInfo
import com.resonote.core.network.model.NetworkArtistSongPage
import com.resonote.core.network.model.NetworkBanner
import com.resonote.core.network.model.NetworkPlaylistCategory
import com.resonote.core.network.model.NetworkPlaylistSummary
import com.resonote.core.network.model.NetworkSong
import com.resonote.core.network.protocol.ApiProtocolConfig
import com.resonote.core.network.protocol.ApiEndpointOrigins
import com.resonote.core.network.protocol.ApiRequestSigner
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

@Singleton
internal class RealCatalogNetworkDataSource @Inject constructor(
    private val musicApi: MusicApi,
    private val registration: DeviceRegistrationCoordinator,
    private val signer: ApiRequestSigner,
    private val clock: Clock,
    private val responses: ApiResponseVerifier,
    private val calls: ApiCallExecutor,
    private val origins: ApiEndpointOrigins,
) : CatalogNetworkDataSource {

    override suspend fun recommendedPlaylists(page: Int, pageSize: Int): List<NetworkPlaylistSummary> =
        loadPlaylists(page, pageSize, categoryId = 0)

    override suspend fun categoryPlaylists(categoryId: Int, page: Int, pageSize: Int): List<NetworkPlaylistSummary> =
        loadPlaylists(page, pageSize, categoryId)

    private suspend fun loadPlaylists(page: Int, pageSize: Int, categoryId: Int): List<NetworkPlaylistSummary> {
        validatePage(page, pageSize)
        require(categoryId >= 0) { "categoryId must not be negative" }
        val session = registration.ensureRegisteredSession()
        val clientTime = (clock.millis() / 1_000).toString()
        val body = RecommendedPlaylistsRequest(
            appid = ApiProtocolConfig.APP_ID.toInt(),
            mid = session.mid,
            clientver = ApiProtocolConfig.CLIENT_VERSION.toInt(),
            platform = "android",
            clienttime = clientTime,
            userid = session.userId?.toLongOrNull() ?: 0,
            moduleId = 1,
            page = page,
            pagesize = pageSize,
            key = signer.signParamsKey(clientTime),
            specialRecommend = SpecialRecommendRequest(
                withtag = 1,
                withsong = 1,
                sort = 1,
                ugc = 1,
                isSelected = 0,
                withrecommend = 1,
                areaCode = 1,
                categoryid = categoryId,
            ),
            requestMultiple = 1,
            returnMinimum = 5,
            returnSpecialFlag = 1,
        )
        return decodePlaylists(callApi { musicApi.recommendedPlaylists(body) })
    }

    override suspend fun banners(): List<NetworkBanner> {
        val session = registration.ensureRegisteredSession()
        val response = callApi { musicApi.banners(BannerRequest(0, 201, 7, 2, session.userId?.toLongOrNull() ?: 0, 0, 0, emptyList(), 5, 2, "normal")) }
        responses.requireSuccess(response)
        val raw = response.data.obj()?.array("ads") ?: throw missingField()
        return raw.mapNotNull { element ->
            val item = element.obj() ?: return@mapNotNull null
            val image = (item.text("img_url") ?: item.text("image"))?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            val extra = item.obj("extra")
            val link = (extra?.text("url") ?: item.text("url") ?: item.text("jump_url"))?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
            NetworkBanner(item.text("id")?.takeIf(String::isNotBlank) ?: image, item.text("title") ?: extra?.text("title"), image, link)
        }.also { requireConsumableItems(raw, it) }
    }

    override suspend fun playlistCategories(): List<NetworkPlaylistCategory> {
        registration.ensureRegisteredSession()
        val response = callApi { musicApi.playlistTags(PlaylistTagsRequest("collection", 0, 3)) }
        responses.requireSuccess(response)
        val raw = response.data as? JsonArray ?: throw missingField()
        return raw.mapNotNull { element ->
            val item = element.obj() ?: return@mapNotNull null
            val name = item.text("tag_name")?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            NetworkPlaylistCategory(
                tagId = item.int("tag_id") ?: 0,
                name = name,
                children = item.array("son").orEmpty().mapNotNull { childElement ->
                    val child = childElement.obj() ?: return@mapNotNull null
                    val childId = child.int("tag_id")?.takeIf { it > 0 } ?: return@mapNotNull null
                    val childName = child.text("tag_name")?.takeIf(String::isNotBlank) ?: return@mapNotNull null
                    NetworkPlaylistCategory(childId, childName, emptyList())
                },
            )
        }.also { requireConsumableItems(raw, it) }
    }

    override suspend fun newAlbums(page: Int, pageSize: Int): List<NetworkAlbum> {
        validatePage(page, pageSize)
        val session = registration.ensureRegisteredSession()
        val response = callApi { musicApi.newAlbums(TopAlbumsRequest(20, session.token.orEmpty(), page, pageSize, 1)) }
        responses.requireSuccess(response)
        val data = response.data.obj() ?: throw missingField()
        val regions = listOf("chn" to NetworkAlbumRegion.Chinese, "eur" to NetworkAlbumRegion.Western, "jpn" to NetworkAlbumRegion.Japanese, "kor" to NetworkAlbumRegion.Korean)
        val raw = regions.flatMap { (key, _) -> data.array(key).orEmpty() }
        return regions.flatMap { (key, region) ->
            data.array(key).orEmpty().mapNotNull { element ->
                val item = element.obj() ?: return@mapNotNull null
                val id = (item.text("albumid") ?: item.text("album_id"))?.takeIf(String::isNotBlank) ?: return@mapNotNull null
                val name = (item.text("albumname") ?: item.text("album_name"))?.takeIf(String::isNotBlank) ?: return@mapNotNull null
                NetworkAlbum(
                    id, name, item.text("singername") ?: item.text("author_name"),
                    item.text("imgurl") ?: item.text("img") ?: item.text("sizable_cover"),
                    (item.text("publishtime") ?: item.text("publish_time")).orEmpty().substringBefore(' '),
                    (item.int("songcount") ?: 0).coerceAtLeast(0), region,
                )
            }
        }.also { requireConsumableItems(raw, it) }
    }

    override suspend fun albumSongs(albumId: String, page: Int, pageSize: Int): NetworkAlbumSongPage {
        require(albumId.isNotBlank()) { "albumId must not be blank" }
        validatePage(page, pageSize)
        registration.ensureRegisteredSession()
        val response = callApi { musicApi.albumSongs(AlbumSongsRequest(albumId.trim(), "", page, pageSize)) }
        responses.requireSuccess(response)
        val data = response.data.obj() ?: throw missingField()
        val raw = data.array("songs") ?: throw missingField()
        val songs = raw.mapNotNull(::decodeAlbumSong)
        requireConsumableItems(raw, songs)
        val total = (data.int("total") ?: songs.size).coerceAtLeast(0)
        return NetworkAlbumSongPage(songs, total, if (total > 0) page.toLong() * pageSize < total else raw.size >= pageSize)
    }

    override suspend fun artistDetail(artistId: String): NetworkArtistInfo? {
        require(artistId.isNotBlank()) { "artistId must not be blank" }
        registration.ensureRegisteredSession()
        val response = callApi { musicApi.artistDetail(ArtistDetailRequest(artistId.trim())) }
        responses.requireSuccess(response)
        val data = response.data.obj() ?: throw missingField()
        val name = (data.text("author_name") ?: data.text("singername"))?.takeIf(String::isNotBlank) ?: return null
        return NetworkArtistInfo(
            name, data.text("sizable_avatar") ?: data.text("avatar") ?: data.text("imgurl"),
            data.text("intro") ?: data.text("description") ?: "", (data.int("song_count") ?: data.int("audio_count") ?: 0).coerceAtLeast(0),
            (data.int("album_count") ?: 0).coerceAtLeast(0), (data.int("mv_count") ?: 0).coerceAtLeast(0),
            (data.long("fansnums") ?: data.long("fans_count") ?: 0).coerceAtLeast(0),
        )
    }

    override suspend fun artistSongs(artistId: String, page: Int, pageSize: Int, newestFirst: Boolean): NetworkArtistSongPage {
        require(artistId.isNotBlank()) { "artistId must not be blank" }
        validatePage(page, pageSize)
        val session = registration.ensureRegisteredSession()
        val clientTime = clock.millis() / 1_000
        val response = callApi {
            musicApi.artistSongs(
                "${origins.openApi}/kmr/v1/audio_group/author",
                ArtistAudiosRequest(ApiProtocolConfig.APP_ID.toInt(), ApiProtocolConfig.CLIENT_VERSION.toInt(), session.mid, clientTime, signer.signParamsKey(clientTime.toString()), artistId.trim(), pageSize, page, if (newestFirst) 2 else 1, "all"),
            )
        }
        responses.requireSuccess(response)
        val raw = response.data as? JsonArray ?: throw missingField()
        val songs = raw.mapNotNull(::decodeFlatContentSong)
        requireConsumableItems(raw, songs)
        return NetworkArtistSongPage(songs, raw.size >= pageSize)
    }

    private suspend fun <T> callApi(block: suspend () -> T): T = calls.execute(block = block)

    private suspend fun decodePlaylists(response: PlaylistRecommendationsResponse): List<NetworkPlaylistSummary> {
        responses.requireSuccess(response)
        val rawItems = response.data?.playlists ?: throw missingField()
        val items = rawItems.mapNotNull { item ->
            val id = (item.globalCollectionId ?: item.specialid)?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            val title = item.specialname?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            NetworkPlaylistSummary(
                id = id,
                title = title,
                coverUrl = (item.flexibleCover ?: item.cover ?: item.imgurl)?.takeIf(String::isNotBlank),
                playCount = item.playCount?.coerceAtLeast(0),
            )
        }
        requireConsumableItems(rawItems, items)
        return items
    }

    private fun decodeAlbumSong(element: kotlinx.serialization.json.JsonElement): NetworkSong? {
        val item = element.obj() ?: return null
        val audio = item.obj("audio_info") ?: return null
        val base = item.obj("base") ?: return null
        val album = item.obj("album_info")
        val trans = item.obj("trans_param")
        val hashOffset = trans?.obj("hash_offset")
        val copyright = item.obj("copyright")
        val hash = audio.text("hash")?.takeIf(String::isNotBlank) ?: return null
        val title = base.text("audio_name")?.takeIf(String::isNotBlank) ?: return null
        val hq = audio.text("hash_320")?.takeIf(String::isNotBlank)
        val sq = audio.text("hash_flac")?.takeIf(String::isNotBlank)
        return NetworkSong(
            hash = hash,
            title = title,
            artist = base.text("author_name"),
            coverUrl = trans?.text("union_cover") ?: album?.text("sizable_cover"),
            albumId = album?.text("album_id"),
            albumAudioId = base.text("album_audio_id") ?: base.text("audio_id"),
            durationMillis = normalizeDurationMillis(audio.long("duration")),
            highQualityHash = hq,
            losslessHash = sq,
            vip = (copyright?.int("privilege") ?: 0) >= 10,
            highQualityAvailable = hq != null,
            losslessAvailable = sq != null,
            albumTitle = album?.text("album_name"),
            previewDurationMillis = previewDurationMillis(hashOffset?.long("start_ms"), hashOffset?.long("end_ms")),
        )
    }

    private fun decodeFlatContentSong(element: kotlinx.serialization.json.JsonElement): NetworkSong? {
        val item = element.obj() ?: return null
        val hash = item.text("hash")?.takeIf(String::isNotBlank) ?: return null
        val title = (item.text("audio_name") ?: item.text("songname"))?.takeIf(String::isNotBlank) ?: return null
        val trans = item.obj("trans_param")
        val hashOffset = trans?.obj("hash_offset")
        val hq = item.text("hash_320")?.takeIf(String::isNotBlank)
        val sq = item.text("hash_flac")?.takeIf(String::isNotBlank)
        return NetworkSong(
            hash = hash,
            title = title,
            artist = item.text("author_name"),
            coverUrl = trans?.text("union_cover") ?: item.text("cover"),
            albumId = item.text("album_id"),
            albumAudioId = item.text("album_audio_id") ?: item.text("audio_id") ?: item.text("mixsongid"),
            durationMillis = normalizeDurationMillis(item.long("timelength") ?: item.long("duration")),
            highQualityHash = hq,
            losslessHash = sq,
            vip = (item.int("privilege") ?: 0) >= 10,
            highQualityAvailable = hq != null,
            losslessAvailable = sq != null,
            albumTitle = item.text("album_name"),
            previewDurationMillis = previewDurationMillis(hashOffset?.long("start_ms"), hashOffset?.long("end_ms")),
        )
    }

    private fun kotlinx.serialization.json.JsonElement?.obj(): JsonObject? = this as? JsonObject
    private fun JsonObject.obj(name: String): JsonObject? = get(name) as? JsonObject
    private fun JsonObject.array(name: String): JsonArray? = get(name) as? JsonArray
    private fun JsonObject.text(name: String): String? = (get(name) as? JsonPrimitive)?.contentOrNull
    private fun JsonObject.long(name: String): Long? = text(name)?.toDoubleOrNull()?.toLong()
    private fun JsonObject.int(name: String): Int? = long(name)?.coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong())?.toInt()

    private fun validatePage(page: Int, pageSize: Int) {
        require(page > 0) { "page must be positive" }
        require(pageSize in 1..100) { "pageSize must be between 1 and 100" }
    }

    private fun <R, T> requireConsumableItems(rawItems: List<R>, items: List<T>) {
        if (rawItems.isNotEmpty() && items.isEmpty()) throw missingField()
    }

    private fun missingField() = ApiProtocolException(ApiProtocolException.Reason.MissingRequiredField)

}
