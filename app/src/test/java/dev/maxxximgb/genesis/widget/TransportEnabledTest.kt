package dev.maxxximgb.genesis.widget

import dev.maxxximgb.genesis.domain.model.LoopState
import org.junit.Assert.assertEquals
import org.junit.Test

class TransportEnabledTest {

    @Test
    fun offDisablesPrevOnFirstTrack() {
        val r = transportEnabled(LoopState.OFF, currentIndex = 0, lastIndex = 4)
        assertEquals(TransportEnabled(previous = false, next = true), r)
    }

    @Test
    fun offDisablesNextOnLastTrack() {
        val r = transportEnabled(LoopState.OFF, currentIndex = 4, lastIndex = 4)
        assertEquals(TransportEnabled(previous = true, next = false), r)
    }

    @Test
    fun offBothDisabledOnSingleTrackPlaylist() {
        val r = transportEnabled(LoopState.OFF, currentIndex = 0, lastIndex = 0)
        assertEquals(TransportEnabled(previous = false, next = false), r)
    }

    @Test
    fun offBothEnabledInMiddle() {
        val r = transportEnabled(LoopState.OFF, currentIndex = 2, lastIndex = 4)
        assertEquals(TransportEnabled(previous = true, next = true), r)
    }

    @Test
    fun repeatAllNeverDisablesOnFirst() {
        val r = transportEnabled(LoopState.REPEAT_ALL, currentIndex = 0, lastIndex = 4)
        assertEquals(TransportEnabled(previous = true, next = true), r)
    }

    @Test
    fun repeatAllNeverDisablesOnLast() {
        val r = transportEnabled(LoopState.REPEAT_ALL, currentIndex = 4, lastIndex = 4)
        assertEquals(TransportEnabled(previous = true, next = true), r)
    }

    @Test
    fun repeatOneNeverDisablesOnLast() {
        val r = transportEnabled(LoopState.REPEAT_ONE, currentIndex = 4, lastIndex = 4)
        assertEquals(TransportEnabled(previous = true, next = true), r)
    }

    @Test
    fun shuffleNeverDisablesOnFirst() {
        val r = transportEnabled(LoopState.SHUFFLE, currentIndex = 0, lastIndex = 4)
        assertEquals(TransportEnabled(previous = true, next = true), r)
    }

    @Test
    fun shuffleNeverDisablesOnLast() {
        val r = transportEnabled(LoopState.SHUFFLE, currentIndex = 4, lastIndex = 4)
        assertEquals(TransportEnabled(previous = true, next = true), r)
    }

    @Test
    fun negativeIndicesAreDisabled() {
        val r = transportEnabled(LoopState.OFF, currentIndex = -1, lastIndex = 4)
        assertEquals(TransportEnabled.DISABLED, r)
    }

    @Test
    fun emptyPlaylistIsDisabled() {
        val r = transportEnabled(LoopState.REPEAT_ALL, currentIndex = 0, lastIndex = -1)
        assertEquals(TransportEnabled.DISABLED, r)
    }

    @Test
    fun indexBeyondLastIsDisabled() {
        val r = transportEnabled(LoopState.OFF, currentIndex = 5, lastIndex = 4)
        assertEquals(TransportEnabled.DISABLED, r)
    }
}
