@file:UnstableApi

package com.dynamixwebdesign.relisten.car.playback

import android.os.Bundle
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaConstants
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import com.dynamixwebdesign.relisten.car.browse.BrowseTree
import com.dynamixwebdesign.relisten.car.browse.MediaId
import com.dynamixwebdesign.relisten.car.browse.MediaItemFactory
import com.dynamixwebdesign.relisten.car.domain.QueueBuilder
import com.dynamixwebdesign.relisten.car.resumption.QueueStateStore
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.guava.future

class RelistenLibrarySessionCallback(
    private val browseTree: BrowseTree,
    private val queueBuilder: QueueBuilder,
    private val queueStateStore: QueueStateStore,
    private val scope: CoroutineScope,
) : MediaLibrarySession.Callback {

    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
    ): MediaSession.ConnectionResult {
        Log.i(TAG, "controller connected package=${controller.packageName}")
        return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
            .setAvailableSessionCommands(
                MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS
            )
            .setAvailablePlayerCommands(MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS)
            .build()
    }

    override fun onGetLibraryRoot(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        params: LibraryParams?,
    ): ListenableFuture<LibraryResult<MediaItem>> {
        val rootParams = LibraryParams.Builder()
            .setExtras(
                Bundle().apply {
                    putInt(
                        MediaConstants.EXTRAS_KEY_CONTENT_STYLE_BROWSABLE,
                        MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM,
                    )
                    putInt(
                        MediaConstants.EXTRAS_KEY_CONTENT_STYLE_PLAYABLE,
                        MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM,
                    )
                }
            )
            .build()
        return scope.future {
            LibraryResult.ofItem(MediaItemFactory.root(), rootParams)
        }
    }

    override fun onGetChildren(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        parentId: String,
        page: Int,
        pageSize: Int,
        params: LibraryParams?,
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = scope.future {
        val children = browseTree.children(parentId)
        val paged = pagedItems(children, page, pageSize)
        Log.i(TAG, "children parent=$parentId total=${children.size} page=$page size=${paged.size}")
        LibraryResult.ofItemList(paged, params)
    }

    override fun onGetItem(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        mediaId: String,
    ): ListenableFuture<LibraryResult<MediaItem>> = scope.future {
        val item = browseTree.item(mediaId)
        if (item != null) {
            LibraryResult.ofItem(item, null)
        } else {
            LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE)
        }
    }

    override fun onSetMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: MutableList<MediaItem>,
        startIndex: Int,
        startPositionMs: Long,
    ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> = scope.future {
        Log.i(TAG, "setMediaItems count=${mediaItems.size} first=${mediaItems.firstOrNull()?.mediaId}")
        if (mediaItems.size == 1) {
            queueBuilder.resolve(mediaItems, startPositionMs)
        } else {
            // A controller set an explicit multi-item playlist: resolve each 1:1.
            val resolved = resolveEach(mediaItems)
            MediaSession.MediaItemsWithStartPosition(
                resolved,
                startIndex.coerceIn(0, (resolved.size - 1).coerceAtLeast(0)),
                startPositionMs,
            )
        }
    }

    override fun onAddMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: MutableList<MediaItem>,
    ): ListenableFuture<MutableList<MediaItem>> = scope.future {
        resolveEach(mediaItems).toMutableList()
    }

    override fun onPlaybackResumption(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        isForPlayback: Boolean,
    ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> = scope.future {
        val saved = queueStateStore.load()
            ?: throw UnsupportedOperationException("nothing to resume")
        Log.i(TAG, "playback resumption show=${saved.showUuid} track=${saved.trackIndex}")
        val queue = queueBuilder.showQueue(saved.showUuid, saved.sourceUuid)
        check(queue.isNotEmpty()) { "resumption queue is empty" }
        MediaSession.MediaItemsWithStartPosition(
            queue,
            saved.trackIndex.coerceIn(0, queue.size - 1),
            saved.positionMs.coerceAtLeast(0),
        )
    }

    private suspend fun resolveEach(items: List<MediaItem>): List<MediaItem> =
        items.mapNotNull { item ->
            when (val id = MediaId.parse(item.mediaId)) {
                is MediaId.Track ->
                    queueBuilder.showQueue(id.showUuid, id.sourceUuid).getOrNull(id.index)
                else -> if (item.localConfiguration != null) item else null
            }
        }

    private fun pagedItems(items: List<MediaItem>, page: Int, pageSize: Int): List<MediaItem> {
        if (page < 0 || pageSize <= 0) return items
        val from = page.toLong() * pageSize
        if (from >= items.size) return emptyList()
        val fromInt = from.toInt()
        return items.subList(fromInt, minOf(fromInt + pageSize, items.size))
    }

    private companion object {
        const val TAG = "RelistenCar"
    }
}
