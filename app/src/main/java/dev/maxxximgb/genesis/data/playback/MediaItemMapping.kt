package dev.maxxximgb.genesis.data.playback

import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import dev.maxxximgb.genesis.data.media.AlbumArt
import dev.maxxximgb.genesis.domain.model.Track

internal const val EXTRA_MEDIA_STORE_ID = "media_store_id"
internal const val EXTRA_PLAYLIST_ID = "playlist_id"
internal const val EXTRA_SOURCE_PLAYLIST_ID = "source_playlist_id"
internal const val EXTRA_SOURCE_ALBUM_ID = "source_album_id"

/**
 * @param playlistId The "owning" playlist for the whole queue context (used for the
 *   resume-bookmark and loop-mode paths). Set when the queue is itself a playlist.
 * @param sourcePlaylistId Per-item: the playlist this track was enqueued FROM (may
 *   differ from [playlistId] when the user appends a different playlist into a queue
 *   that's playing from somewhere else). Drives the queue group-by-source UI.
 * @param sourceAlbumId Per-item: the album this track was enqueued FROM. Same role
 *   as [sourcePlaylistId] but for album sources.
 */
internal fun Track.toMediaItem(
    playlistId: Long? = null,
    sourcePlaylistId: Long? = null,
    sourceAlbumId: Long? = null,
): MediaItem {
    val extras = Bundle().apply {
        putLong(EXTRA_MEDIA_STORE_ID, mediaStoreId)
        if (playlistId != null) putLong(EXTRA_PLAYLIST_ID, playlistId)
        if (sourcePlaylistId != null) putLong(EXTRA_SOURCE_PLAYLIST_ID, sourcePlaylistId)
        if (sourceAlbumId != null) putLong(EXTRA_SOURCE_ALBUM_ID, sourceAlbumId)
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

internal fun MediaItem.sourcePlaylistId(): Long? =
    mediaMetadata.extras?.takeIf { it.containsKey(EXTRA_SOURCE_PLAYLIST_ID) }?.getLong(EXTRA_SOURCE_PLAYLIST_ID)

internal fun MediaItem.sourceAlbumId(): Long? =
    mediaMetadata.extras?.takeIf { it.containsKey(EXTRA_SOURCE_ALBUM_ID) }?.getLong(EXTRA_SOURCE_ALBUM_ID)
