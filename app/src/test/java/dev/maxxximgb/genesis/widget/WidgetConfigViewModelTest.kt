package dev.maxxximgb.genesis.widget

import app.cash.turbine.test
import dev.maxxximgb.genesis.data.preferences.WidgetPreferencesStore
import dev.maxxximgb.genesis.domain.model.Playlist
import dev.maxxximgb.genesis.domain.usecase.playlist.ObservePlaylistsUseCase
import dev.maxxximgb.genesis.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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
class WidgetConfigViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private fun playlist(id: Long, name: String) = Playlist(id = id, name = name, createdAt = id)

    @Test
    fun playlistsExposesUseCaseStream() = runTest {
        val rows = listOf(playlist(1L, "Rock"), playlist(2L, "Chill"))
        val useCase = mock<ObservePlaylistsUseCase> {
            on { invoke() } doReturn flowOf(rows)
        }
        val prefs = mock<WidgetPreferencesStore>()
        val updater = mock<WidgetUpdater>()
        val vm = WidgetConfigViewModel(useCase, prefs, updater)

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
    fun bindPersistsToStoreAndInvokesCallback() = runTest {
        val useCase = mock<ObservePlaylistsUseCase> {
            on { invoke() } doReturn flowOf(emptyList())
        }
        val prefs = mock<WidgetPreferencesStore>()
        val updater = mock<WidgetUpdater>()
        val vm = WidgetConfigViewModel(useCase, prefs, updater)

        var called = false
        vm.bind(appWidgetId = 42, playlistId = 7L) { called = true }

        verify(prefs).setPlaylistFor(eq(42), eq(7L))
        verify(updater).updateWhenBound(eq(42))
        assertTrue(called)
    }

    @Test
    fun bindIgnoresSecondCallWhileBindingInFlight() = runTest {
        val useCase = mock<ObservePlaylistsUseCase> {
            on { invoke() } doReturn flowOf(emptyList())
        }
        val prefs = mock<WidgetPreferencesStore>()
        val updater = mock<WidgetUpdater>()
        val vm = WidgetConfigViewModel(useCase, prefs, updater)

        var firstCalled = 0
        var secondCalled = 0
        vm.bind(appWidgetId = 1, playlistId = 1L) { firstCalled++ }
        vm.bind(appWidgetId = 1, playlistId = 2L) { secondCalled++ }

        assertEquals(1, firstCalled)
        assertEquals(0, secondCalled)
        verify(prefs).setPlaylistFor(eq(1), eq(1L))
        org.mockito.kotlin.verify(prefs, org.mockito.kotlin.never()).setPlaylistFor(eq(1), eq(2L))
    }
}
