package com.nigutu.app.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LrcParserTest {

    @Test
    fun `parses simple timestamps`() {
        val lines = LrcParser.parse(
            """
            [00:12.50]First line
            [00:15.00]Second line
            """.trimIndent(),
        )

        assertEquals(2, lines.size)
        assertEquals(12_500L, lines[0].timeMs)
        assertEquals("First line", lines[0].text)
        assertEquals(15_000L, lines[1].timeMs)
    }

    @Test
    fun `ignores metadata and blank lines`() {
        val lines = LrcParser.parse(
            """
            [ar:Nigutu]
            [ti:Test song]

            [00:01.00]Hello
            [00:02.00]
            """.trimIndent(),
        )

        assertEquals(1, lines.size)
        assertEquals("Hello", lines[0].text)
    }

    @Test
    fun `expands repeated timestamps and sorts by time`() {
        val lines = LrcParser.parse(
            """
            [00:20.00][00:10.00]Chorus
            [00:05.00]Intro
            """.trimIndent(),
        )

        assertEquals(3, lines.size)
        assertEquals(listOf(5_000L, 10_000L, 20_000L), lines.map { it.timeMs })
        assertEquals("Chorus", lines[1].text)
    }

    @Test
    fun `active index uses the latest line at or before the position`() {
        val lines = LrcParser.parse(
            """
            [00:01.00]One
            [00:03.00]Two
            [00:05.00]Three
            """.trimIndent(),
        )

        assertEquals(-1, LrcParser.activeIndex(lines, 500L))
        assertEquals(0, LrcParser.activeIndex(lines, 1_000L))
        assertEquals(1, LrcParser.activeIndex(lines, 4_999L))
        assertEquals(2, LrcParser.activeIndex(lines, 60_000L))
        assertEquals(-1, LrcParser.activeIndex(emptyList(), 1_000L))
    }

    @Test
    fun `supports millisecond precision`() {
        val lines = LrcParser.parse("[00:02.123]Precise")
        assertEquals(2_123L, lines.single().timeMs)
    }

    @Test
    fun `blank input yields no lines`() {
        assertTrue(LrcParser.parse("").isEmpty())
        assertTrue(LrcParser.parse("   \n  ").isEmpty())
    }
}
