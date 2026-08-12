package com.resonote.core.network.retrofit

import com.resonote.core.network.ApiNetworkDataSource
import com.resonote.core.network.ApiException
import com.resonote.core.network.ApiHttpException
import com.resonote.core.network.ApiNetworkException
import com.resonote.core.network.ApiProtocolException
import com.resonote.core.network.ApiPlaybackUnavailableException
import com.resonote.core.network.ApiRiskException
import com.resonote.core.network.ApiServiceException
import com.resonote.core.network.api.MusicApi
import com.resonote.core.network.api.model.MusicApiResponse
import com.resonote.core.network.api.model.MusicSongDto
import com.resonote.core.network.api.model.NewSongsRequest
import com.resonote.core.network.api.model.NewSongsResponse
import com.resonote.core.network.api.model.PlaylistRecommendationsResponse
import com.resonote.core.network.api.model.PlaylistSongsResponse
import com.resonote.core.network.api.model.RadioRecommendationsRequest
import com.resonote.core.network.api.model.RankingSongsRequest
import com.resonote.core.network.api.model.RankingSongsResponse
import com.resonote.core.network.api.model.RankingsResponse
import com.resonote.core.network.api.model.RecommendedPlaylistsRequest
import com.resonote.core.network.api.model.SearchSongsResponse
import com.resonote.core.network.api.model.SongListResponse
import com.resonote.core.network.api.model.SongSourceResponse
import com.resonote.core.network.api.model.SpecialRecommendRequest
import com.resonote.core.network.protocol.DeviceRegistrationCoordinator
import com.resonote.core.network.protocol.MobileAuthProtocolClient
import com.resonote.core.network.model.NetworkPlaylistSummary
import com.resonote.core.network.model.NetworkMobileCodeLoginResult
import com.resonote.core.network.model.NetworkPlaylistInfo
import com.resonote.core.network.model.NetworkPlaylistPage
import com.resonote.core.network.model.NetworkRecommendationMode
import com.resonote.core.network.model.NetworkRanking
import com.resonote.core.network.model.NetworkSearchPage
import com.resonote.core.network.model.NetworkSong
import com.resonote.core.network.model.NetworkSongPage
import com.resonote.core.network.model.NetworkSongSource
import com.resonote.core.network.protocol.ApiProtocolConfig
import com.resonote.core.network.protocol.ApiRequestSigner
import com.resonote.core.network.risk.ApiRiskChallengeDetector
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import retrofit2.HttpException

