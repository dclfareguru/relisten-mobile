package com.dynamixwebdesign.relisten.car.resumption

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val Context.queueDataStore by preferencesDataStore(name = "queue_state")

/**
 * Full snapshot of the playing queue, persisted for AAOS playback resumption after reboot.
 * Carries everything needed to rebuild the ExoPlayer playlist with ZERO network access —
 * the car often wakes up without connectivity, and resumption must not depend on the API.
 */
@Serializable
data class PersistedTrack(
    val mediaId: String,
    val title: String,
    val artist: String? = null,
    val albumTitle: String? = null,
    val durationMs: Long? = null,
    val trackNumber: Int? = null,
    val url: String,
)

@Serializable
data class PersistedQueue(
    val showUuid: String,
    val sourceUuid: String,
    val trackIndex: Int,
    val positionMs: Long,
    val tracks: List<PersistedTrack> = emptyList(),
)

class QueueStateStore(private val context: Context) {
    private val key = stringPreferencesKey("persisted_queue")
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun save(state: PersistedQueue) {
        context.queueDataStore.edit { prefs ->
            prefs[key] = json.encodeToString(PersistedQueue.serializer(), state)
        }
    }

    suspend fun load(): PersistedQueue? =
        context.queueDataStore.data.first()[key]?.let { raw ->
            runCatching { json.decodeFromString(PersistedQueue.serializer(), raw) }.getOrNull()
        }
}
