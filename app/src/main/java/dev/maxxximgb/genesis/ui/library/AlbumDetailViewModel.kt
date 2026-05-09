package dev.maxxximgb.genesis.ui.library

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import androidx.paging.map
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.maxxximgb.genesis.domain.model.Album
import dev.maxxximgb.genesis.domain.model.Playlist
import dev.maxxximgb.genesis.domain.model.SortOrder
import dev.maxxximgb.genesis.domain.model.Track
import dev.maxxximgb.genesis.data.preferences.UserPreferencesStore
import dev.maxxximgb.genesis.domain.repository.MediaLibraryRepository
import dev.maxxximgb.genesis.domain.usecase.library.ObserveAlbumsUseCase
import dev.maxxximgb.genesis.domain.usecase.library.TracksByAlbumUseCase
import dev.maxxximgb.genesis.domain.usecase.playlist.AddTracksToPlaylistUseCase
import dev.maxxximgb.genesis.domain.usecase.playlist.CreatePlaylistUseCase
import dev.maxxximgb.genesis.domain.usecase.playlist.ObservePlaylistsUseCase
import dev.maxxximgb.genesis.ui.navigation.Routes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeAlbums: ObserveAlbumsUseCase,
    observePlaylists: ObservePlaylistsUseCase,
    tracksByAlbum: TracksByAlbumUseCase,
    private val mediaLibraryRepository: MediaLibraryRepository,
    private val addTracksUseCase: AddTracksToPlaylistUseCase,
    private val createPlaylistUseCase: CreatePlaylistUseCase,
    private val userPreferences: UserPreferencesStore,
    private val selectionStateHolder: SelectionStateHolder,
) : ViewModel() {

    val albumId: Long = checkNotNull(savedStateHandle[Routes.AlbumDetail.ARG_ALBUM_ID])

    val album: StateFlow<Album?> = observeAlbums()
        .map { list -> list.firstOrNull { it.id == albumId } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
            initialValue = null,
        )

    /**
     * Session-scoped overlays for kebab-driven rename/delete on this screen — same idea as
     * [LibraryViewModel.pendingRenames] / [pendingDeletes]. Patches the cached pages in place
     * so the row reflects the change without a pager refresh that would scroll-jump to the top.
     */
    private val pendingRenames = MutableStateFlow<Map<Long, String>>(emptyMap())
    private val pendingDeletes = MutableStateFlow<Set<Long>>(emptySet())

    val pagedTracks: Flow<PagingData<Track>> = tracksByAlbum(albumId).flow
        .cachedIn(viewModelScope)
        .let { paging ->
            combine(paging, pendingRenames, pendingDeletes) { p, renames, deletes ->
                var out = p
                if (deletes.isNotEmpty()) out = out.filter { it.mediaStoreId !in deletes }
                if (renames.isNotEmpty()) out = out.map { t ->
                    renames[t.mediaStoreId]?.let { t.copy(title = it) } ?: t
                }
                out
            }
        }

    fun onTrackRenamedLocally(mediaStoreId: Long, newTitle: String) {
        pendingRenames.update { it + (mediaStoreId to newTitle) }
    }

    fun onTrackDeletedLocally(mediaStoreId: Long) {
        pendingDeletes.update { it + mediaStoreId }
    }

    val playlists: StateFlow<List<Playlist>> = observePlaylists()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
            initialValue = emptyList(),
        )

    /** Backs the per-row audiobook icon in the kebab menu — bookmark-add / bookmark-remove. */
    val audiobookOverrides: StateFlow<Set<Long>> = userPreferences.observeAudiobookOverrides()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
            initialValue = emptySet(),
        )

    fun toggleAudiobookOverride(mediaStoreId: Long) {
        viewModelScope.launch { userPreferences.toggleAudiobookOverride(mediaStoreId) }
    }

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    /** Materialises every track in this album (via the same `getTracksByAlbum` repo path
     *  the queue builders use) and appends them to [playlistId]. Returns the number added.
     *  We sort by TITLE_ASC so the order matches what the user sees on this screen. */
    suspend fun addAllToPlaylist(playlistId: Long): Int {
        val tracks = mediaLibraryRepository.getTracksByAlbum(albumId, SortOrder.TITLE_ASC)
        if (tracks.isEmpty()) return 0
        addTracksUseCase(playlistId, tracks)
        return tracks.size
    }

    suspend fun createAndAddAll(name: String): Int {
        val newId = createPlaylistUseCase(name.trim())
        return addAllToPlaylist(newId)
    }

    /** Single-track add from the kebab menu's "Add to playlist" entry. */
    suspend fun addSingleToPlaylist(track: Track, playlistId: Long) {
        addTracksUseCase(playlistId, listOf(track))
    }

    suspend fun addSingleToNewPlaylist(track: Track, name: String): Long {
        val newId = createPlaylistUseCase(name.trim())
        addTracksUseCase(newId, listOf(track))
        return newId
    }

    /** Mark every track in this album as an audiobook (idempotent override). */
    suspend fun markAllAsAudiobook(): Int {
        val tracks = mediaLibraryRepository.getTracksByAlbum(albumId, SortOrder.TITLE_ASC)
        if (tracks.isEmpty()) return 0
        userPreferences.addAudiobookOverrides(tracks.map { it.mediaStoreId })
        return tracks.size
    }

    fun toggleTrackSelection(track: Track) {
        _selectedIds.update { current ->
            if (track.mediaStoreId in current) current - track.mediaStoreId
            else current + track.mediaStoreId
        }
        // Keep the global selection signal in sync — the mini Now Playing bar reads it.
        selectionStateHolder.set(_selectedIds.value.isNotEmpty())
    }

    fun clearTrackSelection() {
        _selectedIds.value = emptySet()
        selectionStateHolder.set(false)
    }

    /**
     * Materialise the album's tracks once and pick the ones the user picked. We can't keep
     * Track instances in [_selectedIds] cheaply because PagingData hands us snapshots — so
     * we re-fetch on commit. Album sizes are bounded so this is fine.
     */
    suspend fun addSelectedToPlaylist(playlistId: Long): Int {
        val ids = _selectedIds.value
        if (ids.isEmpty()) return 0
        val tracks = mediaLibraryRepository.getTracksByAlbum(albumId, SortOrder.TITLE_ASC)
            .filter { it.mediaStoreId in ids }
        if (tracks.isEmpty()) return 0
        addTracksUseCase(playlistId, tracks)
        clearTrackSelection()
        return tracks.size
    }

    suspend fun addSelectedToNewPlaylist(name: String): Int {
        val newId = createPlaylistUseCase(name.trim())
        return addSelectedToPlaylist(newId)
    }

    override fun onCleared() {
        // Avoid stranding the global selection signal when the user back-presses out of
        // the screen with a selection still active.
        selectionStateHolder.set(false)
        super.onCleared()
    }

    private companion object {
        const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
    }
}
