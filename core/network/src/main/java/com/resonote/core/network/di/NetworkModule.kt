package com.resonote.core.network.di

import com.resonote.core.network.ApiNetworkDataSource
import com.resonote.core.network.protocol.ApiDeviceIdentity
import com.resonote.core.network.protocol.ApiDeviceIdentityFactory
import com.resonote.core.network.protocol.ApiProtocolConfig
import com.resonote.core.network.protocol.ApiProtocolInterceptor
import com.resonote.core.network.protocol.RedactedNetworkLoggingInterceptor
import com.resonote.core.network.retrofit.RetrofitApiNetworkDataSource
import com.resonote.core.network.risk.ApiRiskVerifier
import dagger.Binds
import dagger.BindsOptionalOf
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@Module
@InstallIn(SingletonComponent::class)
internal object NetworkModule {
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemUTC()

    @Provides
    @Singleton
    fun provideDeviceIdentity(factory: ApiDeviceIdentityFactory): ApiDeviceIdentity = factory.create()

    @Provides
    @Singleton
    fun provideCallFactory(
        protocolInterceptor: ApiProtocolInterceptor,
        loggingInterceptor: RedactedNetworkLoggingInterceptor,
    ): Call.Factory =
        OkHttpClient.Builder()
            .addInterceptor(protocolInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(
        json: Json,
        callFactory: dagger.Lazy<Call.Factory>,
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl(ApiProtocolConfig.BASE_URL)
            .callFactory { request -> callFactory.get().newCall(request) }
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class NetworkBindings {
    @Binds
    abstract fun bindApiNetworkDataSource(implementation: RetrofitApiNetworkDataSource): ApiNetworkDataSource

    @BindsOptionalOf
    abstract fun optionalRiskVerifier(): ApiRiskVerifier
}
