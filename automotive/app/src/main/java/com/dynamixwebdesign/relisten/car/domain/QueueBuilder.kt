package com.dynamixwebdesign.relisten.car.domain

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaSession
import com.dynamixwebdesign.relisten.car.api.RelistenRepository
import com.dynamixwebdesign.relisten.car.api.ShowWithSourcesDto
import com.dynamixwebdesign.relisten.car.browse.BrowseTree
import com.dynamixwebdesign.relisten.car.browse.MediaId
import com.dynamixwebdesign.relisten.car.browse.MediaItemFactory

/**
 * Turns tapped browse items into a full ExoPlayer playlist: selecting any track queues the
 * entire recording positioned at that track, so skip/previous cover the whole show.
 */
class QueueBuilder(private val repository: RelistenRepository) {

    suspend fun resolve(
        requested: List<MediaItem>,
        startPositionMs: Long,
    ): MediaSession.MediaItemsWithStartPosition {
        // The car host hands us the single tapped row; expand it to the whole recording.
        val first = requested.firstOrNull()
            ?: throw IllegalArgumentException("no media items requested")
        return when (val id = MediaId.parse(first.mediaId)) {
            is MediaId.Track -> {
                val queue = showQueue(id.showUuid, id.sourceUuid)
                val start = id.index.coerceIn(0, (queue.size - 1).coerceAtLeast(0))
                MediaSession.MediaItemsWithStartPosition(queue, start, startPositionMs)
            }

            is MediaId.Show ->
                MediaSession.MediaItemsWithStartPosition(showQueue(id.showUuid, null), 0, C.TIME_UNSET)

            is MediaId.RandomShow -> {
                val show = repository.randomShow(id.artistUuid)
                MediaSession.MediaItemsWithStartPosition(queueFrom(show, null), 0, C.TIME_UNSET)
            }

            else -> throw IllegalArgumentException("not a playable media id: ${first.mediaId}")
        }
    }

    suspend fun showQueue(showUuid: String, sourceUuid: String?): List<MediaItem> =
        queueFrom(repository.show(showUuid), sourceUuid)

    private suspend fun queueFrom(show: ShowWithSourcesDto, sourceUuid: String?): List<MediaItem> {
        val source = show.sources.firstOrNull { it.uuid == sourceUuid }
            ?: SourcePicker.best(show.sources)
            ?: throw IllegalStateException("no streamable source for show ${show.uuid}")
        val artistName = repository.artistName(show.artist_uuid)
        val showTitle = BrowseTree.showTitle(show)
        return SourcePicker.flatTracks(source).mapIndexed { index, track ->
            MediaItemFactory.playableTrack(
                show.uuid, source.uuid, index, track, artistName, showTitle,
                streamUrl(requireNotNull(track.mp3_url)),
            )
        }
    }

    private fun streamUrl(raw: String): String =
        if (USE_RELISTEN_AUDIO_PROXY) {
            raw.replace("://archive.org/", "://audio.relisten.net/archive.org/")
                .replace("://phish.in/", "://audio.relisten.net/phish.in/")
        } else {
            raw
        }

    private companion object {
        /** Relisten's proxy smooths over archive.org throttling/outages; flip to stream direct. */
        const val USE_RELISTEN_AUDIO_PROXY = true
    }
}
