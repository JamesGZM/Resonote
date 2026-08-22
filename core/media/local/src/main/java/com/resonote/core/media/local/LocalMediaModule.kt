package com.resonote.core.media.local

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class LocalMediaModule {
    @Binds
    @Singleton
    abstract fun bindLocalMediaStore(implementation: AndroidLocalMediaStore): LocalMediaStore

    @Binds
    @Singleton
    abstract fun bindLocalMediaTreeSource(implementation: DocumentsLocalMediaTreeSource): LocalMediaTreeSource
}
