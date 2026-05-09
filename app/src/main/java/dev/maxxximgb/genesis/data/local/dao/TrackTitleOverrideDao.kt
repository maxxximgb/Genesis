package dev.maxxximgb.genesis.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.maxxximgb.genesis.data.local.entity.TrackTitleOverrideEntity

@Dao
interface TrackTitleOverrideDao {
    /**
     * Non-suspend on purpose: callers (e.g. [dev.maxxximgb.genesis.data.media.MediaStoreSource])
     * already run on Dispatchers.IO and need to inline the lookup into their non-suspend
     * paged-query path. Room enforces no-main-thread access at runtime, so an accidental
     * UI-thread call still fails loudly.
     */
    @Query("SELECT * FROM track_title_overrides WHERE mediaStoreId IN (:ids)")
    fun getByIds(ids: List<Long>): List<TrackTitleOverrideEntity>

    @Query("SELECT title FROM track_title_overrides WHERE mediaStoreId = :id")
    fun getById(id: Long): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(entity: TrackTitleOverrideEntity)

    @Query("DELETE FROM track_title_overrides WHERE mediaStoreId = :id")
    fun delete(id: Long)
}
