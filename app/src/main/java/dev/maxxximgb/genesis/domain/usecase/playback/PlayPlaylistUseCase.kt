package dev.maxxximgb.genesis.domain.usecase.playback

import dev.maxxximgb.genesis.data.preferences.BookmarkStore
import dev.maxxximgb.genesis.data.preferences.PlaylistModeStore
import dev.maxxximgb.genesis.domain.model.toRepeatAndShuffle
import dev.maxxximgb.genesis.domain.playback.PlaybackController
import dev.maxxximgb.genesis.domain.repository.PlaylistRepository
import javax.inject.Inject

class PlayPlaylistUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val controller: PlaybackController,
    private val modeStore: PlaylistModeStore,
    private val bookmarkStore: BookmarkStore,
) {
    /**
     * @param explicitStartIndex non-null when the user picked a specific track (e.g. tapped row N
     *  of the playlist). null means "resume from saved bookmark, fall back to start".
     */
    suspend operator fun invoke(playlistId: Long, explicitStartIndex: Int? = null) {
        val tracks = playlistRepository.getTracksForPlaylist(playlistId)
        if (tracks.isEmpty()) return
        val (repeat, shuffle) = modeStore.getMode(playlistId).toRepeatAndShuffle()
        controller.setShuffleEnabled(shuffle)
        controller.setRepeatMode(repeat)

        val (resolvedIndex, resolvedPositionMs) = if (explicitStartIndex != null) {
            explicitStartIndex.coerceIn(0, tracks.lastIndex) to 0L
        } else {
            val bookmark = bookmarkStore.get(playlistId)
            val idx = bookmark?.let { bm ->
                tracks.indexOfFirst { it.mediaStoreId == bm.mediaStoreId }
            }?.takeIf { it >= 0 } ?: 0
            idx to (bookmark?.positionMs ?: 0L)
        }
        controller.playQueue(playlistId, tracks, resolvedIndex, resolvedPositionMs)
    }
}
