package dev.maxxximgb.genesis.domain.usecase.playlist

import dev.maxxximgb.genesis.domain.model.PlaylistDetail
import dev.maxxximgb.genesis.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class ObservePlaylistDetailUseCase @Inject constructor(
    private val repository: PlaylistRepository,
) {
    operator fun invoke(playlistId: Long): Flow<PlaylistDetail?> = combine(
        repository.observePlaylist(playlistId),
        repository.observePlaylistTracks(playlistId),
    ) { playlist, tracks ->
        if (playlist == null) null else PlaylistDetail(playlist, tracks)
    }
}
