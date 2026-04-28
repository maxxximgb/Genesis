package dev.maxxximgb.genesis.ui.playlistDetail

import org.junit.Assert.assertEquals
import org.junit.Test

class ComputeMoveTest {

    @Test
    fun emptySelectionLeavesOrderUntouched() {
        val ids = listOf(10L, 20L, 30L)
        assertEquals(ids, computeMove(ids, emptySet(), -1))
        assertEquals(ids, computeMove(ids, emptySet(), 1))
    }

    @Test
    fun zeroDirectionLeavesOrderUntouched() {
        val ids = listOf(10L, 20L, 30L)
        assertEquals(ids, computeMove(ids, setOf(20L), 0))
    }

    @Test
    fun moveUpSwapsWithPredecessor() {
        val ids = listOf(10L, 20L, 30L)
        val result = computeMove(ids, setOf(20L), -1)
        assertEquals(listOf(20L, 10L, 30L), result)
    }

    @Test
    fun moveDownSwapsWithSuccessor() {
        val ids = listOf(10L, 20L, 30L)
        val result = computeMove(ids, setOf(20L), 1)
        assertEquals(listOf(10L, 30L, 20L), result)
    }

    @Test
    fun moveUpFirstElementIsClampedNoop() {
        val ids = listOf(10L, 20L, 30L)
        val result = computeMove(ids, setOf(10L), -1)
        assertEquals(ids, result)
    }

    @Test
    fun moveDownLastElementIsClampedNoop() {
        val ids = listOf(10L, 20L, 30L)
        val result = computeMove(ids, setOf(30L), 1)
        assertEquals(ids, result)
    }

    @Test
    fun moveAdjacentSelectedElementsBubblesAsBlock() {
        // {20, 30} moved up: process idx=1 swap 20↔10 → [20,10,30]; idx=2 swap 30↔10 → [20,30,10]
        val ids = listOf(10L, 20L, 30L)
        val result = computeMove(ids, setOf(20L, 30L), -1)
        assertEquals(listOf(20L, 30L, 10L), result)
    }
}
