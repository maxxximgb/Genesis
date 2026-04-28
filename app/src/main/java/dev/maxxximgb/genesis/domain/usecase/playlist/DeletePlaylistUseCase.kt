package dev.maxxximgb.genesis.domain.usecase.playlist

import dev.maxxximgb.genesis.domain.repository.PlaylistRepository
import javax.inject.Inject

class DeletePlaylistUseCase @Inject constructor(
    private val repository: PlaylistRepository,
) {
    suspend operator fun invoke(id: Long) = repository.delete(id)
}
