package com.resonote.feature.recognition.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class RecognitionNavKey(val sessionId: Long = 0L) : NavKey
