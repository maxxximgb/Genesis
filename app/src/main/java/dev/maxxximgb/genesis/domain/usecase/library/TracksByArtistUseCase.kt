package dev.maxxximgb.genesis.domain.usecase.library

import androidx.paging.Pager
import dev.maxxximgb.genesis.domain.model.SortOrder
import dev.maxxximgb.genesis.domain.model.Track
import dev.maxxximgb.genesis.domain.repository.MediaLibraryRepository
import javax.inject.Inject

class TracksByArtistUseCase @Inject constructor(
    private val repository: MediaLibraryRepository,
) {
    operator fun invoke(artistId: Long, sort: SortOrder = SortOrder.ALBUM_ASC): Pager<Int, Track> =
        repository.pagedTracksByArtist(artistId, sort)
}
