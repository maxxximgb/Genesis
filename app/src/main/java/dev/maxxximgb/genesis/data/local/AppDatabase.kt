package dev.maxxximgb.genesis.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.maxxximgb.genesis.data.local.dao.PlaylistDao
import dev.maxxximgb.genesis.data.local.dao.PlaylistTrackDao
import dev.maxxximgb.genesis.data.local.dao.TrackDao
import dev.maxxximgb.genesis.data.local.entity.PlaylistEntity
import dev.maxxximgb.genesis.data.local.entity.PlaylistTrackEntity
import dev.maxxximgb.genesis.data.local.entity.TrackEntity

@Database(
    version = 1,
    entities = [
        PlaylistEntity::class,
        TrackEntity::class,
        PlaylistTrackEntity::class,
    ],
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun trackDao(): TrackDao
    abstract fun playlistTrackDao(): PlaylistTrackDao

    companion object {
        const val NAME = "genesis.db"
    }
}
