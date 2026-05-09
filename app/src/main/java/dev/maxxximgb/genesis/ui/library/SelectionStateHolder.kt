package dev.maxxximgb.genesis.ui.library

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cross-screen flag for "library is in multi-select mode". `LibraryViewModel` writes to it
 * whenever its selected-set transitions empty ↔ non-empty; `NowPlayingViewModel` (and the
 * host `MainActivity`) read it to slide the mini Now Playing bar out of the way while the
 * user is selecting tracks. Lives outside any single ViewModel because the producer and
 * consumer sit in different Hilt scopes (per-route VMs).
 */
@Singleton
class SelectionStateHolder @Inject constructor() {
    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    fun set(active: Boolean) {
        _isActive.value = active
    }
}
