package com.resonote.app

import android.app.Application
import android.util.Log
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.resonote.core.network.connection.NetworkConnectionRecovery
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient
import javax.inject.Inject

@HiltAndroidApp
class ResonoteApplication :
    Application(),
    SingletonImageLoader.Factory {
    @Inject lateinit var networkConnectionRecovery: NetworkConnectionRecovery

    private val imageHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .retryOnConnectionFailure(true)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", IMAGE_USER_AGENT)
                    .build()
                try {
                    chain.proceed(request).also { response ->
                        if (!response.isSuccessful) {
                            Log.w(IMAGE_LOG_TAG, "${request.url.redact()} returned HTTP ${response.code}")
                        }
                    }
                } catch (error: java.io.IOException) {
                    Log.w(IMAGE_LOG_TAG, "${request.url.redact()} failed", error)
                    throw error
                }
            }
            .build()
            .also(networkConnectionRecovery::register)
    }

    override fun onCreate() {
        super.onCreate()
        networkConnectionRecovery.start()
    }

    override fun newImageLoader(context: coil3.PlatformContext): ImageLoader = ImageLoader.Builder(context)
        .components {
            add(OkHttpNetworkFetcherFactory(imageHttpClient))
        }
        .build()

    private companion object {
        const val IMAGE_LOG_TAG = "ResonoteImage"
        const val IMAGE_USER_AGENT = "KuGou/11490 (Android)"
    }
}
