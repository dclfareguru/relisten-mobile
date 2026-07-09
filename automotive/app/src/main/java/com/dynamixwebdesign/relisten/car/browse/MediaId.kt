package com.dynamixwebdesign.relisten.car.browse

/**
 * Stable string ids for every node in the browse tree. The car's Media Center hands these
 * back on browse and play requests, so they must be parseable without any session state.
 */
sealed interface MediaId {
    data object Root : MediaId
    data object TabTtb : MediaId
    data object TabArtists : MediaId
    data object TabToday : MediaId
    data class Artist(val artistUuid: String) : MediaId
    data class Year(val artistUuid: String, val yearUuid: String) : MediaId
    data class Show(val showUuid: String) : MediaId
    data class Track(val showUuid: String, val sourceUuid: String, val index: Int) : MediaId
    data class RandomShow(val artistUuid: String) : MediaId
    data class Error(val parentId: String) : MediaId

    fun encode(): String = when (this) {
        Root -> ROOT
        TabTtb -> TAB_TTB
        TabArtists -> TAB_ARTISTS
        TabToday -> TAB_TODAY
        is Artist -> "artist:$artistUuid"
        is Year -> "year:$artistUuid:$yearUuid"
        is Show -> "show:$showUuid"
        is Track -> "track:$showUuid:$sourceUuid:$index"
        is RandomShow -> "random:$artistUuid"
        is Error -> "error:$parentId"
    }

    companion object {
        const val ROOT = "root"
        const val TAB_TTB = "tab_ttb"
        const val TAB_ARTISTS = "tab_artists"
        const val TAB_TODAY = "tab_today"

        /** Tedeschi Trucks Band on api.relisten.net — the app's default landing artist. */
        const val TTB_UUID = "4f222dcb-6fe7-6ab3-5571-0faab0be18c4"

        fun parse(id: String): MediaId? {
            when (id) {
                ROOT -> return Root
                TAB_TTB -> return TabTtb
                TAB_ARTISTS -> return TabArtists
                TAB_TODAY -> return TabToday
            }
            val parts = id.split(":")
            return when (parts[0]) {
                "artist" -> parts.getOrNull(1)?.let { Artist(it) }
                "year" -> if (parts.size == 3) Year(parts[1], parts[2]) else null
                "show" -> parts.getOrNull(1)?.let { Show(it) }
                "track" ->
                    if (parts.size == 4) {
                        parts[3].toIntOrNull()?.let { Track(parts[1], parts[2], it) }
                    } else null
                "random" -> parts.getOrNull(1)?.let { RandomShow(it) }
                "error" -> Error(id.removePrefix("error:"))
                else -> null
            }
        }
    }
}
