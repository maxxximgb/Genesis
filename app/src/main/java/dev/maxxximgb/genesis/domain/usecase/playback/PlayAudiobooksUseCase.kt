package dev.maxxximgb.genesis.domain.usecase.playback

import dev.maxxximgb.genesis.data.preferences.BookmarkStore
import dev.maxxximgb.genesis.data.preferences.PlaylistModeStore
import dev.maxxximgb.genesis.data.preferences.UserPreferencesStore
import dev.maxxximgb.genesis.domain.model.toRepeatAndShuffle
import dev.maxxximgb.genesis.domain.playback.PlaybackController
import dev.maxxximgb.genesis.domain.repository.MediaLibraryRepository
import dev.maxxximgb.genesis.widget.AUDIOBOOK_PSEUDO_PLAYLIST_ID
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Starts (or resumes) the audiobook queue. The queue is the union of native IS_AUDIOBOOK
 * tracks and user overrides, ordered by the user's library sort preference. Resume position
 * comes from [BookmarkStore]'s `bookmark_audiobooks` slot — the audiobook target is just
 * another playback context that benefits from the same per-context bookmark plumbing.
 */
class PlayAudiobooksUseCase @Inject constructor(
    private val mediaLibraryRepository: MediaLibraryRepository,
    private val userPreferences: UserPreferencesStore,
    private val controller: PlaybackController,
    private val modeStore: PlaylistModeStore,
    private val bookmarkStore: BookmarkStore,
) {
    /**
     * @param explicitStartIndex non-null when the caller picks a specific track index (e.g. the
     *  widget's prev/next on an idle queue computes bookmark±1). null means "resume from the
     *  bookmark, fall back to track 0". When non-null, position resets to 0 since an explicit
     *  index selection isn't a resume.
     */
    suspend operator fun invoke(explicitStartIndex: Int? = null) {
        val sort = userPreferences.observeLibrarySort().first()
        val overrides = userPreferences.observeAudiobookOverrides().first()
        val tracks = mediaLibraryRepository.getAudiobookTracks(sort, overrides.toList())
        if (tracks.isEmpty()) return

        val (repeat, shuffle) = modeStore.getMode(AUDIOBOOK_PSEUDO_PLAYLIST_ID).toRepeatAndShuffle()
        controller.setShuffleEnabled(shuffle)
        controller.setRepeatMode(repeat)

        val (resolvedIndex, resolvedPositionMs) = if (explicitStartIndex != null) {
            explicitStartIndex.coerceIn(0, tracks.lastIndex) to 0L
        } else {
            val bookmark = bookmarkStore.get(AUDIOBOOK_PSEUDO_PLAYLIST_ID)
            val idx = bookmark?.let { bm ->
                tracks.indexOfFirst { it.mediaStoreId == bm.mediaStoreId }
            }?.takeIf { it >= 0 } ?: 0
            idx to (bookmark?.positionMs ?: 0L)
        }

        controller.playQueue(
            playlistId = AUDIOBOOK_PSEUDO_PLAYLIST_ID,
            tracks = tracks,
            startIndex = resolvedIndex,
            startPositionMs = resolvedPositionMs,
        )
    }
}
