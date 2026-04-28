package dev.maxxximgb.genesis.ui.nowPlaying

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.maxxximgb.genesis.domain.model.Track
import dev.maxxximgb.genesis.domain.playback.PlaybackController
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NowPlayingViewModel @Inject constructor(
    private val controller: PlaybackController,
) : ViewModel() {

    val uiState: StateFlow<NowPlayingUiState> = controller.state

    fun togglePlayPause() {
        viewModelScope.launch { controller.togglePlayPause() }
    }

    fun next() {
        viewModelScope.launch { controller.seekToNext() }
    }

    fun previous() {
        viewModelScope.launch { controller.seekToPrevious() }
    }

    fun playSingle(track: Track) {
        viewModelScope.launch { controller.playSingle(track) }
    }
}
