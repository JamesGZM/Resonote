package com.resonote.core.data

import com.resonote.core.model.HomeContent
import com.resonote.core.model.HomeIssue
import com.resonote.core.model.HomeRefreshResult
import com.resonote.core.model.HomeSection
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.PlaylistSummary
import com.resonote.core.model.RadioRecommendationResult
import com.resonote.core.model.RecommendationMode
import com.resonote.core.network.ApiException
import com.resonote.core.network.CatalogNetworkDataSource
import com.resonote.core.network.HomeNetworkDataSource
import com.resonote.core.network.model.NetworkPlaylistSummary
import com.resonote.core.network.model.NetworkRecommendationMode
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

internal fun interface HomeRecommendationSampler {
    fun sample(songs: List<OnlineSong>, count: Int): List<OnlineSong>
}

internal class RandomHomeRecommendationSampler @Inject constructor() : HomeRecommendationSampler {
    override fun sample(songs: List<OnlineSong>, count: Int): List<OnlineSong> =
        songs.shuffled(Random.Default).take(count)
}

@Singleton
internal class DefaultHomeRepository @Inject constructor(
    private val homeNetwork: HomeNetworkDataSource,
    private val catalogNetwork: CatalogNetworkDataSource,
    private val sampler: HomeRecommendationSampler,
    private val riskChallenges: RiskChallengeRegistry,
) : HomeRepository {
    private val generation = AtomicLong()
    private val stateMutex = Mutex()
    private val mutableContent = MutableStateFlow<HomeContent?>(null)
    override val content: StateFlow<HomeContent?> = mutableContent.asStateFlow()

    override suspend fun refresh(): HomeRefreshResult = coroutineScope {
        val requestGeneration = generation.incrementAndGet()
        val daily =
            async {
                loadSection(HomeSection.DailyRecommendations) {
                    homeNetwork.dailyRecommendations().map { it.toOnlineSong() }
                }
            }
        val playlists =
            async {
                loadSection(HomeSection.RecommendedPlaylists) {
                    catalogNetwork.recommendedPlaylists(page = 1, pageSize = HOME_ITEM_COUNT).map(::mapPlaylist)
                }
            }
        val newSongs =
            async {
                loadSection(HomeSection.NewSongs) {
                    homeNetwork.newSongs(page = 1, pageSize = HOME_ITEM_COUNT).map {
                        it.toOnlineSong()
                    }.take(HOME_ITEM_COUNT)
                }
            }
        val dailyResult = daily.await()
        val playlistResult = playlists.await()
        val newSongResult = newSongs.await()
        val issues = listOfNotNull(dailyResult.issue, playlistResult.issue, newSongResult.issue)

        stateMutex.withLock {
            if (requestGeneration != generation.get()) {
                return@withLock HomeRefreshResult.Superseded
            }
            val previous = mutableContent.value
            val anySuccess = dailyResult.value != null || playlistResult.value != null || newSongResult.value != null
            if (!anySuccess) return@withLock HomeRefreshResult.Failed(issues)
            val updated =
                HomeContent(
                    dailyRecommendations =
                    dailyResult.value?.let { sampler.sample(it, HOME_ITEM_COUNT) }
                        ?: previous?.dailyRecommendations.orEmpty(),
                    recommendedPlaylists =
                    playlistResult.value?.take(HOME_ITEM_COUNT) ?: previous?.recommendedPlaylists.orEmpty(),
                    newSongs = newSongResult.value ?: previous?.newSongs.orEmpty(),
                )
            mutableContent.value = updated
            HomeRefreshResult.Updated(updated, issues)
        }
    }

    override suspend fun loadRadio(mode: RecommendationMode): RadioRecommendationResult = try {
        val songs = homeNetwork.radioRecommendations(mode.toNetworkMode()).map { it.toOnlineSong() }
        RadioRecommendationResult.Available(songs)
    } catch (failure: ApiException) {
        RadioRecommendationResult.Failed(failure.toContentFailure(riskChallenges))
    }

    private suspend fun <T> loadSection(section: HomeSection, block: suspend () -> T): SectionResult<T> = try {
        SectionResult(value = block())
    } catch (failure: ApiException) {
        SectionResult(issue = HomeIssue(section, failure.toContentFailure(riskChallenges)))
    }

    private data class SectionResult<T>(val value: T? = null, val issue: HomeIssue? = null)

    private companion object {
        const val HOME_ITEM_COUNT = 6
    }
}

private fun mapPlaylist(playlist: NetworkPlaylistSummary): PlaylistSummary = PlaylistSummary(
    id = playlist.id,
    title = playlist.title,
    coverUrl = playlist.coverUrl?.replace("{size}", "480"),
    playCount = playlist.playCount,
)

private fun RecommendationMode.toNetworkMode(): NetworkRecommendationMode = when (this) {
    RecommendationMode.Personal -> NetworkRecommendationMode.Personal
    RecommendationMode.Nostalgia -> NetworkRecommendationMode.Nostalgia
    RecommendationMode.Popular -> NetworkRecommendationMode.Popular
    RecommendationMode.HiddenGems -> NetworkRecommendationMode.HiddenGems
    RecommendationMode.Vip -> NetworkRecommendationMode.Vip
}
