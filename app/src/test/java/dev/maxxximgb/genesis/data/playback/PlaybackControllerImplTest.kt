package dev.maxxximgb.genesis.data.playback

import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import dev.maxxximgb.genesis.domain.model.PlaybackState
import dev.maxxximgb.genesis.domain.model.RepeatMode
import dev.maxxximgb.genesis.domain.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PlaybackControllerImplTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val sampleTrack = Track(
        mediaStoreId = 42L,
        title = "Song",
        artist = "Artist",
        album = "Album",
        albumId = 7L,
        durationMs = 1000L,
        contentUri = "content://media/external/audio/media/42",
        dateAdded = 1L,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun makeImpl(controller: MediaController): PlaybackControllerImpl {
        val provider = mock<MediaControllerProvider> {
            onBlocking { get() } doReturn controller
        }
        val store = mock<PlaybackStateStore> {
            on { flow } doReturn flowOf(PlaybackState())
        }
        return PlaybackControllerImpl(provider, store)
    }

    @Test
    fun playSingleSetsItemPreparesAndPlays() = runTest(dispatcher) {
        val mc = mock<MediaController>()
        val impl = makeImpl(mc)

        impl.playSingle(sampleTrack)

        val captor = argumentCaptor<List<MediaItem>>()
        verify(mc).setMediaItems(captor.capture())
        verify(mc).prepare()
        verify(mc).play()
        val items = captor.firstValue
        assertEqualsOne(1, items.size)
        assertEqualsLong(42L, items[0].mediaStoreId() ?: -1L)
        // playSingle does not attribute a playlist
        org.junit.Assert.assertNull(items[0].playlistId())
    }

    @Test
    fun playQueueAttributesPlaylistIdToEveryMediaItem() = runTest(dispatcher) {
        val mc = mock<MediaController>()
        val impl = makeImpl(mc)
        val tracks = listOf(sampleTrack, sampleTrack.copy(mediaStoreId = 99L))

        impl.playQueue(playlistId = 5L, tracks = tracks, startIndex = 1)

        val captor = argumentCaptor<List<MediaItem>>()
        verify(mc).setMediaItems(captor.capture(), eq(1), eq(0L))
        verify(mc).prepare()
        verify(mc).play()
        val items = captor.firstValue
        assertEqualsOne(2, items.size)
        org.junit.Assert.assertEquals(5L, items[0].playlistId())
        org.junit.Assert.assertEquals(5L, items[1].playlistId())
    }

    @Test
    fun playQueueWithEmptyListIsNoop() = runTest(dispatcher) {
        val mc = mock<MediaController>()
        val impl = makeImpl(mc)

        impl.playQueue(playlistId = null, tracks = emptyList(), startIndex = 0)

        org.mockito.kotlin.verifyNoInteractions(mc)
    }

    @Test
    fun togglePlayPauseCallsPauseWhenPlaying() = runTest(dispatcher) {
        val mc = mock<MediaController> {
            on { isPlaying } doReturn true
        }
        val impl = makeImpl(mc)

        impl.togglePlayPause()

        verify(mc).pause()
    }

    @Test
    fun togglePlayPauseCallsPlayWhenPaused() = runTest(dispatcher) {
        val mc = mock<MediaController> {
            on { isPlaying } doReturn false
        }
        val impl = makeImpl(mc)

        impl.togglePlayPause()

        verify(mc).play()
    }

    @Test
    fun setRepeatModeForwardsMappedPlayerInt() = runTest(dispatcher) {
        val mc = mock<MediaController>()
        val impl = makeImpl(mc)

        impl.setRepeatMode(RepeatMode.ONE)

        verify(mc).repeatMode = androidx.media3.common.Player.REPEAT_MODE_ONE
    }

    @Test
    fun setShuffleEnabledForwardsToController() = runTest(dispatcher) {
        val mc = mock<MediaController>()
        val impl = makeImpl(mc)

        impl.setShuffleEnabled(true)

        verify(mc).shuffleModeEnabled = true
    }

    @Test
    fun seekToForwardsPosition() = runTest(dispatcher) {
        val mc = mock<MediaController>()
        val impl = makeImpl(mc)

        impl.seekTo(12_345L)

        verify(mc).seekTo(12_345L)
    }

    @Test
    fun seekToNextAndPreviousAreForwarded() = runTest(dispatcher) {
        val mc = mock<MediaController>()
        val impl = makeImpl(mc)

        impl.seekToNext()
        impl.seekToPrevious()

        verify(mc).seekToNext()
        verify(mc).seekToPrevious()
    }

    private fun assertEqualsOne(expected: Int, actual: Int) =
        org.junit.Assert.assertEquals(expected, actual)

    private fun assertEqualsLong(expected: Long, actual: Long) =
        org.junit.Assert.assertEquals(expected, actual)
}
