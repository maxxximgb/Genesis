package dev.maxxximgb.genesis.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.maxxximgb.genesis.di.PlaylistModePreferences
import dev.maxxximgb.genesis.domain.model.LoopState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistModeStore @Inject constructor(
    @PlaylistModePreferences private val dataStore: DataStore<Preferences>,
) {

    fun observeMode(playlistId: Long): Flow<LoopState> = dataStore.data
        .map { prefs -> readMode(prefs, playlistId) }
        .distinctUntilChanged()

    suspend fun getMode(playlistId: Long): LoopState =
        readMode(dataStore.data.first(), playlistId)

    suspend fun setMode(playlistId: Long, mode: LoopState) {
        dataStore.edit { it[keyFor(playlistId)] = mode.name }
    }

    private fun readMode(prefs: Preferences, playlistId: Long): LoopState {
        val raw = prefs[keyFor(playlistId)] ?: return LoopState.OFF
        return runCatching { LoopState.valueOf(raw) }.getOrDefault(LoopState.OFF)
    }

    private fun keyFor(playlistId: Long) = stringPreferencesKey("loop_mode_$playlistId")
}
