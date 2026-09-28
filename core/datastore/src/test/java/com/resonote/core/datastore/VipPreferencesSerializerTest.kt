package com.resonote.core.datastore

import com.google.common.truth.Truth.assertThat
import com.resonote.core.datastore.proto.VipPreferences
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class VipPreferencesSerializerTest {
    @Test fun defaultIsEnabledAndDisabledSettingSurvivesRoundTrip() = runTest {
        assertThat(VipPreferencesSerializer.readFrom(ByteArrayInputStream(byteArrayOf())).automaticDisabled).isFalse()
        val output = ByteArrayOutputStream()
        VipPreferencesSerializer.writeTo(VipPreferences(true), output)
        assertThat(
            VipPreferencesSerializer.readFrom(ByteArrayInputStream(output.toByteArray())).automaticDisabled,
        ).isTrue()
    }

    @Test fun unknownFieldsAreSkipped() = runTest {
        val data = byteArrayOf(16, 1, 8, 1)
        assertThat(VipPreferencesSerializer.readFrom(ByteArrayInputStream(data)).automaticDisabled).isTrue()
    }
}
