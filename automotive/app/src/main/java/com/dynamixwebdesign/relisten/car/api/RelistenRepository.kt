package com.dynamixwebdesign.relisten.car.api

import android.os.SystemClock
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Calendar

/**
 * In-memory TTL cache over [RelistenApi] so re-browsing (and queue building for a show the
 * user just browsed) doesn't refetch. The OkHttp disk cache underneath covers restarts/offline.
 */
class RelistenRepository(private val api: RelistenApi) {
    private val mutex = Mutex()
    private val cache = mutableMapOf<String, Pair<Long, Any>>()

    suspend fun artists(): List<ArtistDto> =
        cached("artists", DAY_MS) { api.artists() }

    suspend fun artistName(artistUuid: String): String? =
        runCatching { artists().firstOrNull { it.uuid == artistUuid }?.name }.getOrNull()

    suspend fun years(artistUuid: String): List<YearDto> =
        cached("years:$artistUuid", DAY_MS) { api.years(artistUuid) }

    suspend fun yearShows(artistUuid: String, yearUuid: String): YearWithShowsDto =
        cached("yearShows:$yearUuid", HOUR_MS) { api.yearShows(artistUuid, yearUuid) }

    suspend fun show(showUuid: String): ShowWithSourcesDto =
        cached("show:$showUuid", HOUR_MS) { api.show(showUuid) }

    suspend fun today(): List<ShowDto> {
        val cal = Calendar.getInstance()
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        return cached("today:$month-$day", HOUR_MS) { api.today(month, day) }
    }

    /** Client-side random pick: random year, then random show, then the full show payload. */
    suspend fun randomShow(artistUuid: String): ShowWithSourcesDto {
        val years = years(artistUuid).filter { it.show_count > 0 }
        check(years.isNotEmpty()) { "artist $artistUuid has no shows" }
        val shows = yearShows(artistUuid, years.random().uuid).shows
        check(shows.isNotEmpty()) { "year has no shows for artist $artistUuid" }
        return show(shows.random().uuid)
    }

    private suspend fun <T : Any> cached(key: String, ttlMs: Long, fetch: suspend () -> T): T {
        mutex.withLock {
            val hit = cache[key]
            if (hit != null && SystemClock.elapsedRealtime() - hit.first < ttlMs) {
                @Suppress("UNCHECKED_CAST")
                return hit.second as T
            }
        }
        val value = fetch()
        mutex.withLock { cache[key] = SystemClock.elapsedRealtime() to value }
        return value
    }

    private companion object {
        const val HOUR_MS = 60L * 60 * 1000
        const val DAY_MS = 24L * HOUR_MS
    }
}
