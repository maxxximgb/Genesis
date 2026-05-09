package dev.maxxximgb.genesis.domain.usecase.playback

import dev.maxxximgb.genesis.data.preferences.PlaylistModeStore
import dev.maxxximgb.genesis.domain.model.LoopState
import javax.inject.Inject

/**
 * Writes the per-playlist loop mode to [PlaylistModeStore] — the single source of truth.
 *
 * Intentionally does NOT touch the live MediaController. PlayerService observes the store and
 * applies any change for the currently-playing playlist to the player; on cold start it reads
 * the store in restoreQueueFromStore. This decoupling matters for the "service is dead" case:
 * previously we'd call controller.setShuffleEnabled/setRepeatMode here, which goes through
 * MediaControllerProvider.connect() and **respawns the service** just to flip a flag. Worse,
 * the spawn races with restoreQueueFromStore — which read shuffle/repeat from PlaybackStateStore
 * (the prior session's snapshot) and silently overwrote the value we'd just set.
 */
class SetPlaylistLoopModeUseCase @Inject constructor(
    private val store: PlaylistModeStore,
) {
    suspend operator fun invoke(playlistId: Long, mode: LoopState) {
        store.setMode(playlistId, mode)
    }
}
