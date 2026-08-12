package com.resonote.core.data

import com.google.common.truth.Truth.assertThat
import com.resonote.core.model.CollectionLoadResult
import com.resonote.core.network.CatalogNetworkDataSource
import com.resonote.core.network.LyricsNetworkDataSource
import com.resonote.core.network.SearchNetworkDataSource
import com.resonote.core.network.model.NetworkAlbum
import com.resonote.core.network.model.NetworkAlbumSongPage
import com.resonote.core.network.model.NetworkArtistInfo
import com.resonote.core.network.model.NetworkArtistSongPage
import com.resonote.core.network.model.NetworkComplexSearch
import com.resonote.core.network.model.NetworkLyricCandidate
import com.resonote.core.network.model.NetworkPlaylistSummary
import com.resonote.core.network.model.NetworkRecognitionMatch
import com.resonote.core.network.model.NetworkSearchAlbum
import com.resonote.core.network.model.NetworkSearchArtist
import com.resonote.core.network.model.NetworkSearchMv
import com.resonote.core.network.model.NetworkSearchPlaylist
import com.resonote.core.network.model.NetworkSearchResultPage
import kotlinx.coroutines.test.runTest
import org.junit.Test

class MobileBusinessVariantsRepositoryTest {
    @Test
    fun categoryPlaylistsForwardsCategoryPagingAndMapsDomain() = runTest {
        val remote = FakeCatalog()
        val repository = DefaultContentCatalogRepository(remote, RiskChallengeRegistry())

        val result = repository.loadCategoryPlaylists(42, page = 2, pageSize = 30) as CollectionLoadResult.Available

        assertThat(remote.request).isEqualTo(Triple(42, 2, 30))
        assertThat(result.value.single().title).isEqualTo("分类歌单")
        assertThat(result.value.single().coverUrl).isEqualTo("https://cover/480")
    }

    @Test
    fun typedSearchRepositoriesPreservePagingAndMobileFields() = runTest {
        val remote = FakeSearchAndMedia()
        val repository = DefaultSearchRepository(remote, RiskChallengeRegistry())

        val playlists = repository.searchPlaylists(" query ", 2, 30) as CollectionLoadResult.Available
        val albums = repository.searchAlbums("query", 1, 30) as CollectionLoadResult.Available
        val artists = repository.searchArtists("query", 1, 30) as CollectionLoadResult.Available
        val mvs = repository.searchMvs("query", 1, 30) as CollectionLoadResult.Available

        assertThat(playlists.value.page).isEqualTo(2)
        assertThat(playlists.value.items.single().coverUrl).isEqualTo("https://playlist/480")
        assertThat(albums.value.items.single().publishDate).isEqualTo("2026-08-12")
        assertThat(artists.value.items.single().songCount).isEqualTo(9)
        assertThat(mvs.value.items.single().durationMillis).isEqualTo(180_000)
        assertThat(remote.keywords).containsExactly("query", "query", "query", "query")
    }

    @Test
    fun lyricRepositoryNormalizesOneTwoAndThreeDigitFractions() = runTest {
        val repository = DefaultLyricsRepository(
            object : LyricsNetworkDataSource {
                override suspend fun searchLyric(hash: String, albumAudioId: String?) =
                    NetworkLyricCandidate("lyric-id", "access-key")

                override suspend fun downloadLyric(candidate: NetworkLyricCandidate) =
                    "[00:01.5]one\n[00:02.50]two\n[00:03.500]three"
            },
            RiskChallengeRegistry(),
        )

        val result = repository.loadLyrics("hash") as CollectionLoadResult.Available

        assertThat(result.value.map { it.timeMillis }).containsExactly(1_500L, 2_500L, 3_500L).inOrder()
    }

    private class FakeCatalog : CatalogNetworkDataSource {
        var request: Triple<Int, Int, Int>? = null
        override suspend fun categoryPlaylists(categoryId: Int, page: Int, pageSize: Int): List<NetworkPlaylistSummary> {
            request = Triple(categoryId, page, pageSize)
            return listOf(NetworkPlaylistSummary("gid", "分类歌单", "https://cover/{size}", 99))
        }
        override suspend fun recommendedPlaylists(page: Int, pageSize: Int) = error("unused")
        override suspend fun banners() = error("unused")
        override suspend fun playlistCategories() = error("unused")
        override suspend fun newAlbums(page: Int, pageSize: Int): List<NetworkAlbum> = error("unused")
        override suspend fun albumSongs(albumId: String, page: Int, pageSize: Int): NetworkAlbumSongPage = error("unused")
        override suspend fun artistDetail(artistId: String): NetworkArtistInfo? = error("unused")
        override suspend fun artistSongs(artistId: String, page: Int, pageSize: Int, newestFirst: Boolean): NetworkArtistSongPage = error("unused")
    }

    private class FakeSearchAndMedia : SearchNetworkDataSource {
        val keywords = mutableListOf<String>()
        override suspend fun searchPlaylists(keywords: String, page: Int, pageSize: Int) =
            NetworkSearchResultPage(listOf(NetworkSearchPlaylist("gid", "歌单", "作者", "https://playlist/{size}", 8, 99)), 61, true).also { this.keywords += keywords }
        override suspend fun searchAlbums(keywords: String, page: Int, pageSize: Int) =
            NetworkSearchResultPage(listOf(NetworkSearchAlbum("album", "专辑", "歌手", null, 7, "2026-08-12")), 1, false).also { this.keywords += keywords }
        override suspend fun searchArtists(keywords: String, page: Int, pageSize: Int) =
            NetworkSearchResultPage(listOf(NetworkSearchArtist("artist", "歌手", null, 3, 9)), 1, false).also { this.keywords += keywords }
        override suspend fun searchMvs(keywords: String, page: Int, pageSize: Int) =
            NetworkSearchResultPage(listOf(NetworkSearchMv("mv", "MV", "歌手", null, 180_000)), 1, false).also { this.keywords += keywords }
        override suspend fun searchSongs(keywords: String, page: Int, pageSize: Int) = error("unused")
        override suspend fun searchComplex(keywords: String): NetworkComplexSearch = error("unused")
        override suspend fun hotSearchKeywords() = error("unused")
        override suspend fun searchSuggestions(keywords: String) = error("unused")
    }
}
