package dev.maxxximgb.genesis.data.playback

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.maxxximgb.genesis.domain.model.PlaybackState
import dev.maxxximgb.genesis.domain.model.RepeatMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackStateStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {

    val flow: Flow<PlaybackState> = dataStore.data
        .map { it.toPlaybackState() }
        .distinctUntilChanged()

    suspend fun update(state: PlaybackState) {
        dataStore.edit { p ->
            p[Keys.IS_PLAYING] = state.isPlaying
            p.setOrRemove(Keys.TITLE, state.title)
            p.setOrRemove(Keys.ARTIST, state.artist)
            p.setOrRemove(Keys.CURRENT_MEDIA_ID, state.currentMediaStoreId)
            p.setOrRemove(Keys.PLAYLIST_ID, state.playlistId)
            p[Keys.QUEUE_CSV] = QueueSerializer.serialize(state.queue)
            p[Keys.CURRENT_INDEX] = state.currentIndex
            p[Keys.POSITION_MS] = state.positionMs
            p[Keys.DURATION_MS] = state.durationMs
            p[Keys.SHUFFLE_ENABLED] = state.shuffleEnabled
            p[Keys.REPEAT_MODE] = state.repeatMode.ordinal
        }
    }

    suspend fun setPosition(positionMs: Long) {
        dataStore.edit { it[Keys.POSITION_MS] = positionMs }
    }

    private fun Preferences.toPlaybackState(): PlaybackState = PlaybackState(
        isPlaying = this[Keys.IS_PLAYING] ?: false,
        title = this[Keys.TITLE],
        artist = this[Keys.ARTIST],
        currentMediaStoreId = this[Keys.CURRENT_MEDIA_ID],
        playlistId = this[Keys.PLAYLIST_ID],
        queue = QueueSerializer.deserialize(this[Keys.QUEUE_CSV] ?: ""),
        currentIndex = this[Keys.CURRENT_INDEX] ?: -1,
        positionMs = this[Keys.POSITION_MS] ?: 0L,
        durationMs = this[Keys.DURATION_MS] ?: 0L,
        shuffleEnabled = this[Keys.SHUFFLE_ENABLED] ?: false,
        repeatMode = RepeatMode.entries.getOrNull(this[Keys.REPEAT_MODE] ?: 0) ?: RepeatMode.OFF,
    )

    private fun <T : Any> androidx.datastore.preferences.core.MutablePreferences.setOrRemove(
        key: Preferences.Key<T>,
        value: T?,
    ) {
        if (value == null) remove(key) else set(key, value)
    }

    private object Keys {
        val IS_PLAYING = booleanPreferencesKey("is_playing")
        val TITLE = stringPreferencesKey("title")
        val ARTIST = stringPreferencesKey("artist")
        val CURRENT_MEDIA_ID = longPreferencesKey("current_media_id")
        val PLAYLIST_ID = longPreferencesKey("playlist_id")
        val QUEUE_CSV = stringPreferencesKey("queue_csv")
        val CURRENT_INDEX = intPreferencesKey("current_index")
        val POSITION_MS = longPreferencesKey("position_ms")
        val DURATION_MS = longPreferencesKey("duration_ms")
        val SHUFFLE_ENABLED = booleanPreferencesKey("shuffle_enabled")
        val REPEAT_MODE = intPreferencesKey("repeat_mode")
    }
}
