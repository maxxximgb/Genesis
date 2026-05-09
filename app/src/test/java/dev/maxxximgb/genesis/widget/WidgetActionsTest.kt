package dev.maxxximgb.genesis.widget

import dev.maxxximgb.genesis.domain.model.LoopState
import org.junit.Assert.assertEquals
import org.junit.Test

class ResolveOffsetIndexTest {

    @Test
    fun offClampsForwardAtLast() {
        // size=3, base=2 (last), offset=+1 → stays at 2 in OFF (no wrap).
        assertEquals(2, resolveOffsetIndex(base = 2, offset = +1, size = 3, mode = LoopState.OFF))
    }

    @Test
    fun offClampsBackwardAtFirst() {
        assertEquals(0, resolveOffsetIndex(base = 0, offset = -1, size = 3, mode = LoopState.OFF))
    }

    @Test
    fun offNormalStep() {
        assertEquals(2, resolveOffsetIndex(base = 1, offset = +1, size = 3, mode = LoopState.OFF))
        assertEquals(0, resolveOffsetIndex(base = 1, offset = -1, size = 3, mode = LoopState.OFF))
    }

    @Test
    fun repeatOneClampsLikeOff() {
        // REPEAT_ONE doesn't change "step" semantics for prev/next — clamp at edges.
        assertEquals(2, resolveOffsetIndex(base = 2, offset = +1, size = 3, mode = LoopState.REPEAT_ONE))
        assertEquals(0, resolveOffsetIndex(base = 0, offset = -1, size = 3, mode = LoopState.REPEAT_ONE))
    }

    @Test
    fun repeatAllWrapsForward() {
        assertEquals(0, resolveOffsetIndex(base = 2, offset = +1, size = 3, mode = LoopState.REPEAT_ALL))
    }

    @Test
    fun repeatAllWrapsBackward() {
        assertEquals(2, resolveOffsetIndex(base = 0, offset = -1, size = 3, mode = LoopState.REPEAT_ALL))
    }

    @Test
    fun shuffleWrapsLikeRepeatAll() {
        // Picking a random next track is the player's job; the offset semantics here just
        // mean "advance by one in the ordered queue" — same wrap behavior as REPEAT_ALL.
        assertEquals(0, resolveOffsetIndex(base = 2, offset = +1, size = 3, mode = LoopState.SHUFFLE))
        assertEquals(2, resolveOffsetIndex(base = 0, offset = -1, size = 3, mode = LoopState.SHUFFLE))
    }

    @Test
    fun zeroSizeReturnsZero() {
        assertEquals(0, resolveOffsetIndex(base = 0, offset = +1, size = 0, mode = LoopState.OFF))
    }

    @Test
    fun largeOffsetIsHandled() {
        // size=4, base=0, offset=+10 in REPEAT_ALL → ((0+10) % 4 + 4) % 4 = 2
        assertEquals(2, resolveOffsetIndex(base = 0, offset = +10, size = 4, mode = LoopState.REPEAT_ALL))
        // size=4, base=0, offset=-10 in REPEAT_ALL → 2 as well by symmetry
        assertEquals(2, resolveOffsetIndex(base = 0, offset = -10, size = 4, mode = LoopState.REPEAT_ALL))
    }
}
