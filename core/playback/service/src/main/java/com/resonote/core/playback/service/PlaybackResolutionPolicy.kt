package com.resonote.core.playback.service

import javax.inject.Singleton

@Singleton
internal fun shouldRetainLoadedMediaWhileResolvingNext(automatic: Boolean, loadedMediaItemCount: Int): Boolean =
    automatic && loadedMediaItemCount > 0
