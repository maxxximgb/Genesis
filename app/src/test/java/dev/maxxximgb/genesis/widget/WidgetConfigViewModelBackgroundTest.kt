package dev.maxxximgb.genesis.widget

import dev.maxxximgb.genesis.data.preferences.WidgetPreferencesStore
import dev.maxxximgb.genesis.domain.usecase.playlist.ObservePlaylistsUseCase
import dev.maxxximgb.genesis.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class WidgetConfigViewModelBackgroundTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    @Test
    fun initialBackgroundStateIsDynamic() {
        val vm = newVm()
        assertEquals(WidgetBackgroundChoice.Dynamic, vm.selectedBackground.value)
    }

    @Test
    fun hydrateBackgroundLoadsStoredChoice() = runTest {
        val stored = WidgetBackgroundChoice.Solid(0xFFE0506B.toInt())
        val prefs = mock<WidgetPreferencesStore> {
            onBlocking { getBackgroundFor(42) } doReturn stored
        }
        val vm = newVm(prefs = prefs)

        vm.hydrateBackground(appWidgetId = 42)
        advanceUntilIdle()

        assertEquals(stored, vm.selectedBackground.value)
    }

    @Test
    fun onBackgroundChosenUpdatesState() {
        val vm = newVm()
        vm.onBackgroundChosen(WidgetBackgroundChoice.Theme)
        assertEquals(WidgetBackgroundChoice.Theme, vm.selectedBackground.value)
    }

    @Test
    fun bindPersistsBothPlaylistAndBackground() = runTest {
        val prefs = mock<WidgetPreferencesStore>()
        val vm = newVm(prefs = prefs)

        val choice = WidgetBackgroundChoice.Solid(0xFF3D6EE0.toInt())
        vm.onBackgroundChosen(choice)
        vm.bind(appWidgetId = 7, playlistId = 99L) { /* committed */ }
        advanceUntilIdle()

        verify(prefs).setPlaylistFor(eq(7), eq(99L))
        verify(prefs).setBackgroundFor(eq(7), eq(choice))
    }

    private fun newVm(prefs: WidgetPreferencesStore = mock()): WidgetConfigViewModel {
        val useCase = mock<ObservePlaylistsUseCase> { on { invoke() } doReturn flowOf(emptyList()) }
        val updater = mock<WidgetUpdater>()
        return WidgetConfigViewModel(useCase, prefs, updater)
    }
}
