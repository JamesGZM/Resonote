package com.resonote.core.data.di

import com.resonote.core.data.AuthRepository
import com.resonote.core.data.CloudRepository
import com.resonote.core.data.DefaultCloudRepository
import com.resonote.core.data.ContentCatalogRepository
import com.resonote.core.data.DefaultContentCatalogRepository
import com.resonote.core.data.DefaultSearchRepository
import com.resonote.core.data.DefaultLyricsRepository
import com.resonote.core.data.DefaultVideoRepository
import com.resonote.core.data.DefaultRecognitionRepository
import com.resonote.core.data.SearchRepository
import com.resonote.core.data.SearchHistoryRepository
import com.resonote.core.data.DefaultSearchHistoryRepository
import com.resonote.core.data.LyricsRepository
import com.resonote.core.data.VideoRepository
import com.resonote.core.data.RecognitionRepository
import com.resonote.core.data.VipRewardRepository
import com.resonote.core.data.DefaultVipRewardRepository
import com.resonote.core.data.DefaultAuthRepository
import com.resonote.core.data.DefaultHomeRepository
import com.resonote.core.data.DefaultPlaylistRepository
import com.resonote.core.data.DefaultRankingRepository
import com.resonote.core.data.DefaultRiskVerificationRepository
import com.resonote.core.data.DefaultSongPlaybackRepository
import com.resonote.core.data.EncryptedApiSessionStore
import com.resonote.core.data.HomeRecommendationSampler
import com.resonote.core.data.HomeRepository
import com.resonote.core.data.PlaylistRepository
import com.resonote.core.data.PlaybackPreferencesRepository
import com.resonote.core.data.DefaultPlaybackPreferencesRepository
import com.resonote.core.data.DefaultThemePreferencesRepository
import com.resonote.core.data.RankingRepository
import com.resonote.core.data.RandomHomeRecommendationSampler
import com.resonote.core.data.RiskVerificationRepository
import com.resonote.core.data.SongPlaybackRepository
import com.resonote.core.data.ThemePreferencesRepository
import com.resonote.core.data.DefaultUserProfileRepository
import com.resonote.core.data.DefaultLibraryRepository
import com.resonote.core.data.DefaultLocalMediaRepository
import com.resonote.core.data.UserProfileRepository
import com.resonote.core.data.LibraryRepository
import com.resonote.core.data.LocalMediaRepository
import com.resonote.core.data.DefaultListeningHistoryRepository
import com.resonote.core.data.ListeningHistoryRepository
import com.resonote.core.network.session.ApiSessionStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataBindings {
    @Binds
    abstract fun bindApiSessionStore(implementation: EncryptedApiSessionStore): ApiSessionStore

    @Binds
    abstract fun bindAuthRepository(implementation: DefaultAuthRepository): AuthRepository

    @Binds
    abstract fun bindHomeRepository(implementation: DefaultHomeRepository): HomeRepository

    @Binds
    abstract fun bindRankingRepository(implementation: DefaultRankingRepository): RankingRepository

    @Binds
    abstract fun bindPlaylistRepository(implementation: DefaultPlaylistRepository): PlaylistRepository

    @Binds
    abstract fun bindPlaybackPreferencesRepository(
        implementation: DefaultPlaybackPreferencesRepository,
    ): PlaybackPreferencesRepository

    @Binds
    abstract fun bindThemePreferencesRepository(
        implementation: DefaultThemePreferencesRepository,
    ): ThemePreferencesRepository

    @Binds
    abstract fun bindSongPlaybackRepository(implementation: DefaultSongPlaybackRepository): SongPlaybackRepository

    @Binds
    abstract fun bindUserProfileRepository(implementation: DefaultUserProfileRepository): UserProfileRepository

    @Binds
    abstract fun bindLibraryRepository(implementation: DefaultLibraryRepository): LibraryRepository

    @Binds
    abstract fun bindLocalMediaRepository(implementation: DefaultLocalMediaRepository): LocalMediaRepository

    @Binds
    abstract fun bindListeningHistoryRepository(
        implementation: DefaultListeningHistoryRepository,
    ): ListeningHistoryRepository

    @Binds
    abstract fun bindCloudRepository(implementation: DefaultCloudRepository): CloudRepository

    @Binds
    abstract fun bindContentCatalogRepository(implementation: DefaultContentCatalogRepository): ContentCatalogRepository

    @Binds
    abstract fun bindSearchRepository(implementation: DefaultSearchRepository): SearchRepository

    @Binds
    abstract fun bindSearchHistoryRepository(
        implementation: DefaultSearchHistoryRepository,
    ): SearchHistoryRepository

    @Binds
    abstract fun bindLyricsRepository(implementation: DefaultLyricsRepository): LyricsRepository

    @Binds
    abstract fun bindVideoRepository(implementation: DefaultVideoRepository): VideoRepository

    @Binds
    abstract fun bindRecognitionRepository(implementation: DefaultRecognitionRepository): RecognitionRepository

    @Binds
    abstract fun bindVipRewardRepository(implementation: DefaultVipRewardRepository): VipRewardRepository

    @Binds
    abstract fun bindRiskVerificationRepository(
        implementation: DefaultRiskVerificationRepository,
    ): RiskVerificationRepository

    @Binds
    abstract fun bindHomeRecommendationSampler(implementation: RandomHomeRecommendationSampler): HomeRecommendationSampler
}
