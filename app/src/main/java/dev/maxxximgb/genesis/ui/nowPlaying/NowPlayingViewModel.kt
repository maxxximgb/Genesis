package dev.maxxximgb.genesis.ui.nowPlaying

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.maxxximgb.genesis.data.audiofx.AudioFxController
import dev.maxxximgb.genesis.data.preferences.UserPreferencesStore
import dev.maxxximgb.genesis.domain.model.Album
import dev.maxxximgb.genesis.domain.model.Playlist
import dev.maxxximgb.genesis.domain.model.SortOrder
import dev.maxxximgb.genesis.domain.model.Track
import dev.maxxximgb.genesis.domain.model.loopStateOf
import dev.maxxximgb.genesis.domain.model.nextLoopState
import dev.maxxximgb.genesis.domain.model.toRepeatAndShuffle
import dev.maxxximgb.genesis.domain.playback.PlaybackController
import dev.maxxximgb.genesis.domain.repository.MediaLibraryRepository
import dev.maxxximgb.genesis.domain.usecase.playback.SetPlaylistLoopModeUseCase
import dev.maxxximgb.genesis.domain.usecase.playlist.GetPlaylistTracksUseCase
import dev.maxxximgb.genesis.domain.usecase.playlist.ObservePlaylistsUseCase
import dev.maxxximgb.genesis.ui.library.SelectionStateHolder
import dev.maxxximgb.genesis.widget.AUDIOBOOK_PSEUDO_PLAYLIST_ID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NowPlayingViewModel @Inject constructor(
    private val controller: PlaybackController,
    private val mediaLibraryRepository: MediaLibraryRepository,
    private val preferences: UserPreferencesStore,
    private val setPlaylistLoopModeUseCase: SetPlaylistLoopModeUseCase,
    private val getPlaylistTracksUseCase: GetPlaylistTracksUseCase,
    observePlaylists: ObservePlaylistsUseCase,
    audioFxController: AudioFxController,
    selectionStateHolder: SelectionStateHolder,
) : ViewModel() {

    /**
     * Data sources for both the "add to queue" picker AND the queue panel's group-by-
     * source headers. Cached for [SUBSCRIPTION_TIMEOUT_MS] after last subscription so the
     * sheet open / queue expand / now-playing close cycle doesn't thrash Room.
     */
    val allPlaylists: StateFlow<List<Playlist>> = observePlaylists()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
            initialValue = emptyList(),
        )
    val allAlbums: StateFlow<List<Album>> = mediaLibraryRepository.observeAlbums()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
            initialValue = emptyList(),
        )

    /** Id → name map for the queue UI to label "Playlist X" headers. */
    val playlistNameById: StateFlow<Map<Long, String>> = allPlaylists
        .map { playlists -> playlists.associate { it.id to it.name } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
            initialValue = emptyMap(),
        )

    /** Id → name map for the queue UI to label "Album Y" headers. */
    val albumNameById: StateFlow<Map<Long, String>> = allAlbums
        .map { albums -> albums.associate { it.id to it.name } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
            initialValue = emptyMap(),
        )

    /**
     * Snapshot of all library tracks for the in-sheet multi-select picker. Lazily loaded
     * on first sheet open via the same `WhileSubscribed(0)` pattern as [allPlaylists] /
     * [allAlbums]. Sort is title-asc — alphabetical is the most "find what I want" order
     * for a picker; date-desc was confusing testers in the playlist add flow.
     */
    private val tracksRefreshTrigger = MutableStateFlow(0)
    val allTracks: StateFlow<List<Track>> = tracksRefreshTrigger
        .map { mediaLibraryRepository.getLibraryTracks(SortOrder.TITLE_ASC, query = "") }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(0),
            initialValue = emptyList(),
        )

    init {
        // Probe EQ capabilities up front so the TransportRow can hide the EQ slot on devices
        // where it's unsupported, without waiting for the user to open the EQ screen first.
        audioFxController.probeCapabilitiesIfNeeded()
    }

    val uiState: StateFlow<NowPlayingUiState> = controller.state

    /** True while the library is in multi-select mode — host can hide the mini bar. */
    val isSelectionMode: StateFlow<Boolean> = selectionStateHolder.isActive

    /**
     * Mini-bar dismiss flag, scoped to a single mediaStoreId. The user's swipe-down hides the
     * bar without stopping playback; the flag self-clears when the current track changes (so a
     * skip-next or new "play" action surfaces the bar again) or when the bar is opened to the
     * full now-playing screen.
     */
    private val dismissedForMediaId = MutableStateFlow<Long?>(null)
    val isMiniBarDismissed: StateFlow<Boolean> = combine(
        controller.state,
        dismissedForMediaId,
    ) { state, dismissedId ->
        dismissedId != null && dismissedId == state.currentMediaStoreId
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
        initialValue = false,
    )

    fun dismissMiniBar() {
        dismissedForMediaId.value = controller.state.value.currentMediaStoreId
    }

    /** Cleared when the user opens full now-playing so the mini bar reappears on back. */
    fun clearMiniBarDismiss() {
        dismissedForMediaId.value = null
    }

    private val sleepTimer = SleepTimer(
        scope = viewModelScope,
        onFire = { controller.pause() },
    )

    val sleepTimerRemainingMs: StateFlow<Long?> = sleepTimer.remainingMs

    /** True when the equalizer master switch is on — used to tint the EQ button in TransportRow. */
    val eqMasterEnabled: StateFlow<Boolean> = preferences.observeAudioFxState()
        .map { it.masterEnabled }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
            initialValue = false,
        )

    /**
     * True when the device's audio HAL supports `Equalizer`. Initial value `true` is optimistic
     * — the TransportRow assumes the slot is needed until the probe proves otherwise. On
     * unsupported devices the slot disappears within a frame of the probe completing.
     */
    val eqSupported: StateFlow<Boolean> = audioFxController.capabilities
        .map { it.equalizerSupported }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
            initialValue = true,
        )

    /**
     * Smoothly-ticking position derived from [controller.state].positionMs. The DataStore-backed
     * state only updates on transitions; this flow extrapolates between updates by adding the
     * elapsed real time since the last state emission while playback is active.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val livePositionMs: StateFlow<Long> = controller.state
        .flatMapLatest { s ->
            if (!s.isPlaying) {
                flowOf(s.positionMs)
            } else {
                tickingFlow(basisPositionMs = s.positionMs, durationMs = s.durationMs)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
            initialValue = 0L,
        )

    private fun tickingFlow(basisPositionMs: Long, durationMs: Long): Flow<Long> = flow {
        val capturedAt = System.currentTimeMillis()
        val cap = if (durationMs > 0L) durationMs else Long.MAX_VALUE
        while (true) {
            val elapsed = System.currentTimeMillis() - capturedAt
            emit((basisPositionMs + elapsed).coerceAtMost(cap))
            delay(TICK_MS)
        }
    }

    val queueTracks: StateFlow<List<Track>> = controller.state
        .map { it.queue }
        .distinctUntilChanged()
        .map { ids -> if (ids.isEmpty()) emptyList() else mediaLibraryRepository.getTracksByIds(ids) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
            initialValue = emptyList(),
        )

    /** Album id of the currently-playing track, used by the mini bar + fullscreen artwork. */
    val currentAlbumId: StateFlow<Long?> = combine(
        controller.state.map { it.currentMediaStoreId }.distinctUntilChanged(),
        queueTracks,
    ) { id, tracks ->
        id?.let { current -> tracks.firstOrNull { it.mediaStoreId == current }?.albumId }
    }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
            initialValue = null,
        )

    fun togglePlayPause() {
        viewModelScope.launch { controller.togglePlayPause() }
    }

    fun next() {
        viewModelScope.launch { controller.seekToNext() }
    }

    fun previous() {
        viewModelScope.launch { controller.seekToPrevious() }
    }

    fun seekTo(positionMs: Long) {
        viewModelScope.launch { controller.seekTo(positionMs) }
    }

    /**
     * Re-randomizes the shuffle order. Bound to long-press on the loop button — gives the
     * user a way to "shake the bag" without leaving shuffle mode. No-op if shuffle is off.
     */
    fun reshuffleIfShuffleActive() {
        viewModelScope.launch {
            if (controller.state.value.shuffleEnabled) {
                controller.reshuffleQueue()
            }
        }
    }

    /**
     * Cycles the combined shuffle+repeat state (matches widget's loop button):
     * OFF → REPEAT_ALL → REPEAT_ONE → SHUFFLE → OFF.
     */
    fun cycleLoop() {
        viewModelScope.launch {
            val state = controller.state.value
            val current = loopStateOf(
                state.repeatMode,
                state.shuffleEnabled,
            )
            val next = nextLoopState(current)
            val playlistId = state.playlistId
            if (playlistId != null) {
                setPlaylistLoopModeUseCase(playlistId, next)
            } else {
                val (repeat, shuffle) = next.toRepeatAndShuffle()
                controller.setShuffleEnabled(shuffle)
                controller.setRepeatMode(repeat)
            }
        }
    }

    fun jumpToQueueIndex(index: Int) {
        viewModelScope.launch { controller.seekToQueueIndex(index) }
    }

    fun setSleepTimerMinutes(minutes: Int) {
        sleepTimer.start(minutes.toLong() * 60_000L)
    }

    fun cancelSleepTimer() {
        sleepTimer.cancel()
    }

    fun playSingle(track: Track) {
        viewModelScope.launch { controller.playSingle(track) }
    }

    fun playTracks(tracks: List<Track>, startIndex: Int) {
        if (tracks.isEmpty()) return
        viewModelScope.launch { controller.playQueue(playlistId = null, tracks = tracks, startIndex = startIndex) }
    }

    /**
     * Append tracks to the live queue. Routing mirrors [LibraryViewModel.enqueuePlaylist]:
     * if a real Room playlist is currently the source, append to the very end so the
     * playlist plays through; otherwise insert immediately after the current track.
     * [sourcePlaylistId] / [sourceAlbumId] tag this batch so the queue UI can group it
     * under a "Playlist X" / "Album Y" header.
     */
    suspend fun enqueueTracks(
        tracks: List<Track>,
        sourcePlaylistId: Long? = null,
        sourceAlbumId: Long? = null,
    ): Int {
        if (tracks.isEmpty()) return 0
        val pid = controller.state.value.playlistId
        val isRealPlaylist = pid != null && pid != AUDIOBOOK_PSEUDO_PLAYLIST_ID
        if (isRealPlaylist) {
            controller.enqueueAtEnd(tracks, sourcePlaylistId, sourceAlbumId)
        } else {
            controller.enqueueAfterCurrent(tracks, sourcePlaylistId, sourceAlbumId)
        }
        return tracks.size
    }

    suspend fun enqueuePlaylistById(playlistId: Long): Int {
        val tracks = getPlaylistTracksUseCase(playlistId)
        return enqueueTracks(tracks, sourcePlaylistId = playlistId)
    }

    suspend fun enqueueAlbumById(albumId: Long): Int {
        val tracks = mediaLibraryRepository.getTracksByAlbum(albumId, SortOrder.TITLE_ASC)
        return enqueueTracks(tracks, sourceAlbumId = albumId)
    }

    /**
     * Enqueue a subset of [allTracks] picked by the user. Preserves the visual order of
     * the picker (title-asc) so the user gets back what they saw on screen — matching
     * the order of [ids] would let UI selection order leak into playback order, which
     * is surprising for "ticked these five rows from a list". No source tag — the
     * picker has no notion of "from where", so these become orphans (and will be
     * hidden by the grouped queue UI per user request).
     */
    suspend fun enqueueSelectedTrackIds(ids: Set<Long>): Int {
        if (ids.isEmpty()) return 0
        val ordered = allTracks.value.filter { it.mediaStoreId in ids }
        return enqueueTracks(ordered)
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch { controller.moveQueueItem(fromIndex, toIndex) }
    }

    private companion object {
        const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
        const val TICK_MS = 500L
    }
}
