package com.maximg.player.util

import org.junit.Assert.assertEquals
import org.junit.Test

class DurationFormatTest {
    @Test
    fun formatDuration_nonPositive_returnsZeroMinutes() {
        assertEquals("0:00", formatDuration(0L))
        assertEquals("0:00", formatDuration(-1L))
    }

    @Test
    fun formatDuration_underHour_returnsMinutesSeconds() {
        assertEquals("1:05", formatDuration(65_000L))
        assertEquals("59:59", formatDuration(3_599_000L))
    }

    @Test
    fun formatDuration_oneHourOrMore_returnsHoursMinutesSeconds() {
        assertEquals("1:00:00", formatDuration(3_600_000L))
        assertEquals("2:05:09", formatDuration(7_509_000L))
    }
}
