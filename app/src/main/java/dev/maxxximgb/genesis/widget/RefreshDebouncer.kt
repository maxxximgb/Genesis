package dev.maxxximgb.genesis.widget

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Coalesces a burst of [request] calls within [debounceMs] into a single [onTrigger] invocation.
 *
 * While a debounce is pending, additional requests are dropped silently — only the first request
 * in a burst arms the timer.
 */
class RefreshDebouncer(
    private val scope: CoroutineScope,
    private val debounceMs: Long,
    private val onTrigger: suspend () -> Unit,
) {
    private val mutex = Mutex()
    private var pending: Job? = null

    fun request() {
        scope.launch {
            mutex.withLock {
                if (pending?.isActive == true) return@withLock
                pending = scope.launch {
                    delay(debounceMs)
                    onTrigger()
                }
            }
        }
    }
}
