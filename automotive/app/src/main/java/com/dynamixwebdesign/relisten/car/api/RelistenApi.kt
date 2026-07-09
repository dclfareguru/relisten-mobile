package com.dynamixwebdesign.relisten.car.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Thin suspend client for api.relisten.net. Relies on the shared OkHttp disk cache for
 * offline resilience: a network failure falls back to any cached copy, however stale.
 */
class RelistenApi(private val client: OkHttpClient) {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    suspend fun artists(): List<ArtistDto> =
        get("/v3/artists?include_autocreated=false")

    suspend fun years(artistUuid: String): List<YearDto> =
        get("/v3/artists/$artistUuid/years")

    suspend fun yearShows(artistUuid: String, yearUuid: String): YearWithShowsDto =
        get("/v3/artists/$artistUuid/years/$yearUuid")

    suspend fun show(showUuid: String): ShowWithSourcesDto =
        get("/v3/shows/$showUuid")

    suspend fun today(month: Int, day: Int): List<ShowDto> =
        get("/v2/shows/today?month=$month&day=$day")

    private suspend inline fun <reified T> get(path: String): T = withContext(Dispatchers.IO) {
        val url = BASE_URL + path
        val body = try {
            executeForBody(Request.Builder().url(url).build())
        } catch (e: IOException) {
            executeForBody(
                Request.Builder()
                    .url(url)
                    .cacheControl(
                        CacheControl.Builder()
                            .onlyIfCached()
                            .maxStale(365, TimeUnit.DAYS)
                            .build()
                    )
                    .build()
            )
        }
        json.decodeFromString<T>(body)
    }

    private fun executeForBody(request: Request): String {
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code} for ${request.url}")
            return response.body?.string() ?: throw IOException("Empty body for ${request.url}")
        }
    }

    companion object {
        const val BASE_URL = "https://api.relisten.net/api"
    }
}
