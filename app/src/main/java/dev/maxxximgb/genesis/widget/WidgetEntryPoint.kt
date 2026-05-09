package dev.maxxximgb.genesis.widget

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.maxxximgb.genesis.data.playback.PlaybackStateStore
import dev.maxxximgb.genesis.data.preferences.BookmarkStore
import dev.maxxximgb.genesis.data.preferences.PlaylistModeStore
import dev.maxxximgb.genesis.data.preferences.UserPreferencesStore
import dev.maxxximgb.genesis.data.preferences.WidgetPreferencesStore
import dev.maxxximgb.genesis.domain.playback.PlaybackController
import dev.maxxximgb.genesis.domain.repository.MediaLibraryRepository
import dev.maxxximgb.genesis.domain.repository.PlaylistRepository
import dev.maxxximgb.genesis.domain.usecase.playback.PlayAudiobooksUseCase
import dev.maxxximgb.genesis.domain.usecase.playback.PlayPlaylistUseCase
import dev.maxxximgb.genesis.domain.usecase.playback.SetPlaylistLoopModeUseCase

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun playbackStateStore(): PlaybackStateStore
    fun playlistModeStore(): PlaylistModeStore
    fun bookmarkStore(): BookmarkStore
    fun widgetPreferencesStore(): WidgetPreferencesStore
    fun userPreferencesStore(): UserPreferencesStore
    fun playlistRepository(): PlaylistRepository
    fun mediaLibraryRepository(): MediaLibraryRepository
    fun playbackController(): PlaybackController
    fun playPlaylistUseCase(): PlayPlaylistUseCase
    fun playAudiobooksUseCase(): PlayAudiobooksUseCase
    fun setPlaylistLoopModeUseCase(): SetPlaylistLoopModeUseCase
    fun widgetUpdater(): WidgetUpdater
    fun widgetArtLoader(): WidgetArtLoader
}
