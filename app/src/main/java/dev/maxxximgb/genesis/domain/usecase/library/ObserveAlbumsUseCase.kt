package dev.maxxximgb.genesis.domain.usecase.library

import dev.maxxximgb.genesis.domain.model.Album
import dev.maxxximgb.genesis.domain.repository.MediaLibraryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveAlbumsUseCase @Inject constructor(
    private val repository: MediaLibraryRepository,
) {
    operator fun invoke(): Flow<List<Album>> = repository.observeAlbums()
}
