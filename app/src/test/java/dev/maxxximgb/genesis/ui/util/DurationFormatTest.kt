package dev.maxxximgb.genesis.ui.util

import org.junit.Assert.assertEquals
import org.junit.Test

class DurationFormatTest {

    @Test fun zero() = assertEquals("0:00", formatDuration(0L))
    @Test fun subSecond() = assertEquals("0:00", formatDuration(999L))
    @Test fun oneSecond() = assertEquals("0:01", formatDuration(1_000L))
    @Test fun fiftyNineSeconds() = assertEquals("0:59", formatDuration(59_000L))
    @Test fun oneMinute() = assertEquals("1:00", formatDuration(60_000L))
    @Test fun threeFortyTwo() = assertEquals("3:42", formatDuration(3 * 60_000L + 42_000L))
    @Test fun oneHour() = assertEquals("1:00:00", formatDuration(3600_000L))
    @Test fun complexHourly() = assertEquals("1:02:15", formatDuration(3735_000L))
    @Test fun negativeIsZero() = assertEquals("0:00", formatDuration(-5_000L))
}
