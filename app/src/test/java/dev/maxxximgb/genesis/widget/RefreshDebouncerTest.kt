package dev.maxxximgb.genesis.widget

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
class RefreshDebouncerTest {

    @Test
    fun singleRequestTriggersOnceAfterDelay() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)
        val counter = AtomicInteger(0)
        val debouncer = RefreshDebouncer(scope, debounceMs = 250L) { counter.incrementAndGet() }

        debouncer.request()
        scope.advanceUntilIdle()

        assertEquals(1, counter.get())
    }

    @Test
    fun burstWithinWindowCollapsesToOne() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)
        val counter = AtomicInteger(0)
        val debouncer = RefreshDebouncer(scope, debounceMs = 250L) { counter.incrementAndGet() }

        repeat(10) { debouncer.request() }
        scope.advanceUntilIdle()

        assertEquals(1, counter.get())
    }

    @Test
    fun requestsAfterTriggerStartFreshWindow() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)
        val counter = AtomicInteger(0)
        val debouncer = RefreshDebouncer(scope, debounceMs = 250L) { counter.incrementAndGet() }

        debouncer.request()
        scope.advanceUntilIdle()
        assertEquals(1, counter.get())

        debouncer.request()
        scope.advanceUntilIdle()
        assertEquals(2, counter.get())
    }

    @Test
    fun noTriggerBeforeDelayElapses() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)
        val counter = AtomicInteger(0)
        val debouncer = RefreshDebouncer(scope, debounceMs = 250L) { counter.incrementAndGet() }

        debouncer.request()
        scope.testScheduler.advanceTimeBy(200L)

        assertEquals(0, counter.get())

        scope.testScheduler.advanceTimeBy(100L)
        scope.advanceUntilIdle()

        assertEquals(1, counter.get())
    }

    @Test
    fun debounceWindowIsUnderOneSecondToSatisfyWidgetReflectInvariant() {
        // Invariant SPEC §14.4: widget must reflect playback in <1s.
        // WidgetUpdater uses 250ms; this guards against drift if someone bumps the constant.
        assertEquals(true, WidgetUpdater.DEBOUNCE_MS < 1000L)
    }
}
