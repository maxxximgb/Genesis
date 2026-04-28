package com.maximg.player.widget

import androidx.media3.common.Player
import com.maximg.player.playback.PlaybackController
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetLoopModeTest {

    // ── fromState ─────────────────────────────────────────────────────────────

    @Test
    fun fromState_noShuffleRepeatOff_returnsNoRepeat() {
        assertEquals(
            PlaybackController.WidgetLoopMode.NO_REPEAT,
            PlaybackController.WidgetLoopMode.fromState(
                shuffleEnabled = false,
                repeatMode = Player.REPEAT_MODE_OFF
            )
        )
    }

    @Test
    fun fromState_noShuffleRepeatAll_returnsRepeatPlaylist() {
        assertEquals(
            PlaybackController.WidgetLoopMode.REPEAT_PLAYLIST,
            PlaybackController.WidgetLoopMode.fromState(
                shuffleEnabled = false,
                repeatMode = Player.REPEAT_MODE_ALL
            )
        )
    }

    @Test
    fun fromState_noShuffleRepeatOne_returnsRepeatTrack() {
        assertEquals(
            PlaybackController.WidgetLoopMode.REPEAT_TRACK,
            PlaybackController.WidgetLoopMode.fromState(
                shuffleEnabled = false,
                repeatMode = Player.REPEAT_MODE_ONE
            )
        )
    }

    @Test
    fun fromState_shuffleEnabled_returnsShuffle_regardlessOfRepeatMode() {
        listOf(Player.REPEAT_MODE_OFF, Player.REPEAT_MODE_ALL, Player.REPEAT_MODE_ONE).forEach { repeat ->
            assertEquals(
                "shuffle=true, repeat=$repeat should yield SHUFFLE",
                PlaybackController.WidgetLoopMode.SHUFFLE,
                PlaybackController.WidgetLoopMode.fromState(shuffleEnabled = true, repeatMode = repeat)
            )
        }
    }

    // ── next ──────────────────────────────────────────────────────────────────

    @Test
    fun next_cyclesThroughAllFourModes() {
        var mode = PlaybackController.WidgetLoopMode.NO_REPEAT
        mode = mode.next()
        assertEquals(PlaybackController.WidgetLoopMode.REPEAT_PLAYLIST, mode)
        mode = mode.next()
        assertEquals(PlaybackController.WidgetLoopMode.REPEAT_TRACK, mode)
        mode = mode.next()
        assertEquals(PlaybackController.WidgetLoopMode.SHUFFLE, mode)
        mode = mode.next()
        assertEquals(PlaybackController.WidgetLoopMode.NO_REPEAT, mode)
    }

    @Test
    fun next_fullCycleReturnsToStart() {
        val modes = PlaybackController.WidgetLoopMode.entries
        var mode = modes.first()
        repeat(modes.size) { mode = mode.next() }
        assertEquals(modes.first(), mode)
    }

    // ── mode properties ───────────────────────────────────────────────────────

    @Test
    fun noRepeat_hasExpectedPlaybackSettings() {
        val mode = PlaybackController.WidgetLoopMode.NO_REPEAT
        assertEquals(false, mode.shuffleEnabled)
        assertEquals(Player.REPEAT_MODE_OFF, mode.repeatMode)
    }

    @Test
    fun repeatPlaylist_hasExpectedPlaybackSettings() {
        val mode = PlaybackController.WidgetLoopMode.REPEAT_PLAYLIST
        assertEquals(false, mode.shuffleEnabled)
        assertEquals(Player.REPEAT_MODE_ALL, mode.repeatMode)
    }

    @Test
    fun repeatTrack_hasExpectedPlaybackSettings() {
        val mode = PlaybackController.WidgetLoopMode.REPEAT_TRACK
        assertEquals(false, mode.shuffleEnabled)
        assertEquals(Player.REPEAT_MODE_ONE, mode.repeatMode)
    }

    @Test
    fun shuffle_hasExpectedPlaybackSettings() {
        val mode = PlaybackController.WidgetLoopMode.SHUFFLE
        assertEquals(true, mode.shuffleEnabled)
        assertEquals(Player.REPEAT_MODE_OFF, mode.repeatMode)
    }

    // ── fromState ↔ properties round-trip ────────────────────────────────────

    @Test
    fun fromState_roundTrip_allModes() {
        PlaybackController.WidgetLoopMode.entries.forEach { mode ->
            assertEquals(
                "Round-trip failed for $mode",
                mode,
                PlaybackController.WidgetLoopMode.fromState(
                    shuffleEnabled = mode.shuffleEnabled,
                    repeatMode = mode.repeatMode
                )
            )
        }
    }
}
