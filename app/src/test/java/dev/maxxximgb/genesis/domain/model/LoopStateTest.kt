package dev.maxxximgb.genesis.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class LoopStateTest {

    @Test
    fun cycleOffToRepeatAll() {
        assertEquals(LoopState.REPEAT_ALL, nextLoopState(LoopState.OFF))
    }

    @Test
    fun cycleRepeatAllToRepeatOne() {
        assertEquals(LoopState.REPEAT_ONE, nextLoopState(LoopState.REPEAT_ALL))
    }

    @Test
    fun cycleRepeatOneToShuffle() {
        assertEquals(LoopState.SHUFFLE, nextLoopState(LoopState.REPEAT_ONE))
    }

    @Test
    fun cycleShuffleBackToOff() {
        assertEquals(LoopState.OFF, nextLoopState(LoopState.SHUFFLE))
    }

    @Test
    fun loopStateOfMapsRepeatModeAll() {
        assertEquals(LoopState.REPEAT_ALL, loopStateOf(RepeatMode.ALL, shuffleEnabled = false))
    }

    @Test
    fun loopStateOfMapsRepeatModeOne() {
        assertEquals(LoopState.REPEAT_ONE, loopStateOf(RepeatMode.ONE, shuffleEnabled = false))
    }

    @Test
    fun loopStateOfMapsShuffle() {
        assertEquals(LoopState.SHUFFLE, loopStateOf(RepeatMode.OFF, shuffleEnabled = true))
    }

    @Test
    fun loopStateOfMapsShuffleEvenWhenRepeatActive() {
        // Shuffle takes precedence in the displayed loop state.
        assertEquals(LoopState.SHUFFLE, loopStateOf(RepeatMode.ALL, shuffleEnabled = true))
    }

    @Test
    fun loopStateOfMapsOff() {
        assertEquals(LoopState.OFF, loopStateOf(RepeatMode.OFF, shuffleEnabled = false))
    }

    @Test
    fun toRepeatAndShuffleRoundTrip() {
        for (mode in LoopState.entries) {
            val (repeat, shuffle) = mode.toRepeatAndShuffle()
            assertEquals(mode, loopStateOf(repeat, shuffle))
        }
    }
}
