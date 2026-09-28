package com.resonote.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import com.resonote.core.datastore.proto.VipPreferences
import kotlinx.coroutines.flow.map
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

object VipPreferencesSerializer : Serializer<VipPreferences> {
    override val defaultValue = VipPreferences()
    override suspend fun readFrom(input: InputStream) = VipPreferences.parseFrom(input)
    override suspend fun writeTo(t: VipPreferences, output: OutputStream) = t.writeTo(output)
}

interface VipPreferencesStorage {
    val automaticEnabled: kotlinx.coroutines.flow.Flow<Boolean>
    suspend fun setAutomaticEnabled(enabled: Boolean)
}

@Singleton
internal class ProtoVipPreferencesStorage @Inject constructor(private val store: DataStore<VipPreferences>) :
    VipPreferencesStorage {
    override val automaticEnabled = store.data.map { !it.automaticDisabled }
    override suspend fun setAutomaticEnabled(enabled: Boolean) {
        store.updateData { VipPreferences(!enabled) }
    }
}
