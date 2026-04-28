package com.maximg.player.util

import org.junit.Assert.assertEquals
import org.junit.Test

class CollectionsMoveTest {
    @Test
    fun move_forward_insertsAtAdjustedIndex() {
        val items = mutableListOf("a", "b", "c", "d")

        items.move(from = 1, to = 3)

        assertEquals(listOf("a", "c", "b", "d"), items)
    }

    @Test
    fun move_backward_insertsAtTargetIndex() {
        val items = mutableListOf("a", "b", "c", "d")

        items.move(from = 3, to = 1)

        assertEquals(listOf("a", "d", "b", "c"), items)
    }

    @Test
    fun move_sameIndex_keepsListUnchanged() {
        val items = mutableListOf("a", "b", "c")

        items.move(from = 2, to = 2)

        assertEquals(listOf("a", "b", "c"), items)
    }
}
