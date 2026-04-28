package com.maximg.player.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun observePlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    fun observePlaylist(id: Long): Flow<PlaylistEntity?>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylist(id: Long): PlaylistEntity?

    @Query("SELECT name FROM playlists WHERE id = :id")
    suspend fun getPlaylistName(id: Long): String?

    @Insert
    suspend fun insertPlaylist(entity: PlaylistEntity): Long

    @Update
    suspend fun updatePlaylist(entity: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylistById(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTrack(entity: TrackEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylistTrack(entity: PlaylistTrackEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylistTracks(entities: List<PlaylistTrackEntity>)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun deleteTracksForPlaylist(playlistId: Long)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND mediaStoreId = :mediaStoreId")
    suspend fun removeTrackFromPlaylist(playlistId: Long, mediaStoreId: Long)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND mediaStoreId IN (:mediaIds)")
    suspend fun removeTracksFromPlaylist(playlistId: Long, mediaIds: List<Long>)

    @Query("SELECT MAX(position) FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun getMaxPosition(playlistId: Long): Int?

    @Query(
        """
        SELECT t.mediaStoreId, t.title, t.artist, t.album, t.durationMs, t.contentUri, pt.position
        FROM tracks t
        INNER JOIN playlist_tracks pt ON t.mediaStoreId = pt.mediaStoreId
        WHERE pt.playlistId = :playlistId
        ORDER BY pt.position
        """
    )
    fun observePlaylistTrackItems(playlistId: Long): Flow<List<PlaylistTrackItem>>

    @Query(
        """
        SELECT t.mediaStoreId, t.title, t.artist, t.album, t.durationMs, t.contentUri, pt.position
        FROM tracks t
        INNER JOIN playlist_tracks pt ON t.mediaStoreId = pt.mediaStoreId
        WHERE pt.playlistId = :playlistId
        ORDER BY pt.position
        """
    )
    suspend fun getPlaylistTrackItems(playlistId: Long): List<PlaylistTrackItem>

    @Query("UPDATE playlist_tracks SET position = :position WHERE playlistId = :playlistId AND mediaStoreId = :mediaStoreId")
    suspend fun updateTrackPosition(playlistId: Long, mediaStoreId: Long, position: Int)

    @androidx.room.Transaction
    suspend fun reorderTracks(playlistId: Long, orderedMediaIds: List<Long>) {
        orderedMediaIds.forEachIndexed { index, mediaId ->
            updateTrackPosition(playlistId, mediaId, index)
        }
    }
}
