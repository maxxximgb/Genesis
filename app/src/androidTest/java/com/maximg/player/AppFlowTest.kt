package com.maximg.player

import android.Manifest
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import androidx.compose.ui.test.waitUntilDoesNotExist
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.junit.runners.model.Statement

/**
 * Сквозные UI-тесты на устройстве.
 * Выполняются по алфавиту (a→g) с накоплением состояния в БД.
 * Требование: минимум 2 аудиофайла в MediaStore устройства.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class AppFlowTest {

    /** Узлы с onLongClick (строки треков, карточки плейлистов) */
    private val hasOnLongClick: SemanticsMatcher =
        SemanticsMatcher.keyIsDefined(SemanticsActions.OnLongClick)

    /**
     * Grants READ_MEDIA_AUDIO via shell command before the activity starts.
     * More reliable than GrantPermissionRule on Samsung Android 15+.
     */
    @get:Rule(order = 0)
    val shellPermissionRule: TestRule = object : TestRule {
        override fun apply(base: Statement, description: Description) = object : Statement() {
            override fun evaluate() {
                val uia = InstrumentationRegistry.getInstrumentation().uiAutomation
                val pkg = InstrumentationRegistry.getInstrumentation().targetContext.packageName
                uia.executeShellCommand(
                    "pm grant $pkg ${Manifest.permission.READ_MEDIA_AUDIO}"
                ).close()
                Thread.sleep(200)
                base.evaluate()
            }
        }
    }

    @get:Rule(order = 1)
    val rule = createAndroidComposeRule<MainActivity>()

    /** Возвращает строку из ресурсов приложения с учётом текущей локали устройства. */
    private fun str(resId: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(resId)

    private fun str(resId: Int, vararg args: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(resId, *args)

    // ── вспомогательные функции ───────────────────────────────────────────────

    private fun ensureAppReady() {
        val libraryTitle = str(R.string.library_title)
        val playlistsTitle = str(R.string.playlists_title)
        val permissionBtn = str(R.string.permission_grant)
        val mainNavMatcher = hasText(libraryTitle) or hasText(playlistsTitle) or hasText(permissionBtn)

        // If the bottom navigation is not visible (e.g. we're on PlaylistDetailScreen),
        // press back to return to a screen that has it.
        if (rule.onAllNodes(mainNavMatcher).fetchSemanticsNodes().isEmpty()) {
            Espresso.pressBackUnconditionally()
            rule.waitForIdle()
        }

        rule.waitUntilAtLeastOneExists(mainNavMatcher, timeoutMillis = 12_000)

        if (rule.onAllNodesWithText(permissionBtn).fetchSemanticsNodes().isNotEmpty()) {
            rule.onNodeWithText(permissionBtn).performClick()
            rule.waitForIdle()
            rule.waitUntilAtLeastOneExists(hasText(libraryTitle), timeoutMillis = 10_000)
        }
    }

    private fun goToLibrary() {
        ensureAppReady()
        rule.onNode(hasText(str(R.string.library_title)) and hasClickAction()).performClick()
        rule.waitForIdle()
    }

    private fun goToPlaylists() {
        ensureAppReady()
        rule.onNode(hasText(str(R.string.playlists_title)) and hasClickAction()).performClick()
        rule.waitForIdle()
    }

    private fun waitForLibraryTracks() {
        rule.waitUntilAtLeastOneExists(hasOnLongClick, timeoutMillis = 8_000)
    }

    private fun createPlaylist(name: String) {
        goToPlaylists()
        rule.onNodeWithContentDescription(str(R.string.new_playlist)).performClick()
        rule.waitForIdle()
        rule.onNode(hasSetTextAction()).performTextInput(name)
        rule.onNodeWithText(str(R.string.save)).performClick()
        rule.waitForIdle()
    }

    private fun openPlaylist(name: String) {
        goToPlaylists()
        rule.onNodeWithText(name).performClick()
        rule.waitForIdle()
    }

    private fun addFirstTrackTo(playlistName: String) {
        openPlaylist(playlistName)
        rule.onNodeWithContentDescription(str(R.string.add_tracks)).performClick()
        rule.waitForIdle()
        waitForLibraryTracks()
        rule.onAllNodes(isToggleable()).onFirst().performClick()
        rule.waitForIdle()
        rule.onNodeWithText("${str(R.string.add)} 1").performClick()
        rule.waitForIdle()
    }

    // ── тесты ────────────────────────────────────────────────────────────────

    /**
     * a. Создание плейлиста.
     * FAB → ввод имени → Save → плейлист виден в списке.
     */
    @Test
    fun a_createPlaylist() {
        goToPlaylists()

        rule.onNodeWithContentDescription(str(R.string.new_playlist)).performClick()
        rule.waitForIdle()

        rule.onAllNodesWithText(str(R.string.new_playlist)).onFirst().assertIsDisplayed()

        rule.onNode(hasSetTextAction()).performTextInput("AutoTest Playlist")
        rule.onNodeWithText(str(R.string.save)).performClick()
        rule.waitForIdle()

        rule.onNodeWithText("AutoTest Playlist").assertIsDisplayed()
    }

    /**
     * b. Добавление одного трека через диалог "Добавить в плейлист".
     * Library → иконка добавления → выбрать плейлист → диалог закрывается.
     */
    @Test
    fun b_addSingleTrack_viaPlaylistAddDialog() {
        createPlaylist("AutoTest Single")

        goToLibrary()
        waitForLibraryTracks()

        // Tap the PlaylistAdd icon on the first track row (same callback as long-press)
        rule.onAllNodes(hasContentDescription(str(R.string.add_to_playlist))).onFirst()
            .performClick()

        rule.waitUntilAtLeastOneExists(hasText(str(R.string.add_to_playlist)), timeoutMillis = 5_000)
        rule.waitUntilAtLeastOneExists(hasText("AutoTest Single"), timeoutMillis = 3_000)

        rule.onNodeWithText("AutoTest Single").performClick()
        rule.waitForIdle()

        rule.waitUntilDoesNotExist(hasText(str(R.string.add_to_playlist)), timeoutMillis = 3_000)
    }

    /**
     * c. Добавление нескольких треков через экран плейлиста.
     * Open playlist → "+" → выбрать 2 трека → "Add 2" → 2 трека в плейлисте.
     */
    @Test
    fun c_addMultipleTracks_viaPlaylistScreen() {
        createPlaylist("AutoTest Multi")
        openPlaylist("AutoTest Multi")

        rule.onNodeWithContentDescription(str(R.string.add_tracks)).performClick()
        rule.waitForIdle()
        waitForLibraryTracks()

        val checkboxes = rule.onAllNodes(isToggleable())
        val count = checkboxes.fetchSemanticsNodes().size
        assertTrue("Нужно минимум 2 аудиофайла (найдено: $count)", count >= 2)

        checkboxes.onFirst().performClick()
        rule.waitForIdle()
        rule.onAllNodes(isToggleable())[1].performClick()
        rule.waitForIdle()

        val addTwo = "${str(R.string.add)} 2"
        rule.onNodeWithText(addTwo).assertIsDisplayed()
        rule.onNodeWithText(addTwo).performClick()
        rule.waitForIdle()

        rule.onNode(hasText(str(R.string.playlist_detail_subtitle, 2, ""), substring = true))
            .assertIsDisplayed()
    }

    /**
     * d. Воспроизведение плейлиста кнопкой Play.
     * Open playlist (с треком) → Play → NowPlayingBar появляется.
     */
    @Test
    fun d_playPlaylist() {
        createPlaylist("AutoTest Playback")
        addFirstTrackTo("AutoTest Playback")

        rule.onNodeWithContentDescription(str(R.string.play)).performClick()
        rule.waitForIdle()

        rule.waitUntilAtLeastOneExists(
            hasContentDescription(str(R.string.play_pause)),
            timeoutMillis = 5_000
        )
        rule.onNodeWithContentDescription(str(R.string.play_pause)).assertIsDisplayed()
    }

    /**
     * e. Воспроизведение конкретного трека нажатием на строку.
     * Open playlist → нажать на трек → NowPlayingBar показывает Play/Pause.
     */
    @Test
    fun e_playSpecificTrack() {
        createPlaylist("AutoTest SpecificTrack")
        addFirstTrackTo("AutoTest SpecificTrack")
        openPlaylist("AutoTest SpecificTrack")

        rule.waitUntilAtLeastOneExists(
            hasClickAction() and hasOnLongClick,
            timeoutMillis = 5_000
        )

        rule.onAllNodes(hasClickAction() and hasOnLongClick).onFirst().performClick()
        rule.waitForIdle()

        rule.waitUntilAtLeastOneExists(
            hasContentDescription(str(R.string.play_pause)),
            timeoutMillis = 5_000
        )
        rule.onNodeWithContentDescription(str(R.string.play_pause)).assertIsDisplayed()
    }

    /**
     * f. Поиск треков.
     * Library → ввод несуществующего → "Nothing found" → очистить → треки вернулись.
     */
    @Test
    fun f_searchTracks() {
        goToLibrary()
        waitForLibraryTracks()

        val initialCount = rule.onAllNodes(hasOnLongClick).fetchSemanticsNodes().size
        assertTrue("Нужен хотя бы 1 аудиофайл на устройстве", initialCount > 0)

        rule.onNode(hasSetTextAction() and !hasScrollAction()).performTextInput("XxZzQ_nonexistent_999")
        rule.waitUntilAtLeastOneExists(
            hasText(str(R.string.library_empty_search_title)),
            timeoutMillis = 6_000
        )
        rule.onNodeWithText(str(R.string.library_empty_search_title)).assertIsDisplayed()

        rule.onNode(hasSetTextAction() and !hasScrollAction()).performTextClearance()
        rule.waitUntilAtLeastOneExists(hasOnLongClick, timeoutMillis = 8_000)

        val remaining = rule.onAllNodes(hasText(str(R.string.library_empty_search_title)))
            .fetchSemanticsNodes()
        assertFalse("Заглушка 'не найдено' не должна быть видна после очистки", remaining.isNotEmpty())
    }

    /**
     * g. Диалог настроек размера интерфейса.
     * Library → Settings → все опции видны → сменить размер → сбросить → закрыть.
     */
    @Test
    fun g_settings_fontScaleDialog() {
        goToLibrary()

        rule.onNodeWithContentDescription(str(R.string.settings)).performClick()
        rule.waitForIdle()

        rule.onNodeWithText(str(R.string.ui_size)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.ui_size_small)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.ui_size_normal)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.ui_size_large)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.ui_size_xlarge)).assertIsDisplayed()

        rule.onNodeWithText(str(R.string.ui_size_large)).performClick()
        rule.waitForIdle()

        // Сбрасываем в нормальный размер, чтобы не ломать последующие запуски
        rule.onNodeWithText(str(R.string.ui_size_normal)).performClick()
        rule.waitForIdle()

        rule.onNodeWithText(str(R.string.close)).performClick()
        rule.waitForIdle()

        rule.waitUntilDoesNotExist(hasText(str(R.string.ui_size)), timeoutMillis = 2_000)
    }
}
