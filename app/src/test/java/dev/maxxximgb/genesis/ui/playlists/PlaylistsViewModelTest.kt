package dev.maxxximgb.genesis.ui.playlists

import app.cash.turbine.test
import dev.maxxximgb.genesis.domain.model.Playlist
import dev.maxxximgb.genesis.domain.usecase.playlist.CreatePlaylistUseCase
import dev.maxxximgb.genesis.domain.usecase.playlist.DeletePlaylistUseCase
import dev.maxxximgb.genesis.domain.usecase.playlist.ObservePlaylistsUseCase
import dev.maxxximgb.genesis.domain.usecase.playlist.RenamePlaylistUseCase
import dev.maxxximgb.genesis.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class PlaylistsViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val sample = listOf(
        Playlist(id = 1L, name = "First", createdAt = 1L),
        Playlist(id = 2L, name = "Second", createdAt = 2L),
    )

    @Test
    fun emitsContentWhenObserveEmits() = runTest {
        val flow = MutableStateFlow<List<Playlist>>(emptyList())
        val observe = mock<ObservePlaylistsUseCase> { on { invoke() } doReturn flow }
        val vm = PlaylistsViewModel(
            observe,
            mock<CreatePlaylistUseCase>(),
            mock<RenamePlaylistUseCase>(),
            mock<DeletePlaylistUseCase>(),
        )

        vm.uiState.test {
            // With UnconfinedTestDispatcher the initial Loading value may
            // collapse with the first upstream emission. Accept either.
            val first = awaitItem()
            assertTrue(
                first is PlaylistsUiState.Loading || first == PlaylistsUiState.Content(emptyList()),
            )
            if (first is PlaylistsUiState.Loading) {
                assertEquals(PlaylistsUiState.Content(emptyList()), awaitItem())
            }

            flow.value = sample
            assertEquals(PlaylistsUiState.Content(sample), awaitItem())

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun createDelegatesToUseCase() = runTest {
        val observe = mock<ObservePlaylistsUseCase> {
            on { invoke() } doReturn MutableStateFlow(emptyList())
        }
        val create = mock<CreatePlaylistUseCase>()
        val vm = PlaylistsViewModel(
            observe, create, mock<RenamePlaylistUseCase>(), mock<DeletePlaylistUseCase>(),
        )

        vm.create("My Playlist")

        verify(create).invoke(eq("My Playlist"))
    }

    @Test
    fun renameDelegatesToUseCase() = runTest {
        val observe = mock<ObservePlaylistsUseCase> {
            on { invoke() } doReturn MutableStateFlow(emptyList())
        }
        val rename = mock<RenamePlaylistUseCase>()
        val vm = PlaylistsViewModel(
            observe, mock<CreatePlaylistUseCase>(), rename, mock<DeletePlaylistUseCase>(),
        )

        vm.rename(7L, "New name")

        verify(rename).invoke(eq(7L), eq("New name"))
    }

    @Test
    fun deleteDelegatesToUseCase() = runTest {
        val observe = mock<ObservePlaylistsUseCase> {
            on { invoke() } doReturn MutableStateFlow(emptyList())
        }
        val delete = mock<DeletePlaylistUseCase>()
        val vm = PlaylistsViewModel(
            observe, mock<CreatePlaylistUseCase>(), mock<RenamePlaylistUseCase>(), delete,
        )

        vm.delete(42L)

        verify(delete).invoke(eq(42L))
    }
}
