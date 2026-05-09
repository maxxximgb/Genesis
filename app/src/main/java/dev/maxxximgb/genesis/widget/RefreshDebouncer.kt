package dev.maxxximgb.genesis.widget

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Coalesces a burst of [request] calls within [debounceMs] into at most two [onTrigger]
 * invocations:
 *  - the first request arms a leading-edge timer that fires after [debounceMs];
 *  - any request that arrives while the timer is pending sets a trailing flag — when the
 *    timer fires, if the flag was set, we run again after another [debounceMs] window.
 *
 * Without the trailing pass, a state change that lands at the very end of a burst (e.g., the
 * user toggles repeat → shuffle → off in 200ms) would be dropped: only the first request
 * would have triggered, and refreshAllWidgets would render the leading state. The trailing
 * pass guarantees the *latest* state is always reflected, at the cost of one extra refresh
 * per burst.
 */
class RefreshDebouncer(
    private val scope: CoroutineScope,
    private val debounceMs: Long,
    private val onTrigger: suspend () -> Unit,
) {
    private val mutex = Mutex()
    private var pending: Job? = null
    private var trailing = false

    fun request() {
        scope.launch {
            mutex.withLock {
                if (pending?.isActive == true) {
                    // A loop is already running; flag the trailing pass and let it pick this up.
                    trailing = true
                } else {
                    pending = scope.launch { runFireLoop() }
                }
            }
        }
    }

    private suspend fun runFireLoop() {
        while (true) {
            delay(debounceMs)
            onTrigger()
            val keepGoing = mutex.withLock {
                if (trailing) {
                    trailing = false
                    true
                } else {
                    false
                }
            }
            if (!keepGoing) break
        }
    }
}