@Singleton
internal class RealApiNetworkDataSource @Inject constructor(
    private val musicApi: MusicApi,
    private val registration: DeviceRegistrationCoordinator,
    private val mobileAuth: MobileAuthProtocolClient,
    private val signer: ApiRequestSigner,
    private val clock: Clock,
    private val riskDetector: ApiRiskChallengeDetector,
) : ApiNetworkDataSource {
    override suspend fun dailyRecommendations(): List<NetworkSong> {
        registration.ensureRegisteredSession()
        return decodeSongList(callApi { musicApi.dailyRecommendations() })
    }

    override suspend fun recommendedPlaylists(page: Int, pageSize: Int): List<NetworkPlaylistSummary> {
        validatePage(page, pageSize)
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
                categoryid = 0,
            ),
            requestMultiple = 1,
            returnMinimum = 5,
            returnSpecialFlag = 1,
        )
        return decodePlaylists(callApi { musicApi.recommendedPlaylists(body) })
    }

    override suspend fun newSongs(page: Int, pageSize: Int): List<NetworkSong> {
        validatePage(page, pageSize)
        val session = registration.ensureRegisteredSession()
        val body = NewSongsRequest(
            rankId = 21608,
            userid = session.userId?.toLongOrNull() ?: 0,
            page = page,
            pagesize = pageSize,
            tags = emptyList(),
        )
        return decodeNewSongs(callApi { musicApi.newSongs(body) })
    }

    override suspend fun radioRecommendations(mode: NetworkRecommendationMode): List<NetworkSong> {
        val session = registration.ensureRegisteredSession()
        val nowMillis = clock.millis()
        val body = RadioRecommendationsRequest(
            appid = ApiProtocolConfig.APP_ID.toInt(),
            clientver = ApiProtocolConfig.CLIENT_VERSION.toInt(),
            platform = "android",
            clienttime = nowMillis,
            userid = session.userId?.toLongOrNull() ?: 0,
            key = signer.signParamsKey(nowMillis.toString()),
            fakem = TOP_CARD_FAKEM,
            areaCode = 1,
            mid = session.mid,
            uuid = "-",
            clientPlaylist = emptyList(),
            userInfo = TOP_CARD_USER_INFO,
        )
        return decodeSongList(callApi { musicApi.radioRecommendations(mode.cardId, TOP_CARD_FAKEM, body = body) })
    }

    override suspend fun resolveSongSource(hash: String, albumId: String?, albumAudioId: String?): NetworkSongSource {
        require(hash.isNotBlank()) { "hash must not be blank" }
        val session = registration.ensureRegisteredSession()
        val normalizedHash = hash.trim().lowercase()
        val response = callApi {
            musicApi.songSource(
                albumId = (albumId?.toLongOrNull() ?: 0).toString(),
                hash = normalizedHash,
                albumAudioId = (albumAudioId?.toLongOrNull() ?: 0).toString(),
                key = signer.signSongKey(normalizedHash, session.mid, session.userId),
            )
        }
        return decodeSongSource(response)
    }

    override suspend fun rankings(): List<NetworkRanking> {
        registration.ensureRegisteredSession()
        return decodeRankings(callApi { musicApi.rankings() })
    }

    override suspend fun rankingSongs(rankId: String, page: Int, pageSize: Int): NetworkSongPage {
        require(rankId.isNotBlank()) { "rankId must not be blank" }
        validatePage(page, pageSize)
        registration.ensureRegisteredSession()
        val body = RankingSongsRequest(
            showPortraitMv = 1,
            showTypeTotal = 1,
            filterOriginalRemarks = 1,
            areaCode = 1,
            pagesize = pageSize,
            rankCid = 0,
            type = 1,
            page = page,
            rankId = rankId.trim(),
        )
        return decodeRankingSongs(callApi { musicApi.rankingSongs(body) }, page, pageSize)
    }

    override suspend fun playlistSongs(globalCollectionId: String, page: Int, pageSize: Int): NetworkPlaylistPage {
        require(globalCollectionId.isNotBlank()) { "globalCollectionId must not be blank" }
        validatePage(page, pageSize)
        registration.ensureRegisteredSession()
        val response = callApi {
            musicApi.playlistSongs(
                beginIndex = (page - 1) * pageSize,
                pageSize = pageSize,
                globalCollectionId = globalCollectionId.trim(),
            )
        }
        return decodePlaylistPage(response, globalCollectionId.trim(), page, pageSize)
    }

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

    override suspend fun sendMobileCode(mobile: String) {
        mobileAuth.sendMobileCode(mobile)
    }

    override suspend fun loginWithMobileCode(
        mobile: String,
        code: String,
        selectedUserId: String?,
    ): NetworkMobileCodeLoginResult {
        return mobileAuth.loginWithMobileCode(mobile, code, selectedUserId)
    }

    private suspend fun <T> callApi(block: suspend () -> T): T =
        try {
            block()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (api: ApiException) {
            throw api
        } catch (http: HttpException) {
            throw ApiHttpException(http.code())
        } catch (timeout: SocketTimeoutException) {
            throw ApiNetworkException(ApiNetworkException.Kind.Timeout, timeout)
        } catch (offline: UnknownHostException) {
            throw ApiNetworkException(ApiNetworkException.Kind.Offline, offline)
        } catch (malformed: SerializationException) {
            throw ApiProtocolException(ApiProtocolException.Reason.MalformedResponse)
        } catch (connection: IOException) {
            throw ApiNetworkException(ApiNetworkException.Kind.Connection, connection)
        }

    private fun decodeSearchPage(response: SearchSongsResponse): NetworkSearchPage {
        response.requireSuccess()
        val rawItems = response.data?.songs ?: throw missingField()
        val items = rawItems.mapNotNull { it.toNetworkSongOrNull() }
        requireConsumableItems(rawItems, items)
        val total = response.data.total?.coerceIn(0, Int.MAX_VALUE.toLong())?.toInt() ?: items.size
        return NetworkSearchPage(items, total)
    }

    private fun decodeSongList(response: SongListResponse): List<NetworkSong> {
        response.requireSuccess()
        val rawItems = response.data?.songs ?: throw missingField()
        val items = rawItems.mapNotNull { it.toNetworkSongOrNull() }
        requireConsumableItems(rawItems, items)
        return items
    }

    private fun decodeNewSongs(response: NewSongsResponse): List<NetworkSong> {
        response.requireSuccess()
        val rawItems = response.data ?: throw missingField()
        val items = rawItems.mapNotNull { it.toNetworkSongOrNull() }
        requireConsumableItems(rawItems, items)
        return items
    }

    private fun decodePlaylists(response: PlaylistRecommendationsResponse): List<NetworkPlaylistSummary> {
        response.requireSuccess()
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

    private fun decodeRankings(response: RankingsResponse): List<NetworkRanking> {
        response.requireSuccess()
        val rawItems = response.data?.rankings ?: throw missingField()
        val items = rawItems.mapNotNull { item ->
            val id = (item.rankid ?: item.rankId)?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            val title = item.rankname?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            NetworkRanking(
                id = id,
                title = title,
                coverUrl = (item.imgurl ?: item.image9 ?: item.banner9)?.takeIf(String::isNotBlank),
            )
        }
        requireConsumableItems(rawItems, items)
        return items
    }

    private fun decodeRankingSongs(response: RankingSongsResponse, page: Int, pageSize: Int): NetworkSongPage {
        response.requireSuccess()
        val data = response.data ?: throw missingField()
        val rawItems = data.songs ?: throw missingField()
        val songs = rawItems.mapNotNull { it.toNetworkSongOrNull() }
        requireConsumableItems(rawItems, songs)
        val total =
            sequenceOf(data.total, data.totalCount, response.total)
                .filterNotNull()
                .firstOrNull()
                ?.coerceIn(0, Int.MAX_VALUE.toLong())
                ?.toInt()
                ?.takeIf { it > 0 }
        return NetworkSongPage(
            songs = songs,
            total = total,
            hasMore = songs.size >= pageSize,
        )
    }

    private fun decodePlaylistPage(
        response: PlaylistSongsResponse,
        globalCollectionId: String,
        page: Int,
        pageSize: Int,
    ): NetworkPlaylistPage {
        response.requireSuccess()
        val data = response.data ?: throw missingField()
        val rawItems = data.songs ?: throw missingField()
        val songs = rawItems.mapNotNull { it.toNetworkSongOrNull() }
        requireConsumableItems(rawItems, songs)
        val listInfo = data.info
        val count =
            sequenceOf(data.count, listInfo?.count)
                .filterNotNull()
                .firstOrNull()
                ?.coerceIn(0, Int.MAX_VALUE.toLong())
                ?.toInt()
        val title = listInfo?.name?.takeIf(String::isNotBlank)
        if (listInfo != null && title == null) throw missingField()
        val info =
            if (title != null) {
                NetworkPlaylistInfo(
                    id = globalCollectionId,
                    title = title,
                    description = listInfo.intro.orEmpty(),
                    coverUrl = listInfo.pic?.takeIf(String::isNotBlank),
                    songCount = count ?: 0,
                )
            } else {
                null
            }
        return NetworkPlaylistPage(
            info = info,
            songs = songs,
            hasMore =
                rawItems.size >= pageSize &&
                    (count == null || count <= 0 || page.toLong() * pageSize < count),
        )
    }

    private fun decodeSongSource(response: SongSourceResponse): NetworkSongSource {
        response.requireNoRiskChallenge()
        response.serviceFailureCodeOrNull()?.let { serviceCode ->
            if (serviceCode.trim() == SONG_SOURCE_VIP_REQUIRED_CODE) {
                throw ApiPlaybackUnavailableException(ApiPlaybackUnavailableException.Reason.Vip)
            }
            throw ApiServiceException(serviceCode)
        }
        val status = response.status?.toLongOrNull() ?: throw missingField()
        val rawUrls =
            sequenceOf(response.url, response.backupUrl, response.legacyBackupUrl)
                .flatten()
                .map(String::trim)
                .filter(String::isNotEmpty)
                .toList()
        val parsedUrls = rawUrls.mapNotNull { it.toHttpUrlOrNull() }
        val secureUrl = parsedUrls.firstOrNull { it.isHttps }
        if (rawUrls.isNotEmpty() && secureUrl == null) {
            if (parsedUrls.any { it.scheme == "http" }) {
                throw ApiProtocolException(ApiProtocolException.Reason.InsecureMediaUrl)
            }
            throw malformedResponse()
        }
        if (secureUrl == null) {
            val reason =
                if (status == 3L) {
                    ApiPlaybackUnavailableException.Reason.Copyright
                } else {
                    ApiPlaybackUnavailableException.Reason.Vip
                }
            throw ApiPlaybackUnavailableException(reason)
        }
        return NetworkSongSource(
            uri = secureUrl.toString(),
            durationMillis = normalizeDurationMillis(response.timeLength),
            extension = response.extension?.takeIf(String::isNotBlank),
        )
    }

    private fun MusicSongDto.toNetworkSongOrNull(): NetworkSong? {
        val hash = hash ?: fileHash ?: deprecated?.hash ?: return null
        val filename = filename ?: fileName ?: name.orEmpty()
        val (filenameArtist, filenameTitle) = splitArtistTitle(filename)
        val title = originalAudioName ?: songname ?: originalSongName ?: songName ?: filenameTitle
        if (hash.isBlank() || title.isBlank()) return null
        val highQualityHash = highQualityFileHash ?: this.highQualityHash
        val losslessHash = losslessFileHash ?: sqhash ?: this.losslessHash
        val relatedGoodsCount = relatedGoods.size
        return NetworkSong(
            hash = hash,
            title = title,
            artist = (authorName ?: singerName ?: filenameArtist).takeIf(String::isNotBlank),
            coverUrl = transform?.unionCover ?: sizableCover ?: albumSizableCover ?: image ?: cover,
            albumId = albumId,
            albumAudioId = albumAudioId ?: audioId ?: mixsongid,
            durationMillis = normalizeDurationMillis(timeLength ?: duration ?: deprecated?.duration ?: timelength ?: timelen ?: searchDuration),
            highQualityHash = highQualityHash,
            losslessHash = losslessHash,
            vip = (privilege ?: deprecated?.payType ?: 0) >= 10,
            highQualityAvailable = relatedGoodsCount > 1 || !highQualityHash.isNullOrBlank(),
            losslessAvailable = relatedGoodsCount > 2 || !losslessHash.isNullOrBlank(),
            albumTitle = albuminfo?.name ?: albumName ?: albumname ?: remark,
            fileId = fileid,
        )
    }

    private fun normalizeDurationMillis(value: Long?): Long =
        value?.takeIf { it > 0 }?.let { if (it < 10_000) it * 1_000 else it } ?: 0

    private fun splitArtistTitle(filename: String): Pair<String, String> {
        val separator = filename.indexOf(" - ")
        return if (separator > 0) filename.substring(0, separator) to filename.substring(separator + 3) else "" to filename
    }

    private fun validatePage(page: Int, pageSize: Int) {
        require(page > 0) { "page must be positive" }
        require(pageSize in 1..100) { "pageSize must be between 1 and 100" }
    }

    private fun validateSearchRequest(keywords: String, page: Int, pageSize: Int) {
        require(keywords.isNotBlank()) { "keywords must not be blank" }
        require(page > 0) { "page must be positive" }
        require(pageSize in 1..100) { "pageSize must be between 1 and 100" }
    }

    private fun MusicApiResponse.requireSuccess() {
        requireNoRiskChallenge()
        serviceFailureCodeOrNull()?.let { throw ApiServiceException(it) }
    }

    private fun MusicApiResponse.requireNoRiskChallenge() {
        riskDetector.detect(this)?.let { challenge ->
            throw ApiRiskException(challenge, ApiRiskException.Reason.VerificationUnavailable)
        }
    }

    private fun MusicApiResponse.serviceFailureCodeOrNull(): String? {
        val failedStatus = status?.toDoubleOrNull() == 0.0
        val normalizedCode = errorCode?.trim()?.takeIf(String::isNotEmpty)
        val failedCode = normalizedCode != null && normalizedCode.toDoubleOrNull() != 0.0
        return (errorCode ?: status).takeIf { failedStatus || failedCode }
    }

    private fun <R, T> requireConsumableItems(rawItems: List<R>, items: List<T>) {
        if (rawItems.isNotEmpty() && items.isEmpty()) throw missingField()
    }

    private fun missingField() = ApiProtocolException(ApiProtocolException.Reason.MissingRequiredField)

    private fun malformedResponse() = ApiProtocolException(ApiProtocolException.Reason.MalformedResponse)

    private companion object {
        const val SONG_SOURCE_VIP_REQUIRED_CODE = "35104"
        const val TOP_CARD_FAKEM = "60f7ebf1f812edbac3c63a7310001701760f"
        const val TOP_CARD_USER_INFO = "a0c35cd40af564444b5584c2754dedec"
    }
}
