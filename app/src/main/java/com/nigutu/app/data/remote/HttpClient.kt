package com.nigutu.app.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/**
 * Small JSON-over-HTTP helper shared by the online providers.
 *
 * Centralising timeouts, retries and error mapping keeps provider code focused
 * on their own payloads. Retries happen in [fetch] so the inline decoder stays
 * a single, non-recursive call site.
 */
class JsonHttpClient(
    @PublishedApi internal val client: OkHttpClient,
    @PublishedApi internal val json: Json,
) {
    /** GETs [url] and decodes the JSON body into [T]. */
    suspend inline fun <reified T> get(url: HttpUrl): Result<T> =
        fetch(url).mapCatching { body -> json.decodeFromString<T>(body) }

    /** Performs the request with one retry on a transport failure. */
    suspend fun fetch(url: HttpUrl): Result<String> = withContext(Dispatchers.IO) {
        var lastError: Throwable? = null
        repeat(MAX_ATTEMPTS) { attempt ->
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("Accept", "application/json")
                    .build()
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        throw IOException("HTTP ${response.code} from ${url.host}")
                    }
                    return@withContext Result.success(body)
                }
            } catch (io: IOException) {
                lastError = io
                if (attempt < MAX_ATTEMPTS - 1) {
                    kotlinx.coroutines.delay(RETRY_DELAY_MS)
                }
            } catch (t: Throwable) {
                return@withContext Result.failure(t)
            }
        }
        Result.failure(lastError ?: IOException("Request to ${url.host} failed"))
    }

    /** Builds a URL with query parameters, skipping null values. */
    fun url(base: String, params: Map<String, String?> = emptyMap()): HttpUrl {
        val builder = base.toHttpUrl().newBuilder()
        params.forEach { (key, value) -> if (value != null) builder.addQueryParameter(key, value) }
        return builder.build()
    }

    private companion object {
        const val MAX_ATTEMPTS = 2
        const val RETRY_DELAY_MS = 400L
    }
}
