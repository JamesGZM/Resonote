package com.resonote.core.playback.service

import com.resonote.core.playback.DesktopLyricsController
import com.resonote.core.playback.PlaybackCacheController
import com.resonote.core.playback.PlaybackController
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class PlaybackModule {
    @Binds
    @Singleton
    abstract fun bindDesktopLyricsController(implementation: DefaultDesktopLyricsController): DesktopLyricsController

    @Binds
    abstract fun bindPlaybackCacheController(implementation: DefaultPlaybackCacheController): PlaybackCacheController

    @Binds
    @Singleton
    abstract fun bindPlaybackController(implementation: DefaultPlaybackController): PlaybackController

    @Binds
    @Singleton
    abstract fun bindPlaybackAudioPreloader(implementation: DefaultPlaybackAudioPreloader): PlaybackAudioPreloader
}
