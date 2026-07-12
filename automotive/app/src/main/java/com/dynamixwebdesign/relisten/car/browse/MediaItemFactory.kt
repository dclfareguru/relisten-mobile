package com.dynamixwebdesign.relisten.car.browse

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.dynamixwebdesign.relisten.car.api.ArtistDto
import com.dynamixwebdesign.relisten.car.api.ShowDto
import com.dynamixwebdesign.relisten.car.api.TrackDto
import com.dynamixwebdesign.relisten.car.api.YearDto
import java.util.Locale

object MediaItemFactory {

    fun root(): MediaItem =
        browsable(MediaId.ROOT, "Relisten", mediaType = MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)

    fun tabTtb(): MediaItem =
        browsable(MediaId.TAB_TTB, "Tedeschi Trucks Band", mediaType = MediaMetadata.MEDIA_TYPE_ARTIST)

    fun tabArtists(): MediaItem =
        browsable(MediaId.TAB_ARTISTS, "All Artists", mediaType = MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS)

    fun tabToday(): MediaItem =
        browsable(MediaId.TAB_TODAY, "On This Day", mediaType = MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)

    fun artist(a: ArtistDto): MediaItem = browsable(
        MediaId.Artist(a.uuid).encode(),
        a.name,
        subtitle = countsSubtitle(a.show_count, a.source_count),
        mediaType = MediaMetadata.MEDIA_TYPE_ARTIST,
    )

    fun year(artistUuid: String, y: YearDto): MediaItem = browsable(
        MediaId.Year(artistUuid, y.uuid).encode(),
        y.year,
        subtitle = countsSubtitle(y.show_count, y.source_count),
        mediaType = MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS,
    )

    fun show(s: ShowDto, titlePrefix: String? = null): MediaItem {
        val venue = s.venue?.let {
            listOfNotNull(it.name.ifBlank { null }, it.location.ifBlank { null }).joinToString(", ")
        }?.ifBlank { null }
        val rating = if (s.avg_rating > 0.0) {
            String.format(Locale.US, "★ %.1f", s.avg_rating)
        } else null
        val soundboard = if (s.has_soundboard_source) "SBD" else null
        val title = listOfNotNull(titlePrefix, s.display_date).joinToString(" — ")
        return browsable(
            MediaId.Show(s.uuid).encode(),
            title,
            subtitle = listOfNotNull(venue, rating, soundboard).joinToString(" • ").ifBlank { null },
            mediaType = MediaMetadata.MEDIA_TYPE_ALBUM,
        )
    }

    fun randomShow(artistUuid: String): MediaItem = playable(
        MediaId.RandomShow(artistUuid).encode(),
        "Play a random show",
        subtitle = "Surprise me",
    )

    /** A browse-list track row: playable, no URI (resolved at play time by QueueBuilder). */
    fun trackRow(
        showUuid: String,
        sourceUuid: String,
        index: Int,
        track: TrackDto,
        artistName: String?,
        showTitle: String?,
    ): MediaItem = playable(
        MediaId.Track(showUuid, sourceUuid, index).encode(),
        track.title,
        subtitle = track.duration?.let { formatDuration(it) },
        artistName = artistName,
        albumTitle = showTitle,
        trackNumber = index + 1,
        durationMs = track.duration?.let { (it * 1000).toLong() },
    )

    /** The same track carrying its stream URI — what actually goes into the ExoPlayer playlist. */
    fun playableTrack(
        showUuid: String,
        sourceUuid: String,
        index: Int,
        track: TrackDto,
        artistName: String?,
        showTitle: String?,
        streamUrl: String,
    ): MediaItem = playable(
        MediaId.Track(showUuid, sourceUuid, index).encode(),
        track.title,
        subtitle = track.duration?.let { formatDuration(it) },
        artistName = artistName,
        albumTitle = showTitle,
        trackNumber = index + 1,
        durationMs = track.duration?.let { (it * 1000).toLong() },
        uri = Uri.parse(streamUrl),
    )

    /** Rebuilds a playable track from persisted resumption state (no API objects involved). */
    fun persistedTrack(
        mediaId: String,
        title: String,
        artistName: String?,
        albumTitle: String?,
        durationMs: Long?,
        trackNumber: Int?,
        streamUrl: String,
    ): MediaItem = playable(
        mediaId,
        title,
        artistName = artistName,
        albumTitle = albumTitle,
        trackNumber = trackNumber,
        durationMs = durationMs,
        uri = Uri.parse(streamUrl),
    )

    fun error(parentId: String, message: String = "Couldn't load — check the connection"): MediaItem =
        MediaItem.Builder()
            .setMediaId(MediaId.Error(parentId).encode())
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(message)
                    .setIsBrowsable(false)
                    .setIsPlayable(false)
                    .build()
            )
            .build()

    private fun browsable(
        mediaId: String,
        title: String,
        subtitle: String? = null,
        mediaType: Int,
    ): MediaItem = MediaItem.Builder()
        .setMediaId(mediaId)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setIsBrowsable(true)
                .setIsPlayable(false)
                .setMediaType(mediaType)
                .build()
        )
        .build()

    private fun playable(
        mediaId: String,
        title: String,
        subtitle: String? = null,
        artistName: String? = null,
        albumTitle: String? = null,
        trackNumber: Int? = null,
        durationMs: Long? = null,
        uri: Uri? = null,
    ): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setArtist(artistName)
            .setAlbumTitle(albumTitle)
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
        trackNumber?.let { metadata.setTrackNumber(it) }
        durationMs?.let { metadata.setDurationMs(it) }
        val builder = MediaItem.Builder()
            .setMediaId(mediaId)
            .setMediaMetadata(metadata.build())
        uri?.let { builder.setUri(it) }
        return builder.build()
    }

    private fun countsSubtitle(shows: Int, sources: Int): String =
        "$shows shows • $sources tapes"

    private fun formatDuration(seconds: Double): String {
        val total = seconds.toLong()
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) {
            String.format(Locale.US, "%d:%02d:%02d", h, m, s)
        } else {
            String.format(Locale.US, "%d:%02d", m, s)
        }
    }
}
