package dev.maxxximgb.genesis.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import androidx.paging.map
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.maxxximgb.genesis.data.playback.PlaybackStateStore
import dev.maxxximgb.genesis.data.preferences.SearchHistoryStore
import dev.maxxximgb.genesis.data.preferences.UserPreferencesStore
import dev.maxxximgb.genesis.domain.model.Album
import dev.maxxximgb.genesis.domain.model.Artist
import dev.maxxximgb.genesis.domain.model.Folder
import dev.maxxximgb.genesis.domain.model.Playlist
import dev.maxxximgb.genesis.domain.model.PlaylistSummary
import dev.maxxximgb.genesis.domain.model.SortOrder
import dev.maxxximgb.genesis.domain.model.Track
import dev.maxxximgb.genesis.domain.playback.PlaybackController
import dev.maxxximgb.genesis.domain.repository.MediaLibraryRepository
import dev.maxxximgb.genesis.widget.AUDIOBOOK_PSEUDO_PLAYLIST_ID
import dev.maxxximgb.genesis.domain.usecase.library.ObserveAlbumsUseCase
import dev.maxxximgb.genesis.domain.usecase.library.ObserveArtistsUseCase
import dev.maxxximgb.genesis.domain.usecase.library.ObserveFoldersUseCase
import dev.maxxximgb.genesis.domain.usecase.library.SearchLibraryUseCase
import dev.maxxximgb.genesis.domain.usecase.playlist.AddTracksToPlaylistUseCase
import dev.maxxximgb.genesis.domain.usecase.playlist.CreatePlaylistUseCase
import dev.maxxximgb.genesis.domain.usecase.playlist.DeletePlaylistUseCase
import dev.maxxximgb.genesis.domain.usecase.playlist.GetPlaylistTracksUseCase
import dev.maxxximgb.genesis.domain.usecase.playlist.ObservePlaylistSummariesUseCase
import dev.maxxximgb.genesis.domain.usecase.playlist.ObservePlaylistsUseCase
import dev.maxxximgb.genesis.domain.usecase.playlist.RenamePlaylistUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val searchLibrary: SearchLibraryUseCase,
    private val userPreferences: UserPreferencesStore,
    private val addTracksToPlaylist: AddTracksToPlaylistUseCase,
    private val createPlaylistUseCase: CreatePlaylistUseCase,
    private val renamePlaylistUseCase: RenamePlaylistUseCase,
    private val deletePlaylistUseCase: DeletePlaylistUseCase,
    private val getPlaylistTracksUseCase: GetPlaylistTracksUseCase,
    private val searchHistoryStore: SearchHistoryStore,
    private val mediaLibraryRepository: MediaLibraryRepository,
    private val selectionStateHolder: SelectionStateHolder,
    private val playbackController: PlaybackController,
    playbackStateStore: PlaybackStateStore,
    observePlaylists: ObservePlaylistsUseCase,
    observePlaylistSummaries: ObservePlaylistSummariesUseCase,
    observeAlbums: ObserveAlbumsUseCase,
    observeArtists: ObserveArtistsUseCase,
    observeFolders: ObserveFoldersUseCase,
    refreshSignal: dev.maxxximgb.genesis.data.media.MediaStoreRefreshSignal,
) : ViewModel() {

    /**
     * Combined into the pager flows below as an extra source — emits once at start
     * and then on every external MediaStore mutation we want to react to. Rename and
     * delete done via [dev.maxxximgb.genesis.ui.components.TrackMutator] no longer
     * tick this signal; instead they update [pendingRenames] / [pendingDeletes] which
     * the screen overlays on top of the cached PagingData, so the user keeps their
     * scroll position and the LazyColumn can animate the change in place.
     */
    private val pagerRefresh: Flow<Unit> = refreshSignal.ticks.onStart { emit(Unit) }

    /**
     * Session-scoped overlay for renames performed in the current screen lifetime.
     * Persisted titles still live in the Room override DAO ([applyTitleOverrides]
     * applies them on next pager rebuild), but until that rebuild happens the
     * already-emitted PagingData carries the old TITLE — this map patches it
     * without re-fetching, so the row updates in place.
     */
    private val pendingRenames = MutableStateFlow<Map<Long, String>>(emptyMap())

    /**
     * Same idea for deletes: the MediaStore row is gone, but the LazyPagingItems
     * still holds the cached page that included it. Filtering by this set hides
     * the row immediately so neighbouring items can slide up via animateItemPlacement.
     */
    private val pendingDeletes = MutableStateFlow<Set<Long>>(emptySet())

    fun onTrackRenamedLocally(mediaStoreId: Long, newTitle: String) {
        pendingRenames.update { it + (mediaStoreId to newTitle) }
    }

    fun onTrackDeletedLocally(mediaStoreId: Long) {
        pendingDeletes.update { it + mediaStoreId }
    }

    private val searchQuery = MutableStateFlow("")
    private val selectedTrackIds = MutableStateFlow<Set<Long>>(emptySet())
    private val selectedPlaylistIds = MutableStateFlow<Set<Long>>(emptySet())
    private val selectedAlbumIds = MutableStateFlow<Set<Long>>(emptySet())
    private val browseTab = MutableStateFlow(LibraryBrowseTab.TRACKS)

    // The three selection sets are combined separately so the outer 5-arg combine has
    // room for everything else. Each set independently flips its mode in UiState.
    private val selectionTriple = combine(
        selectedTrackIds.asStateFlow(),
        selectedPlaylistIds.asStateFlow(),
        selectedAlbumIds.asStateFlow(),
    ) { tracks, playlists, albums -> Triple(tracks, playlists, albums) }

    private val baseTriple = combine(
        searchQuery.asStateFlow(),
        userPreferences.observeLibrarySort(),
        playbackStateStore.flow.map { it.currentMediaStoreId }.distinctUntilChanged(),
    ) { query, sort, currentId -> Triple(query, sort, currentId) }

    val uiState: StateFlow<LibraryUiState> = combine(
        baseTriple,
        selectionTriple,
        browseTab.asStateFlow(),
        searchHistoryStore.observeRecent(),
        userPreferences.observeAlbumsLayout(),
    ) { (query, sort, currentId), (selTracks, selPl, selAl), tab, recent, layout ->
        LibraryUiState(
            searchQuery = query,
            sort = sort,
            selectedIds = selTracks,
            selectedPlaylistIds = selPl,
            selectedAlbumIds = selAl,
            currentMediaStoreId = currentId,
            browseTab = tab,
            recentSearches = recent,
            albumsLayout = layout,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
        initialValue = LibraryUiState(),
    )

    val playlists: StateFlow<List<Playlist>> = observePlaylists()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
            initialValue = emptyList(),
        )

    val playlistSummaries: StateFlow<List<PlaylistSummary>> = observePlaylistSummaries()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
            initialValue = emptyList(),
        )

    val albums: StateFlow<List<Album>> = observeAlbums()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
            initialValue = emptyList(),
        )

    val artists: StateFlow<List<Artist>> = observeArtists()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
            initialValue = emptyList(),
        )

    val folders: StateFlow<List<Folder>> = observeFolders()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
            initialValue = emptyList(),
        )

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val pagedTracks: Flow<PagingData<Track>> = combine(
        userPreferences.observeLibrarySort(),
        searchQuery.debounce { if (it.isEmpty()) 0L else SEARCH_DEBOUNCE_MS },
        pagerRefresh,
    ) { sort, query, _ -> sort to query }
        .flatMapLatest { (sort, query) -> searchLibrary(sort, query).flow }
        .cachedIn(viewModelScope)
        .applySessionMutations()

    val audiobookOverrides: StateFlow<Set<Long>> = userPreferences.observeAudiobookOverrides()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
            initialValue = emptySet(),
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val pagedAudiobooks: Flow<PagingData<Track>> = combine(
        userPreferences.observeLibrarySort(),
        userPreferences.observeAudiobookOverrides(),
        pagerRefresh,
    ) { sort, overrides, _ -> sort to overrides }
        .flatMapLatest { (sort, overrides) ->
            mediaLibraryRepository.pagedAudiobooks(sort, overrides.toList()).flow
        }
        .cachedIn(viewModelScope)
        .applySessionMutations()

    /**
     * Layer the session rename/delete overlays on top of a cached PagingData stream.
     * Both inputs are stable when nothing has been mutated, so the combine just
     * re-emits the same PagingData instance and LazyPagingItems treats it as a
     * no-op. When an overlay changes, PagingData.map / .filter rebuilds the
     * downstream view on the SAME cached pages — no source refresh, no scroll jump.
     */
    private fun Flow<PagingData<Track>>.applySessionMutations(): Flow<PagingData<Track>> =
        combine(this, pendingRenames, pendingDeletes) { paging, renames, deletes ->
            var p = paging
            if (deletes.isNotEmpty()) p = p.filter { it.mediaStoreId !in deletes }
            if (renames.isNotEmpty()) p = p.map { t ->
                renames[t.mediaStoreId]?.let { t.copy(title = it) } ?: t
            }
            p
        }

    fun toggleAudiobookOverride(mediaStoreId: Long) {
        viewModelScope.launch { userPreferences.toggleAudiobookOverride(mediaStoreId) }
    }

    /**
     * Bulk action from the multi-select TopAppBar: marks every selected track as an
     * audiobook (idempotent — already-marked tracks stay marked) and clears the selection.
     * Returns the number of tracks affected for snackbar feedback.
     */
    suspend fun markSelectedAsAudiobooks(): Int {
        val ids = selectedTrackIds.value
        if (ids.isEmpty()) return 0
        userPreferences.addAudiobookOverrides(ids)
        val n = ids.size
        clearSelection()
        return n
    }

    /**
     * Counterpart to [markSelectedAsAudiobooks] — for tracks marked via the override set,
     * removes them. Native IS_AUDIOBOOK=1 tracks aren't affected (we can't write that
     * column without IntentSender) and will still appear in the Audiobooks tab.
     */
    suspend fun unmarkSelectedAudiobooks(): Int {
        val ids = selectedTrackIds.value
        if (ids.isEmpty()) return 0
        userPreferences.removeAudiobookOverrides(ids)
        val n = ids.size
        clearSelection()
        return n
    }

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun onSearchSubmit() {
        val q = searchQuery.value.trim()
        if (q.isEmpty()) return
        viewModelScope.launch { searchHistoryStore.add(q) }
    }

    fun onRecentSearchClick(query: String) {
        searchQuery.value = query
        viewModelScope.launch { searchHistoryStore.add(query) }
    }

    fun onRecentSearchRemove(query: String) {
        viewModelScope.launch { searchHistoryStore.remove(query) }
    }

    fun onBrowseTabChange(tab: LibraryBrowseTab) {
        browseTab.value = tab
    }

    fun onAlbumsLayoutToggle() {
        viewModelScope.launch {
            val current = uiState.value.albumsLayout
            val next = if (current == AlbumsLayout.LIST) AlbumsLayout.GRID else AlbumsLayout.LIST
            userPreferences.setAlbumsLayout(next)
        }
    }

    fun onSortChange(sort: SortOrder) {
        viewModelScope.launch { userPreferences.setLibrarySort(sort) }
    }

    fun toggleSelection(track: Track) {
        selectedTrackIds.update { current ->
            if (track.mediaStoreId in current) current - track.mediaStoreId
            else current + track.mediaStoreId
        }
        publishSelectionState()
    }

    fun clearSelection() {
        selectedTrackIds.value = emptySet()
        publishSelectionState()
    }

    fun togglePlaylistSelection(playlistId: Long) {
        selectedPlaylistIds.update { current ->
            if (playlistId in current) current - playlistId
            else current + playlistId
        }
        publishSelectionState()
    }

    fun clearPlaylistSelection() {
        selectedPlaylistIds.value = emptySet()
        publishSelectionState()
    }

    /**
     * Bulk-delete every selected playlist, then clear the selection. Returns the count
     * for snackbar feedback.
     */
    suspend fun deleteSelectedPlaylists(): Int {
        val ids = selectedPlaylistIds.value.toList()
        ids.forEach { deletePlaylistUseCase(it) }
        clearPlaylistSelection()
        return ids.size
    }

    suspend fun renamePlaylist(id: Long, name: String) {
        renamePlaylistUseCase(id, name)
    }

    suspend fun deletePlaylist(id: Long) {
        deletePlaylistUseCase(id)
    }

    fun toggleAlbumSelection(albumId: Long) {
        selectedAlbumIds.update { current ->
            if (albumId in current) current - albumId else current + albumId
        }
        publishSelectionState()
    }

    fun clearAlbumSelection() {
        selectedAlbumIds.value = emptySet()
        publishSelectionState()
    }

    /**
     * Materialise every track from each selected album (preserving the album's TITLE_ASC
     * order) and append them to [playlistId]. Returns total tracks added.
     */
    suspend fun addSelectedAlbumsToPlaylist(playlistId: Long): Int {
        val ids = selectedAlbumIds.value.toList()
        if (ids.isEmpty()) return 0
        // Load sequentially — N is small (user-selected albums) and parallel queries on
        // MediaStore tend to thrash the cursor cache.
        val tracks = ids.flatMap { id ->
            mediaLibraryRepository.getTracksByAlbum(id, SortOrder.TITLE_ASC)
        }
        if (tracks.isEmpty()) return 0
        addTracksToPlaylist(playlistId, tracks)
        clearAlbumSelection()
        return tracks.size
    }

    suspend fun addSelectedAlbumsToNewPlaylist(name: String): Int {
        val newId = createPlaylistUseCase(name.trim())
        return addSelectedAlbumsToPlaylist(newId)
    }

    /**
     * Mark every track of every selected album as an audiobook (idempotent override). Same
     * idea as [markSelectedAsAudiobooks] but the unit of selection is the album rather than
     * the individual track. Returns total tracks marked for snackbar feedback.
     */
    suspend fun markSelectedAlbumsAsAudiobook(): Int {
        val ids = selectedAlbumIds.value.toList()
        if (ids.isEmpty()) return 0
        val trackIds = ids.flatMap { albumId ->
            mediaLibraryRepository.getTracksByAlbum(albumId, SortOrder.TITLE_ASC)
                .map { it.mediaStoreId }
        }
        if (trackIds.isEmpty()) return 0
        userPreferences.addAudiobookOverrides(trackIds)
        clearAlbumSelection()
        return trackIds.size
    }

    /**
     * Same shape as [markSelectedAlbumsAsAudiobook] but for selected playlists. Pulls each
     * playlist's tracks from Room (via [getPlaylistTracksUseCase]) and merges the ids.
     */
    /**
     * Append a playlist's tracks to the live playback queue. Routing depends on what's
     * playing right now:
     *  - A real Room playlist (state.playlistId is a positive id) — append at the end so
     *    the running playlist plays through to its last track first.
     *  - Audiobook pseudo-playlist or library queue (null / pseudo id) — insert immediately
     *    after the current track so the user hears them next.
     * Returns the number of tracks added (0 if nothing was queued).
     */
    suspend fun enqueuePlaylist(playlistId: Long): Int {
        val tracks = getPlaylistTracksUseCase(playlistId)
        if (tracks.isEmpty()) return 0
        val pid = playbackController.state.value.playlistId
        val isRealPlaylist = pid != null && pid != AUDIOBOOK_PSEUDO_PLAYLIST_ID
        if (isRealPlaylist) {
            playbackController.enqueueAtEnd(tracks, sourcePlaylistId = playlistId)
        } else {
            playbackController.enqueueAfterCurrent(tracks, sourcePlaylistId = playlistId)
        }
        return tracks.size
    }

    suspend fun markSelectedPlaylistsAsAudiobook(): Int {
        val ids = selectedPlaylistIds.value.toList()
        if (ids.isEmpty()) return 0
        val trackIds = ids.flatMap { pid ->
            getPlaylistTracksUseCase(pid).map { it.mediaStoreId }
        }
        if (trackIds.isEmpty()) return 0
        userPreferences.addAudiobookOverrides(trackIds)
        clearPlaylistSelection()
        return trackIds.size
    }

    private fun publishSelectionState() {
        selectionStateHolder.set(
            selectedTrackIds.value.isNotEmpty() ||
                selectedPlaylistIds.value.isNotEmpty() ||
                selectedAlbumIds.value.isNotEmpty(),
        )
    }

    suspend fun addSingleToPlaylist(track: Track, playlistId: Long) {
        addTracksToPlaylist(playlistId, listOf(track))
    }

    suspend fun addSelectedToPlaylist(playlistId: Long): Int {
        val ids = selectedTrackIds.value.toList()
        if (ids.isEmpty()) return 0
        val tracks = mediaLibraryRepository.getTracksByIds(ids)
        if (tracks.isEmpty()) return 0
        addTracksToPlaylist(playlistId, tracks)
        clearSelection()
        return tracks.size
    }

    suspend fun createPlaylist(name: String): Long = createPlaylistUseCase(name)

    /**
     * Plays the library queue starting from [track]. Materialises the full ordered list under
     * the user's current sort + search query so seekToNext/seekToPrevious have somewhere to
     * go — playSingle would only put one track in the queue and prev/next would be no-ops.
     * playlistId = null marks this as the library context (ties into the `bookmark_lib` slot).
     */
    fun playLibraryQueueStartingFrom(track: Track) {
        viewModelScope.launch {
            val sort = userPreferences.observeLibrarySort().first()
            val query = searchQuery.value
            val tracks = mediaLibraryRepository.getLibraryTracks(sort, query)
            startQueue(tracks, track, playlistId = null)
        }
    }

    /**
     * Plays the audiobook queue starting from [track]. Same idea as the library queue but
     * scoped to native IS_AUDIOBOOK ∪ overrides; uses [AUDIOBOOK_PSEUDO_PLAYLIST_ID] so
     * widget bookmark slot + per-context loop mode work.
     */
    fun playAudiobookQueueStartingFrom(track: Track) {
        viewModelScope.launch {
            val sort = userPreferences.observeLibrarySort().first()
            val overrides = userPreferences.observeAudiobookOverrides().first()
            val tracks = mediaLibraryRepository.getAudiobookTracks(sort, overrides.toList())
            startQueue(tracks, track, playlistId = AUDIOBOOK_PSEUDO_PLAYLIST_ID)
        }
    }

    private suspend fun startQueue(tracks: List<Track>, target: Track, playlistId: Long?) {
        if (tracks.isEmpty()) return
        val index = tracks.indexOfFirst { it.mediaStoreId == target.mediaStoreId }
            .takeIf { it >= 0 } ?: 0
        playbackController.playQueue(playlistId = playlistId, tracks = tracks, startIndex = index)
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 300L
        const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
    }
}
