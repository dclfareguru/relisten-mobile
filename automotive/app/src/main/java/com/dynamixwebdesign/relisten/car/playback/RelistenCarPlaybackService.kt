@file:UnstableApi

package com.dynamixwebdesign.relisten.car.playback

import android.content.Intent
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.dynamixwebdesign.relisten.car.RelistenCarApp
import com.dynamixwebdesign.relisten.car.browse.BrowseTree
import com.dynamixwebdesign.relisten.car.browse.MediaId
import com.dynamixwebdesign.relisten.car.domain.QueueBuilder
import com.dynamixwebdesign.relisten.car.resumption.PersistedQueue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class RelistenCarPlaybackService : MediaLibraryService() {
    private var mediaSession: MediaLibrarySession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        val app = application as RelistenCarApp

        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(OkHttpDataSource.Factory(app.okHttp))
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
        player.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            /* handleAudioFocus = */ true,
        )

        val callback = RelistenLibrarySessionCallback(
            browseTree = BrowseTree(app.repository),
            queueBuilder = QueueBuilder(app.repository),
            queueStateStore = app.queueStateStore,
            scope = serviceScope,
        )
        mediaSession = MediaLibrarySession.Builder(this, player, callback).build()

        player.addListener(object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                if (events.containsAny(
                        Player.EVENT_MEDIA_ITEM_TRANSITION,
                        Player.EVENT_IS_PLAYING_CHANGED,
                        Player.EVENT_TIMELINE_CHANGED,
                    )
                ) {
                    persistQueueState(player)
                }
            }
        })

        // Keep the resumption position fresh while playing.
        serviceScope.launch {
            while (isActive) {
                delay(15_000)
                mediaSession?.player?.let { if (it.isPlaying) persistQueueState(it) }
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        Log.i(TAG, "session requested package=${controllerInfo.packageName}")
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player ?: return
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun persistQueueState(player: Player) {
        val mediaId = player.currentMediaItem?.mediaId ?: return
        val trackId = MediaId.parse(mediaId) as? MediaId.Track ?: return
        val state = PersistedQueue(
            showUuid = trackId.showUuid,
            sourceUuid = trackId.sourceUuid,
            trackIndex = player.currentMediaItemIndex,
            positionMs = player.currentPosition.coerceAtLeast(0),
        )
        val app = application as RelistenCarApp
        serviceScope.launch { app.queueStateStore.save(state) }
    }

    private companion object {
        const val TAG = "RelistenCar"
    }
}
