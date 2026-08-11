package com.resonote.core.network.di

import com.resonote.core.network.ApiNetworkDataSource
import com.resonote.core.network.protocol.ProtocolRandom
import com.resonote.core.network.protocol.ApiEndpointOrigins
import com.resonote.core.network.protocol.ApiOriginPolicy
import com.resonote.core.network.protocol.ProductionApiOriginPolicy
import com.resonote.core.network.protocol.RedactedNetworkLoggingInterceptor
import com.resonote.core.network.retrofit.RealApiNetworkDataSource
import com.resonote.core.network.risk.ApiRiskVerifier
import com.resonote.core.network.risk.ApiRiskGateway
import com.resonote.core.network.risk.RealApiRiskGateway
import com.resonote.core.network.session.ApiSessionStore
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
import okhttp3.OkHttpClient

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
    fun provideCallFactory(
        loggingInterceptor: RedactedNetworkLoggingInterceptor,
    ): Call.Factory =
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class NetworkBindings {
    @Binds
    abstract fun bindApiNetworkDataSource(implementation: RealApiNetworkDataSource): ApiNetworkDataSource

    @Binds
    abstract fun bindApiRiskGateway(implementation: RealApiRiskGateway): ApiRiskGateway

    @BindsOptionalOf
    abstract fun optionalRiskVerifier(): ApiRiskVerifier

    @BindsOptionalOf
    abstract fun optionalSessionStore(): ApiSessionStore
}
