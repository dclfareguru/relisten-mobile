@file:Suppress("PropertyName")

package com.dynamixwebdesign.relisten.car.api

import kotlinx.serialization.Serializable

// Field names mirror api.relisten.net's snake_case JSON (verified against live responses).

@Serializable
data class ArtistDto(
    val uuid: String,
    val name: String,
    val sort_name: String = "",
    val featured: Int = 0,
    val show_count: Int = 0,
    val source_count: Int = 0,
)

@Serializable
data class YearDto(
    val uuid: String,
    val artist_uuid: String = "",
    val year: String = "",
    val show_count: Int = 0,
    val source_count: Int = 0,
    val avg_rating: Double = 0.0,
)

@Serializable
data class VenueDto(
    val name: String = "",
    val location: String = "",
)

/** Embedded artist on /v2/shows/today responses. */
@Serializable
data class EmbeddedArtistDto(
    val uuid: String = "",
    val name: String = "",
)

@Serializable
data class ShowDto(
    val uuid: String,
    val artist_uuid: String = "",
    val display_date: String = "",
    val avg_rating: Double = 0.0,
    val has_soundboard_source: Boolean = false,
    val source_count: Int = 0,
    val venue: VenueDto? = null,
    val artist: EmbeddedArtistDto? = null,
)

@Serializable
data class YearWithShowsDto(
    val uuid: String,
    val year: String = "",
    val shows: List<ShowDto> = emptyList(),
)

@Serializable
data class ShowWithSourcesDto(
    val uuid: String,
    val artist_uuid: String = "",
    val display_date: String = "",
    val avg_rating: Double = 0.0,
    val venue: VenueDto? = null,
    val sources: List<SourceDto> = emptyList(),
)

@Serializable
data class SourceDto(
    val uuid: String,
    val display_date: String = "",
    val is_soundboard: Boolean = false,
    val avg_rating: Double = 0.0,
    val avg_rating_weighted: Double = 0.0,
    val num_reviews: Int = 0,
    val duration: Double? = null,
    val taper: String? = null,
    val transferrer: String? = null,
    val source: String? = null,
    val sets: List<SourceSetDto> = emptyList(),
)

@Serializable
data class SourceSetDto(
    val uuid: String,
    val index: Int = 0,
    val is_encore: Boolean = false,
    val name: String = "",
    val tracks: List<TrackDto> = emptyList(),
)

@Serializable
data class TrackDto(
    val uuid: String,
    val source_uuid: String = "",
    val track_position: Int = 0,
    val duration: Double? = null,
    val title: String = "",
    val mp3_url: String? = null,
    val flac_url: String? = null,
)
