package dev.maxxximgb.genesis.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.maxxximgb.genesis.data.local.entity.TrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(track: TrackEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(tracks: List<TrackEntity>)

    @Query("DELETE FROM tracks WHERE mediaStoreId = :mediaStoreId")
    suspend fun delete(mediaStoreId: Long)

    /** Updates the cached title for a track that was renamed in MediaStore. */
    @Query("UPDATE tracks SET title = :newTitle WHERE mediaStoreId = :mediaStoreId")
    suspend fun updateTitle(mediaStoreId: Long, newTitle: String)

    @Query("SELECT * FROM tracks WHERE mediaStoreId = :mediaStoreId")
    suspend fun findById(mediaStoreId: Long): TrackEntity?

    @Query("SELECT * FROM tracks ORDER BY dateAdded DESC")
    fun observeAll(): Flow<List<TrackEntity>>
}
