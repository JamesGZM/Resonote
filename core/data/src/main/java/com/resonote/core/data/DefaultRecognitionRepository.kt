package com.resonote.core.data

import com.resonote.core.model.RecognitionMatch
import com.resonote.core.network.RecognitionNetworkDataSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultRecognitionRepository @Inject constructor(
    private val network: RecognitionNetworkDataSource,
    private val riskChallenges: RiskChallengeRegistry,
) : RecognitionRepository {
    override suspend fun recognizeAudio(pcm: ByteArray) = loadCollection(riskChallenges) {
        require(pcm.isNotEmpty()) { "pcm must not be empty" }
        network.recognizeAudio(pcm).map { RecognitionMatch(it.confidence, it.song.toOnlineSong()) }
    }
}
