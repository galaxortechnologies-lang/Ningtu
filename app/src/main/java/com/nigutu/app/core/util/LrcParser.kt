package com.nigutu.app.core.util

import com.nigutu.app.domain.model.LyricLine

/**
 * Parser for LRC lyric files: `[mm:ss.xx]line`, optionally with several
 * timestamps on one line and metadata tags like `[ar:...]`.
 */
object LrcParser {

    private val TIMESTAMP = Regex("""\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?]""")
    private val METADATA = Regex("""^\[(ar|ti|al|by|offset):.*]$""", RegexOption.IGNORE_CASE)

    /** Parses LRC text into sorted lyric lines. Blank lines are dropped. */
    fun parse(lrc: String): List<LyricLine> {
        if (lrc.isBlank()) return emptyList()
        val lines = mutableListOf<LyricLine>()
        lrc.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty() || METADATA.matches(line)) return@forEach
            val stamps = TIMESTAMP.findAll(line).toList()
            if (stamps.isEmpty()) return@forEach
            val text = line.substring(stamps.last().range.last + 1).trim()
            if (text.isEmpty()) return@forEach
            stamps.forEach { match ->
                val minutes = match.groupValues[1].toLongOrNull() ?: 0L
                val seconds = match.groupValues[2].toLongOrNull() ?: 0L
                val fractionRaw = match.groupValues[3]
                val fraction = when (fractionRaw.length) {
                    0 -> 0L
                    1 -> fractionRaw.toLong() * 100L
                    2 -> fractionRaw.toLong() * 10L
                    else -> fractionRaw.take(3).toLong()
                }
                lines += LyricLine(minutes * 60_000L + seconds * 1_000L + fraction, text)
            }
        }
        return lines.sortedBy { it.timeMs }
    }

    /** Index of the line active at [positionMs], or -1 before the first line. */
    fun activeIndex(lines: List<LyricLine>, positionMs: Long): Int {
        if (lines.isEmpty()) return -1
        var low = 0
        var high = lines.size - 1
        var result = -1
        while (low <= high) {
            val mid = (low + high) / 2
            if (lines[mid].timeMs <= positionMs) {
                result = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return result
    }
}
