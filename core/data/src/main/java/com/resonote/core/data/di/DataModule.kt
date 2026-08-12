package com.resonote.core.data.di

import com.resonote.core.data.AuthRepository
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
import com.resonote.core.data.RankingRepository
import com.resonote.core.data.RandomHomeRecommendationSampler
import com.resonote.core.data.RiskVerificationRepository
import com.resonote.core.data.SongPlaybackRepository
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
    abstract fun bindSongPlaybackRepository(implementation: DefaultSongPlaybackRepository): SongPlaybackRepository

    @Binds
    abstract fun bindRiskVerificationRepository(
        implementation: DefaultRiskVerificationRepository,
    ): RiskVerificationRepository

    @Binds
    abstract fun bindHomeRecommendationSampler(implementation: RandomHomeRecommendationSampler): HomeRecommendationSampler
}
