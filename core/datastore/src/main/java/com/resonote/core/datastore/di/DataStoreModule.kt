package com.resonote.core.datastore.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import com.resonote.core.datastore.AndroidKeystoreSessionCipher
import com.resonote.core.datastore.EncryptedApiSessionSerializer
import com.resonote.core.datastore.EncryptedSessionStorage
import com.resonote.core.datastore.ProtoEncryptedSessionStorage
import com.resonote.core.datastore.SessionCipher
import com.resonote.core.datastore.proto.EncryptedApiSession
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DataStoreModule {
    @Provides
    @Singleton
    fun provideEncryptedSessionDataStore(
        @ApplicationContext context: Context,
    ): DataStore<EncryptedApiSession> =
        DataStoreFactory.create(
            serializer = EncryptedApiSessionSerializer,
            produceFile = { File(context.filesDir, "datastore/api_session.pb") },
        )
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataStoreBindings {
    @Binds
    abstract fun bindEncryptedSessionStorage(implementation: ProtoEncryptedSessionStorage): EncryptedSessionStorage

    @Binds
    abstract fun bindSessionCipher(implementation: AndroidKeystoreSessionCipher): SessionCipher
}
