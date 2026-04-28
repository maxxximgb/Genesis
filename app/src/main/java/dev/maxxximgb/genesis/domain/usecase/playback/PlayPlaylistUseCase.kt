package dev.maxxximgb.genesis.domain.usecase.playback

import dev.maxxximgb.genesis.domain.playback.PlaybackController
import dev.maxxximgb.genesis.domain.repository.PlaylistRepository
import javax.inject.Inject

class PlayPlaylistUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val controller: PlaybackController,
) {
    suspend operator fun invoke(playlistId: Long, startIndex: Int = 0) {
        val tracks = playlistRepository.getTracksForPlaylist(playlistId)
        if (tracks.isEmpty()) return
        controller.playQueue(playlistId, tracks, startIndex)
    }
}
