package com.resonote.core.network.di

import com.resonote.core.network.ApiNetworkDataSource
import com.resonote.core.network.api.MusicApi
import com.resonote.core.network.protocol.ApiDefaultsInterceptor
import com.resonote.core.network.protocol.ApiResponseMetadataInterceptor
import com.resonote.core.network.protocol.ApiSigningInterceptor
import com.resonote.core.network.protocol.AndroidDeviceRegistrationProfileProvider
import com.resonote.core.network.protocol.DeviceRegistrationProfileProvider
import com.resonote.core.network.protocol.ProtocolRandom
import com.resonote.core.network.protocol.ApiEndpointOrigins
import com.resonote.core.network.protocol.ApiOriginPolicy
import com.resonote.core.network.protocol.ProductionApiOriginPolicy
import com.resonote.core.network.protocol.RedactedNetworkLoggingInterceptor
import com.resonote.core.network.retrofit.RealApiNetworkDataSource
import com.resonote.core.network.risk.ApiRiskVerificationService
import com.resonote.core.network.risk.RealApiRiskVerificationService
import com.resonote.core.network.session.ApiSessionStore
import dagger.Binds
import dagger.BindsOptionalOf
import dagger.Lazy
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
    }

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemUTC()

    @Provides
    @Singleton
    fun provideEndpointOrigins(): ApiEndpointOrigins = ApiEndpointOrigins()

    @Provides
    @Singleton
    fun provideOriginPolicy(): ApiOriginPolicy = ProductionApiOriginPolicy()

    @Provides
    @Singleton
    fun provideProtocolRandom(): ProtocolRandom = ProtocolRandom { length ->
        val alphabet = "1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val random = java.security.SecureRandom()
        buildString(length) { repeat(length) { append(alphabet[random.nextInt(alphabet.length)]) } }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        defaultsInterceptor: ApiDefaultsInterceptor,
        signingInterceptor: ApiSigningInterceptor,
        responseMetadataInterceptor: ApiResponseMetadataInterceptor,
        loggingInterceptor: RedactedNetworkLoggingInterceptor,
    ): OkHttpClient =
        OkHttpClient.Builder()
            .retryOnConnectionFailure(false)
            .addInterceptor(defaultsInterceptor)
            .addInterceptor(signingInterceptor)
            .addInterceptor(responseMetadataInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()

    @Provides
    @Singleton
    fun provideCallFactory(client: OkHttpClient): Call.Factory = client

    @Provides
    @Singleton
    fun provideRetrofit(
        callFactory: Lazy<Call.Factory>,
        json: Json,
        origins: ApiEndpointOrigins,
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl(origins.gateway.ensureTrailingSlash())
            .callFactory { request -> callFactory.get().newCall(request) }
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideMusicApi(retrofit: Retrofit): MusicApi = retrofit.create(MusicApi::class.java)

    private fun String.ensureTrailingSlash(): String = if (endsWith('/')) this else "$this/"
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class NetworkBindings {
    @Binds
    abstract fun bindApiNetworkDataSource(implementation: RealApiNetworkDataSource): ApiNetworkDataSource

    @Binds
    abstract fun bindApiRiskVerificationService(
        implementation: RealApiRiskVerificationService,
    ): ApiRiskVerificationService

    @Binds
    abstract fun bindDeviceRegistrationProfileProvider(
        implementation: AndroidDeviceRegistrationProfileProvider,
    ): DeviceRegistrationProfileProvider

    @BindsOptionalOf
    abstract fun optionalSessionStore(): ApiSessionStore
}
