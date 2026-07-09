package com.dynamixwebdesign.relisten.car.resumption

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val Context.queueDataStore by preferencesDataStore(name = "queue_state")

/** Snapshot of what's playing, persisted for AAOS playback resumption after reboot. */
@Serializable
data class PersistedQueue(
    val showUuid: String,
    val sourceUuid: String,
    val trackIndex: Int,
    val positionMs: Long,
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
