package dev.maxxximgb.genesis.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.maxxximgb.genesis.data.local.AppDatabase
import dev.maxxximgb.genesis.data.local.dao.PlaylistDao
import dev.maxxximgb.genesis.data.local.dao.PlaylistTrackDao
import dev.maxxximgb.genesis.data.local.dao.TrackDao
import dev.maxxximgb.genesis.data.local.dao.TrackTitleOverrideDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            .addMigrations(AppDatabase.MIGRATION_1_2)
            // Recovery: a transient v2 schema was shipped during 2.3 polish (composer column)
            // before the user rolled it back. If a device still has v2 on disk, Room cannot
            // downgrade automatically; let it wipe and recreate the playlist tables instead
            // of crashing on startup. Tracks are sourced from MediaStore on every launch.
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()

    @Provides
    fun providePlaylistDao(db: AppDatabase): PlaylistDao = db.playlistDao()

    @Provides
    fun provideTrackDao(db: AppDatabase): TrackDao = db.trackDao()

    @Provides
    fun providePlaylistTrackDao(db: AppDatabase): PlaylistTrackDao = db.playlistTrackDao()

    @Provides
    fun provideTrackTitleOverrideDao(db: AppDatabase): TrackTitleOverrideDao =
        db.trackTitleOverrideDao()
}
