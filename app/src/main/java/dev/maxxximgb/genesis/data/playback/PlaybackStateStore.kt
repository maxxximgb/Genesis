package dev.maxxximgb.genesis.data.playback

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.maxxximgb.genesis.di.PlaybackPreferences
import dev.maxxximgb.genesis.domain.model.PlaybackState
import dev.maxxximgb.genesis.domain.model.QueueEntry
import dev.maxxximgb.genesis.domain.model.RepeatMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackStateStore @Inject constructor(
    @PlaybackPreferences private val dataStore: DataStore<Preferences>,
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
            // queueEntries are serialized as `mediaStoreId:sourcePlaylistId:sourceAlbumId`
            // tuples, joined by `,`. Empty source slots are written as the empty string,
            // so a track with no source ends up as `42::`. This survives process death the
            // same way the rest of the snapshot does, so the queue UI can group tracks
            // immediately after a cold restart instead of falling back to "all orphans".
            p[Keys.QUEUE_ENTRIES_CSV] = QueueEntrySerializer.serialize(state.queueEntries)
            p[Keys.PLAY_ORDER_CSV] = state.playOrderIndices.joinToString(",")
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

    /**
     * Used by [GenesisApp] at process start to clear a stale isPlaying=true flag that survived
     * a hard kill (force-stop, OOM kill -9, system reclaim). The rest of the snapshot stays so
     * the user can still resume — only the play/pause indicator is reset to match reality
     * (no service running ⇒ nothing is actually playing).
     */
    suspend fun setIsPlaying(isPlaying: Boolean) {
        dataStore.edit { it[Keys.IS_PLAYING] = isPlaying }
    }

    private fun Preferences.toPlaybackState(): PlaybackState {
        val queue = QueueSerializer.deserialize(this[Keys.QUEUE_CSV] ?: "")
        val entries = QueueEntrySerializer.deserialize(this[Keys.QUEUE_ENTRIES_CSV] ?: "")
        // Backward compat: if the entries blob is missing (older app version) but the
        // queue isn't, synthesize all-orphan entries so PlaybackState's invariant
        // (queueEntries.size == queue.size) holds.
        val resolvedEntries = if (entries.size == queue.size) {
            entries
        } else {
            queue.map { QueueEntry(mediaStoreId = it, sourcePlaylistId = null, sourceAlbumId = null) }
        }
        return PlaybackState(
            isPlaying = this[Keys.IS_PLAYING] ?: false,
            title = this[Keys.TITLE],
            artist = this[Keys.ARTIST],
            currentMediaStoreId = this[Keys.CURRENT_MEDIA_ID],
            playlistId = this[Keys.PLAYLIST_ID],
            queue = queue,
            queueEntries = resolvedEntries,
            currentIndex = this[Keys.CURRENT_INDEX] ?: -1,
            positionMs = this[Keys.POSITION_MS] ?: 0L,
            durationMs = this[Keys.DURATION_MS] ?: 0L,
            shuffleEnabled = this[Keys.SHUFFLE_ENABLED] ?: false,
            repeatMode = RepeatMode.entries.getOrNull(this[Keys.REPEAT_MODE] ?: 0) ?: RepeatMode.OFF,
            playOrderIndices = (this[Keys.PLAY_ORDER_CSV] ?: "")
                .split(",")
                .mapNotNull { it.trim().toIntOrNull() },
        )
    }

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
        val QUEUE_ENTRIES_CSV = stringPreferencesKey("queue_entries_csv")
        val PLAY_ORDER_CSV = stringPreferencesKey("play_order_csv")
        val CURRENT_INDEX = intPreferencesKey("current_index")
        val POSITION_MS = longPreferencesKey("position_ms")
        val DURATION_MS = longPreferencesKey("duration_ms")
        val SHUFFLE_ENABLED = booleanPreferencesKey("shuffle_enabled")
        val REPEAT_MODE = intPreferencesKey("repeat_mode")
    }
}
