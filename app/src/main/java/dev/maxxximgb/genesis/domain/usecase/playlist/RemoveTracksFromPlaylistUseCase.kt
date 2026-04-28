package dev.maxxximgb.genesis.domain.usecase.playlist

import dev.maxxximgb.genesis.domain.repository.PlaylistRepository
import javax.inject.Inject

class RemoveTracksFromPlaylistUseCase @Inject constructor(
    private val repository: PlaylistRepository,
) {
    suspend operator fun invoke(playlistId: Long, mediaStoreIds: List<Long>) =
        repository.removeTracks(playlistId, mediaStoreIds)
}
