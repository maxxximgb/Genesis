package dev.maxxximgb.genesis.domain.usecase.playback

import dev.maxxximgb.genesis.data.preferences.PlaylistModeStore
import dev.maxxximgb.genesis.domain.model.LoopState
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions

class SetPlaylistLoopModeUseCaseTest {

    @Test
    fun writesModeToStore() = runTest {
        val store = mock<PlaylistModeStore>()
        val useCase = SetPlaylistLoopModeUseCase(store)

        useCase(playlistId = 1L, mode = LoopState.SHUFFLE)

        verify(store).setMode(eq(1L), eq(LoopState.SHUFFLE))
        verifyNoMoreInteractions(store)
    }

    @Test
    fun differentModesAreForwardedToStoreVerbatim() = runTest {
        val store = mock<PlaylistModeStore>()
        val useCase = SetPlaylistLoopModeUseCase(store)

        useCase(playlistId = 5L, mode = LoopState.REPEAT_ONE)
        useCase(playlistId = 5L, mode = LoopState.OFF)
        useCase(playlistId = 7L, mode = LoopState.REPEAT_ALL)

        verify(store).setMode(eq(5L), eq(LoopState.REPEAT_ONE))
        verify(store).setMode(eq(5L), eq(LoopState.OFF))
        verify(store).setMode(eq(7L), eq(LoopState.REPEAT_ALL))
        verifyNoMoreInteractions(store)
    }
}
