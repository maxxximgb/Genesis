package com.maximg.player.data

import com.maximg.player.media.MediaTrack
import kotlinx.coroutines.flow.Flow

class PlaylistRepository(private val dao: PlaylistDao) {
    fun observePlaylists(): Flow<List<PlaylistEntity>> = dao.observePlaylists()

    fun observePlaylist(id: Long): Flow<PlaylistEntity?> = dao.observePlaylist(id)

    fun observePlaylistTracks(playlistId: Long): Flow<List<PlaylistTrackItem>> =
        dao.observePlaylistTrackItems(playlistId)

    suspend fun getPlaylistTracksOnce(playlistId: Long): List<PlaylistTrackItem> =
        dao.getPlaylistTrackItems(playlistId)

    suspend fun getPlaylistName(playlistId: Long): String? = dao.getPlaylistName(playlistId)

    suspend fun createPlaylist(name: String): Long {
        return dao.insertPlaylist(PlaylistEntity(name = name))
    }

    suspend fun renamePlaylist(id: Long, newName: String) {
        val current = dao.getPlaylist(id) ?: return
        dao.updatePlaylist(current.copy(name = newName))
    }

    suspend fun deletePlaylist(id: Long) {
        dao.deleteTracksForPlaylist(id)
        dao.deletePlaylistById(id)
    }

    suspend fun addTrackToPlaylist(playlistId: Long, track: MediaTrack) {
        addTracksToPlaylist(playlistId, listOf(track))
    }

    suspend fun addTracksToPlaylist(playlistId: Long, tracks: List<MediaTrack>) {
        if (tracks.isEmpty()) return

        tracks.forEach { track ->
            dao.upsertTrack(
                TrackEntity(
                    mediaStoreId = track.mediaStoreId,
                    title = track.title,
                    artist = track.artist,
                    album = track.album,
                    durationMs = track.durationMs,
                    contentUri = track.contentUri.toString()
                )
            )
        }

        val startPosition = (dao.getMaxPosition(playlistId) ?: -1) + 1
        dao.insertPlaylistTracks(
            tracks.mapIndexed { index, track ->
                PlaylistTrackEntity(
                    playlistId = playlistId,
                    mediaStoreId = track.mediaStoreId,
                    position = startPosition + index
                )
            }
        )
    }

    suspend fun removeTrackFromPlaylist(playlistId: Long, mediaStoreId: Long) {
        removeTracksFromPlaylist(playlistId, listOf(mediaStoreId))
    }

    suspend fun removeTracksFromPlaylist(playlistId: Long, mediaIds: List<Long>) {
        if (mediaIds.isEmpty()) return
        dao.removeTracksFromPlaylist(playlistId, mediaIds)
    }

    suspend fun reorderTracks(playlistId: Long, orderedMediaIds: List<Long>) {
        dao.reorderTracks(playlistId, orderedMediaIds)
    }
}
