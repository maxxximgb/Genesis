package com.maximg.player.playback

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.maximg.player.util.playbackDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PlaybackStateStore(private val context: Context) {
    private val isPlayingKey = booleanPreferencesKey("is_playing")
    private val titleKey = stringPreferencesKey("title")
    private val artistKey = stringPreferencesKey("artist")
    private val shuffleKey = booleanPreferencesKey("shuffle")
    private val repeatKey = intPreferencesKey("repeat")
    private val canPreviousKey = booleanPreferencesKey("can_previous")
    private val canNextKey = booleanPreferencesKey("can_next")
    private val playlistIdKey = longPreferencesKey("playlist_id")

    val stateFlow: Flow<PlaybackState> = context.playbackDataStore.data.map { prefs ->
        PlaybackState(
            isPlaying = prefs[isPlayingKey] ?: false,
            title = prefs[titleKey],
            artist = prefs[artistKey],
            shuffle = prefs[shuffleKey] ?: false,
            repeatMode = prefs[repeatKey] ?: 0,
            canGoPrevious = prefs[canPreviousKey] ?: false,
            canGoNext = prefs[canNextKey] ?: false,
            playlistId = prefs[playlistIdKey]
        )
    }

    suspend fun setPlaying(isPlaying: Boolean) {
        context.playbackDataStore.edit { it[isPlayingKey] = isPlaying }
    }

    suspend fun setMetadata(title: String?, artist: String?) {
        context.playbackDataStore.edit {
            if (title == null) it.remove(titleKey) else it[titleKey] = title
            if (artist == null) it.remove(artistKey) else it[artistKey] = artist
        }
    }

    suspend fun setShuffle(enabled: Boolean) {
        context.playbackDataStore.edit { it[shuffleKey] = enabled }
    }

    suspend fun setRepeat(mode: Int) {
        context.playbackDataStore.edit { it[repeatKey] = mode }
    }

    suspend fun setLoopMode(shuffleEnabled: Boolean, repeatMode: Int) {
        context.playbackDataStore.edit {
            it[shuffleKey] = shuffleEnabled
            it[repeatKey] = repeatMode
        }
    }

    suspend fun setNavigation(canGoPrevious: Boolean, canGoNext: Boolean) {
        context.playbackDataStore.edit {
            it[canPreviousKey] = canGoPrevious
            it[canNextKey] = canGoNext
        }
    }

    suspend fun setPlaylistId(playlistId: Long?) {
        context.playbackDataStore.edit {
            if (playlistId == null) it.remove(playlistIdKey) else it[playlistIdKey] = playlistId
        }
    }
}

data class PlaybackState(
    val isPlaying: Boolean,
    val title: String?,
    val artist: String?,
    val shuffle: Boolean,
    val repeatMode: Int,
    val canGoPrevious: Boolean,
    val canGoNext: Boolean,
    val playlistId: Long?
)
