package com.resonote.core.data

import com.resonote.core.model.AudioQuality
import com.resonote.core.model.HomeContent
import com.resonote.core.model.HomeIssue
import com.resonote.core.model.HomePlaylist
import com.resonote.core.model.HomeRefreshResult
import com.resonote.core.model.HomeSection
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.RadioRecommendationResult
import com.resonote.core.model.RecommendationMode
import com.resonote.core.network.ApiNetworkDataSource
import com.resonote.core.network.model.NetworkHomePlaylist
import com.resonote.core.network.model.NetworkHomeSong
import com.resonote.core.network.model.NetworkRecommendationMode
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal fun interface HomeRecommendationSampler {
    fun sample(songs: List<OnlineSong>, count: Int): List<OnlineSong>
}

internal class RandomHomeRecommendationSampler @Inject constructor() : HomeRecommendationSampler {
    override fun sample(songs: List<OnlineSong>, count: Int): List<OnlineSong> = songs.shuffled(Random.Default).take(count)
}

@Singleton
internal class DefaultHomeRepository @Inject constructor(
    private val network: ApiNetworkDataSource,
    private val sampler: HomeRecommendationSampler,
) : HomeRepository {
    private val generation = AtomicLong()
    private val stateMutex = Mutex()
    private val mutableContent = MutableStateFlow<HomeContent?>(null)
    override val content: StateFlow<HomeContent?> = mutableContent.asStateFlow()

    override suspend fun refresh(): HomeRefreshResult = coroutineScope {
        val requestGeneration = generation.incrementAndGet()
        val daily = async { loadSection(HomeSection.DailyRecommendations) { network.dailyRecommendations().map(::mapSong) } }
        val playlists =
            async {
                loadSection(HomeSection.RecommendedPlaylists) {
                    network.recommendedPlaylists(page = 1, pageSize = HOME_ITEM_COUNT).map(::mapPlaylist)
                }
            }
        val newSongs =
            async {
                loadSection(HomeSection.NewSongs) {
                    network.newSongs(page = 1, pageSize = HOME_ITEM_COUNT).map(::mapSong).take(HOME_ITEM_COUNT)
                }
            }
        val dailyResult = daily.await()
        val playlistResult = playlists.await()
        val newSongResult = newSongs.await()
        val issues = listOfNotNull(dailyResult.issue, playlistResult.issue, newSongResult.issue)

        stateMutex.withLock {
            if (requestGeneration != generation.get()) {
                return@withLock mutableContent.value?.let { HomeRefreshResult.Updated(it, issues) }
                    ?: HomeRefreshResult.Failed(issues)
            }
            val previous = mutableContent.value
            val anySuccess = dailyResult.value != null || playlistResult.value != null || newSongResult.value != null
            if (!anySuccess) return@withLock HomeRefreshResult.Failed(issues)
            val updated =
                HomeContent(
                    dailyRecommendations =
                        dailyResult.value?.let { sampler.sample(it, HOME_ITEM_COUNT) }
                            ?: previous?.dailyRecommendations.orEmpty(),
                    recommendedPlaylists = playlistResult.value?.take(HOME_ITEM_COUNT) ?: previous?.recommendedPlaylists.orEmpty(),
                    newSongs = newSongResult.value ?: previous?.newSongs.orEmpty(),
                )
            mutableContent.value = updated
            HomeRefreshResult.Updated(updated, issues)
        }
    }

    override suspend fun loadRadio(mode: RecommendationMode): RadioRecommendationResult =
        try {
            val songs = network.radioRecommendations(mode.toNetworkMode()).map(::mapSong)
            RadioRecommendationResult.Available(songs)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Throwable) {
            RadioRecommendationResult.Failed(failure.toContentFailure())
        }

    private suspend fun <T> loadSection(section: HomeSection, block: suspend () -> T): SectionResult<T> =
        try {
            SectionResult(value = block())
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Throwable) {
            SectionResult(issue = HomeIssue(section, failure.toContentFailure()))
        }

    private data class SectionResult<T>(
        val value: T? = null,
        val issue: HomeIssue? = null,
    )

    private companion object {
        const val HOME_ITEM_COUNT = 6
    }
}

private fun NetworkHomeSong.toAudioQuality(): AudioQuality =
    when {
        !losslessHash.isNullOrBlank() -> AudioQuality.Lossless
        !highQualityHash.isNullOrBlank() -> AudioQuality.HighResolution
        else -> AudioQuality.Standard
    }

private fun mapSong(song: NetworkHomeSong): OnlineSong =
    OnlineSong(
        hash = song.hash,
        title = song.title,
        artist = song.artist,
        coverUrl = song.coverUrl?.replace("{size}", "480"),
        albumId = song.albumId,
        albumAudioId = song.albumAudioId,
        durationMillis = song.durationMillis,
        quality = song.toAudioQuality(),
        vip = song.vip,
    )

private fun mapPlaylist(playlist: NetworkHomePlaylist): HomePlaylist =
    HomePlaylist(
        id = playlist.id,
        title = playlist.title,
        coverUrl = playlist.coverUrl?.replace("{size}", "480"),
        playCount = playlist.playCount,
    )

private fun RecommendationMode.toNetworkMode(): NetworkRecommendationMode =
    when (this) {
        RecommendationMode.Personal -> NetworkRecommendationMode.Personal
        RecommendationMode.Nostalgia -> NetworkRecommendationMode.Nostalgia
        RecommendationMode.Popular -> NetworkRecommendationMode.Popular
        RecommendationMode.HiddenGems -> NetworkRecommendationMode.HiddenGems
        RecommendationMode.Vip -> NetworkRecommendationMode.Vip
    }
