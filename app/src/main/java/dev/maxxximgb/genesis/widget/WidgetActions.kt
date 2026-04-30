package dev.maxxximgb.genesis.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import dagger.hilt.EntryPoints
import dev.maxxximgb.genesis.domain.model.LoopState
import dev.maxxximgb.genesis.domain.model.PlaybackState
import dev.maxxximgb.genesis.domain.model.nextLoopState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

internal val APP_WIDGET_ID_KEY = ActionParameters.Key<Int>("app_widget_id")

private const val STATE_CHANGE_TIMEOUT_MS = 1500L

private fun entry(context: Context): WidgetEntryPoint =
    EntryPoints.get(context.applicationContext, WidgetEntryPoint::class.java)

class TogglePlayPauseAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val deps = entry(context)
        val widgetPlaylistId = resolvePlaylistId(deps, parameters) ?: return
        val initial = deps.playbackStateStore().flow.first()
        val ownPlaylistLoaded = initial.playlistId == widgetPlaylistId &&
            initial.queue.isNotEmpty()

        if (ownPlaylistLoaded) {
            // Decide direction from DataStore, not MediaController.isPlaying — the controller can
            // be stale right after the broadcast respawns the process.
            if (initial.isPlaying) {
                deps.playbackController().pause()
            } else {
                deps.playbackController().play()
            }
            awaitStateChange(deps) { current -> current.isPlaying != initial.isPlaying }
        } else {
            deps.playPlaylistUseCase().invoke(widgetPlaylistId, startIndex = 0)
            awaitStateChange(deps) { current ->
                current.playlistId == widgetPlaylistId && current.isPlaying
            }
        }
        deps.widgetUpdater().updateNow()
    }
}

class NextTrackAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val deps = entry(context)
        val widgetPlaylistId = resolvePlaylistId(deps, parameters) ?: return
        val initial = deps.playbackStateStore().flow.first()
        if (initial.playlistId == widgetPlaylistId) {
            deps.playbackController().seekToNext()
            awaitStateChange(deps) { current ->
                current.currentMediaStoreId != initial.currentMediaStoreId
            }
        }
        deps.widgetUpdater().updateNow()
    }
}

class PreviousTrackAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val deps = entry(context)
        val widgetPlaylistId = resolvePlaylistId(deps, parameters) ?: return
        val initial = deps.playbackStateStore().flow.first()
        if (initial.playlistId == widgetPlaylistId) {
            deps.playbackController().seekToPrevious()
            awaitStateChange(deps) { current ->
                current.currentMediaStoreId != initial.currentMediaStoreId ||
                    current.positionMs < initial.positionMs
            }
        }
        deps.widgetUpdater().updateNow()
    }
}

class CycleLoopAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val deps = entry(context)
        val widgetPlaylistId = resolvePlaylistId(deps, parameters) ?: return
        val current = deps.playlistModeStore().getMode(widgetPlaylistId)
        val next = nextLoopState(current)
        deps.setPlaylistLoopModeUseCase().invoke(widgetPlaylistId, next)
        deps.widgetUpdater().updateNow()
    }
}

private suspend fun awaitStateChange(
    deps: WidgetEntryPoint,
    predicate: (PlaybackState) -> Boolean,
) {
    withTimeoutOrNull(STATE_CHANGE_TIMEOUT_MS) {
        deps.playbackStateStore().flow.first(predicate)
    }
}

private suspend fun resolvePlaylistId(
    deps: WidgetEntryPoint,
    parameters: ActionParameters,
): Long? {
    val appWidgetId = parameters[APP_WIDGET_ID_KEY] ?: return null
    return deps.widgetPreferencesStore().getPlaylistFor(appWidgetId)
}

internal fun loopStateContentDescription(state: LoopState): Int = when (state) {
    LoopState.OFF -> dev.maxxximgb.genesis.R.string.cd_loop_off
    LoopState.REPEAT_ALL -> dev.maxxximgb.genesis.R.string.cd_loop_all
    LoopState.REPEAT_ONE -> dev.maxxximgb.genesis.R.string.cd_loop_one
    LoopState.SHUFFLE -> dev.maxxximgb.genesis.R.string.cd_shuffle
}
