package com.nigutu.app.domain.model

/**
 * A single playable track, from the device library or an online catalog.
 *
 * [id] is stable and namespaced: `local:<mediaId>` or `remote:<provider>:<trackId>`.
 */
data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uri: String,
    val artworkUri: String? = null,
    val isLocal: Boolean = true,
    val trackNumber: Int = 0,
    val year: Int = 0,
)

/** Aggregated album view built from the track list. */
data class Album(
    val name: String,
    val artist: String,
    val artworkUri: String?,
    val songCount: Int,
)

/** Aggregated artist view built from the track list. */
data class Artist(
    val name: String,
    val songCount: Int,
    val artworkUri: String?,
)

/** A user-created playlist with its track count. */
data class Playlist(
    val id: Long,
    val name: String,
    val description: String,
    val createdAt: Long,
    val songCount: Int,
)

/** One timed lyric line, used for synchronised lyrics. */
data class LyricLine(
    val timeMs: Long,
    val text: String,
)

/** Result of a lyrics lookup. Synced lines are empty when only plain lyrics exist. */
data class Lyrics(
    val plain: String,
    val synced: List<LyricLine>,
    val source: String,
) {
    val isSynced: Boolean get() = synced.isNotEmpty()
}

/** A genre tile on the Explore screen. */
data class Genre(
    val id: Long,
    val name: String,
)

/** Snapshot of playback state exposed to the UI. */
data class PlaybackState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isBuffering: Boolean = false,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val queue: List<Song> = emptyList(),
) {
    val hasNext: Boolean get() = queue.isNotEmpty()
    val progress: Float
        get() = if (durationMs > 0L) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
}

/** Repeat behaviour, mirroring Media3's repeat modes. */
enum class RepeatMode { OFF, ONE, ALL }

/** Theme preference, persisted in DataStore. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** User preferences persisted in DataStore. */
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val wifiOnlyStreaming: Boolean = true,
    val equalizerEnabled: Boolean = false,
    val bassBoostStrength: Int = 0,
    val crossfadeMs: Int = 0,
    val playbackSpeed: Float = 1f,
)
