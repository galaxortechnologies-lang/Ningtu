package com.nigutu.app.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class FormattersTest {

    @Test
    fun `formats short durations`() {
        assertEquals("0:00", formatDuration(0))
        assertEquals("0:05", formatDuration(5_000))
        assertEquals("3:45", formatDuration(225_000))
    }

    @Test
    fun `formats long durations with hours`() {
        assertEquals("1:00:00", formatDuration(3_600_000))
        assertEquals("2:03:04", formatDuration(7_384_000))
    }

    @Test
    fun `formats clock countdowns`() {
        assertEquals("0:00", formatClock(0))
        assertEquals("1:30", formatClock(90_000))
        assertEquals("0:00", formatClock(-500))
    }
}
