package com.maximg.player.widget

import android.content.Context
import android.content.Intent
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.maximg.player.AppContainerProvider
import com.maximg.player.MainActivity
import com.maximg.player.playback.PlaybackController
import com.maximg.player.util.EXTRA_PLAYLIST_ID
import kotlinx.coroutines.flow.first

val PlaylistIdKey = ActionParameters.Key<Long>("playlist_id")

class WidgetPlayAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val container = AppContainerProvider.get(context)
        val playlistInfo = container.widgetPreferences.getPlaylistInfo(glanceId) ?: return
        container.playbackController.applyWidgetLoopMode(playlistInfo.loopMode)
        val state = container.playbackStateStore.stateFlow.first()
        if (state.playlistId != playlistInfo.playlistId) {
            container.playbackController.playPlaylist(playlistInfo.playlistId)
        } else {
            val toggleResult = container.playbackController.togglePlayPause(
                currentlyPlaying = false
            )
            if (toggleResult == PlaybackController.TogglePlayPauseResult.NEEDS_QUEUE) {
                container.playbackController.playPlaylist(playlistInfo.playlistId)
            }
        }
        WidgetUpdater.updateAll(context)
    }
}

class WidgetPauseAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val container = AppContainerProvider.get(context)
        val playlistInfo = container.widgetPreferences.getPlaylistInfo(glanceId) ?: return
        val state = container.playbackStateStore.stateFlow.first()
        if (state.playlistId == playlistInfo.playlistId) {
            container.playbackController.togglePlayPause(currentlyPlaying = true)
        }
        WidgetUpdater.updateAll(context)
    }
}

class WidgetNextAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val container = AppContainerProvider.get(context)
        val playlistInfo = container.widgetPreferences.getPlaylistInfo(glanceId) ?: return
        val state = container.playbackStateStore.stateFlow.first()
        if (state.playlistId != playlistInfo.playlistId || !container.playbackController.canGoNext()) {
            WidgetUpdater.updateAll(context)
            return
        }
        container.playbackController.next()
        WidgetUpdater.updateAll(context)
    }
}

class WidgetPrevAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val container = AppContainerProvider.get(context)
        val playlistInfo = container.widgetPreferences.getPlaylistInfo(glanceId) ?: return
        val state = container.playbackStateStore.stateFlow.first()
        if (state.playlistId != playlistInfo.playlistId || !container.playbackController.canGoPrevious()) {
            WidgetUpdater.updateAll(context)
            return
        }
        container.playbackController.previous()
        WidgetUpdater.updateAll(context)
    }
}

class WidgetRepeatAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val container = AppContainerProvider.get(context)
        val playlistInfo = container.widgetPreferences.getPlaylistInfo(glanceId) ?: return
        val next = playlistInfo.loopMode.next()
        container.widgetPreferences.setLoopMode(glanceId, next)
        val state = container.playbackStateStore.stateFlow.first()
        if (state.playlistId == playlistInfo.playlistId) {
            container.playbackController.applyWidgetLoopMode(next)
        }
        WidgetUpdater.updateAll(context)
    }
}

class WidgetOpenPlaylistAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val playlistId = parameters[PlaylistIdKey]
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (playlistId != null && playlistId > 0) {
                putExtra(EXTRA_PLAYLIST_ID, playlistId)
            }
        }
        context.startActivity(intent)
    }
}
