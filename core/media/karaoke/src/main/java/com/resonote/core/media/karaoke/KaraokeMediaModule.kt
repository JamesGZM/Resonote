package com.resonote.core.media.karaoke

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class KaraokeMediaModule {
    @Binds
    abstract fun bindKaraokeAssetStore(implementation: AndroidKaraokeAssetStore): KaraokeAssetStore
}
