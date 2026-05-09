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
import dev.maxxximgb.genesis.domain.model.SortOrder
import dev.maxxximgb.genesis.domain.model.Track
import dev.maxxximgb.genesis.domain.usecase.library.ObserveAlbumsUseCase
import dev.maxxximgb.genesis.domain.usecase.library.ObserveArtistsUseCase
import dev.maxxximgb.genesis.domain.usecase.library.ObserveFoldersUseCase
import dev.maxxximgb.genesis.domain.usecase.library.SearchLibraryUseCase
import dev.maxxximgb.genesis.domain.playback.PlaybackController
import dev.maxxximgb.genesis.domain.usecase.playlist.AddTracksToPlaylistUseCase
import dev.maxxximgb.genesis.domain.usecase.playlist.CreatePlaylistUseCase
import dev.maxxximgb.genesis.domain.usecase.playlist.DeletePlaylistUseCase
import dev.maxxximgb.genesis.domain.usecase.playlist.GetPlaylistTracksUseCase
import dev.maxxximgb.genesis.domain.repository.MediaLibraryRepository
import dev.maxxximgb.genesis.domain.usecase.playlist.ObservePlaylistSummariesUseCase
import dev.maxxximgb.genesis.domain.usecase.playlist.ObservePlaylistsUseCase
import dev.maxxximgb.genesis.domain.usecase.playlist.RenamePlaylistUseCase
import dev.maxxximgb.genesis.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {

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

    @Test
    fun uiStateExposesCurrentSearchQueryAndSort() = runTest {
        val sortFlow = MutableStateFlow(SortOrder.DATE_ADDED_DESC)
        val prefs = mock<UserPreferencesStore> {
            on { observeLibrarySort() } doReturn sortFlow
            on { observeAlbumsLayout() } doReturn kotlinx.coroutines.flow.flowOf(AlbumsLayout.LIST)
            on { observeAudiobookOverrides() } doReturn kotlinx.coroutines.flow.flowOf(emptySet())
        }
        val pager = emptyPager()
        val searchUseCase = mock<SearchLibraryUseCase> {
            on { invoke(any(), any()) } doReturn pager
        }
        val vm = LibraryViewModel(
            searchUseCase,
            prefs,
            mock<AddTracksToPlaylistUseCase>(),
            mock<CreatePlaylistUseCase>(),
            mock<RenamePlaylistUseCase>(),
            mock<DeletePlaylistUseCase>(),
            mock<GetPlaylistTracksUseCase>(),
            mock<SearchHistoryStore> {
                on { observeRecent() } doReturn kotlinx.coroutines.flow.flowOf(emptyList())
            },
            mock<MediaLibraryRepository> {
                on { pagedAudiobooks(any(), any()) } doReturn pager
            },
            SelectionStateHolder(),
            mock<PlaybackController>(),
            mock<PlaybackStateStore> {
                on { flow } doReturn kotlinx.coroutines.flow.flowOf(PlaybackState())
            },
            mock<ObservePlaylistsUseCase> {
                on { invoke() } doReturn kotlinx.coroutines.flow.flowOf(emptyList())
            },
            mock<ObservePlaylistSummariesUseCase> {
                on { invoke() } doReturn kotlinx.coroutines.flow.flowOf(emptyList())
            },
            mock<ObserveAlbumsUseCase> {
                on { invoke() } doReturn kotlinx.coroutines.flow.flowOf(emptyList())
            },
            mock<ObserveArtistsUseCase> {
                on { invoke() } doReturn kotlinx.coroutines.flow.flowOf(emptyList())
            },
            mock<ObserveFoldersUseCase> {
                on { invoke() } doReturn kotlinx.coroutines.flow.flowOf(emptyList())
            },
        )

        vm.uiState.test {
            assertEquals(LibraryUiState("", SortOrder.DATE_ADDED_DESC), awaitItem())

            vm.onSearchQueryChange("abba")
            assertEquals(LibraryUiState("abba", SortOrder.DATE_ADDED_DESC), awaitItem())

            sortFlow.value = SortOrder.TITLE_ASC
            assertEquals(LibraryUiState("abba", SortOrder.TITLE_ASC), awaitItem())

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun onSortChangePersistsViaPreferences() = runTest {
        val sortFlow = MutableStateFlow(SortOrder.DATE_ADDED_DESC)
        val prefs = mock<UserPreferencesStore> {
            on { observeLibrarySort() } doReturn sortFlow
            on { observeAlbumsLayout() } doReturn kotlinx.coroutines.flow.flowOf(AlbumsLayout.LIST)
            on { observeAudiobookOverrides() } doReturn kotlinx.coroutines.flow.flowOf(emptySet())
        }
        val pager = emptyPager()
        val searchUseCase = mock<SearchLibraryUseCase> {
            on { invoke(any(), any()) } doReturn pager
        }
        val vm = LibraryViewModel(
            searchUseCase,
            prefs,
            mock<AddTracksToPlaylistUseCase>(),
            mock<CreatePlaylistUseCase>(),
            mock<RenamePlaylistUseCase>(),
            mock<DeletePlaylistUseCase>(),
            mock<GetPlaylistTracksUseCase>(),
            mock<SearchHistoryStore> {
                on { observeRecent() } doReturn kotlinx.coroutines.flow.flowOf(emptyList())
            },
            mock<MediaLibraryRepository> {
                on { pagedAudiobooks(any(), any()) } doReturn pager
            },
            SelectionStateHolder(),
            mock<PlaybackController>(),
            mock<PlaybackStateStore> {
                on { flow } doReturn kotlinx.coroutines.flow.flowOf(PlaybackState())
            },
            mock<ObservePlaylistsUseCase> {
                on { invoke() } doReturn kotlinx.coroutines.flow.flowOf(emptyList())
            },
            mock<ObservePlaylistSummariesUseCase> {
                on { invoke() } doReturn kotlinx.coroutines.flow.flowOf(emptyList())
            },
            mock<ObserveAlbumsUseCase> {
                on { invoke() } doReturn kotlinx.coroutines.flow.flowOf(emptyList())
            },
            mock<ObserveArtistsUseCase> {
                on { invoke() } doReturn kotlinx.coroutines.flow.flowOf(emptyList())
            },
            mock<ObserveFoldersUseCase> {
                on { invoke() } doReturn kotlinx.coroutines.flow.flowOf(emptyList())
            },
        )

        vm.onSortChange(SortOrder.ARTIST_ASC)

        verify(prefs).setLibrarySort(SortOrder.ARTIST_ASC)
    }
}
