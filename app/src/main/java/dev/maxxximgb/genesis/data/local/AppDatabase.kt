package dev.maxxximgb.genesis.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.maxxximgb.genesis.data.local.dao.PlaylistDao
import dev.maxxximgb.genesis.data.local.dao.PlaylistTrackDao
import dev.maxxximgb.genesis.data.local.dao.TrackDao
import dev.maxxximgb.genesis.data.local.dao.TrackTitleOverrideDao
import dev.maxxximgb.genesis.data.local.entity.PlaylistEntity
import dev.maxxximgb.genesis.data.local.entity.PlaylistTrackEntity
import dev.maxxximgb.genesis.data.local.entity.TrackEntity
import dev.maxxximgb.genesis.data.local.entity.TrackTitleOverrideEntity

@Database(
    version = 2,
    entities = [
        PlaylistEntity::class,
        TrackEntity::class,
        PlaylistTrackEntity::class,
        TrackTitleOverrideEntity::class,
    ],
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun trackDao(): TrackDao
    abstract fun playlistTrackDao(): PlaylistTrackDao
    abstract fun trackTitleOverrideDao(): TrackTitleOverrideDao

    companion object {
        const val NAME = "genesis.db"

        // 1 → 2: track_title_overrides backs the user-rename feature. We can't write
        // TITLE back to MediaStore reliably on Android 11+, so the user-facing title
        // for a renamed track lives here and is overlaid on top of MediaStore data.
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `track_title_overrides` (" +
                        "`mediaStoreId` INTEGER NOT NULL, " +
                        "`title` TEXT NOT NULL, " +
                        "PRIMARY KEY(`mediaStoreId`))"
                )
            }
        }
    }
}
