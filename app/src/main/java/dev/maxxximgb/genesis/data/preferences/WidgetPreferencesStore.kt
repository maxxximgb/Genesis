package dev.maxxximgb.genesis.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import dev.maxxximgb.genesis.di.WidgetPreferences
import dev.maxxximgb.genesis.widget.WidgetBackgroundChoice
import dev.maxxximgb.genesis.widget.decodeBackgroundChoice
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

    fun observePlaylistFor(appWidgetId: Int): Flow<Long?> = dataStore.data
        .map { it[playlistKeyFor(appWidgetId)] }
        .distinctUntilChanged()

    suspend fun getPlaylistFor(appWidgetId: Int): Long? =
        dataStore.data.first()[playlistKeyFor(appWidgetId)]

    suspend fun setPlaylistFor(appWidgetId: Int, playlistId: Long) {
        dataStore.edit { it[playlistKeyFor(appWidgetId)] = playlistId }
    }

    fun observeBackgroundFor(appWidgetId: Int): Flow<WidgetBackgroundChoice> = dataStore.data
        .map { decodeBackgroundChoice(it[bgModeKeyFor(appWidgetId)], it[bgColorKeyFor(appWidgetId)]) }
        .distinctUntilChanged()

    suspend fun getBackgroundFor(appWidgetId: Int): WidgetBackgroundChoice =
        dataStore.data.first().let {
            decodeBackgroundChoice(it[bgModeKeyFor(appWidgetId)], it[bgColorKeyFor(appWidgetId)])
        }

    suspend fun setBackgroundFor(appWidgetId: Int, choice: WidgetBackgroundChoice) {
        dataStore.edit { prefs ->
            prefs[bgModeKeyFor(appWidgetId)] = choice.encodeMode()
            when (choice) {
                is WidgetBackgroundChoice.Solid -> prefs[bgColorKeyFor(appWidgetId)] = choice.argb
                else -> prefs.remove(bgColorKeyFor(appWidgetId))
            }
        }
    }

    suspend fun clear(appWidgetId: Int) {
        dataStore.edit {
            it.remove(playlistKeyFor(appWidgetId))
            it.remove(bgModeKeyFor(appWidgetId))
            it.remove(bgColorKeyFor(appWidgetId))
        }
    }

    private fun playlistKeyFor(appWidgetId: Int) = longPreferencesKey("widget_playlist_$appWidgetId")
    private fun bgModeKeyFor(appWidgetId: Int) = intPreferencesKey("widget_bg_mode_$appWidgetId")
    private fun bgColorKeyFor(appWidgetId: Int) = intPreferencesKey("widget_bg_color_$appWidgetId")
}
