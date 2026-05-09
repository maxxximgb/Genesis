package dev.maxxximgb.genesis.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.maxxximgb.genesis.di.WidgetPreferences
import dev.maxxximgb.genesis.widget.WidgetBackgroundChoice
import dev.maxxximgb.genesis.widget.WidgetTarget
import dev.maxxximgb.genesis.widget.decodeBackgroundChoice
import dev.maxxximgb.genesis.widget.decodeWidgetTarget
import dev.maxxximgb.genesis.widget.encode
import dev.maxxximgb.genesis.widget.encodeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetPreferencesStore @Inject constructor(
    @WidgetPreferences private val dataStore: DataStore<Preferences>,
) {

    /**
     * New API. Returns the bound target (playlist id or audiobooks-pseudo) for the widget,
     * or null if the widget hasn't been bound yet. Falls back to the legacy long-keyed playlist
     * binding so widgets created before 2.3 keep working without a forced reconfig.
     */
    fun observeTargetFor(appWidgetId: Int): Flow<WidgetTarget?> = dataStore.data
        .map { prefs -> readTarget(prefs, appWidgetId) }
        .distinctUntilChanged()

    suspend fun getTargetFor(appWidgetId: Int): WidgetTarget? =
        readTarget(dataStore.data.first(), appWidgetId)

    suspend fun setTarget(appWidgetId: Int, target: WidgetTarget) {
        dataStore.edit { prefs ->
            prefs[targetKeyFor(appWidgetId)] = target.encode()
            // Keep the legacy long key in sync so older code paths still work, and clear it
            // when the new target isn't a playlist (so a stale playlist id can't leak through).
            when (target) {
                is WidgetTarget.Playlist -> prefs[playlistKeyFor(appWidgetId)] = target.playlistId
                WidgetTarget.Audiobooks -> prefs.remove(playlistKeyFor(appWidgetId))
            }
        }
    }

    /**
     * Legacy API kept for callers that only care about the playlist binding (Glance widget,
     * actions). Returns null when the widget is bound to a non-playlist target.
     */
    fun observePlaylistFor(appWidgetId: Int): Flow<Long?> = observeTargetFor(appWidgetId)
        .map { (it as? WidgetTarget.Playlist)?.playlistId }
        .distinctUntilChanged()

    suspend fun getPlaylistFor(appWidgetId: Int): Long? =
        (getTargetFor(appWidgetId) as? WidgetTarget.Playlist)?.playlistId

    suspend fun setPlaylistFor(appWidgetId: Int, playlistId: Long) =
        setTarget(appWidgetId, WidgetTarget.Playlist(playlistId))

    /** Falls back to Dark when nothing is stored — safe default for widget render path. */
    fun observeBackgroundFor(appWidgetId: Int): Flow<WidgetBackgroundChoice> = dataStore.data
        .map {
            decodeBackgroundChoice(it[bgModeKeyFor(appWidgetId)])
                ?: WidgetBackgroundChoice.Dark
        }
        .distinctUntilChanged()

    /** Returns null when no choice has been persisted yet. Caller decides the default. */
    suspend fun getStoredBackgroundFor(appWidgetId: Int): WidgetBackgroundChoice? =
        decodeBackgroundChoice(dataStore.data.first()[bgModeKeyFor(appWidgetId)])

    suspend fun setBackgroundFor(appWidgetId: Int, choice: WidgetBackgroundChoice) {
        dataStore.edit { it[bgModeKeyFor(appWidgetId)] = choice.encodeMode() }
    }

    suspend fun clear(appWidgetId: Int) {
        dataStore.edit {
            it.remove(targetKeyFor(appWidgetId))
            it.remove(playlistKeyFor(appWidgetId))
            it.remove(bgModeKeyFor(appWidgetId))
        }
    }

    private fun readTarget(prefs: Preferences, appWidgetId: Int): WidgetTarget? {
        val newKey = prefs[targetKeyFor(appWidgetId)]
        val decoded = decodeWidgetTarget(newKey)
        if (decoded != null) return decoded
        // Backwards-compat: widgets bound before 2.3 only have the long-typed playlist key.
        val legacyId = prefs[playlistKeyFor(appWidgetId)] ?: return null
        return WidgetTarget.Playlist(legacyId)
    }

    private fun targetKeyFor(appWidgetId: Int) = stringPreferencesKey("widget_target_$appWidgetId")
    private fun playlistKeyFor(appWidgetId: Int) = longPreferencesKey("widget_playlist_$appWidgetId")
    private fun bgModeKeyFor(appWidgetId: Int) = intPreferencesKey("widget_bg_mode_$appWidgetId")
}
