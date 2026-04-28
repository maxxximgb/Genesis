package dev.maxxximgb.genesis.domain.usecase.playlist

import dev.maxxximgb.genesis.domain.repository.PlaylistRepository
import javax.inject.Inject

class ReorderPlaylistTracksUseCase @Inject constructor(
    private val repository: PlaylistRepository,
) {
    suspend operator fun invoke(playlistId: Long, orderedMediaIds: List<Long>) =
        repository.reorder(playlistId, orderedMediaIds)
}
