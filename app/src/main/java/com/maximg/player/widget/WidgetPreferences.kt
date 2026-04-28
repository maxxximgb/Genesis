package com.maximg.player.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.appwidget.AppWidgetId
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.maximg.player.playback.PlaybackController
import com.maximg.player.util.widgetDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class WidgetPreferences(private val context: Context) {
    private fun playlistIdKey(appWidgetId: Int) = longPreferencesKey("widget_playlist_$appWidgetId")
    private fun playlistNameKey(appWidgetId: Int) = stringPreferencesKey("widget_playlist_name_$appWidgetId")
    private fun loopModeKey(appWidgetId: Int) = intPreferencesKey("widget_loop_mode_$appWidgetId")

    suspend fun setPlaylist(appWidgetId: Int, playlistId: Long, playlistName: String) {
        context.widgetDataStore.edit { prefs ->
            prefs[playlistIdKey(appWidgetId)] = playlistId
            prefs[playlistNameKey(appWidgetId)] = playlistName
        }
    }

    suspend fun setPlaylist(glanceId: GlanceId, playlistId: Long, playlistName: String) {
        val appWidgetId = resolveAppWidgetId(glanceId)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
        setPlaylist(appWidgetId, playlistId, playlistName)
    }

    suspend fun setLoopMode(appWidgetId: Int, mode: PlaybackController.WidgetLoopMode) {
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
        context.widgetDataStore.edit { prefs ->
            prefs[loopModeKey(appWidgetId)] = mode.ordinal
        }
    }

    suspend fun setLoopMode(glanceId: GlanceId, mode: PlaybackController.WidgetLoopMode) {
        var appWidgetId = resolveAppWidgetId(glanceId)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            val prefs = context.widgetDataStore.data.first()
            appWidgetId = singleConfiguredWidgetId(prefs) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        }
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
        setLoopMode(appWidgetId, mode)
    }

    suspend fun getPlaylistInfo(glanceId: GlanceId): WidgetPlaylistInfo? {
        val appWidgetId = resolveAppWidgetId(glanceId)
        val prefs = context.widgetDataStore.data.first()
        return getPlaylistInfo(prefs, appWidgetId) ?: singleConfiguredPlaylistInfo(prefs)
    }

    suspend fun observePlaylistInfo(glanceId: GlanceId): Flow<WidgetPlaylistInfo?> {
        val appWidgetId = resolveAppWidgetId(glanceId)
        return observePlaylistInfo(appWidgetId)
    }

    fun observePlaylistInfo(appWidgetId: Int): Flow<WidgetPlaylistInfo?> {
        return context.widgetDataStore.data.map { prefs ->
            getPlaylistInfo(prefs, appWidgetId) ?: singleConfiguredPlaylistInfo(prefs)
        }
    }

    suspend fun getPlaylistInfo(appWidgetId: Int): WidgetPlaylistInfo? {
        val prefs = context.widgetDataStore.data.first()
        return getPlaylistInfo(prefs, appWidgetId) ?: singleConfiguredPlaylistInfo(prefs)
    }

    private fun getPlaylistInfo(prefs: Preferences, appWidgetId: Int): WidgetPlaylistInfo? {
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return null
        val id = prefs[playlistIdKey(appWidgetId)] ?: return null
        val name = prefs[playlistNameKey(appWidgetId)]
        return WidgetPlaylistInfo(
            playlistId = id,
            playlistName = name,
            loopMode = readLoopMode(prefs, appWidgetId)
        )
    }

    private fun readLoopMode(
        prefs: Preferences,
        appWidgetId: Int
    ): PlaybackController.WidgetLoopMode {
        val ordinal = prefs[loopModeKey(appWidgetId)]
        return PlaybackController.WidgetLoopMode.entries.getOrNull(ordinal ?: -1)
            ?: PlaybackController.WidgetLoopMode.NO_REPEAT
    }

    private fun singleConfiguredPlaylistInfo(prefs: Preferences): WidgetPlaylistInfo? {
        val widgetId = singleConfiguredWidgetId(prefs) ?: return null
        return getPlaylistInfo(prefs, widgetId)
    }

    private fun singleConfiguredWidgetId(prefs: Preferences): Int? {
        val ids = prefs.asMap().keys.mapNotNull { key ->
            widgetPlaylistIdRegex.matchEntire(key.name)?.groupValues?.get(1)?.toIntOrNull()
        }.distinct()

        if (ids.isEmpty()) return null

        val activeIds = activeWidgetIds()
        if (activeIds.isEmpty()) {
            // During initial widget creation the host may not report active ids yet.
            // If only one widget is configured in prefs, treat it as the target.
            return ids.singleOrNull()
        }

        val configuredActiveIds = ids.filter { it in activeIds }
        return configuredActiveIds.singleOrNull()
    }

    private fun activeWidgetIds(): Set<Int> {
        val provider = ComponentName(context, MorningPlayerWidgetReceiver::class.java)
        return AppWidgetManager.getInstance(context).getAppWidgetIds(provider).toSet()
    }

    private suspend fun resolveAppWidgetId(glanceId: GlanceId): Int {
        if (glanceId is AppWidgetId) return glanceId.appWidgetId
        return runCatching {
            GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
        }.getOrDefault(AppWidgetManager.INVALID_APPWIDGET_ID)
    }

    companion object {
        private val widgetPlaylistIdRegex = Regex("^widget_playlist_(\\d+)$")
    }
}

data class WidgetPlaylistInfo(
    val playlistId: Long,
    val playlistName: String?,
    val loopMode: PlaybackController.WidgetLoopMode
)
