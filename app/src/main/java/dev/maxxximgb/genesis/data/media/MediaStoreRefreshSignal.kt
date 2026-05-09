package dev.maxxximgb.genesis.data.media

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-level "MediaStore changed under us" tick. The Library pager is one-shot per
 * query, so a delete or rename through [TrackMutator] doesn't propagate until the
 * pager rebuilds — observers of MediaStore exist but we don't run any. Anything that
 * mutates MediaStore on our side calls [emit]; anything that displays MediaStore
 * data (currently `LibraryViewModel.pagedTracks`) `flatMapLatest`s on this flow so a
 * tick rebuilds the pager from a fresh query.
 *
 * Replay 0 — the signal is a "right now, please refresh" trigger, not a state to
 * resubscribe to. New subscribers don't need to retry past mutations.
 */
@Singleton
class MediaStoreRefreshSignal @Inject constructor() {

    private val _ticks = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)
    val ticks: SharedFlow<Unit> = _ticks.asSharedFlow()

    suspend fun emit() {
        _ticks.emit(Unit)
    }
}
