package dev.maxxximgb.genesis.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.maxxximgb.genesis.di.UserPreferences
import dev.maxxximgb.genesis.widget.AUDIOBOOK_PSEUDO_PLAYLIST_ID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Per-context resume points: each playlist gets its own (mediaStoreId, positionMs) bookmark,
 * library has one. Updated continuously while playing; consulted when (re)starting a context.
 */
@Singleton
class BookmarkStore @Inject constructor(
    @UserPreferences private val dataStore: DataStore<Preferences>,
) {

    data class Bookmark(val mediaStoreId: Long, val positionMs: Long)

    suspend fun save(playlistId: Long?, mediaStoreId: Long, positionMs: Long) {
        dataStore.edit { prefs ->
            prefs[bookmarkKey(playlistId)] = "$mediaStoreId:${positionMs.coerceAtLeast(0L)}"
        }
    }

    suspend fun get(playlistId: Long?): Bookmark? {
        val raw = dataStore.data.firstOrNull()?.get(bookmarkKey(playlistId)) ?: return null
        return parse(raw)
    }

    /**
     * Streams bookmark updates for [playlistId]. Used by the widget's OWN_IDLE rendering: when
     * a different playlist is the active queue, the widget still shows its own playlist's last
     * track + position; observing the bookmark means it stays fresh as soon as user playback
     * advances inside this playlist (which writes a new bookmark every position tick).
     */
    fun observe(playlistId: Long?): Flow<Bookmark?> = dataStore.data
        .map { it[bookmarkKey(playlistId)]?.let(::parse) }
        .distinctUntilChanged()

    private fun bookmarkKey(playlistId: Long?) = stringPreferencesKey(
        when (playlistId) {
            null -> "bookmark_lib"
            AUDIOBOOK_PSEUDO_PLAYLIST_ID -> "bookmark_audiobooks"
            else -> "bookmark_pl_$playlistId"
        },
    )

    private fun parse(raw: String): Bookmark? {
        val parts = raw.split(':')
        if (parts.size != 2) return null
        val id = parts[0].toLongOrNull() ?: return null
        val pos = parts[1].toLongOrNull() ?: return null
        return Bookmark(id, pos)
    }
}
