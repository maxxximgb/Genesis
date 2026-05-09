package dev.maxxximgb.genesis.ui.nowPlaying

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Schedules a one-shot fire after [start] is called with a duration. Cancellable; restartable.
 * Exposes a coarse 1-second-tick countdown via [remainingMs] for UI display.
 *
 * Doesn't pause playback itself — caller passes [onFire] (typically a controller.pause() bridge).
 */
class SleepTimer(
    private val scope: CoroutineScope,
    private val onFire: suspend () -> Unit,
    private val tickMs: Long = TICK_MS,
    private val nowMs: () -> Long = { System.currentTimeMillis() },
) {
    private val _remainingMs = MutableStateFlow<Long?>(null)
    val remainingMs: StateFlow<Long?> = _remainingMs.asStateFlow()

    private var job: Job? = null

    /** Starts (or restarts) a timer that fires after [durationMs]. Cancels any in-flight timer. */
    fun start(durationMs: Long) {
        cancel()
        if (durationMs <= 0L) return
        val deadline = nowMs() + durationMs
        _remainingMs.value = durationMs
        job = scope.launch {
            while (isActive) {
                val remaining = deadline - nowMs()
                if (remaining <= 0L) {
                    _remainingMs.value = null
                    onFire()
                    return@launch
                }
                _remainingMs.value = remaining
                delay(minOf(tickMs, remaining))
            }
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
        _remainingMs.value = null
    }

    val isActive: Boolean get() = job?.isActive == true

    private companion object {
        const val TICK_MS = 1_000L
    }
}
