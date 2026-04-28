package dev.maxxximgb.genesis.domain.usecase.playlist

import dev.maxxximgb.genesis.domain.repository.PlaylistRepository
import javax.inject.Inject

class CreatePlaylistUseCase @Inject constructor(
    private val repository: PlaylistRepository,
) {
    suspend operator fun invoke(name: String): Long = repository.create(name.trim())
}
