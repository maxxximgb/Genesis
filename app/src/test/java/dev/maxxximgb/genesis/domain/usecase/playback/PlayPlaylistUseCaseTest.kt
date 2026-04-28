package dev.maxxximgb.genesis.domain.usecase.playback

import dev.maxxximgb.genesis.domain.model.Track
import dev.maxxximgb.genesis.domain.playback.PlaybackController
import dev.maxxximgb.genesis.domain.repository.PlaylistRepository
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

    @Test
    fun fetchesTracksAndDelegatesToController() = runTest {
        val tracks = listOf(track(1L), track(2L), track(3L))
        val repo = mock<PlaylistRepository> {
            onBlocking { getTracksForPlaylist(42L) } doReturn tracks
        }
        val controller = mock<PlaybackController>()
        val useCase = PlayPlaylistUseCase(repo, controller)

        useCase(playlistId = 42L, startIndex = 1)

        verify(controller).playQueue(eq(42L), eq(tracks), eq(1))
    }

    @Test
    fun emptyPlaylistIsNoop() = runTest {
        val repo = mock<PlaylistRepository> {
            onBlocking { getTracksForPlaylist(99L) } doReturn emptyList()
        }
        val controller = mock<PlaybackController>()
        val useCase = PlayPlaylistUseCase(repo, controller)

        useCase(playlistId = 99L, startIndex = 0)

        verifyNoInteractions(controller)
    }
}
