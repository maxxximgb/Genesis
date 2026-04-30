package dev.maxxximgb.genesis.ui.library

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import app.cash.turbine.test
import dev.maxxximgb.genesis.data.playback.PlaybackStateStore
import dev.maxxximgb.genesis.data.preferences.SearchHistoryStore
import dev.maxxximgb.genesis.data.preferences.UserPreferencesStore
import dev.maxxximgb.genesis.domain.model.PlaybackState
import dev.maxxximgb.genesis.domain.model.Playlist
import dev.maxxximgb.genesis.domain.model.SortOrder
import dev.maxxximgb.genesis.domain.model.Track
import dev.maxxximgb.genesis.domain.usecase.library.ObserveAlbumsUseCase
import dev.maxxximgb.genesis.domain.usecase.library.ObserveArtistsUseCase
import dev.maxxximgb.genesis.domain.usecase.library.ObserveFoldersUseCase
import dev.maxxximgb.genesis.domain.usecase.library.SearchLibraryUseCase
import dev.maxxximgb.genesis.domain.usecase.playlist.AddTracksToPlaylistUseCase
import dev.maxxximgb.genesis.domain.usecase.playlist.ObservePlaylistsUseCase
import dev.maxxximgb.genesis.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelSelectionTest {

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

    private fun makeVm(
        addUc: AddTracksToPlaylistUseCase = mock(),
    ): LibraryViewModel {
        val sortFlow = MutableStateFlow(SortOrder.DATE_ADDED_DESC)
        val prefs = mock<UserPreferencesStore> {
            on { observeLibrarySort() } doReturn sortFlow
            on { observeAlbumsLayout() } doReturn flowOf(AlbumsLayout.LIST)
        }
        val pager = emptyPager()
        val search = mock<SearchLibraryUseCase> { on { invoke(any(), any()) } doReturn pager }
        val observePlaylists = mock<ObservePlaylistsUseCase> {
            on { invoke() } doReturn flowOf(emptyList())
        }
        val playback = mock<PlaybackStateStore> { on { flow } doReturn flowOf(PlaybackState()) }
        val searchHistory = mock<SearchHistoryStore> { on { observeRecent() } doReturn flowOf(emptyList()) }
        val observeAlbums = mock<ObserveAlbumsUseCase> { on { invoke() } doReturn flowOf(emptyList()) }
        val observeArtists = mock<ObserveArtistsUseCase> { on { invoke() } doReturn flowOf(emptyList()) }
        val observeFolders = mock<ObserveFoldersUseCase> { on { invoke() } doReturn flowOf(emptyList()) }
        return LibraryViewModel(
            search, prefs, addUc, mock(), searchHistory, playback,
            observePlaylists, observeAlbums, observeArtists, observeFolders,
        )
    }

    private fun track(id: Long) = Track(
        mediaStoreId = id, title = "T$id", artist = null, album = null,
        albumId = null, durationMs = 1L, contentUri = "u://$id", dateAdded = id,
    )

    @Test
    fun toggleSelectionAddsAndRemovesIds() = runTest {
        val vm = makeVm()
        vm.uiState.test {
            assertTrue(awaitItem().selectedIds.isEmpty())

            vm.toggleSelection(track(1L))
            assertEquals(setOf(1L), awaitItem().selectedIds)

            vm.toggleSelection(track(2L))
            assertEquals(setOf(1L, 2L), awaitItem().selectedIds)

            vm.toggleSelection(track(1L))
            assertEquals(setOf(2L), awaitItem().selectedIds)

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun selectionModeIsTrueOnlyWhenSelectionNonEmpty() = runTest {
        val vm = makeVm()

        vm.uiState.test {
            assertFalse(awaitItem().selectionMode)
            vm.toggleSelection(track(1L))
            assertTrue(awaitItem().selectionMode)
            vm.clearSelection()
            assertFalse(awaitItem().selectionMode)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun addSelectedDelegatesToUseCaseAndClears() = runTest {
        val addUc = mock<AddTracksToPlaylistUseCase>()
        val vm = makeVm(addUc = addUc)

        val tracks = listOf(track(10L), track(20L))
        tracks.forEach { vm.toggleSelection(it) }

        val added = vm.addSelectedToPlaylist(playlistId = 5L)

        assertEquals(2, added)
        verify(addUc).invoke(eq(5L), eq(tracks))
        assertTrue(vm.uiState.value.selectedIds.isEmpty())
    }

    @Test
    fun addSelectedWithoutSelectionIsNoop() = runTest {
        val addUc = mock<AddTracksToPlaylistUseCase>()
        val vm = makeVm(addUc = addUc)

        val added = vm.addSelectedToPlaylist(playlistId = 5L)

        assertEquals(0, added)
        org.mockito.kotlin.verifyNoInteractions(addUc)
    }
}
