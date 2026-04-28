package dev.maxxximgb.genesis.ui.playlistDetail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import dev.maxxximgb.genesis.domain.model.Playlist
import dev.maxxximgb.genesis.domain.model.PlaylistDetail
import dev.maxxximgb.genesis.domain.model.Track
import dev.maxxximgb.genesis.domain.usecase.playback.PlayPlaylistUseCase
import dev.maxxximgb.genesis.domain.usecase.playlist.AddTracksToPlaylistUseCase
import dev.maxxximgb.genesis.domain.usecase.playlist.ObservePlaylistDetailUseCase
import dev.maxxximgb.genesis.domain.usecase.playlist.RemoveTracksFromPlaylistUseCase
import dev.maxxximgb.genesis.domain.usecase.playlist.ReorderPlaylistTracksUseCase
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
class PlaylistDetailViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private fun track(id: Long) = Track(
        mediaStoreId = id, title = "T$id", artist = null, album = null,
        albumId = null, durationMs = 1L, contentUri = "u://$id", dateAdded = id,
    )

    private fun savedStateWithPlaylistId(id: Long) = SavedStateHandle(mapOf("playlistId" to id))

    @Test
    fun emitsContentWhenDetailNonNull() = runTest {
        val detail = PlaylistDetail(
            playlist = Playlist(id = 7L, name = "P", createdAt = 1L),
            tracks = listOf(track(1L), track(2L)),
        )
        val flow = MutableStateFlow<PlaylistDetail?>(null)
        val observe = mock<ObservePlaylistDetailUseCase> { on { invoke(7L) } doReturn flow }

        val vm = PlaylistDetailViewModel(
            savedStateWithPlaylistId(7L), observe,
            mock<AddTracksToPlaylistUseCase>(),
            mock<RemoveTracksFromPlaylistUseCase>(),
            mock<ReorderPlaylistTracksUseCase>(),
            mock<PlayPlaylistUseCase>(),
        )

        vm.uiState.test {
            // With UnconfinedTestDispatcher Loading collapses with the
            // first upstream emission. Accept either initial item.
            val first = awaitItem()
            assertTrue(
                first is PlaylistDetailUiState.Loading || first == PlaylistDetailUiState.NotFound,
            )
            if (first is PlaylistDetailUiState.Loading) {
                assertEquals(PlaylistDetailUiState.NotFound, awaitItem())
            }

            flow.value = detail
            assertEquals(PlaylistDetailUiState.Content(detail), awaitItem())

            flow.value = null
            assertEquals(PlaylistDetailUiState.NotFound, awaitItem())

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun playDelegatesToUseCaseWithCorrectArgs() = runTest {
        val observe = mock<ObservePlaylistDetailUseCase> {
            on { invoke(11L) } doReturn MutableStateFlow(null)
        }
        val play = mock<PlayPlaylistUseCase>()
        val vm = PlaylistDetailViewModel(
            savedStateWithPlaylistId(11L), observe,
            mock<AddTracksToPlaylistUseCase>(),
            mock<RemoveTracksFromPlaylistUseCase>(),
            mock<ReorderPlaylistTracksUseCase>(),
            play,
        )

        vm.play(startIndex = 3)

        verify(play).invoke(eq(11L), eq(3))
    }

    @Test
    fun addRemoveAndReorderDelegateToCorrectUseCases() = runTest {
        val observe = mock<ObservePlaylistDetailUseCase> {
            on { invoke(99L) } doReturn MutableStateFlow(null)
        }
        val addUc = mock<AddTracksToPlaylistUseCase>()
        val removeUc = mock<RemoveTracksFromPlaylistUseCase>()
        val reorderUc = mock<ReorderPlaylistTracksUseCase>()
        val vm = PlaylistDetailViewModel(
            savedStateWithPlaylistId(99L), observe,
            addUc, removeUc, reorderUc, mock<PlayPlaylistUseCase>(),
        )

        val tracks = listOf(track(1L))
        vm.addTracks(tracks)
        verify(addUc).invoke(eq(99L), eq(tracks))
        // removeSelected / moveSelected are tested via ComputeMoveTest +
        // PlaylistDetailViewModelMoveTest; selection-based API doesn't accept
        // explicit lists anymore.
    }
}
