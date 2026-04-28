package dev.maxxximgb.genesis.domain.usecase.playlist

import dev.maxxximgb.genesis.domain.model.Playlist
import dev.maxxximgb.genesis.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObservePlaylistsUseCase @Inject constructor(
    private val repository: PlaylistRepository,
) {
    operator fun invoke(): Flow<List<Playlist>> = repository.observePlaylists()
}
