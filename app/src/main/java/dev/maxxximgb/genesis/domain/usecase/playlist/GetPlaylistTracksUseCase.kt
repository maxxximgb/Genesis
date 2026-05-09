package dev.maxxximgb.genesis.domain.usecase.playlist

import dev.maxxximgb.genesis.domain.model.Track
import dev.maxxximgb.genesis.domain.repository.PlaylistRepository
import javax.inject.Inject

class GetPlaylistTracksUseCase @Inject constructor(
    private val repository: PlaylistRepository,
) {
    suspend operator fun invoke(playlistId: Long): List<Track> =
        repository.getTracksForPlaylist(playlistId)
}
