package dev.maxxximgb.genesis.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.maxxximgb.genesis.data.playback.PlaybackControllerImpl
import dev.maxxximgb.genesis.domain.playback.PlaybackController
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlaybackProvidersModule {

    @Provides
    @Singleton
    @PlaybackPreferences
    fun providePlaybackDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("playback_state") },
        )

    @Provides
    @Singleton
    @UserPreferences
    fun provideUserPreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("app_prefs") },
        )
}

@Module
@InstallIn(SingletonComponent::class)
abstract class PlaybackBindingsModule {

    @Binds
    @Singleton
    abstract fun bindPlaybackController(impl: PlaybackControllerImpl): PlaybackController
}
