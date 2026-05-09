package dev.maxxximgb.genesis.ui.nowPlaying

import dev.maxxximgb.genesis.data.audiofx.AudioFxController
import dev.maxxximgb.genesis.data.preferences.UserPreferencesStore
import dev.maxxximgb.genesis.domain.audiofx.AudioFxCapabilities
import dev.maxxximgb.genesis.domain.audiofx.AudioFxState
import dev.maxxximgb.genesis.domain.model.LoopState
import dev.maxxximgb.genesis.domain.model.PlaybackState
import dev.maxxximgb.genesis.domain.model.RepeatMode
import dev.maxxximgb.genesis.domain.model.Track
import dev.maxxximgb.genesis.domain.playback.PlaybackController
import dev.maxxximgb.genesis.domain.repository.MediaLibraryRepository
import dev.maxxximgb.genesis.domain.usecase.playback.SetPlaylistLoopModeUseCase
import dev.maxxximgb.genesis.ui.library.SelectionStateHolder
import app.cash.turbine.test
import dev.maxxximgb.genesis.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class NowPlayingViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private fun track(id: Long) = Track(
        mediaStoreId = id,
        title = "T$id",
        artist = "A$id",
        album = null,
        albumId = null,
        durationMs = 100_000L,
        contentUri = "uri/$id",
        dateAdded = id,
    )

    private fun controllerWith(state: PlaybackState): Pair<PlaybackController, MutableStateFlow<PlaybackState>> {
        val flow = MutableStateFlow(state)
        val c = mock<PlaybackController> {
            on { this.state } doReturn flow
        }
        return c to flow
    }

    private fun stubPrefs(): UserPreferencesStore = mock {
        on { observeAudioFxState() } doReturn flowOf(AudioFxState.Default)
    }

    private fun stubAudioFx(): AudioFxController = mock {
        on { capabilities } doReturn MutableStateFlow(AudioFxCapabilities.Unsupported)
    }

    private fun stubLoopUseCase(): SetPlaylistLoopModeUseCase = mock()

    @Test
    fun cycleLoopTraversesAllFourStates() = runTest {
        val (controller, flow) = controllerWith(
            PlaybackState(repeatMode = RepeatMode.OFF, shuffleEnabled = false),
        )
        val repo = mock<MediaLibraryRepository>()
        val vm = NowPlayingViewModel(
            controller,
            repo,
            stubPrefs(),
            stubLoopUseCase(),
            stubAudioFx(),
            SelectionStateHolder(),
        )

        // OFF → REPEAT_ALL: repeat=ALL, shuffle=false
        vm.cycleLoop(); advanceUntilIdle()
        verify(controller).setRepeatMode(eq(RepeatMode.ALL))
        verify(controller).setShuffleEnabled(eq(false))

        // REPEAT_ALL → REPEAT_ONE
        flow.value = flow.value.copy(repeatMode = RepeatMode.ALL, shuffleEnabled = false)
        vm.cycleLoop(); advanceUntilIdle()
        verify(controller).setRepeatMode(eq(RepeatMode.ONE))

        // REPEAT_ONE → SHUFFLE: repeat=OFF, shuffle=true
        flow.value = flow.value.copy(repeatMode = RepeatMode.ONE, shuffleEnabled = false)
        vm.cycleLoop(); advanceUntilIdle()
        verify(controller).setShuffleEnabled(eq(true))

        // SHUFFLE → OFF: repeat=OFF, shuffle=false
        flow.value = flow.value.copy(repeatMode = RepeatMode.OFF, shuffleEnabled = true)
        vm.cycleLoop(); advanceUntilIdle()
        // verifies the call happened — Mockito tracks all setShuffleEnabled invocations.
        org.mockito.kotlin.verify(controller, org.mockito.kotlin.atLeast(2))
            .setShuffleEnabled(eq(false))
    }

    @Test
    fun cycleLoopForPlaylistPersistsThroughPlaylistModeUseCase() = runTest {
        val (controller, _) = controllerWith(
            PlaybackState(
                playlistId = 42L,
                repeatMode = RepeatMode.OFF,
                shuffleEnabled = false,
            ),
        )
        val repo = mock<MediaLibraryRepository>()
        val loopUseCase = stubLoopUseCase()
        val vm = NowPlayingViewModel(
            controller,
            repo,
            stubPrefs(),
            loopUseCase,
            stubAudioFx(),
            SelectionStateHolder(),
        )

        vm.cycleLoop()
        advanceUntilIdle()

        verify(loopUseCase).invoke(eq(42L), eq(LoopState.REPEAT_ALL))
    }

    @Test
    fun seekToForwardsPosition() = runTest {
        val (controller, _) = controllerWith(PlaybackState())
        val repo = mock<MediaLibraryRepository>()
        val vm = NowPlayingViewModel(
            controller,
            repo,
            stubPrefs(),
            stubLoopUseCase(),
            stubAudioFx(),
            SelectionStateHolder(),
        )

        vm.seekTo(42_000L)
        advanceUntilIdle()

        verify(controller).seekTo(eq(42_000L))
    }

    @Test
    fun jumpToQueueIndexForwardsIndex() = runTest {
        val (controller, _) = controllerWith(PlaybackState())
        val repo = mock<MediaLibraryRepository>()
        val vm = NowPlayingViewModel(
            controller,
            repo,
            stubPrefs(),
            stubLoopUseCase(),
            stubAudioFx(),
            SelectionStateHolder(),
        )

        vm.jumpToQueueIndex(3)
        advanceUntilIdle()

        verify(controller).seekToQueueIndex(eq(3))
    }

    @Test
    fun queueTracksResolvesByIdsInOrder() = runTest {
        val ids = listOf(10L, 20L, 30L)
        val (controller, _) = controllerWith(PlaybackState(queue = ids))
        val repo = mock<MediaLibraryRepository> {
            onBlocking { getTracksByIds(ids) } doReturn ids.map(::track)
        }
        val vm = NowPlayingViewModel(
            controller,
            repo,
            stubPrefs(),
            stubLoopUseCase(),
            stubAudioFx(),
            SelectionStateHolder(),
        )

        vm.queueTracks.test {
            // Initial empty + the resolved list. Skip until non-empty.
            var emission = awaitItem()
            while (emission.isEmpty()) emission = awaitItem()
            assertEquals(ids, emission.map { it.mediaStoreId })
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun setSleepTimerStartsCountdown() {
        // Don't advanceUntilIdle: SleepTimer's countdown loop uses real-time `System.currentTimeMillis`,
        // which never advances under TestScope, so the loop would spin forever. With
        // UnconfinedTestDispatcher the launched coroutine runs synchronously until its first
        // delay() and overwrites _remainingMs with deadline - now() — usually a few ms below
        // the requested duration. Assert a range, not exact equality.
        val (controller, _) = controllerWith(PlaybackState())
        val repo = mock<MediaLibraryRepository>()
        val vm = NowPlayingViewModel(
            controller,
            repo,
            stubPrefs(),
            stubLoopUseCase(),
            stubAudioFx(),
            SelectionStateHolder(),
        )

        vm.setSleepTimerMinutes(15)
        val remaining = vm.sleepTimerRemainingMs.value
        assertNotNull(remaining)
        assertTrue("remaining was $remaining", remaining!! in 899_000L..900_000L)
        vm.cancelSleepTimer()
    }

    @Test
    fun cancelSleepTimerClearsRemaining() {
        val (controller, _) = controllerWith(PlaybackState())
        val repo = mock<MediaLibraryRepository>()
        val vm = NowPlayingViewModel(
            controller,
            repo,
            stubPrefs(),
            stubLoopUseCase(),
            stubAudioFx(),
            SelectionStateHolder(),
        )

        vm.setSleepTimerMinutes(15)
        vm.cancelSleepTimer()
        // SleepTimer.cancel() clears _remainingMs synchronously.
        assertEquals(null, vm.sleepTimerRemainingMs.value)
    }
}
