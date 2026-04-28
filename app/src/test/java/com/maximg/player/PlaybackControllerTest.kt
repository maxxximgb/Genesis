package com.maximg.player

import androidx.media3.common.Player
import com.maximg.player.playback.PlaybackController
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackControllerTest {
    @Test
    fun cycleRepeatMode_cyclesThroughModes() {
        assertEquals(Player.REPEAT_MODE_ONE, PlaybackController.cycleRepeatMode(Player.REPEAT_MODE_OFF))
        assertEquals(Player.REPEAT_MODE_ALL, PlaybackController.cycleRepeatMode(Player.REPEAT_MODE_ONE))
        assertEquals(Player.REPEAT_MODE_OFF, PlaybackController.cycleRepeatMode(Player.REPEAT_MODE_ALL))
    }

    @Test
    fun cycleWidgetLoopMode_cyclesThroughFourStates() {
        assertEquals(
            PlaybackController.WidgetLoopMode.REPEAT_PLAYLIST,
            PlaybackController.cycleWidgetLoopMode(
                shuffleEnabled = false,
                repeatMode = Player.REPEAT_MODE_OFF
            )
        )
        assertEquals(
            PlaybackController.WidgetLoopMode.REPEAT_TRACK,
            PlaybackController.cycleWidgetLoopMode(
                shuffleEnabled = false,
                repeatMode = Player.REPEAT_MODE_ALL
            )
        )
        assertEquals(
            PlaybackController.WidgetLoopMode.SHUFFLE,
            PlaybackController.cycleWidgetLoopMode(
                shuffleEnabled = false,
                repeatMode = Player.REPEAT_MODE_ONE
            )
        )
        assertEquals(
            PlaybackController.WidgetLoopMode.NO_REPEAT,
            PlaybackController.cycleWidgetLoopMode(
                shuffleEnabled = true,
                repeatMode = Player.REPEAT_MODE_OFF
            )
        )
    }
}
