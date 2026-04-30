package dev.maxxximgb.genesis.widget

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val debouncer = RefreshDebouncer(scope, DEBOUNCE_MS) { refreshAllWidgets() }

    /** Coalesces bursts of update requests within the debounce window into a single refresh. */
    fun requestUpdate() = debouncer.request()

    suspend fun updateNow() {
        refreshAllWidgets()
    }

    /**
     * Bumps a widget's tick AFTER the launcher finishes binding it, since binding only completes
     * once the configure activity returns RESULT_OK. Polls the GlanceAppWidgetManager until the
     * id appears, then writes a fresh tick to wake the session.
     */
    fun updateWhenBound(appWidgetId: Int) {
        scope.launch {
            val manager = GlanceAppWidgetManager(context)
            val match: GlanceId? = withTimeoutOrNull(BIND_WAIT_TIMEOUT_MS) {
                var resolved: GlanceId? = null
                while (resolved == null) {
                    resolved = glanceIdFor(manager, appWidgetId)
                    if (resolved == null) delay(BIND_POLL_INTERVAL_MS)
                }
                resolved
            }
            if (match != null) bumpRefreshTick(match)
            else Log.w(TAG, "updateWhenBound timed out for appWidgetId=$appWidgetId")
        }
    }

    private suspend fun refreshAllWidgets() {
        runCatching {
            val manager = GlanceAppWidgetManager(context)
            manager.getGlanceIds(GenesisWidget::class.java).forEach { glanceId ->
                bumpRefreshTick(glanceId)
            }
        }.onFailure { Log.w(TAG, "refreshAllWidgets failed", it) }
    }

    private suspend fun glanceIdFor(manager: GlanceAppWidgetManager, appWidgetId: Int): GlanceId? =
        runCatching {
            manager.getGlanceIds(GenesisWidget::class.java)
                .firstOrNull { manager.getAppWidgetId(it) == appWidgetId }
        }.getOrNull()

    private suspend fun bumpRefreshTick(glanceId: GlanceId) {
        runCatching {
            updateAppWidgetState(
                context = context,
                definition = PreferencesGlanceStateDefinition,
                glanceId = glanceId,
            ) { prefs ->
                prefs.toMutablePreferences().apply {
                    this[REFRESH_TICK_KEY] = (prefs[REFRESH_TICK_KEY] ?: 0) + 1
                }
            }
        }.onFailure { Log.w(TAG, "bumpRefreshTick failed for $glanceId", it) }
    }

    companion object {
        const val DEBOUNCE_MS = 250L
        const val BIND_POLL_INTERVAL_MS = 75L
        const val BIND_WAIT_TIMEOUT_MS = 4000L
        private const val TAG = "WidgetUpdater"
        private val REFRESH_TICK_KEY = intPreferencesKey("refresh_tick")
    }
}
