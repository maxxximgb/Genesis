package dev.maxxximgb.genesis.data.playback

import androidx.media3.common.Player
import dev.maxxximgb.genesis.domain.model.RepeatMode
import org.junit.Assert.assertEquals
import org.junit.Test

class RepeatModeMappingTest {

    @Test
    fun domainEnumMapsToPlayerInts() {
        assertEquals(Player.REPEAT_MODE_OFF, RepeatMode.OFF.toPlayer())
        assertEquals(Player.REPEAT_MODE_ALL, RepeatMode.ALL.toPlayer())
        assertEquals(Player.REPEAT_MODE_ONE, RepeatMode.ONE.toPlayer())
    }

    @Test
    fun playerIntsMapBackToDomainEnum() {
        assertEquals(RepeatMode.OFF, Player.REPEAT_MODE_OFF.toRepeatMode())
        assertEquals(RepeatMode.ALL, Player.REPEAT_MODE_ALL.toRepeatMode())
        assertEquals(RepeatMode.ONE, Player.REPEAT_MODE_ONE.toRepeatMode())
    }

    @Test
    fun unknownPlayerValueFallsBackToOff() {
        assertEquals(RepeatMode.OFF, 999.toRepeatMode())
    }

    @Test
    fun roundTripIsStable() {
        for (mode in RepeatMode.entries) {
            assertEquals(mode, mode.toPlayer().toRepeatMode())
        }
    }
}
