package dev.maxxximgb.genesis.domain.usecase.playback

import dev.maxxximgb.genesis.data.preferences.PlaylistModeStore
import dev.maxxximgb.genesis.domain.model.toRepeatAndShuffle
import dev.maxxximgb.genesis.domain.playback.PlaybackController
import dev.maxxximgb.genesis.domain.repository.PlaylistRepository
import javax.inject.Inject

class PlayPlaylistUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val controller: PlaybackController,
    private val modeStore: PlaylistModeStore,
) {
    suspend operator fun invoke(playlistId: Long, startIndex: Int = 0) {
        val tracks = playlistRepository.getTracksForPlaylist(playlistId)
        if (tracks.isEmpty()) return
        val (repeat, shuffle) = modeStore.getMode(playlistId).toRepeatAndShuffle()
        controller.setShuffleEnabled(shuffle)
        controller.setRepeatMode(repeat)
        controller.playQueue(playlistId, tracks, startIndex)
    }
}
