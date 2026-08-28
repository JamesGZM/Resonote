package com.resonote.core.karaoke.service

import com.resonote.core.karaoke.KaraokeController
import com.resonote.core.karaoke.KaraokeExportController
import com.resonote.core.karaoke.KaraokePreviewController
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class KaraokeServiceModule {
    @Binds
    abstract fun bindKaraokeController(implementation: DefaultKaraokeController): KaraokeController

    @Binds
    abstract fun bindKaraokeExportController(
        implementation: WorkManagerKaraokeExportController,
    ): KaraokeExportController

    @Binds
    abstract fun bindKaraokePreviewController(implementation: Media3KaraokePreviewController): KaraokePreviewController
}
