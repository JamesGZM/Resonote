package com.resonote.core.network.protocol

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

internal data class DeviceRegistrationProfile(
    val totalMemoryBytes: Long,
    val availableInternalStorageBytes: Long,
    val availableExternalStorageBytes: Long,
    val brand: String,
    val buildId: String,
    val device: String,
    val manufacturer: String,
)

internal fun interface DeviceRegistrationProfileProvider {
    fun current(): DeviceRegistrationProfile
}

internal class AndroidDeviceRegistrationProfileProvider @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : DeviceRegistrationProfileProvider {
    override fun current(): DeviceRegistrationProfile {
        val memoryInfo = ActivityManager.MemoryInfo()
        (context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)?.getMemoryInfo(memoryInfo)
        return DeviceRegistrationProfile(
            totalMemoryBytes = memoryInfo.totalMem.takeIf { it > 0 } ?: DEFAULT_MEMORY_BYTES,
            availableInternalStorageBytes = DEFAULT_INTERNAL_STORAGE_BYTES,
            availableExternalStorageBytes = DEFAULT_EXTERNAL_STORAGE_BYTES,
            brand = Build.BRAND.nonBlankOrUnknown(),
            buildId = Build.ID.nonBlankOrUnknown(),
            device = sequenceOf(Build.DEVICE, Build.MODEL).firstOrNull { it.isNotBlank() } ?: UNKNOWN,
            manufacturer = Build.MANUFACTURER.nonBlankOrUnknown(),
        )
    }

    private fun String.nonBlankOrUnknown(): String = takeIf(String::isNotBlank) ?: UNKNOWN

    private companion object {
        const val DEFAULT_MEMORY_BYTES = 4_983_533_568L
        const val DEFAULT_INTERNAL_STORAGE_BYTES = 48_114_719L
        const val DEFAULT_EXTERNAL_STORAGE_BYTES = 48_114_717L
        const val UNKNOWN = "unknown"
    }
}
