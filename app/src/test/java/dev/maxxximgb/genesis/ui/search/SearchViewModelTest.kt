package dev.maxxximgb.genesis.ui.search

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import dev.maxxximgb.genesis.data.preferences.SearchHistoryStore
import dev.maxxximgb.genesis.data.preferences.UserPreferencesStore
import dev.maxxximgb.genesis.domain.model.SortOrder
import dev.maxxximgb.genesis.domain.model.Track
import dev.maxxximgb.genesis.domain.usecase.library.SearchLibraryUseCase
import dev.maxxximgb.genesis.domain.usecase.playlist.AddTracksToPlaylistUseCase
import dev.maxxximgb.genesis.domain.usecase.playlist.CreatePlaylistUseCase
import dev.maxxximgb.genesis.domain.usecase.playlist.ObservePlaylistsUseCase
import dev.maxxximgb.genesis.ui.library.SelectionStateHolder
import dev.maxxximgb.genesis.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private fun emptyPager(): Pager<Int, Track> = Pager(
        config = PagingConfig(pageSize = 1, enablePlaceholders = false),
        pagingSourceFactory = {
            object : PagingSource<Int, Track>() {
                override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Track> =
                    LoadResult.Page(emptyList(), null, null)

                override fun getRefreshKey(state: PagingState<Int, Track>): Int? = null
            }
        },
    )

    private fun track(id: Long) = Track(
        mediaStoreId = id,
        title = "T$id",
        artist = null,
        album = null,
        albumId = null,
        durationMs = 1L,
        contentUri = "u://$id",
        dateAdded = id,
    )

    private fun makeVm(
        prefs: UserPreferencesStore = mock {
            on { observeLibrarySort() } doReturn flowOf(SortOrder.DATE_ADDED_DESC)
            on { observeAudiobookOverrides() } doReturn flowOf(emptySet())
        },
        addUc: AddTracksToPlaylistUseCase = mock(),
        holder: SelectionStateHolder = SelectionStateHolder(),
    ): SearchViewModel {
        val search = mock<SearchLibraryUseCase> {
            on { invoke(any(), any()) } doReturn emptyPager()
        }
        val history = mock<SearchHistoryStore> {
            on { observeRecent() } doReturn flowOf(emptyList())
        }
        val observePlaylists = mock<ObservePlaylistsUseCase> {
            on { invoke() } doReturn flowOf(emptyList())
        }
        return SearchViewModel(
            searchLibrary = search,
            userPreferences = prefs,
            searchHistoryStore = history,
            addTracksToPlaylist = addUc,
            createPlaylistUseCase = mock<CreatePlaylistUseCase>(),
            selectionStateHolder = holder,
            observePlaylists = observePlaylists,
        )
    }

    @Test
    fun toggleSelectionUpdatesSelectedIdsAndGlobalSelectionFlag() {
        val holder = SelectionStateHolder()
        val vm = makeVm(holder = holder)

        vm.toggleSelection(track(1L))

        assertEquals(setOf(1L), vm.selectedIds.value)
        assertTrue(holder.isActive.value)

        vm.clearSelection()

        assertTrue(vm.selectedIds.value.isEmpty())
        assertFalse(holder.isActive.value)
    }

    @Test
    fun addSelectedDelegatesAndClearsSelection() = runTest {
        val addUc = mock<AddTracksToPlaylistUseCase>()
        val vm = makeVm(addUc = addUc)
        val tracks = listOf(track(10L), track(20L))
        tracks.forEach(vm::toggleSelection)

        val added = vm.addSelectedToPlaylist(playlistId = 5L)

        assertEquals(2, added)
        verify(addUc).invoke(eq(5L), eq(tracks))
        assertTrue(vm.selectedIds.value.isEmpty())
    }

    @Test
    fun addSelectedWithoutSelectionIsNoop() = runTest {
        val addUc = mock<AddTracksToPlaylistUseCase>()
        val vm = makeVm(addUc = addUc)

        val added = vm.addSelectedToPlaylist(playlistId = 5L)

        assertEquals(0, added)
        verifyNoInteractions(addUc)
    }

    @Test
    fun markSelectedAsAudiobooksAddsOverridesAndClearsSelection() = runTest {
        val prefs = mock<UserPreferencesStore> {
            on { observeLibrarySort() } doReturn flowOf(SortOrder.DATE_ADDED_DESC)
            on { observeAudiobookOverrides() } doReturn flowOf(emptySet())
        }
        val vm = makeVm(prefs = prefs)
        vm.toggleSelection(track(1L))
        vm.toggleSelection(track(2L))

        val marked = vm.markSelectedAsAudiobooks()

        assertEquals(2, marked)
        verify(prefs).addAudiobookOverrides(eq(setOf(1L, 2L)))
        assertTrue(vm.selectedIds.value.isEmpty())
    }
}
