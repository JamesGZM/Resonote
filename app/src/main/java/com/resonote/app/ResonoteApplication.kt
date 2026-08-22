package com.resonote.app

import android.app.Application
import coil3.ImageLoader
import coil3.SingletonImageLoader
import com.resonote.core.network.connection.NetworkConnectionRecovery
import dagger.Lazy
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ResonoteApplication :
    Application(),
    SingletonImageLoader.Factory {
    @Inject lateinit var networkConnectionRecovery: NetworkConnectionRecovery

    @Inject lateinit var imageLoader: Lazy<ImageLoader>

    override fun onCreate() {
        super.onCreate()
        networkConnectionRecovery.start()
    }

    override fun newImageLoader(context: coil3.PlatformContext): ImageLoader = imageLoader.get()
}
