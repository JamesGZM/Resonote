package com.resonote.core.database.di

import android.content.Context
import androidx.room.Room
import com.resonote.core.database.ResonoteDatabase
import com.resonote.core.database.local.LocalMediaDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): ResonoteDatabase = Room.databaseBuilder(
        context,
        ResonoteDatabase::class.java,
        "resonote.db",
    ).build()

    @Provides
    fun provideLocalMediaDao(database: ResonoteDatabase): LocalMediaDao = database.localMediaDao()
}
