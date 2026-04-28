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
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME).build()

    @Provides
    fun providePlaylistDao(db: AppDatabase): PlaylistDao = db.playlistDao()

    @Provides
    fun provideTrackDao(db: AppDatabase): TrackDao = db.trackDao()

    @Provides
    fun providePlaylistTrackDao(db: AppDatabase): PlaylistTrackDao = db.playlistTrackDao()
}
