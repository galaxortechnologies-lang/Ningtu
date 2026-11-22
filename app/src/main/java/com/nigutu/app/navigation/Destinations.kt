package com.nigutu.app.navigation

/** All navigation routes in the app. */
sealed class Destination(val route: String) {
    data object Home : Destination("home")
    data object Explore : Destination("explore")
    data object Search : Destination("search")
    data object Library : Destination("library")
    data object Settings : Destination("settings")
    data object Player : Destination("player")
    data object Queue : Destination("queue")
    data object Lyrics : Destination("lyrics")

    data object Playlist : Destination("playlist/{playlistId}") {
        const val ARG_PLAYLIST_ID = "playlistId"
        fun create(playlistId: Long): String = "playlist/$playlistId"
    }

    data object Album : Destination("album/{albumName}") {
        const val ARG_ALBUM_NAME = "albumName"
        fun create(albumName: String): String =
            "album/${java.net.URLEncoder.encode(albumName, "UTF-8")}"
    }

    data object Artist : Destination("artist/{artistName}") {
        const val ARG_ARTIST_NAME = "artistName"
        fun create(artistName: String): String =
            "artist/${java.net.URLEncoder.encode(artistName, "UTF-8")}"
    }
}

/** The five primary tabs shown in the bottom bar. */
val PrimaryDestinations = listOf(
    Destination.Home,
    Destination.Explore,
    Destination.Search,
    Destination.Library,
    Destination.Settings,
)
