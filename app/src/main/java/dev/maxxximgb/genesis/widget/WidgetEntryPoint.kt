package dev.maxxximgb.genesis.widget

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.maxxximgb.genesis.data.playback.PlaybackStateStore
import dev.maxxximgb.genesis.data.preferences.PlaylistModeStore
import dev.maxxximgb.genesis.data.preferences.WidgetPreferencesStore
import dev.maxxximgb.genesis.domain.playback.PlaybackController
import dev.maxxximgb.genesis.domain.repository.PlaylistRepository
import dev.maxxximgb.genesis.domain.usecase.playback.PlayPlaylistUseCase
import dev.maxxximgb.genesis.domain.usecase.playback.SetPlaylistLoopModeUseCase

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun playbackStateStore(): PlaybackStateStore
    fun playlistModeStore(): PlaylistModeStore
    fun widgetPreferencesStore(): WidgetPreferencesStore
    fun playlistRepository(): PlaylistRepository
    fun playbackController(): PlaybackController
    fun playPlaylistUseCase(): PlayPlaylistUseCase
    fun setPlaylistLoopModeUseCase(): SetPlaylistLoopModeUseCase
    fun widgetUpdater(): WidgetUpdater
    fun widgetArtLoader(): WidgetArtLoader
}
