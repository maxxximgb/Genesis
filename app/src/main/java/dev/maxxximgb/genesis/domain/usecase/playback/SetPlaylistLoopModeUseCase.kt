package dev.maxxximgb.genesis.domain.usecase.playback

import dev.maxxximgb.genesis.data.preferences.PlaylistModeStore
import dev.maxxximgb.genesis.domain.model.LoopState
import dev.maxxximgb.genesis.domain.model.toRepeatAndShuffle
import dev.maxxximgb.genesis.domain.playback.PlaybackController
import javax.inject.Inject

class SetPlaylistLoopModeUseCase @Inject constructor(
    private val store: PlaylistModeStore,
    private val controller: PlaybackController,
) {
    suspend operator fun invoke(playlistId: Long, mode: LoopState) {
        store.setMode(playlistId, mode)
        if (controller.state.value.playlistId == playlistId) {
            val (repeat, shuffle) = mode.toRepeatAndShuffle()
            controller.setShuffleEnabled(shuffle)
            controller.setRepeatMode(repeat)
        }
    }
}
