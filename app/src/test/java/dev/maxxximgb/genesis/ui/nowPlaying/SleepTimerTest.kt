package dev.maxxximgb.genesis.ui.nowPlaying

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
class SleepTimerTest {

    @Test
    fun firesOnceWhenWindowElapses() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)
        val fires = AtomicInteger(0)
        var virtualClock = 0L
        val timer = SleepTimer(
            scope = scope,
            onFire = { fires.incrementAndGet() },
            nowMs = { virtualClock },
        )

        timer.start(60_000L)
        virtualClock += 60_001L
        scope.testScheduler.advanceTimeBy(60_001L)
        scope.advanceUntilIdle()

        assertEquals(1, fires.get())
        assertNull(timer.remainingMs.value)
        assertFalse(timer.isActive)
    }

    @Test
    fun cancelBeforeFireDoesNotInvoke() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)
        val fires = AtomicInteger(0)
        var virtualClock = 0L
        val timer = SleepTimer(
            scope = scope,
            onFire = { fires.incrementAndGet() },
            nowMs = { virtualClock },
        )

        timer.start(60_000L)
        virtualClock += 30_000L
        scope.testScheduler.advanceTimeBy(30_000L)
        timer.cancel()
        virtualClock += 60_000L
        scope.testScheduler.advanceTimeBy(60_000L)
        scope.advanceUntilIdle()

        assertEquals(0, fires.get())
        assertNull(timer.remainingMs.value)
    }

    @Test
    fun restartReplacesPreviousJob() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)
        val fires = AtomicInteger(0)
        var virtualClock = 0L
        val timer = SleepTimer(
            scope = scope,
            onFire = { fires.incrementAndGet() },
            nowMs = { virtualClock },
        )

        timer.start(60_000L)
        // Restart with a shorter window before the first one would have fired.
        virtualClock += 10_000L
        scope.testScheduler.advanceTimeBy(10_000L)
        timer.start(30_000L)

        virtualClock += 30_001L
        scope.testScheduler.advanceTimeBy(30_001L)
        scope.advanceUntilIdle()

        // Only the restarted one should have fired; original 60s window cancelled.
        assertEquals(1, fires.get())
    }

    @Test
    fun remainingMsCountsDownWhileActive() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)
        var virtualClock = 0L
        val timer = SleepTimer(
            scope = scope,
            onFire = { /* no-op */ },
            nowMs = { virtualClock },
        )

        timer.start(60_000L)
        // Initial value set synchronously by SleepTimer.start() before launching the coroutine.
        assertEquals(60_000L, timer.remainingMs.value)

        // Don't advanceUntilIdle — the timer's loop with virtualClock pinned at 0 would spin
        // forever (each tick recomputes remaining = 60_000, schedules another delay).
        // advanceTimeBy is bounded, so it processes ticks up to its budget and stops.
        virtualClock += 10_000L
        scope.testScheduler.advanceTimeBy(10_000L)
        val remaining = timer.remainingMs.value
        assertNotNull(remaining)
        assertTrue("remaining was $remaining, expected ~50_000ms", remaining!! in 49_000L..51_000L)
        timer.cancel()
    }
}
