package dev.maxxximgb.genesis.data.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class QueueSerializerTest {

    @Test
    fun emptyListSerializesToEmptyString() {
        assertEquals("", QueueSerializer.serialize(emptyList()))
    }

    @Test
    fun emptyStringDeserializesToEmptyList() {
        assertEquals(emptyList<Long>(), QueueSerializer.deserialize(""))
    }

    @Test
    fun blankStringDeserializesToEmptyList() {
        assertEquals(emptyList<Long>(), QueueSerializer.deserialize("   "))
    }

    @Test
    fun roundTripPreservesOrder() {
        val ids = listOf(10L, 42L, 99L, 7L, 13L)
        val csv = QueueSerializer.serialize(ids)
        assertEquals("10,42,99,7,13", csv)
        assertEquals(ids, QueueSerializer.deserialize(csv))
    }

    @Test
    fun roundTripPreservesLargeQueue() {
        val ids = (1L..100L).toList()
        assertEquals(ids, QueueSerializer.deserialize(QueueSerializer.serialize(ids)))
    }

    @Test
    fun deserializeSkipsNonNumericTokens() {
        assertEquals(listOf(1L, 2L, 3L), QueueSerializer.deserialize("1,foo,2,,3"))
    }

    @Test
    fun deserializeTrimsWhitespace() {
        assertEquals(listOf(1L, 2L, 3L), QueueSerializer.deserialize(" 1 , 2 , 3 "))
    }
}
