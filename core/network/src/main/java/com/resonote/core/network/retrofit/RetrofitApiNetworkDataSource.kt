package com.resonote.core.network.retrofit

import com.resonote.core.network.ApiHttpException
import com.resonote.core.network.ApiNetworkDataSource
import com.resonote.core.network.ApiNetworkException
import com.resonote.core.network.ApiProtocolException
import com.resonote.core.network.ApiServiceException
import com.resonote.core.network.model.NetworkSearchPage
import com.resonote.core.network.model.NetworkSong
import com.resonote.core.network.risk.RiskAwareApiExecutor
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.longOrNull
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Query

private interface RetrofitApiService {
    @GET("v3/search/song")
    suspend fun searchSongs(
        @Header("x-router") router: String = "complexsearch.kugou.com",
        @Query("albumhide") albumHide: Int = 0,
        @Query("iscorrection") isCorrection: Int = 1,
        @Query("keyword") keyword: String,
        @Query("nocollect") noCollect: Int = 0,
        @Query("page") page: Int,
        @Query("pagesize") pageSize: Int,
        @Query("platform") platform: String = "AndroidFilter",
    ): Response<ResponseBody>

    @Headers("X-Resonote-Bypass-Api-Protocol: true")
    @GET("https://songsearch.kugou.com/song_search_v2")
    suspend fun searchSongsAnonymous(
        @Query("keyword") keyword: String,
        @Query("page") page: Int,
        @Query("pagesize") pageSize: Int,
        @Query("platform") platform: String = "WebFilter",
    ): Response<ResponseBody>
}

@Singleton
internal class RetrofitApiNetworkDataSource @Inject constructor(
    retrofit: Retrofit,
    private val json: Json,
    private val riskExecutor: RiskAwareApiExecutor,
) : ApiNetworkDataSource {
    private val service = retrofit.create(RetrofitApiService::class.java)

    override suspend fun searchSongs(
        keywords: String,
        page: Int,
        pageSize: Int,
    ): NetworkSearchPage = searchSongsAnonymous(keywords, page, pageSize)

    internal suspend fun searchSongsSignedCanary(
        keywords: String,
        page: Int = 1,
        pageSize: Int = 5,
    ): NetworkSearchPage {
        validateSearchRequest(keywords, page, pageSize)
        val response =
            riskExecutor.execute {
                executeCall { service.searchSongs(keyword = keywords.trim(), page = page, pageSize = pageSize) }
            }
        return response.decodeSearchPage()
    }

    private suspend fun searchSongsAnonymous(
        keywords: String,
        page: Int,
        pageSize: Int,
    ): NetworkSearchPage {
        validateSearchRequest(keywords, page, pageSize)
        val response =
            riskExecutor.execute {
                executeCall { service.searchSongsAnonymous(keywords.trim(), page, pageSize) }
            }
        return response.decodeSearchPage()
    }

    private fun validateSearchRequest(
        keywords: String,
        page: Int,
        pageSize: Int,
    ) {
        require(keywords.isNotBlank()) { "keywords must not be blank" }
        require(page > 0) { "page must be positive" }
        require(pageSize in 1..100) { "pageSize must be between 1 and 100" }
    }

    private fun Response<ResponseBody>.toRawResponse(): ApiRawResponse {
        val text = if (isSuccessful) body()?.string() else errorBody()?.string()
        val parsed =
            text?.let { bodyText -> runCatching { json.parseToJsonElement(bodyText) }.getOrNull() }
                as? JsonObject
                ?: throw ApiProtocolException(ApiProtocolException.Reason.MalformedResponse)
        return ApiRawResponse(code(), headers().toMultimap(), parsed)
    }

    private suspend fun executeCall(call: suspend () -> Response<ResponseBody>): ApiRawResponse {
        val response =
            try {
                call()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (timeout: SocketTimeoutException) {
                throw ApiNetworkException(ApiNetworkException.Kind.Timeout, timeout)
            } catch (offline: UnknownHostException) {
                throw ApiNetworkException(ApiNetworkException.Kind.Offline, offline)
            } catch (connection: IOException) {
                throw ApiNetworkException(ApiNetworkException.Kind.Connection, connection)
            }
        return response.toRawResponse()
    }

    private fun ApiRawResponse.decodeSearchPage(): NetworkSearchPage {
        if (statusCode !in 200..299) throw ApiHttpException(statusCode)
        val status = body["status"].intValue()
        val errorCode = body["error_code"].textValue()
        if (status == 0 || errorCode?.toIntOrNull()?.let { it != 0 } == true) throw ApiServiceException(errorCode)
        val data = body["data"] as? JsonObject ?: throw ApiProtocolException(ApiProtocolException.Reason.MissingRequiredField)
        val rawItems = runCatching { data["lists"]?.jsonArray }.getOrNull()
            ?: throw ApiProtocolException(ApiProtocolException.Reason.MissingRequiredField)
        val items = rawItems.mapNotNull { it.toNetworkSongOrNull() }
        if (rawItems.isNotEmpty() && items.isEmpty()) throw ApiProtocolException(ApiProtocolException.Reason.MissingRequiredField)
        val total = data["total"].longValue()?.coerceIn(0, Int.MAX_VALUE.toLong())?.toInt() ?: items.size
        return NetworkSearchPage(items = items, total = total)
    }

    private fun JsonElement.toNetworkSongOrNull(): NetworkSong? =
        runCatching {
            val item = this as? JsonObject ?: return null
            val hash = item.text("FileHash")?.takeIf(String::isNotBlank) ?: return null
            val title = item.text("OriSongName", "SongName", "FileName")?.takeIf(String::isNotBlank) ?: return null
            NetworkSong(
                hash = hash,
                title = title,
                singerName = item.text("SingerName").orEmpty(),
                imageUrl = item.text("Image")?.takeIf(String::isNotBlank),
                durationSeconds = item["Duration"].longValue()?.coerceAtLeast(0) ?: 0,
                highQualityHash = item.text("HQFileHash")?.takeIf(String::isNotBlank),
                losslessHash = item.text("SQFileHash")?.takeIf(String::isNotBlank),
            )
        }.getOrNull()

    private fun JsonObject.text(vararg names: String): String? =
        names.firstNotNullOfOrNull { name -> (get(name) as? JsonPrimitive)?.contentOrNull }

    private fun JsonElement?.textValue(): String? = (this as? JsonPrimitive)?.contentOrNull

    private fun JsonElement?.intValue(): Int? = textValue()?.toIntOrNull()

    private fun JsonElement?.longValue(): Long? = (this as? JsonPrimitive)?.longOrNull
}
