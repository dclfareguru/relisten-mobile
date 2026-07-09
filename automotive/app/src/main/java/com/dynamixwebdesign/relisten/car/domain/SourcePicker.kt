package com.dynamixwebdesign.relisten.car.domain

import com.dynamixwebdesign.relisten.car.api.SourceDto
import com.dynamixwebdesign.relisten.car.api.TrackDto

/**
 * Auto-picks the best recording of a show so nobody chooses tapes while driving.
 * Port of the phone app's `sortSources` ("our magic live music sort",
 * relisten/realm/models/show_repo.ts): soundboard > famous tapers > weighted rating.
 */
object SourcePicker {
    private val FAMOUS_TAPERS = Regex("(charlie miller)|(peter costello)", RegexOption.IGNORE_CASE)

    fun best(sources: List<SourceDto>): SourceDto? = sources
        .filter { source -> source.sets.any { set -> set.tracks.any { it.mp3_url != null } } }
        .sortedWith(
            compareByDescending<SourceDto> { it.is_soundboard }
                .thenByDescending { hasFamousTaper(it) }
                .thenByDescending { it.avg_rating_weighted }
                .thenByDescending { it.num_reviews }
        )
        .firstOrNull()

    /** Flattens a source's sets into one play order: sets by index, tracks by position. */
    fun flatTracks(source: SourceDto): List<TrackDto> = source.sets
        .sortedBy { it.index }
        .flatMap { set -> set.tracks.sortedBy { it.track_position } }
        .filter { it.mp3_url != null }

    private fun hasFamousTaper(source: SourceDto): Boolean =
        FAMOUS_TAPERS.containsMatchIn(
            listOfNotNull(source.taper, source.transferrer, source.source).joinToString(" ")
        )
}
