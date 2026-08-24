package com.resonote.core.network.retrofit

import com.resonote.core.network.ApiProtocolException
import com.resonote.core.network.CatalogNetworkDataSource
import com.resonote.core.network.api.MusicApi
import com.resonote.core.network.api.model.AlbumSongsRequest
import com.resonote.core.network.api.model.ArtistAlbumsRequest
import com.resonote.core.network.api.model.ArtistAudiosRequest
import com.resonote.core.network.api.model.ArtistDetailRequest
import com.resonote.core.network.api.model.ArtistFollowListRequest
import com.resonote.core.network.api.model.ArtistFollowMutationRequest
import com.resonote.core.network.api.model.BannerRequest
import com.resonote.core.network.api.model.PlaylistRecommendationsResponse
import com.resonote.core.network.api.model.PlaylistTagsRequest
import com.resonote.core.network.api.model.RecommendedPlaylistsRequest
import com.resonote.core.network.api.model.SpecialRecommendRequest
import com.resonote.core.network.api.model.TopAlbumsRequest
import com.resonote.core.network.model.NetworkAlbum
import com.resonote.core.network.model.NetworkAlbumRegion
import com.resonote.core.network.model.NetworkAlbumSongPage
import com.resonote.core.network.model.NetworkArtistAlbum
import com.resonote.core.network.model.NetworkArtistAlbumPage
import com.resonote.core.network.model.NetworkArtistInfo
import com.resonote.core.network.model.NetworkArtistSongPage
import com.resonote.core.network.model.NetworkArtistVideo
import com.resonote.core.network.model.NetworkArtistVideoPage
import com.resonote.core.network.model.NetworkBanner
import com.resonote.core.network.model.NetworkFollowedArtist
import com.resonote.core.network.model.NetworkPlaylistCategory
import com.resonote.core.network.model.NetworkPlaylistSummary
import com.resonote.core.network.model.NetworkSong
import com.resonote.core.network.protocol.ApiEndpointOrigins
import com.resonote.core.network.protocol.ApiProtocolConfig
import com.resonote.core.network.protocol.ApiProtocolCrypto
import com.resonote.core.network.protocol.ApiRequestSigner
import com.resonote.core.network.protocol.DeviceRegistrationCoordinator
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class RealCatalogNetworkDataSource @Inject constructor(
    private val musicApi: MusicApi,
    private val registration: DeviceRegistrationCoordinator,
    private val signer: ApiRequestSigner,
    private val clock: Clock,
    private val crypto: ApiProtocolCrypto,
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
                withsong = 0,
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
        return mapPlaylists(callApi { musicApi.recommendedPlaylists(body) })
    }

    override suspend fun banners(): List<NetworkBanner> {
        val session = registration.ensureRegisteredSession()
        val request = BannerRequest(
            plat = 0,
            channel = 201,
            operator = 7,
            networktype = 2,
            userid = session.userId?.toLongOrNull() ?: 0,
            vipType = 0,
            mobileType = 0,
            tags = emptyList(),
            apiver = 5,
            ability = 2,
            mode = "normal",
        )
        val response = callApi { musicApi.banners(request) }
        responses.requireSuccess(response)
        val raw = response.data.obj()?.array("ads") ?: throw missingField()
        return raw.mapNotNull { element ->
            val item = element.obj() ?: return@mapNotNull null
            val image =
                (item.text("img_url") ?: item.text("image"))?.takeIf(String::isNotBlank)
                    ?: return@mapNotNull null
            val extra = item.obj("extra")
            val link =
                (extra?.text("url") ?: item.text("url") ?: item.text("jump_url"))
                    ?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
            NetworkBanner(
                item.text("id")?.takeIf(String::isNotBlank) ?: image,
                item.text("title") ?: extra?.text("title"),
                image,
                link,
            )
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
        val regions = listOf(
            "chn" to NetworkAlbumRegion.Chinese,
            "eur" to NetworkAlbumRegion.Western,
            "jpn" to NetworkAlbumRegion.Japanese,
            "kor" to NetworkAlbumRegion.Korean,
        )
        val raw = regions.flatMap { (key, _) -> data.array(key).orEmpty() }
        return regions.flatMap { (key, region) ->
            data.array(key).orEmpty().mapNotNull { element ->
                val item = element.obj() ?: return@mapNotNull null
                val id =
                    (item.text("albumid") ?: item.text("album_id"))?.takeIf(String::isNotBlank)
                        ?: return@mapNotNull null
                val name =
                    (item.text("albumname") ?: item.text("album_name"))?.takeIf(String::isNotBlank)
                        ?: return@mapNotNull null
                NetworkAlbum(
                    id,
                    name,
                    item.text("singername") ?: item.text("author_name"),
                    item.text("imgurl") ?: item.text("img") ?: item.text("sizable_cover"),
                    (item.text("publishtime") ?: item.text("publish_time")).orEmpty().substringBefore(' '),
                    (item.int("songcount") ?: 0).coerceAtLeast(0),
                    region,
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
        val hasMore = if (total > 0) page.toLong() * pageSize < total else raw.size >= pageSize
        return NetworkAlbumSongPage(songs, total, hasMore)
    }

    override suspend fun artistDetail(artistId: String): NetworkArtistInfo? {
        require(artistId.isNotBlank()) { "artistId must not be blank" }
        registration.ensureRegisteredSession()
        val response = callApi { musicApi.artistDetail(ArtistDetailRequest(artistId.trim())) }
        responses.requireSuccess(response)
        val data = response.data.obj() ?: throw missingField()
        val name = (data.text("author_name") ?: data.text("singername"))?.takeIf(String::isNotBlank) ?: return null
        return NetworkArtistInfo(
            name,
            data.text("sizable_avatar") ?: data.text("avatar") ?: data.text("imgurl"),
            data.text("intro") ?: data.text("description") ?: "",
            (data.int("song_count") ?: data.int("audio_count") ?: 0).coerceAtLeast(0),
            (data.int("album_count") ?: 0).coerceAtLeast(0),
            (data.int("mv_count") ?: 0).coerceAtLeast(0),
            (data.long("fansnums") ?: data.long("fans_count") ?: 0).coerceAtLeast(0),
        )
    }

    override suspend fun artistSongs(
        artistId: String,
        page: Int,
        pageSize: Int,
        newestFirst: Boolean,
    ): NetworkArtistSongPage {
        require(artistId.isNotBlank()) { "artistId must not be blank" }
        validatePage(page, pageSize)
        val session = registration.ensureRegisteredSession()
        val clientTime = clock.millis() / 1_000
        val response = callApi {
            musicApi.artistSongs(
                "${origins.openApi}/kmr/v1/audio_group/author",
                ArtistAudiosRequest(
                    appid = ApiProtocolConfig.APP_ID.toInt(),
                    clientver = ApiProtocolConfig.CLIENT_VERSION.toInt(),
                    mid = session.mid,
                    clienttime = clientTime,
                    key = signer.signParamsKey(clientTime.toString()),
                    artistId = artistId.trim(),
                    pagesize = pageSize,
                    page = page,
                    sort = if (newestFirst) 2 else 1,
                    areaCode = "all",
                ),
            )
        }
        responses.requireSuccess(response)
        val raw = response.data as? JsonArray ?: throw missingField()
        val songs = raw.mapNotNull(::decodeFlatContentSong)
        requireConsumableItems(raw, songs)
        return NetworkArtistSongPage(songs, raw.size >= pageSize)
    }

    override suspend fun artistAlbums(
        artistId: String,
        page: Int,
        pageSize: Int,
        newestFirst: Boolean,
    ): NetworkArtistAlbumPage {
        require(artistId.isNotBlank()) { "artistId must not be blank" }
        validatePage(page, pageSize)
        registration.ensureRegisteredSession()
        val response = callApi {
            musicApi.artistAlbums(
                ArtistAlbumsRequest(
                    artistId = artistId.trim(),
                    pagesize = pageSize,
                    page = page,
                    sort = if (newestFirst) 1 else 3,
                    category = 1,
                    areaCode = "all",
                ),
            )
        }
        responses.requireSuccess(response)
        val (raw, container) = response.data.artistCollection("albums")
        val albums = raw.mapNotNull { element ->
            val item = element.obj() ?: return@mapNotNull null
            val id = (item.text("album_id") ?: item.text("albumid"))?.takeIf(String::isNotBlank)
                ?: return@mapNotNull null
            val name = (item.text("album_name") ?: item.text("albumname"))?.takeIf(String::isNotBlank)
                ?: return@mapNotNull null
            NetworkArtistAlbum(
                id = id,
                name = name,
                artist = item.text("author_name") ?: item.text("singername"),
                coverUrl = item.text("sizable_cover") ?: item.text("imgurl") ?: item.text("img"),
                publishDate = (item.text("publish_date") ?: item.text("publishtime")).orEmpty().substringBefore(' '),
                songCount = (item.int("audio_count") ?: item.int("song_count") ?: item.int("songcount") ?: 0)
                    .coerceAtLeast(0),
            )
        }
        requireConsumableItems(raw, albums)
        val total = container?.int("total") ?: container?.int("total_count")
        return NetworkArtistAlbumPage(albums, total, hasMore(page, pageSize, raw.size, total))
    }

    override suspend fun artistVideos(artistId: String, page: Int, pageSize: Int): NetworkArtistVideoPage {
        require(artistId.isNotBlank()) { "artistId must not be blank" }
        validatePage(page, pageSize)
        registration.ensureRegisteredSession()
        val response = callApi {
            musicApi.artistVideos(
                url = "${origins.openApiCdn}/kmr/v1/author/videos",
                artistId = artistId.trim(),
                pageSize = pageSize,
                page = page,
            )
        }
        responses.requireSuccess(response)
        val (raw, container) = response.data.artistCollection("videos")
        val videos = raw.mapNotNull { element ->
            val item = element.obj() ?: return@mapNotNull null
            val hash = (
                item.text("mvhash") ?: item.text("MvHash") ?: item.text("hash")
                    ?: item.text("mkv_sd_hash") ?: item.obj("h264")?.text("sd_hash")
                )
                ?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            val name = (
                item.text("mvname") ?: item.text("MvName") ?: item.text("name")
                    ?: item.text("video_name")
                )
                ?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            val duration = item.long("duration") ?: item.long("Duration") ?: item.long("timelength") ?: 0
            NetworkArtistVideo(
                hash = hash,
                name = name,
                singer = item.text("singername") ?: item.text("author_name"),
                coverUrl = resolveArtistVideoCover(
                    item.text("sizable_cover") ?: item.text("hdpic") ?: item.text("cover")
                        ?: item.text("imgurl") ?: item.text("pic") ?: item.text("Pic"),
                ),
                durationMillis = if (duration > 1_000) duration else duration * 1_000,
            )
        }
        requireConsumableItems(raw, videos)
        val total = container?.int("total") ?: container?.int("total_count")
            ?: response.total?.coerceIn(0, Int.MAX_VALUE.toLong())?.toInt()
        return NetworkArtistVideoPage(videos, total, hasMore(page, pageSize, raw.size, total))
    }

    override suspend fun followedArtists(): List<NetworkFollowedArtist> {
        val raw = artistFollowItems()
        val artistItems = raw.filter { it.obj()?.text("source") == "7" }
        val artists = artistItems.mapNotNull { element ->
            val item = element.obj() ?: return@mapNotNull null
            val id = (item.text("singerid") ?: item.text("singer_id"))?.takeIf(String::isNotBlank)
                ?: return@mapNotNull null
            val name = (
                item.text("singername") ?: item.text("singer_name") ?: item.text("author_name")
                    ?: item.text("name") ?: item.text("nickname")
                )?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            NetworkFollowedArtist(
                id = id,
                name = name,
                avatarUrl = item.text("k_pic") ?: item.text("sizable_avatar") ?: item.text("pic"),
            )
        }
        requireConsumableItems(artistItems, artists)
        return artists.distinctBy(NetworkFollowedArtist::id)
    }

    override suspend fun isArtistFollowed(artistId: String): Boolean {
        require(artistId.isNotBlank()) { "artistId must not be blank" }
        return artistFollowItems().any { element ->
            val item = element.obj() ?: return@any false
            item.text("source") == "7" &&
                (item.text("singerid") ?: item.text("singer_id")) == artistId.trim()
        }
    }

    private suspend fun artistFollowItems(): JsonArray {
        val session = registration.requireAuthenticatedSession()
        val clientTime = clock.millis() / 1_000
        val response = callApi {
            musicApi.artistFollowList(
                body = ArtistFollowListRequest(
                    merge = 2,
                    needIdentityType = 1,
                    extendedParams = "k_pic,jumptype,singerid,score",
                    userid = session.userId?.toLongOrNull() ?: throw missingField(),
                    type = 0,
                    idType = 0,
                    p = crypto.rawLiteRsa(
                        buildJsonObject {
                            put("clienttime", clientTime)
                            put("token", session.token ?: throw missingField())
                        }.toString(),
                    ).uppercase(),
                ),
            )
        }
        responses.requireSuccess(response)
        val data = response.data.obj() ?: throw missingField()
        val total = data.int("total") ?: 0
        return data.array("lists") ?: if (total == 0) JsonArray(emptyList()) else throw missingField()
    }

    override suspend fun setArtistFollowed(artistId: String, followed: Boolean) {
        require(artistId.isNotBlank()) { "artistId must not be blank" }
        val singerId = artistId.trim().toLongOrNull() ?: throw missingField()
        val session = registration.requireAuthenticatedSession()
        val token = session.token ?: throw missingField()
        val clientTime = clock.millis() / 1_000
        val encrypted = crypto.encryptTemporary(
            buildJsonObject {
                put("singerid", singerId)
                put("token", token)
            }.toString(),
        )
        val response = callApi {
            musicApi.mutateArtistFollow(
                url = "${origins.gateway}/followservice/v3/${if (followed) "follow_singer" else "unfollow_singer"}",
                clientTime = clientTime,
                body = ArtistFollowMutationRequest(
                    plat = 0,
                    userid = session.userId?.toLongOrNull() ?: throw missingField(),
                    singerid = singerId,
                    source = 7,
                    p = crypto.pkcs1LiteRsa(
                        buildJsonObject {
                            put("clienttime", clientTime)
                            put("key", encrypted.temporaryKey)
                        }.toString(),
                    ),
                    params = encrypted.ciphertextHex,
                ),
            )
        }
        responses.requireWriteSuccess(response)
    }

    private fun JsonElement?.artistCollection(preferredKey: String): Pair<JsonArray, JsonObject?> {
        val direct = this as? JsonArray
        if (direct != null) return direct to null
        val container = obj() ?: throw missingField()
        val raw = container.array(preferredKey) ?: container.array("list") ?: container.array("items")
            ?: container.array("data") ?: throw missingField()
        return raw to container
    }

    private fun hasMore(page: Int, pageSize: Int, rawSize: Int, total: Int?): Boolean =
        if (total != null && total > 0) page.toLong() * pageSize < total else rawSize >= pageSize

    private fun resolveArtistVideoCover(raw: String?): String? {
        val value = raw?.takeIf(String::isNotBlank) ?: return null
        return when {
            value.startsWith("http://") || value.startsWith("https://") -> value
            value.matches(Regex("^\\d{8,}\\.[a-zA-Z0-9]+$")) ->
                "https://imge.kugou.com/mvhdpic/480/${value.take(8)}/$value"
            value.startsWith('/') -> "https://imge.kugou.com$value"
            else -> value
        }
    }

    private suspend fun <T> callApi(block: suspend () -> T): T = calls.execute(block = block)

    private suspend fun mapPlaylists(value: PlaylistRecommendationsResponse): List<NetworkPlaylistSummary> {
        responses.requireSuccess(value)
        val rawItems = value.data?.playlists ?: throw missingField()
        val items = rawItems.mapNotNull { item ->
            val id =
                (item.globalCollectionId ?: item.specialid)?.takeIf(String::isNotBlank)
                    ?: return@mapNotNull null
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
    private fun JsonObject.int(name: String): Int? =
        long(name)?.coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong())?.toInt()

    private fun validatePage(page: Int, pageSize: Int) {
        require(page > 0) { "page must be positive" }
        require(pageSize in 1..100) { "pageSize must be between 1 and 100" }
    }

    private fun <R, T> requireConsumableItems(rawItems: List<R>, items: List<T>) {
        if (rawItems.isNotEmpty() && items.isEmpty()) throw missingField()
    }

    private fun missingField() = ApiProtocolException(ApiProtocolException.Reason.MissingRequiredField)
}
