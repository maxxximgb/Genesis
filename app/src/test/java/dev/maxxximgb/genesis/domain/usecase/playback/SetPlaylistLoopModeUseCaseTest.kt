package dev.maxxximgb.genesis.domain.usecase.playback

import dev.maxxximgb.genesis.data.preferences.PlaylistModeStore
import dev.maxxximgb.genesis.domain.model.LoopState
import dev.maxxximgb.genesis.domain.model.PlaybackState
import dev.maxxximgb.genesis.domain.model.RepeatMode
import dev.maxxximgb.genesis.domain.playback.PlaybackController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

class SetPlaylistLoopModeUseCaseTest {

    private fun controllerWith(playlistId: Long?): PlaybackController = mock {
        on { state } doReturn MutableStateFlow(PlaybackState(playlistId = playlistId))
    }

    @Test
    fun whenForeignPlaylistPlayingOnlyStoreIsUpdated() = runTest {
        val store = mock<PlaylistModeStore>()
        val controller = controllerWith(playlistId = 99L)
        val useCase = SetPlaylistLoopModeUseCase(store, controller)

        useCase(playlistId = 1L, mode = LoopState.SHUFFLE)

        verify(store).setMode(eq(1L), eq(LoopState.SHUFFLE))
        verify(controller, never()).setShuffleEnabled(any())
        verify(controller, never()).setRepeatMode(any())
    }

    @Test
    fun whenNothingPlayingOnlyStoreIsUpdated() = runTest {
        val store = mock<PlaylistModeStore>()
        val controller = controllerWith(playlistId = null)
        val useCase = SetPlaylistLoopModeUseCase(store, controller)

        useCase(playlistId = 1L, mode = LoopState.REPEAT_ONE)

        verify(store).setMode(eq(1L), eq(LoopState.REPEAT_ONE))
        verify(controller, never()).setShuffleEnabled(any())
        verify(controller, never()).setRepeatMode(any())
    }

    @Test
    fun whenOwnPlaylistPlayingBothStoreAndControllerAreUpdated() = runTest {
        val store = mock<PlaylistModeStore>()
        val controller = controllerWith(playlistId = 5L)
        val useCase = SetPlaylistLoopModeUseCase(store, controller)

        useCase(playlistId = 5L, mode = LoopState.REPEAT_ONE)

        verify(store).setMode(eq(5L), eq(LoopState.REPEAT_ONE))
        verify(controller).setShuffleEnabled(false)
        verify(controller).setRepeatMode(RepeatMode.ONE)
    }

    @Test
    fun shuffleModeAppliesShuffleTrueWhenOwnPlaying() = runTest {
        val store = mock<PlaylistModeStore>()
        val controller = controllerWith(playlistId = 5L)
        val useCase = SetPlaylistLoopModeUseCase(store, controller)

        useCase(playlistId = 5L, mode = LoopState.SHUFFLE)

        verify(controller).setShuffleEnabled(true)
        verify(controller).setRepeatMode(RepeatMode.OFF)
    }
}
