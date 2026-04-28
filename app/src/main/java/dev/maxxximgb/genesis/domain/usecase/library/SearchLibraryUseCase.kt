package dev.maxxximgb.genesis.domain.usecase.library

import androidx.paging.Pager
import dev.maxxximgb.genesis.domain.model.SortOrder
import dev.maxxximgb.genesis.domain.model.Track
import dev.maxxximgb.genesis.domain.repository.MediaLibraryRepository
import javax.inject.Inject

class SearchLibraryUseCase @Inject constructor(
    private val repository: MediaLibraryRepository,
) {
    operator fun invoke(sort: SortOrder, query: String): Pager<Int, Track> =
        repository.pagedLibrary(sort, query)
}
