package dev.maxxximgb.genesis.widget

import app.cash.turbine.test
import dev.maxxximgb.genesis.data.preferences.UserPreferencesStore
import dev.maxxximgb.genesis.data.preferences.WidgetPreferencesStore
import dev.maxxximgb.genesis.domain.model.Playlist
import dev.maxxximgb.genesis.domain.model.SortOrder
import dev.maxxximgb.genesis.domain.repository.MediaLibraryRepository
import dev.maxxximgb.genesis.domain.usecase.playlist.ObservePlaylistsUseCase
import dev.maxxximgb.genesis.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class WidgetConfigViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private fun playlist(id: Long, name: String) = Playlist(id = id, name = name, createdAt = id)

    private fun userPrefsMock(): UserPreferencesStore = mock {
        on { observeLibrarySort() } doReturn flowOf(SortOrder.DATE_ADDED_DESC)
        on { observeAudiobookOverrides() } doReturn flowOf(emptySet())
    }

    private suspend fun mediaRepoMock(): MediaLibraryRepository = mock {
        onBlocking { getAudiobookTracks(any(), any()) } doReturn emptyList()
    }

    @Test
    fun playlistsExposesUseCaseStream() = runTest {
        val rows = listOf(playlist(1L, "Rock"), playlist(2L, "Chill"))
        val useCase = mock<ObservePlaylistsUseCase> {
            on { invoke() } doReturn flowOf(rows)
        }
        val prefs = mock<WidgetPreferencesStore>()
        val updater = mock<WidgetUpdater>()
        val vm = WidgetConfigViewModel(useCase, prefs, userPrefsMock(), updater, mediaRepoMock())

        vm.playlists.test {
            // Initial value emitted by stateIn before upstream collection completes.
            // Under WhileSubscribed, awaitItem() activates the subscription which then collects rows.
            val seen = mutableListOf(awaitItem())
            while (seen.last() != rows) seen += awaitItem()
            assertEquals(rows, seen.last())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun bindPersistsPlaylistTargetAndInvokesCallback() = runTest {
        val useCase = mock<ObservePlaylistsUseCase> {
            on { invoke() } doReturn flowOf(emptyList())
        }
        val prefs = mock<WidgetPreferencesStore>()
        val updater = mock<WidgetUpdater>()
        val vm = WidgetConfigViewModel(useCase, prefs, userPrefsMock(), updater, mediaRepoMock())

        var called = false
        vm.bind(appWidgetId = 42, playlistId = 7L) { called = true }

        verify(prefs).setTarget(eq(42), eq(WidgetTarget.Playlist(7L)))
        verify(updater).updateWhenBound(eq(42))
        assertTrue(called)
    }

    @Test
    fun bindAudiobooksPersistsAudiobookTarget() = runTest {
        val useCase = mock<ObservePlaylistsUseCase> {
            on { invoke() } doReturn flowOf(emptyList())
        }
        val prefs = mock<WidgetPreferencesStore>()
        val updater = mock<WidgetUpdater>()
        val vm = WidgetConfigViewModel(useCase, prefs, userPrefsMock(), updater, mediaRepoMock())

        var called = false
        vm.bindAudiobooks(appWidgetId = 5) { called = true }

        verify(prefs).setTarget(eq(5), eq(WidgetTarget.Audiobooks))
        verify(updater).updateWhenBound(eq(5))
        assertTrue(called)
    }

    @Test
    fun bindIgnoresSecondCallWhileBindingInFlight() = runTest {
        val useCase = mock<ObservePlaylistsUseCase> {
            on { invoke() } doReturn flowOf(emptyList())
        }
        val prefs = mock<WidgetPreferencesStore>()
        val updater = mock<WidgetUpdater>()
        val vm = WidgetConfigViewModel(useCase, prefs, userPrefsMock(), updater, mediaRepoMock())

        var firstCalled = 0
        var secondCalled = 0
        vm.bind(appWidgetId = 1, playlistId = 1L) { firstCalled++ }
        vm.bind(appWidgetId = 1, playlistId = 2L) { secondCalled++ }

        assertEquals(1, firstCalled)
        assertEquals(0, secondCalled)
        verify(prefs).setTarget(eq(1), eq(WidgetTarget.Playlist(1L)))
        verify(prefs, never()).setTarget(eq(1), eq(WidgetTarget.Playlist(2L)))
    }
}
