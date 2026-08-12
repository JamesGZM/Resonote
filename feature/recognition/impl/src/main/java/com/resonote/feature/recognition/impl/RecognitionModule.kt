package com.resonote.feature.recognition.impl

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class RecognitionModule {
    @Binds
    @Singleton
    abstract fun bindRecognitionRecorder(implementation: AndroidRecognitionRecorder): RecognitionRecorder
}
