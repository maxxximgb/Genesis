package dev.maxxximgb.genesis.domain.repository

import dev.maxxximgb.genesis.domain.model.Playlist
import dev.maxxximgb.genesis.domain.model.Track
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {

    fun observePlaylists(): Flow<List<Playlist>>

    fun observePlaylist(id: Long): Flow<Playlist?>

    fun observePlaylistTracks(playlistId: Long): Flow<List<Track>>

    suspend fun getPlaylistName(id: Long): String?

    suspend fun getTracksForPlaylist(playlistId: Long): List<Track>

    suspend fun create(name: String): Long

    suspend fun rename(id: Long, name: String)

    suspend fun delete(id: Long)

    suspend fun addTracks(playlistId: Long, tracks: List<Track>)

    suspend fun removeTracks(playlistId: Long, mediaStoreIds: List<Long>)

    suspend fun reorder(playlistId: Long, orderedMediaIds: List<Long>)
}
