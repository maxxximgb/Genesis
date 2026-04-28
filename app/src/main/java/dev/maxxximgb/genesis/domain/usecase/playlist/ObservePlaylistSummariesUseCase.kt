package dev.maxxximgb.genesis.domain.usecase.playlist

import dev.maxxximgb.genesis.domain.model.PlaylistSummary
import dev.maxxximgb.genesis.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObservePlaylistSummariesUseCase @Inject constructor(
    private val repository: PlaylistRepository,
) {
    operator fun invoke(): Flow<List<PlaylistSummary>> = repository.observePlaylistsWithCounts()
}
