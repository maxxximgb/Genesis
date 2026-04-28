package com.maximg.player.ui.screens

import com.maximg.player.data.PlaylistTrackItem
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaylistDetailScreenMoveTest {

    private fun track(id: Long) = PlaylistTrackItem(
        mediaStoreId = id,
        title = "Track $id",
        artist = null,
        album = null,
        durationMs = 0L,
        contentUri = "",
        position = 0
    )

    private fun ids(vararg ids: Long): Set<Long> = ids.toSet()

    // ── moveSelectedTracksUp ──────────────────────────────────────────────────

    @Test
    fun moveUp_noSelection_returnsSameOrder() {
        val list = listOf(track(1), track(2), track(3))
        assertEquals(list, list.moveSelectedTracksUp(emptySet()))
    }

    @Test
    fun moveUp_allSelected_returnsSameOrder() {
        val list = listOf(track(1), track(2), track(3))
        assertEquals(list, list.moveSelectedTracksUp(ids(1, 2, 3)))
    }

    @Test
    fun moveUp_singleTrackAtTop_returnsSameOrder() {
        val list = listOf(track(1), track(2), track(3))
        assertEquals(list, list.moveSelectedTracksUp(ids(1)))
    }

    @Test
    fun moveUp_singleTrackInMiddle_movesUpByOne() {
        val list = listOf(track(1), track(2), track(3), track(4), track(5))
        val result = list.moveSelectedTracksUp(ids(3))
        assertEquals(listOf(track(1), track(3), track(2), track(4), track(5)), result)
    }

    @Test
    fun moveUp_singleTrackAtBottom_movesUpByOne() {
        val list = listOf(track(1), track(2), track(3))
        val result = list.moveSelectedTracksUp(ids(3))
        assertEquals(listOf(track(1), track(3), track(2)), result)
    }

    @Test
    fun moveUp_twoNonAdjacentTracks_bothMoveUp() {
        val list = listOf(track(1), track(2), track(3), track(4), track(5))
        val result = list.moveSelectedTracksUp(ids(2, 4))
        assertEquals(listOf(track(2), track(1), track(4), track(3), track(5)), result)
    }

    @Test
    fun moveUp_adjacentSelectedTracks_moveTogetherUpByOne() {
        val list = listOf(track(1), track(2), track(3), track(4), track(5))
        val result = list.moveSelectedTracksUp(ids(3, 4))
        assertEquals(listOf(track(1), track(3), track(4), track(2), track(5)), result)
    }

    @Test
    fun moveUp_adjacentSelectedTracksAtTop_returnsSameOrder() {
        val list = listOf(track(1), track(2), track(3), track(4))
        assertEquals(list, list.moveSelectedTracksUp(ids(1, 2)))
    }

    @Test
    fun moveUp_singleElementList_returnsSameOrder() {
        val list = listOf(track(1))
        assertEquals(list, list.moveSelectedTracksUp(ids(1)))
    }

    // ── moveSelectedTracksDown ────────────────────────────────────────────────

    @Test
    fun moveDown_noSelection_returnsSameOrder() {
        val list = listOf(track(1), track(2), track(3))
        assertEquals(list, list.moveSelectedTracksDown(emptySet()))
    }

    @Test
    fun moveDown_allSelected_returnsSameOrder() {
        val list = listOf(track(1), track(2), track(3))
        assertEquals(list, list.moveSelectedTracksDown(ids(1, 2, 3)))
    }

    @Test
    fun moveDown_singleTrackAtBottom_returnsSameOrder() {
        val list = listOf(track(1), track(2), track(3))
        assertEquals(list, list.moveSelectedTracksDown(ids(3)))
    }

    @Test
    fun moveDown_singleTrackInMiddle_movesDownByOne() {
        val list = listOf(track(1), track(2), track(3), track(4), track(5))
        val result = list.moveSelectedTracksDown(ids(3))
        assertEquals(listOf(track(1), track(2), track(4), track(3), track(5)), result)
    }

    @Test
    fun moveDown_singleTrackAtTop_movesDownByOne() {
        val list = listOf(track(1), track(2), track(3))
        val result = list.moveSelectedTracksDown(ids(1))
        assertEquals(listOf(track(2), track(1), track(3)), result)
    }

    @Test
    fun moveDown_twoNonAdjacentTracks_bothMoveDown() {
        val list = listOf(track(1), track(2), track(3), track(4), track(5))
        val result = list.moveSelectedTracksDown(ids(2, 4))
        assertEquals(listOf(track(1), track(3), track(2), track(5), track(4)), result)
    }

    @Test
    fun moveDown_adjacentSelectedTracks_moveTogetherDownByOne() {
        val list = listOf(track(1), track(2), track(3), track(4), track(5))
        val result = list.moveSelectedTracksDown(ids(3, 4))
        assertEquals(listOf(track(1), track(2), track(5), track(3), track(4)), result)
    }

    @Test
    fun moveDown_adjacentSelectedTracksAtBottom_returnsSameOrder() {
        val list = listOf(track(1), track(2), track(3), track(4))
        assertEquals(list, list.moveSelectedTracksDown(ids(3, 4)))
    }

    @Test
    fun moveDown_singleElementList_returnsSameOrder() {
        val list = listOf(track(1))
        assertEquals(list, list.moveSelectedTracksDown(ids(1)))
    }
}
