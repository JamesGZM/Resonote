package com.resonote.core.network.retrofit

import com.resonote.core.network.RecognitionNetworkDataSource
import com.resonote.core.network.api.MusicApi
import com.resonote.core.network.model.NetworkRecognitionMatch
import com.resonote.core.network.model.NetworkSong
import com.resonote.core.network.protocol.DeviceRegistrationCoordinator
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

@Singleton
internal class RealRecognitionNetworkDataSource @Inject constructor(
    private val musicApi: MusicApi,
    private val registration: DeviceRegistrationCoordinator,
    private val clock: Clock,
    private val responses: ApiResponseVerifier,
    private val calls: ApiCallExecutor,
) : RecognitionNetworkDataSource {
    override suspend fun recognizeAudio(pcm: ByteArray): List<NetworkRecognitionMatch> {
        require(pcm.isNotEmpty()) { "pcm must not be empty" }
        val session = registration.ensureRegisteredSession()
        val response = calls.execute {
            musicApi.recognizeAudio(
                fingerprintId = clock.millis(), userId = session.userId?.toLongOrNull() ?: 0,
                body = pcm.toRequestBody("application/octet-stream".toMediaType()),
            )
        }
        responses.requireNoRiskChallenge(response)
        if (response.status != "1") return emptyList()
        val raw = response.data as? JsonArray ?: return emptyList()
        return raw.mapNotNull { element ->
            val item = element.obj() ?: return@mapNotNull null
            val song = decodeSong(item) ?: return@mapNotNull null
            NetworkRecognitionMatch((1.0 - (item.text("dist")?.toDoubleOrNull() ?: 1.0)).coerceIn(0.0, 1.0), song)
        }.sortedByDescending(NetworkRecognitionMatch::confidence)
    }

    private fun decodeSong(item: JsonObject): NetworkSong? {
        val hash = sequenceOf("hash", "hash_128", "FileHash", "hash_320", "hash_flac").mapNotNull { item.text(it) }.firstOrNull(String::isNotBlank) ?: return null
        val album = item.array("album").orEmpty().firstOrNull().obj()
        val hq = item.text("hash_320")
        val sq = item.text("hash_flac") ?: item.text("hash_high")
        return NetworkSong(
            hash, item.text("songname") ?: item.text("filename") ?: item.text("name") ?: return null,
            item.text("singername") ?: item.text("author_name") ?: item.text("singer"),
            item.text("union_cover") ?: album?.text("sizable_cover") ?: item.text("album_sizable_cover") ?: item.text("cover"),
            null, item.text("album_audio_id"), normalizeDurationMillis(item.long("timelength") ?: item.long("timelength_128") ?: item.long("timelength_320") ?: item.long("duration")),
            hq, sq, false, hq != null, sq != null, album?.text("albumname") ?: item.text("album_name") ?: item.text("albumname"),
        )
    }

    private fun JsonElement?.obj(): JsonObject? = this as? JsonObject
    private fun JsonObject.array(name: String): JsonArray? = get(name) as? JsonArray
    private fun JsonObject.text(name: String): String? = (get(name) as? JsonPrimitive)?.contentOrNull
    private fun JsonObject.long(name: String): Long? = text(name)?.toDoubleOrNull()?.toLong()
}
