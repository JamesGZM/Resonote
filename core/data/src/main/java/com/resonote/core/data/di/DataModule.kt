package com.resonote.core.data.di

import com.resonote.core.data.AuthRepository
import com.resonote.core.data.DefaultAuthRepository
import com.resonote.core.data.EncryptedApiSessionStore
import com.resonote.core.network.session.ApiSessionStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataBindings {
    @Binds
    abstract fun bindApiSessionStore(implementation: EncryptedApiSessionStore): ApiSessionStore

    @Binds
    abstract fun bindAuthRepository(implementation: DefaultAuthRepository): AuthRepository
}
