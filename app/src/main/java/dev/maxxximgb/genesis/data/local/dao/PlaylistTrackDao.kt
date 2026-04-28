package dev.maxxximgb.genesis.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import dev.maxxximgb.genesis.data.local.entity.PlaylistTrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class PlaylistTrackDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertIgnore(row: PlaylistTrackEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertIgnore(rows: List<PlaylistTrackEntity>): List<Long>

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND mediaStoreId = :mediaStoreId")
    abstract suspend fun delete(playlistId: Long, mediaStoreId: Long)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND mediaStoreId IN (:mediaStoreIds)")
    abstract suspend fun deleteAll(playlistId: Long, mediaStoreIds: List<Long>)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId")
    abstract suspend fun deleteByPlaylist(playlistId: Long)

    @Query("SELECT * FROM playlist_tracks WHERE playlistId = :playlistId ORDER BY position ASC")
    abstract fun observeForPlaylist(playlistId: Long): Flow<List<PlaylistTrackEntity>>

    @Query(
        """
        SELECT t.* FROM tracks t
        INNER JOIN playlist_tracks pt ON pt.mediaStoreId = t.mediaStoreId
        WHERE pt.playlistId = :playlistId
        ORDER BY pt.position ASC
        """
    )
    abstract fun observeTracksForPlaylist(
        playlistId: Long,
    ): Flow<List<dev.maxxximgb.genesis.data.local.entity.TrackEntity>>

    @Query(
        """
        SELECT t.* FROM tracks t
        INNER JOIN playlist_tracks pt ON pt.mediaStoreId = t.mediaStoreId
        WHERE pt.playlistId = :playlistId
        ORDER BY pt.position ASC
        """
    )
    abstract suspend fun getTracksForPlaylist(
        playlistId: Long,
    ): List<dev.maxxximgb.genesis.data.local.entity.TrackEntity>

    @Query("SELECT COUNT(*) FROM playlist_tracks WHERE playlistId = :playlistId")
    abstract suspend fun countForPlaylist(playlistId: Long): Int

    @Query("SELECT MAX(position) FROM playlist_tracks WHERE playlistId = :playlistId")
    abstract suspend fun maxPositionForPlaylist(playlistId: Long): Int?

    /**
     * Reorders join rows to match the given sequence of media ids using a single
     * CASE-based UPDATE inside a transaction. Long values are typed, so direct
     * interpolation into SQL is safe (no injection surface).
     */
    @Transaction
    open suspend fun reorder(playlistId: Long, orderedMediaIds: List<Long>) {
        if (orderedMediaIds.isEmpty()) return
        val sb = StringBuilder("UPDATE playlist_tracks SET position = CASE mediaStoreId ")
        orderedMediaIds.forEachIndexed { idx, id ->
            sb.append("WHEN ").append(id).append(" THEN ").append(idx).append(' ')
        }
        sb.append("ELSE position END WHERE playlistId = ").append(playlistId)
        sb.append(" AND mediaStoreId IN (")
        orderedMediaIds.forEachIndexed { idx, id ->
            if (idx > 0) sb.append(',')
            sb.append(id)
        }
        sb.append(')')
        reorderRaw(SimpleSQLiteQuery(sb.toString()))
    }

    @RawQuery
    abstract suspend fun reorderRaw(query: SupportSQLiteQuery): Int
}
