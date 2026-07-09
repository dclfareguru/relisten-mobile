package com.dynamixwebdesign.relisten.car.browse

import android.util.Log
import androidx.media3.common.MediaItem
import com.dynamixwebdesign.relisten.car.api.RelistenRepository
import com.dynamixwebdesign.relisten.car.api.ShowWithSourcesDto
import com.dynamixwebdesign.relisten.car.domain.SourcePicker
import kotlinx.coroutines.CancellationException

/**
 * Serves the browse hierarchy the car's Media Center renders:
 * root tabs (TTB / All Artists / On This Day) → years → shows → tracks.
 * Mirrors the iOS CarPlay hierarchy from relisten/carplay/artists.ts.
 */
class BrowseTree(private val repository: RelistenRepository) {

    suspend fun children(parentId: String): List<MediaItem> {
        val id = MediaId.parse(parentId) ?: return emptyList()
        return try {
            when (id) {
                MediaId.Root -> listOf(
                    MediaItemFactory.tabTtb(),
                    MediaItemFactory.tabArtists(),
                    MediaItemFactory.tabToday(),
                )

                MediaId.TabTtb -> artistChildren(MediaId.TTB_UUID)

                MediaId.TabArtists -> repository.artists()
                    .sortedBy { (it.sort_name.ifBlank { it.name }).lowercase() }
                    .map { MediaItemFactory.artist(it) }

                MediaId.TabToday -> repository.today()
                    .sortedByDescending { it.display_date }
                    .map { MediaItemFactory.show(it, titlePrefix = it.artist?.name?.ifBlank { null }) }

                is MediaId.Artist -> artistChildren(id.artistUuid)

                is MediaId.Year -> repository.yearShows(id.artistUuid, id.yearUuid).shows
                    .sortedBy { it.display_date }
                    .map { MediaItemFactory.show(it) }

                is MediaId.Show -> showTracks(id.showUuid)

                else -> emptyList()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "browse children failed for $parentId", e)
            listOf(MediaItemFactory.error(parentId))
        }
    }

    suspend fun item(mediaId: String): MediaItem? = when (MediaId.parse(mediaId)) {
        MediaId.Root -> MediaItemFactory.root()
        MediaId.TabTtb -> MediaItemFactory.tabTtb()
        MediaId.TabArtists -> MediaItemFactory.tabArtists()
        MediaId.TabToday -> MediaItemFactory.tabToday()
        is MediaId.RandomShow -> MediaItemFactory.randomShow(MediaId.TTB_UUID)
        else -> null
    }

    private suspend fun artistChildren(artistUuid: String): List<MediaItem> {
        val years = repository.years(artistUuid).sortedByDescending { it.year }
        return listOf(MediaItemFactory.randomShow(artistUuid)) +
            years.map { MediaItemFactory.year(artistUuid, it) }
    }

    private suspend fun showTracks(showUuid: String): List<MediaItem> {
        val show = repository.show(showUuid)
        val source = SourcePicker.best(show.sources)
            ?: return listOf(MediaItemFactory.error(MediaId.Show(showUuid).encode(), "No streamable recording"))
        val artistName = repository.artistName(show.artist_uuid)
        val showTitle = showTitle(show)
        return SourcePicker.flatTracks(source).mapIndexed { index, track ->
            MediaItemFactory.trackRow(showUuid, source.uuid, index, track, artistName, showTitle)
        }
    }

    companion object {
        private const val TAG = "RelistenCar"

        fun showTitle(show: ShowWithSourcesDto): String = listOfNotNull(
            show.display_date.ifBlank { null },
            show.venue?.name?.ifBlank { null },
        ).joinToString(" • ")
    }
}
