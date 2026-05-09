package dev.maxxximgb.genesis.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import dagger.hilt.EntryPoints
import dev.maxxximgb.genesis.domain.model.LoopState
import dev.maxxximgb.genesis.domain.model.PlaybackState
import dev.maxxximgb.genesis.domain.model.Track
import dev.maxxximgb.genesis.domain.model.nextLoopState
import dev.maxxximgb.genesis.domain.model.toRepeatAndShuffle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

internal val APP_WIDGET_ID_KEY = ActionParameters.Key<Int>("app_widget_id")

// Cold-starting PlayerService takes 2-3s on a typical device (ExoPlayer init + Hilt graph +
// restoreQueueFromStore). 1500ms used to time out before the state actually flipped, leaving
// updateNow() to refresh the widget on stale state — making taps look unresponsive. 4000ms
// covers cold-start while still bounding the action so it can't block Glance forever.
private const val STATE_CHANGE_TIMEOUT_MS = 4000L

private fun entry(context: Context): WidgetEntryPoint =
    EntryPoints.get(context.applicationContext, WidgetEntryPoint::class.java)

class TogglePlayPauseAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val deps = entry(context)
        val target = resolveTarget(deps, parameters) ?: return
        val pid = target.toQueueId()
        val initial = deps.playbackStateStore().flow.first()
        val ownLoaded = initial.playlistId == pid && initial.queue.isNotEmpty()

        if (ownLoaded) {
            // Decide direction from DataStore, not MediaController.isPlaying — the controller can
            // be stale right after the broadcast respawns the process.
            if (initial.isPlaying) {
                deps.playbackController().pause()
            } else {
                deps.playbackController().play()
            }
            awaitStateChange(deps) { current -> current.isPlaying != initial.isPlaying }
        } else {
            // No explicit index → resume from saved bookmark for this target (falls back to 0).
            when (target) {
                is WidgetTarget.Playlist -> deps.playPlaylistUseCase().invoke(target.playlistId)
                WidgetTarget.Audiobooks -> deps.playAudiobooksUseCase().invoke()
            }
            awaitStateChange(deps) { current -> current.playlistId == pid && current.isPlaying }
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
        val target = resolveTarget(deps, parameters) ?: return
        val pid = target.toQueueId()
        val initial = deps.playbackStateStore().flow.first()
        if (initial.playlistId == pid && initial.queue.isNotEmpty()) {
            deps.playbackController().seekToNext()
            awaitStateChange(deps) { current ->
                current.currentMediaStoreId != initial.currentMediaStoreId
            }
        } else {
            // OWN_IDLE branch: this widget's playlist isn't on the player. Load it from the
            // bookmark + 1, so the user gets "advance one track" semantics consistent with
            // having tapped next inside an active queue.
            startWidgetTargetAtOffset(deps, target, offset = +1)
            awaitStateChange(deps) { current -> current.playlistId == pid && current.isPlaying }
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
        val target = resolveTarget(deps, parameters) ?: return
        val pid = target.toQueueId()
        val initial = deps.playbackStateStore().flow.first()
        if (initial.playlistId == pid && initial.queue.isNotEmpty()) {
            deps.playbackController().seekToPrevious()
            awaitStateChange(deps) { current ->
                current.currentMediaStoreId != initial.currentMediaStoreId ||
                    current.positionMs < initial.positionMs
            }
        } else {
            // Same idea as NextTrackAction's OWN_IDLE branch, but stepping back.
            startWidgetTargetAtOffset(deps, target, offset = -1)
            awaitStateChange(deps) { current -> current.playlistId == pid && current.isPlaying }
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
        val target = resolveTarget(deps, parameters) ?: return
        val pid = target.toQueueId()
        // PlaylistModeStore is authoritative regardless of which playlist is currently the active
        // queue. Reading from PlaybackStateStore.repeatMode/shuffleEnabled would give us the live
        // *player* mode for some other playlist — wrong for this widget.
        val current = deps.playlistModeStore().getMode(pid)
        val next = nextLoopState(current)
        deps.setPlaylistLoopModeUseCase().invoke(pid, next)
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

/**
 * Loads [target] onto the player and starts playback at `bookmarkIndex + offset`. Used by
 * NextTrackAction/PreviousTrackAction when this widget's playlist isn't the active queue,
 * so prev/next replace the current queue rather than no-op.
 *
 * Index resolution:
 *  - Base = index of bookmark.mediaStoreId in [target]'s tracks (or 0 if no bookmark / unmapped).
 *  - For LoopState.OFF and REPEAT_ONE: clamp base+offset to [0, lastIndex] (no wrap at edges).
 *  - For LoopState.REPEAT_ALL and SHUFFLE: wrap modulo size (matches "transport always enabled"
 *    semantics from [transportEnabled]).
 *
 * Mode flags (shuffle, repeat) are applied to the controller before [playQueue] so the player
 * starts in the right mode without a separate listener round-trip.
 */
private suspend fun startWidgetTargetAtOffset(
    deps: WidgetEntryPoint,
    target: WidgetTarget,
    offset: Int,
) {
    val pid = target.toQueueId()
    val tracks = resolveTracksFor(deps, target)
    if (tracks.isEmpty()) return

    val bookmark = deps.bookmarkStore().get(pid)
    val baseIndex = bookmark?.let { bm ->
        tracks.indexOfFirst { it.mediaStoreId == bm.mediaStoreId }
    }?.takeIf { it >= 0 } ?: 0
    val mode = deps.playlistModeStore().getMode(pid)
    val newIndex = resolveOffsetIndex(baseIndex, offset, tracks.size, mode)

    val (repeat, shuffle) = mode.toRepeatAndShuffle()
    deps.playbackController().setShuffleEnabled(shuffle)
    deps.playbackController().setRepeatMode(repeat)
    deps.playbackController().playQueue(
        playlistId = pid,
        tracks = tracks,
        startIndex = newIndex,
        startPositionMs = 0L,
    )
}

private suspend fun resolveTracksFor(
    deps: WidgetEntryPoint,
    target: WidgetTarget,
): List<Track> = when (target) {
    is WidgetTarget.Playlist -> deps.playlistRepository().getTracksForPlaylist(target.playlistId)
    WidgetTarget.Audiobooks -> {
        val sort = deps.userPreferencesStore().observeLibrarySort().first()
        val overrides = deps.userPreferencesStore().observeAudiobookOverrides().first()
        deps.mediaLibraryRepository().getAudiobookTracks(sort, overrides.toList())
    }
}

internal fun resolveOffsetIndex(
    base: Int,
    offset: Int,
    size: Int,
    mode: LoopState,
): Int {
    if (size <= 0) return 0
    val raw = base + offset
    return when (mode) {
        LoopState.OFF, LoopState.REPEAT_ONE -> raw.coerceIn(0, size - 1)
        LoopState.REPEAT_ALL, LoopState.SHUFFLE -> ((raw % size) + size) % size
    }
}

private suspend fun resolveTarget(
    deps: WidgetEntryPoint,
    parameters: ActionParameters,
): WidgetTarget? {
    val appWidgetId = parameters[APP_WIDGET_ID_KEY] ?: return null
    return deps.widgetPreferencesStore().getTargetFor(appWidgetId)
}

private fun WidgetTarget.toQueueId(): Long = when (this) {
    is WidgetTarget.Playlist -> playlistId
    WidgetTarget.Audiobooks -> AUDIOBOOK_PSEUDO_PLAYLIST_ID
}

internal fun loopStateContentDescription(state: LoopState): Int = when (state) {
    LoopState.OFF -> dev.maxxximgb.genesis.R.string.cd_loop_off
    LoopState.REPEAT_ALL -> dev.maxxximgb.genesis.R.string.cd_loop_all
    LoopState.REPEAT_ONE -> dev.maxxximgb.genesis.R.string.cd_loop_one
    LoopState.SHUFFLE -> dev.maxxximgb.genesis.R.string.cd_shuffle
}
