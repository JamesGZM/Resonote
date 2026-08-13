package com.resonote.core.data

import com.resonote.core.network.AuthNetworkDataSource
import com.resonote.core.network.CatalogNetworkDataSource
import com.resonote.core.network.CloudNetworkDataSource
import com.resonote.core.network.HomeNetworkDataSource
import com.resonote.core.network.LyricsNetworkDataSource
import com.resonote.core.network.VideoNetworkDataSource
import com.resonote.core.network.RecognitionNetworkDataSource
import com.resonote.core.network.LibraryNetworkDataSource
import com.resonote.core.network.PlaybackNetworkDataSource
import com.resonote.core.network.PlaylistNetworkDataSource
import com.resonote.core.network.RankingNetworkDataSource
import com.resonote.core.network.SearchNetworkDataSource
import com.resonote.core.network.UserProfileNetworkDataSource
import com.resonote.core.network.VipNetworkDataSource
import com.resonote.core.network.model.*

/** Test-only convenience base while individual repository fakes migrate to narrow contracts. */
internal abstract class TestApiNetworkDataSource :
    HomeNetworkDataSource,
    CatalogNetworkDataSource,
    RankingNetworkDataSource,
    PlaylistNetworkDataSource,
    PlaybackNetworkDataSource,
    AuthNetworkDataSource,
    UserProfileNetworkDataSource,
    LibraryNetworkDataSource,
    CloudNetworkDataSource,
    SearchNetworkDataSource,
    LyricsNetworkDataSource,
    VideoNetworkDataSource,
    RecognitionNetworkDataSource,
    VipNetworkDataSource {
    protected fun unused(): Nothing = error("Unused test API capability")

    override suspend fun dailyRecommendations(): List<NetworkSong> = unused()
    override suspend fun newSongs(page: Int, pageSize: Int): List<NetworkSong> = unused()
    override suspend fun radioRecommendations(mode: NetworkRecommendationMode): List<NetworkSong> = unused()
    override suspend fun recommendedPlaylists(page: Int, pageSize: Int): List<NetworkPlaylistSummary> = unused()
    override suspend fun categoryPlaylists(categoryId: Int, page: Int, pageSize: Int): List<NetworkPlaylistSummary> = unused()
    override suspend fun banners(): List<NetworkBanner> = unused()
    override suspend fun playlistCategories(): List<NetworkPlaylistCategory> = unused()
    override suspend fun newAlbums(page: Int, pageSize: Int): List<NetworkAlbum> = unused()
    override suspend fun albumSongs(albumId: String, page: Int, pageSize: Int): NetworkAlbumSongPage = unused()
    override suspend fun artistDetail(artistId: String): NetworkArtistInfo? = unused()
    override suspend fun artistSongs(artistId: String, page: Int, pageSize: Int, newestFirst: Boolean): NetworkArtistSongPage = unused()
    override suspend fun rankings(): List<NetworkRanking> = unused()
    override suspend fun rankingSongs(rankId: String, page: Int, pageSize: Int): NetworkSongPage = unused()
    override suspend fun playlistSongs(globalCollectionId: String, page: Int, pageSize: Int): NetworkPlaylistPage = unused()
    override suspend fun resolveSongSource(hash: String, albumId: String?, albumAudioId: String?, requestedQuality: String): NetworkSongSource = unused()
    override suspend fun sendMobileCode(mobile: String): Unit = unused()
    override suspend fun loginWithMobileCode(mobile: String, code: String, selectedUserId: String?): NetworkMobileCodeLoginResult = unused()
    override suspend fun loginWithPassword(username: String, password: String): NetworkPasswordLoginResult = unused()
    override suspend fun createQrLoginKey(): String = unused()
    override suspend fun checkQrLogin(key: String): NetworkQrLoginStatus = unused()
    override suspend fun userDetail(): NetworkUserDetail = unused()
    override suspend fun userVip(): NetworkUserVip = unused()
    override suspend fun userPlaylists(page: Int, pageSize: Int): List<NetworkUserPlaylist> = unused()
    override suspend fun createPlaylist(name: String): String = unused()
    override suspend fun addPlaylistTracks(listId: String, tracks: List<NetworkPlaylistTrackInput>): Unit = unused()
    override suspend fun deletePlaylistTracks(listId: String, fileIds: List<String>): Unit = unused()
    override suspend fun cloudTracks(page: Int, pageSize: Int): NetworkCloudPage = unused()
    override suspend fun resolveCloudSongSource(hash: String, albumAudioId: String?, name: String): NetworkSongSource = unused()
    override suspend fun searchSongs(keywords: String, page: Int, pageSize: Int): NetworkSearchPage = unused()
    override suspend fun searchPlaylists(keywords: String, page: Int, pageSize: Int): NetworkSearchResultPage<NetworkSearchPlaylist> = unused()
    override suspend fun searchAlbums(keywords: String, page: Int, pageSize: Int): NetworkSearchResultPage<NetworkSearchAlbum> = unused()
    override suspend fun searchArtists(keywords: String, page: Int, pageSize: Int): NetworkSearchResultPage<NetworkSearchArtist> = unused()
    override suspend fun searchMvs(keywords: String, page: Int, pageSize: Int): NetworkSearchResultPage<NetworkSearchMv> = unused()
    override suspend fun searchComplex(keywords: String): NetworkComplexSearch = unused()
    override suspend fun hotSearchKeywords(): List<NetworkSearchKeyword> = unused()
    override suspend fun searchSuggestions(keywords: String): List<String> = unused()
    override suspend fun searchLyric(hash: String, albumAudioId: String?): NetworkLyricCandidate? = unused()
    override suspend fun downloadLyric(candidate: NetworkLyricCandidate): String? = unused()
    override suspend fun resolveVideoUrl(hash: String): String? = unused()
    override suspend fun recognizeAudio(pcm: ByteArray): List<NetworkRecognitionMatch> = unused()
    override suspend fun claimDailyVip(receiveDay: String): NetworkVipRewardResult = unused()
    override suspend fun upgradeDailyVip(): NetworkVipRewardResult = unused()
}
