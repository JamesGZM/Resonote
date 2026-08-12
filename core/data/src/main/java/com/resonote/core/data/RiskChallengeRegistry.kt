package com.resonote.core.data

import com.resonote.core.model.RiskChallengeHandle
import com.resonote.core.network.risk.ApiRiskChallenge
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class RiskChallengeRegistry @Inject constructor() {
    private val lock = Any()
    private val challenges = linkedMapOf<String, ApiRiskChallenge>()

    fun register(challenge: ApiRiskChallenge): RiskChallengeHandle {
        val handle = RiskChallengeHandle(UUID.randomUUID().toString())
        synchronized(lock) {
            if (challenges.size >= MAX_CHALLENGES) challenges.remove(challenges.keys.first())
            challenges[handle.value] = challenge
        }
        return handle
    }

    fun find(handle: RiskChallengeHandle): ApiRiskChallenge? = synchronized(lock) { challenges[handle.value] }

    fun remove(handle: RiskChallengeHandle) {
        synchronized(lock) { challenges.remove(handle.value) }
    }

    private companion object {
        const val MAX_CHALLENGES = 32
    }
}
