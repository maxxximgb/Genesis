package dev.maxxximgb.genesis.domain.usecase.playback

import dev.maxxximgb.genesis.data.preferences.BookmarkStore
import dev.maxxximgb.genesis.data.preferences.PlaylistModeStore
import dev.maxxximgb.genesis.domain.model.LoopState
import dev.maxxximgb.genesis.domain.model.PlaybackState
import dev.maxxximgb.genesis.domain.model.RepeatMode
import dev.maxxximgb.genesis.domain.model.Track
import dev.maxxximgb.genesis.domain.playback.PlaybackController
import dev.maxxximgb.genesis.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

class PlayPlaylistUseCaseTest {

    private fun track(id: Long) = Track(
        mediaStoreId = id,
        title = "T$id",
        artist = null,
        album = null,
        albumId = null,
        durationMs = 1000L,
        contentUri = "u://$id",
        dateAdded = id,
    )

    private fun controllerWithIdleState(): PlaybackController = mock {
        on { state } doReturn MutableStateFlow(PlaybackState())
    }

    @Test
    fun fetchesTracksAndDelegatesToController() = runTest {
        val tracks = listOf(track(1L), track(2L), track(3L))
        val repo = mock<PlaylistRepository> {
            onBlocking { getTracksForPlaylist(42L) } doReturn tracks
        }
        val controller = controllerWithIdleState()
        val modeStore = mock<PlaylistModeStore> {
            onBlocking { getMode(42L) } doReturn LoopState.OFF
        }
        val useCase = PlayPlaylistUseCase(repo, controller, modeStore, mock<BookmarkStore>())

        useCase(playlistId = 42L, explicitStartIndex = 1)

        verify(controller).playQueue(eq(42L), eq(tracks), eq(1), eq(0L))
    }

    @Test
    fun appliesPersistedShuffleAndRepeatBeforePlay() = runTest {
        val tracks = listOf(track(1L))
        val repo = mock<PlaylistRepository> {
            onBlocking { getTracksForPlaylist(7L) } doReturn tracks
        }
        val controller = controllerWithIdleState()
        val modeStore = mock<PlaylistModeStore> {
            onBlocking { getMode(7L) } doReturn LoopState.REPEAT_ONE
        }
        val useCase = PlayPlaylistUseCase(repo, controller, modeStore, mock<BookmarkStore>())

        useCase(playlistId = 7L)

        verify(controller).setShuffleEnabled(false)
        verify(controller).setRepeatMode(RepeatMode.ONE)
        verify(controller).playQueue(eq(7L), eq(tracks), eq(0), eq(0L))
    }

    @Test
    fun shuffleModeAppliesShuffleTrue() = runTest {
        val tracks = listOf(track(1L))
        val repo = mock<PlaylistRepository> {
            onBlocking { getTracksForPlaylist(8L) } doReturn tracks
        }
        val controller = controllerWithIdleState()
        val modeStore = mock<PlaylistModeStore> {
            onBlocking { getMode(8L) } doReturn LoopState.SHUFFLE
        }
        val useCase = PlayPlaylistUseCase(repo, controller, modeStore, mock<BookmarkStore>())

        useCase(playlistId = 8L)

        verify(controller).setShuffleEnabled(true)
        verify(controller).setRepeatMode(RepeatMode.OFF)
    }

    @Test
    fun emptyPlaylistIsNoop() = runTest {
        val repo = mock<PlaylistRepository> {
            onBlocking { getTracksForPlaylist(99L) } doReturn emptyList()
        }
        val controller = mock<PlaybackController>()
        val modeStore = mock<PlaylistModeStore>()
        val useCase = PlayPlaylistUseCase(repo, controller, modeStore, mock<BookmarkStore>())

        useCase(playlistId = 99L, explicitStartIndex = 0)

        verifyNoInteractions(controller)
    }
}
