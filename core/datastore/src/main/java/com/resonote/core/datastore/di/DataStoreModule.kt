package com.resonote.core.datastore.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import com.resonote.core.datastore.AndroidKeystoreSessionCipher
import com.resonote.core.datastore.AppearancePreferencesSerializer
import com.resonote.core.datastore.AppearancePreferencesStorage
import com.resonote.core.datastore.EncryptedApiSessionSerializer
import com.resonote.core.datastore.EncryptedSessionStorage
import com.resonote.core.datastore.HomeSnapshotSerializer
import com.resonote.core.datastore.HomeSnapshotStorage
import com.resonote.core.datastore.LyricsPreferencesSerializer
import com.resonote.core.datastore.LyricsPreferencesStorage
import com.resonote.core.datastore.PlaybackPreferencesSerializer
import com.resonote.core.datastore.PlaybackPreferencesStorage
import com.resonote.core.datastore.PlaybackSessionSnapshotSerializer
import com.resonote.core.datastore.PlaybackSessionSnapshotStorage
import com.resonote.core.datastore.ProtoAppearancePreferencesStorage
import com.resonote.core.datastore.ProtoEncryptedSessionStorage
import com.resonote.core.datastore.ProtoHomeSnapshotStorage
import com.resonote.core.datastore.ProtoLyricsPreferencesStorage
import com.resonote.core.datastore.ProtoPlaybackPreferencesStorage
import com.resonote.core.datastore.ProtoPlaybackSessionSnapshotStorage
import com.resonote.core.datastore.ProtoSearchHistoryStorage
import com.resonote.core.datastore.SearchHistorySerializer
import com.resonote.core.datastore.SearchHistoryStorage
import com.resonote.core.datastore.SessionCipher
import com.resonote.core.datastore.proto.AppearancePreferences
import com.resonote.core.datastore.proto.EncryptedApiSession
import com.resonote.core.datastore.proto.HomeSnapshot
import com.resonote.core.datastore.proto.LyricsPreferences
import com.resonote.core.datastore.proto.PlaybackPreferences
import com.resonote.core.datastore.proto.PlaybackSessionSnapshot
import com.resonote.core.datastore.proto.SearchHistory
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
    fun provideEncryptedSessionDataStore(@ApplicationContext context: Context): DataStore<EncryptedApiSession> =
        DataStoreFactory.create(
            serializer = EncryptedApiSessionSerializer,
            produceFile = { File(context.filesDir, "datastore/api_session.pb") },
        )

    @Provides
    @Singleton
    fun provideSearchHistoryDataStore(@ApplicationContext context: Context): DataStore<SearchHistory> =
        DataStoreFactory.create(
            serializer = SearchHistorySerializer,
            produceFile = { File(context.filesDir, "datastore/search_history.pb") },
        )

    @Provides
    @Singleton
    fun providePlaybackPreferencesDataStore(@ApplicationContext context: Context): DataStore<PlaybackPreferences> =
        DataStoreFactory.create(
            serializer = PlaybackPreferencesSerializer,
            produceFile = { File(context.filesDir, "datastore/playback_preferences.pb") },
        )

    @Provides
    @Singleton
    fun provideLyricsPreferencesDataStore(@ApplicationContext context: Context): DataStore<LyricsPreferences> =
        DataStoreFactory.create(
            serializer = LyricsPreferencesSerializer,
            produceFile = { File(context.filesDir, "datastore/lyrics_preferences.pb") },
        )

    @Provides
    @Singleton
    fun provideHomeSnapshotDataStore(@ApplicationContext context: Context): DataStore<HomeSnapshot> =
        DataStoreFactory.create(
            serializer = HomeSnapshotSerializer,
            produceFile = { File(context.filesDir, "datastore/home_snapshot.pb") },
        )

    @Provides
    @Singleton
    fun providePlaybackSessionSnapshotDataStore(
        @ApplicationContext context: Context,
    ): DataStore<PlaybackSessionSnapshot> = DataStoreFactory.create(
        serializer = PlaybackSessionSnapshotSerializer,
        produceFile = { File(context.filesDir, "datastore/playback_session_snapshot.pb") },
    )

    @Provides
    @Singleton
    fun provideAppearancePreferencesDataStore(@ApplicationContext context: Context): DataStore<AppearancePreferences> =
        DataStoreFactory.create(
            serializer = AppearancePreferencesSerializer,
            produceFile = { File(context.filesDir, "datastore/appearance_preferences.pb") },
        )
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataStoreBindings {
    @Binds
    abstract fun bindHomeSnapshotStorage(implementation: ProtoHomeSnapshotStorage): HomeSnapshotStorage

    @Binds
    abstract fun bindAppearancePreferencesStorage(
        implementation: ProtoAppearancePreferencesStorage,
    ): AppearancePreferencesStorage

    @Binds
    abstract fun bindEncryptedSessionStorage(implementation: ProtoEncryptedSessionStorage): EncryptedSessionStorage

    @Binds
    abstract fun bindPlaybackSessionSnapshotStorage(
        implementation: ProtoPlaybackSessionSnapshotStorage,
    ): PlaybackSessionSnapshotStorage

    @Binds
    abstract fun bindSessionCipher(implementation: AndroidKeystoreSessionCipher): SessionCipher

    @Binds
    abstract fun bindSearchHistoryStorage(implementation: ProtoSearchHistoryStorage): SearchHistoryStorage

    @Binds
    abstract fun bindPlaybackPreferencesStorage(
        implementation: ProtoPlaybackPreferencesStorage,
    ): PlaybackPreferencesStorage

    @Binds
    abstract fun bindLyricsPreferencesStorage(implementation: ProtoLyricsPreferencesStorage): LyricsPreferencesStorage
}
