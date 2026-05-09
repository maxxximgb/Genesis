package dev.maxxximgb.genesis.widget

import dev.maxxximgb.genesis.data.preferences.UserPreferencesStore
import dev.maxxximgb.genesis.data.preferences.WidgetPreferencesStore
import dev.maxxximgb.genesis.domain.model.SortOrder
import dev.maxxximgb.genesis.domain.repository.MediaLibraryRepository
import dev.maxxximgb.genesis.domain.usecase.playlist.ObservePlaylistsUseCase
import dev.maxxximgb.genesis.ui.theme.ThemeMode
import dev.maxxximgb.genesis.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class WidgetConfigViewModelBackgroundTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    @Test
    fun initialBackgroundStateIsDark() {
        val vm = newVm()
        assertEquals(WidgetBackgroundChoice.Dark, vm.selectedBackground.value)
    }

    @Test
    fun hydrateLoadsStoredChoice() = runTest {
        val widgetPrefs = mock<WidgetPreferencesStore> {
            onBlocking { getStoredBackgroundFor(42) } doReturn WidgetBackgroundChoice.Light
        }
        val vm = newVm(widgetPrefs = widgetPrefs)

        vm.hydrateBackground(appWidgetId = 42, isSystemInDarkMode = false)
        advanceUntilIdle()

        assertEquals(WidgetBackgroundChoice.Light, vm.selectedBackground.value)
    }

    @Test
    fun hydrateUsesAppLightThemeAsDefaultWhenNothingStored() = runTest {
        val widgetPrefs = mock<WidgetPreferencesStore> {
            onBlocking { getStoredBackgroundFor(any()) } doReturn null
        }
        val userPrefs = mock<UserPreferencesStore> {
            on { observeThemeMode() } doReturn flowOf(ThemeMode.LIGHT)
        }
        val vm = newVm(widgetPrefs = widgetPrefs, userPrefs = userPrefs)

        vm.hydrateBackground(appWidgetId = 1, isSystemInDarkMode = true /* should be ignored */)
        advanceUntilIdle()

        assertEquals(WidgetBackgroundChoice.Light, vm.selectedBackground.value)
    }

    @Test
    fun hydrateFallsBackToSystemWhenAppThemeIsAuto() = runTest {
        val widgetPrefs = mock<WidgetPreferencesStore> {
            onBlocking { getStoredBackgroundFor(any()) } doReturn null
        }
        val userPrefs = mock<UserPreferencesStore> {
            on { observeThemeMode() } doReturn flowOf(ThemeMode.AUTO)
        }
        val vm = newVm(widgetPrefs = widgetPrefs, userPrefs = userPrefs)

        vm.hydrateBackground(appWidgetId = 1, isSystemInDarkMode = true)
        advanceUntilIdle()
        assertEquals(WidgetBackgroundChoice.Dark, vm.selectedBackground.value)
    }

    @Test
    fun onBackgroundChosenUpdatesState() {
        val vm = newVm()
        vm.onBackgroundChosen(WidgetBackgroundChoice.Light)
        assertEquals(WidgetBackgroundChoice.Light, vm.selectedBackground.value)
    }

    @Test
    fun bindPersistsBothTargetAndBackground() = runTest {
        val widgetPrefs = mock<WidgetPreferencesStore>()
        val vm = newVm(widgetPrefs = widgetPrefs)

        vm.onBackgroundChosen(WidgetBackgroundChoice.Light)
        vm.bind(appWidgetId = 7, playlistId = 99L) { /* committed */ }
        advanceUntilIdle()

        verify(widgetPrefs).setTarget(eq(7), eq(WidgetTarget.Playlist(99L)))
        verify(widgetPrefs).setBackgroundFor(eq(7), eq(WidgetBackgroundChoice.Light))
    }

    private fun newVm(
        widgetPrefs: WidgetPreferencesStore = mock(),
        userPrefs: UserPreferencesStore = mock {
            on { observeThemeMode() } doReturn flowOf(ThemeMode.AUTO)
            on { observeLibrarySort() } doReturn flowOf(SortOrder.DATE_ADDED_DESC)
            on { observeAudiobookOverrides() } doReturn flowOf(emptySet())
        },
    ): WidgetConfigViewModel {
        val useCase = mock<ObservePlaylistsUseCase> { on { invoke() } doReturn flowOf(emptyList()) }
        val updater = mock<WidgetUpdater>()
        val mediaRepo = mock<MediaLibraryRepository> {
            onBlocking { getAudiobookTracks(any(), any()) } doReturn emptyList()
        }
        return WidgetConfigViewModel(useCase, widgetPrefs, userPrefs, updater, mediaRepo)
    }
}
