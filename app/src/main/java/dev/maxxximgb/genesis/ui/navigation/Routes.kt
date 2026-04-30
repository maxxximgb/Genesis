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

    object AlbumDetail {
        const val ARG_ALBUM_ID = "albumId"
        const val pattern = "album/{$ARG_ALBUM_ID}"

        fun forId(albumId: Long): String = "album/$albumId"
    }

    object ArtistDetail {
        const val ARG_ARTIST_ID = "artistId"
        const val pattern = "artist/{$ARG_ARTIST_ID}"

        fun forId(artistId: Long): String = "artist/$artistId"
    }

    object FolderDetail {
        const val ARG_BUCKET_ID = "bucketId"
        const val pattern = "folder/{$ARG_BUCKET_ID}"

        fun forId(bucketId: Long): String = "folder/$bucketId"
    }
}
