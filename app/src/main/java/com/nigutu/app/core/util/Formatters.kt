package com.nigutu.app.core.util

import java.util.concurrent.TimeUnit

/** Formats a duration in milliseconds as `m:ss` or `h:mm:ss`. */
fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0L) return "0:00"
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(durationMs)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

/** Formats a millisecond timestamp as a clock value, used by the sleep timer. */
fun formatClock(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

/** Builds a one-line `Title · Artist` summary. */
fun songSummary(title: String, artist: String): String = "$title · $artist"
