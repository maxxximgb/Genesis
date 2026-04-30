package dev.maxxximgb.genesis.domain.usecase.library

import androidx.paging.Pager
import dev.maxxximgb.genesis.domain.model.SortOrder
import dev.maxxximgb.genesis.domain.model.Track
import dev.maxxximgb.genesis.domain.repository.MediaLibraryRepository
import javax.inject.Inject

class TracksInFolderUseCase @Inject constructor(
    private val repository: MediaLibraryRepository,
) {
    operator fun invoke(bucketId: Long, sort: SortOrder = SortOrder.TITLE_ASC): Pager<Int, Track> =
        repository.pagedTracksInFolder(bucketId, sort)
}
