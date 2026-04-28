package dev.maxxximgb.genesis.data.playback

import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import dev.maxxximgb.genesis.data.media.AlbumArt
import dev.maxxximgb.genesis.domain.model.Track

internal const val EXTRA_MEDIA_STORE_ID = "media_store_id"
internal const val EXTRA_PLAYLIST_ID = "playlist_id"

internal fun Track.toMediaItem(playlistId: Long? = null): MediaItem {
    val extras = Bundle().apply {
        putLong(EXTRA_MEDIA_STORE_ID, mediaStoreId)
        if (playlistId != null) putLong(EXTRA_PLAYLIST_ID, playlistId)
    }
    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artist)
        .setAlbumTitle(album)
        .setArtworkUri(AlbumArt.uri(albumId))
        .setExtras(extras)
        .build()
    return MediaItem.Builder()
        .setMediaId(mediaStoreId.toString())
        .setUri(contentUri)
        .setMediaMetadata(metadata)
        .build()
}

internal fun MediaItem.mediaStoreId(): Long? =
    mediaMetadata.extras?.takeIf { it.containsKey(EXTRA_MEDIA_STORE_ID) }?.getLong(EXTRA_MEDIA_STORE_ID)

internal fun MediaItem.playlistId(): Long? =
    mediaMetadata.extras?.takeIf { it.containsKey(EXTRA_PLAYLIST_ID) }?.getLong(EXTRA_PLAYLIST_ID)
