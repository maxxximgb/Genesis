package dev.maxxximgb.genesis.ui.navigation

object Routes {

    object Library {
        const val path = "library"
        const val ARG_ADD_TO_PLAYLIST_ID = "addToPlaylistId"
        const val pattern = "$path?$ARG_ADD_TO_PLAYLIST_ID={$ARG_ADD_TO_PLAYLIST_ID}"

        fun browse(): String = path
        fun addToPlaylist(playlistId: Long): String = "$path?$ARG_ADD_TO_PLAYLIST_ID=$playlistId"
    }

    object Playlists {
        const val path = "playlists"
    }

    object PlaylistDetail {
        const val ARG_PLAYLIST_ID = "playlistId"
        const val pattern = "playlist/{$ARG_PLAYLIST_ID}"

        fun forId(playlistId: Long): String = "playlist/$playlistId"
    }
}
