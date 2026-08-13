package com.resonote.core.network.retrofit

import com.resonote.core.network.ApiProtocolException
import com.resonote.core.network.SearchNetworkDataSource
import com.resonote.core.network.api.MusicApi
import com.resonote.core.network.api.model.SearchSongsResponse
import com.resonote.core.network.model.NetworkComplexSearch
import com.resonote.core.network.model.NetworkSearchAlbum
import com.resonote.core.network.model.NetworkSearchArtist
import com.resonote.core.network.model.NetworkSearchKeyword
import com.resonote.core.network.model.NetworkSearchMv
import com.resonote.core.network.model.NetworkSearchPage
import com.resonote.core.network.model.NetworkSearchPlaylist
import com.resonote.core.network.model.NetworkSearchResultPage
import com.resonote.core.network.model.NetworkSong
import com.resonote.core.network.protocol.ApiEndpointOrigins
import com.resonote.core.network.protocol.DeviceRegistrationCoordinator
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

@Singleton
internal class RealSearchNetworkDataSource @Inject constructor(
    private val musicApi: MusicApi,
    private val registration: DeviceRegistrationCoordinator,
    private val responses: ApiResponseVerifier,
    private val calls: ApiCallExecutor,
    private val origins: ApiEndpointOrigins,
) : SearchNetworkDataSource {
    override suspend fun searchSongs(keywords: String, page: Int, pageSize: Int): NetworkSearchPage {
        validateSearchRequest(keywords, page, pageSize)
        registration.ensureRegisteredSession()
        val response = callApi {
            musicApi.searchSongs(
                keywords = keywords.trim(),
                page = page,
                pageSize = pageSize,
            )
        }
        return decodeSearchPage(response)
    }

    override suspend fun searchPlaylists(
        keywords: String,
        page: Int,
        pageSize: Int,
    ): NetworkSearchResultPage<NetworkSearchPlaylist> = searchTyped(keywords, page, pageSize, "special", ::decodeSearchPlaylist)

    override suspend fun searchAlbums(
        keywords: String,
        page: Int,
        pageSize: Int,
    ): NetworkSearchResultPage<NetworkSearchAlbum> = searchTyped(keywords, page, pageSize, "album", ::decodeSearchAlbum)

    override suspend fun searchArtists(
        keywords: String,
        page: Int,
        pageSize: Int,
    ): NetworkSearchResultPage<NetworkSearchArtist> = searchTyped(keywords, page, pageSize, "author", ::decodeSearchArtist)

    override suspend fun searchMvs(
        keywords: String,
        page: Int,
        pageSize: Int,
    ): NetworkSearchResultPage<NetworkSearchMv> = searchTyped(keywords, page, pageSize, "mv", ::decodeSearchMv)

    private suspend fun <T> searchTyped(
        keywords: String,
        page: Int,
        pageSize: Int,
        type: String,
        decode: (JsonElement) -> T?,
    ): NetworkSearchResultPage<T> {
        validateSearchRequest(keywords, page, pageSize)
        registration.ensureRegisteredSession()
        val response = callApi {
            musicApi.searchTyped(
                url = "${origins.complexSearch}/v1/search/$type",
                keywords = keywords.trim(),
                page = page,
                pageSize = pageSize,
            )
        }
        responses.requireSuccess(response, SEARCH_ENDPOINT_ID)
        val data = response.data.obj() ?: throw missingField()
        val raw = data.array("lists") ?: throw missingField()
        val items = raw.mapNotNull(decode)
        requireConsumableItems(raw, items)
        val total = (data.int("total") ?: items.size).coerceAtLeast(0)
        val hasMore = if (total > 0) page.toLong() * pageSize < total else raw.size >= pageSize
        return NetworkSearchResultPage(items, total, hasMore)
    }

    override suspend fun searchComplex(keywords: String): NetworkComplexSearch {
        require(keywords.isNotBlank()) { "keywords must not be blank" }
        registration.ensureRegisteredSession()
        val response = callApi { musicApi.searchComplex("${origins.complexSearch}/v6/search/complex", keyword = keywords.trim()) }
        responses.requireSuccess(response)
        val sections = response.data.obj()?.array("lists") ?: throw missingField()
        var artists = emptyList<NetworkSearchArtist>()
        var songs = emptyList<NetworkSong>()
        var songsTotal = 0
        var albums = emptyList<NetworkSearchAlbum>()
        var albumsTotal = 0
        var playlists = emptyList<NetworkSearchPlaylist>()
        var playlistsTotal = 0
        var mvs = emptyList<NetworkSearchMv>()
        var mvsTotal = 0
        sections.forEach { element ->
            val section = element.obj() ?: return@forEach
            val raw = section.array("lists").orEmpty()
            val total = (section.int("total") ?: 0).coerceAtLeast(0)
            when (section.text("type")) {
                "author" -> artists = raw.mapNotNull(::decodeSearchArtist)
                "song" -> { songs = raw.mapNotNull(::decodeSearchSong); songsTotal = total.takeIf { it > 0 } ?: songs.size }
                "album" -> { albums = raw.mapNotNull(::decodeSearchAlbum); albumsTotal = total.takeIf { it > 0 } ?: albums.size }
                "collect" -> { playlists = raw.mapNotNull(::decodeSearchPlaylist); playlistsTotal = total.takeIf { it > 0 } ?: playlists.size }
                "mv" -> { mvs = raw.mapNotNull(::decodeSearchMv); mvsTotal = total.takeIf { it > 0 } ?: mvs.size }
            }
        }
        return NetworkComplexSearch(artists, songs, songsTotal, albums, albumsTotal, playlists, playlistsTotal, mvs, mvsTotal)
    }

    override suspend fun hotSearchKeywords(): List<NetworkSearchKeyword> {
        registration.ensureRegisteredSession()
        val response = callApi { musicApi.hotSearch() }
        responses.requireSuccess(response)
        val groups = response.data.obj()?.array("list") ?: throw missingField()
        val raw = groups.firstOrNull().obj()?.array("keywords").orEmpty()
        return raw.mapNotNull { element ->
            val item = element.obj() ?: return@mapNotNull null
            val keyword = item.text("keyword")?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            NetworkSearchKeyword(keyword, item.text("reason").orEmpty())
        }.take(12)
    }

    override suspend fun searchSuggestions(keywords: String): List<String> {
        require(keywords.isNotBlank()) { "keywords must not be blank" }
        registration.ensureRegisteredSession()
        val response = callApi { musicApi.searchSuggestions(keywords.trim()) }
        responses.requireSuccess(response)
        val groups = response.data as? JsonArray ?: throw missingField()
        return groups.firstOrNull().obj()?.array("RecordDatas").orEmpty()
            .mapNotNull { it.obj()?.text("HintInfo")?.takeIf(String::isNotBlank) }
            .distinct().take(8)
    }

    private suspend fun decodeSearchPage(response: SearchSongsResponse): NetworkSearchPage {
        responses.requireSuccess(response, SEARCH_ENDPOINT_ID)
        val rawItems = response.data?.songs ?: throw missingField()
        val items = rawItems.mapNotNull { it.toNetworkSongOrNull() }
        requireConsumableItems(rawItems, items)
        val total = response.data.total?.coerceIn(0, Int.MAX_VALUE.toLong())?.toInt() ?: items.size
        return NetworkSearchPage(items, total)
    }

    private fun decodeSearchSong(element: JsonElement): NetworkSong? {
        val item = element.obj() ?: return null
        val hash = item.text("FileHash")?.takeIf(String::isNotBlank) ?: return null
        val title = (item.text("OriSongName") ?: item.text("SongName") ?: item.text("FileName"))?.stripEm()?.takeIf(String::isNotBlank) ?: return null
        val hq = item.text("HQFileHash") ?: item.text("FileHash320")
        val sq = item.text("SQFileHash") ?: item.text("FileHashFlac")
        return NetworkSong(
            hash, title, item.text("SingerName")?.stripEm(), item.text("Image"),
            item.text("AlbumID"), item.text("MixSongID"), normalizeDurationMillis(item.long("Duration")),
            hq, sq, (item.int("Privilege") ?: 0) >= 10, hq != null, sq != null, item.text("AlbumName")?.stripEm(),
        )
    }

    private fun decodeSearchArtist(element: JsonElement): NetworkSearchArtist? {
        val item = element.obj() ?: return null
        val id = (item.text("AuthorId") ?: item.text("SingerId"))?.takeIf(String::isNotBlank) ?: return null
        val name = (item.text("AuthorName") ?: item.text("SingerName"))?.stripEm()?.takeIf(String::isNotBlank) ?: return null
        return NetworkSearchArtist(id, name, item.text("Avatar") ?: item.text("Image"), item.int("AlbumCount") ?: 0, item.int("AudioCount") ?: 0)
    }

    private fun decodeSearchAlbum(element: JsonElement): NetworkSearchAlbum? {
        val item = element.obj() ?: return null
        val id = item.text("albumid")?.takeIf(String::isNotBlank) ?: return null
        val name = item.text("albumname")?.stripEm()?.takeIf(String::isNotBlank) ?: return null
        val singers = item.array("singers").orEmpty().mapNotNull { it.obj()?.text("name") }.joinToString("、")
        val artist = singers.takeIf(String::isNotBlank) ?: item.text("singername")?.stripEm()?.takeIf(String::isNotBlank)
        return NetworkSearchAlbum(id, name, artist, item.text("img") ?: item.text("imgurl"), item.int("songcount") ?: 0, item.text("publish_time").orEmpty().substringBefore(' '))
    }

    private fun decodeSearchPlaylist(element: JsonElement): NetworkSearchPlaylist? {
        val item = element.obj() ?: return null
        val id = (item.text("gid") ?: item.text("global_collection_id"))?.takeIf(String::isNotBlank) ?: return null
        val name = item.text("specialname")?.stripEm()?.takeIf(String::isNotBlank) ?: return null
        val creator = (item.text("nickname") ?: item.text("username"))?.stripEm()?.takeIf(String::isNotBlank)
        return NetworkSearchPlaylist(id, name, creator, item.text("img") ?: item.text("imgurl"), item.int("song_count") ?: item.int("songcount") ?: 0, item.long("play_count") ?: 0)
    }

    private fun decodeSearchMv(element: JsonElement): NetworkSearchMv? {
        val item = element.obj() ?: return null
        val hash = (item.text("MvHash") ?: item.text("FileHash"))?.takeIf(String::isNotBlank) ?: return null
        val name = (item.text("MvName") ?: item.text("FileName"))?.stripEm()?.takeIf(String::isNotBlank) ?: return null
        val duration = item.long("Duration") ?: 0
        return NetworkSearchMv(hash, name, item.text("SingerName")?.stripEm()?.takeIf(String::isNotBlank), resolveMvCover(item.text("Pic") ?: item.text("ErectPic")), if (duration > 1_000) duration else duration * 1_000)
    }

    private fun resolveMvCover(raw: String?): String? {
        val value = raw?.takeIf(String::isNotBlank) ?: return null
        return when {
            value.startsWith("http://") || value.startsWith("https://") -> value
            value.matches(Regex("^\\d{8,}\\.[a-zA-Z0-9]+$")) -> "https://imge.kugou.com/mvhdpic/480/${value.take(8)}/$value"
            value.startsWith('/') -> "https://imge.kugou.com$value"
            else -> value
        }
    }

    private suspend fun <T> callApi(block: suspend () -> T): T = calls.execute(block = block)
    private fun String.stripEm(): String = replace(Regex("</?em>", RegexOption.IGNORE_CASE), "")
    private fun JsonElement?.obj(): JsonObject? = this as? JsonObject
    private fun JsonObject.array(name: String): JsonArray? = get(name) as? JsonArray
    private fun JsonObject.text(name: String): String? = (get(name) as? JsonPrimitive)?.contentOrNull
    private fun JsonObject.long(name: String): Long? = text(name)?.toDoubleOrNull()?.toLong()
    private fun JsonObject.int(name: String): Int? = long(name)?.coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong())?.toInt()

    private fun validateSearchRequest(keywords: String, page: Int, pageSize: Int) {
        require(keywords.isNotBlank()) { "keywords must not be blank" }
        require(page > 0) { "page must be positive" }
        require(pageSize in 1..100) { "pageSize must be between 1 and 100" }
    }

    private fun <R, T> requireConsumableItems(rawItems: List<R>, items: List<T>) {
        if (rawItems.isNotEmpty() && items.isEmpty()) throw missingField()
    }

    private fun missingField() = ApiProtocolException(ApiProtocolException.Reason.MissingRequiredField)
    private companion object {
        const val SEARCH_ENDPOINT_ID = "API-SEARCH-001"
    }
}
