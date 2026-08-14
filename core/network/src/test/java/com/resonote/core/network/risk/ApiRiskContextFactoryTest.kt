package com.resonote.core.network.risk

import com.google.common.truth.Truth.assertThat
import com.resonote.core.network.session.ApiSession
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Base64

class ApiRiskContextFactoryTest {
    private val factory = ApiRiskContextFactory(Clock.fixed(Instant.ofEpochMilli(1_700_000_000_123), ZoneOffset.UTC))
    private val session = ApiSession("guid", "mid", "dev", dfid = "dfid", userId = "42")

    @Test
    fun headerOnlyChallengeGetsPcCompatibleSidAndEdt() {
        val completed = factory.complete(ApiRiskChallenge("event"), session)

        assertThat(Base64.getDecoder().decode(completed.sid)).hasLength(256)
        assertThat(Base64.getDecoder().decode(completed.edt)).isNotEmpty()
    }

    @Test
    fun existingContextIsNotReplaced() {
        val challenge = ApiRiskChallenge("event", sid = "existing-sid", edt = "existing-edt")

        assertThat(factory.complete(challenge, session)).isSameInstanceAs(challenge)
    }
}
