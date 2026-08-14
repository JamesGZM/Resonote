package com.resonote.core.network.protocol

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ApiOriginPolicyTest {
    @Test
    fun sessionPropagationOnlyAllowsKugouHostsAndDebugLoopback() {
        assertThat(ApiSessionOriginPolicy.isAllowed("gateway.kugou.com")).isTrue()
        assertThat(ApiSessionOriginPolicy.isAllowed("kugou.com")).isTrue()
        assertThat(ApiSessionOriginPolicy.isAllowed("evil-kugou.com")).isFalse()
        assertThat(ApiSessionOriginPolicy.isAllowed("example.com")).isFalse()
        assertThat(ApiSessionOriginPolicy.isAllowed("localhost")).isTrue()
    }

    private val policy = ProductionApiOriginPolicy()

    @Test
    fun onlyExactMobileCodeEndpointCanUseCleartext() {
        val allowed = spec("http://login.user.kugou.com", ApiCleartextPolicy.LoginMobileCode)
        val wrongPort = spec("http://login.user.kugou.com:8080", ApiCleartextPolicy.LoginMobileCode)
        val subdomain = spec("http://sub.login.user.kugou.com", ApiCleartextPolicy.LoginMobileCode)
        val missingDeclaration = spec("http://login.user.kugou.com", ApiCleartextPolicy.Deny)

        assertThat(policy.isAllowed(allowed)).isTrue()
        assertThat(policy.isAllowed(wrongPort)).isFalse()
        assertThat(policy.isAllowed(subdomain)).isFalse()
        assertThat(policy.isAllowed(missingDeclaration)).isFalse()
        assertThat(policy.isAllowed(spec("https://example.test", ApiCleartextPolicy.Deny))).isTrue()
    }

    private fun spec(origin: String, cleartext: ApiCleartextPolicy) = ApiEndpointSpec(
        origin = origin,
        path = "/test",
        method = ApiHttpMethod.Get,
        cleartextPolicy = cleartext,
    )
}
