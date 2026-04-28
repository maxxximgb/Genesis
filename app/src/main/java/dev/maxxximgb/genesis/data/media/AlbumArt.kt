package dev.maxxximgb.genesis.data.media

import android.net.Uri

object AlbumArt {

    private const val BASE = "content://media/external/audio/albumart/"

    fun uri(albumId: Long?): Uri? = albumId?.let { Uri.parse(BASE + it) }
}
