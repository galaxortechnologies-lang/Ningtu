package com.nigutu.app.data.remote

import com.nigutu.app.core.util.LrcParser
import com.nigutu.app.domain.model.Lyrics
import com.nigutu.app.domain.model.Song
import com.nigutu.app.domain.repository.LyricsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lyrics from LRCLIB, a free and open lyrics database.
 *
 * LRCLIB returns both plain and time-synchronised lyrics; Nigutu prefers the
 * synced variant and falls back to plain text.
 */
@Singleton
class LrcLibLyricsRepository @Inject constructor(
    private val http: JsonHttpClient,
) : LyricsRepository {

    override suspend fun lyricsFor(song: Song): Result<Lyrics> = withContext(Dispatchers.IO) {
        http.get<List<LrcLibRecord>>(
            http.url(
                "$BASE/api/search",
                mapOf(
                    "track_name" to song.title,
                    "artist_name" to song.artist,
                    "album_name" to song.album.ifBlank { null },
                ),
            ),
        ).mapCatching { records ->
            val withSynced = records.firstOrNull { !it.syncedLyrics.isNullOrBlank() }
            val withPlain = records.firstOrNull { !it.plainLyrics.isNullOrBlank() }
            when {
                withSynced?.syncedLyrics != null -> Lyrics(
                    plain = withSynced.plainLyrics.orEmpty(),
                    synced = LrcParser.parse(withSynced.syncedLyrics),
                    source = "LRCLIB",
                )
                withPlain?.plainLyrics != null -> Lyrics(
                    plain = withPlain.plainLyrics,
                    synced = emptyList(),
                    source = "LRCLIB",
                )
                else -> throw NoSuchElementException("No lyrics found for ${song.title}")
            }
        }
    }

    @Serializable
    private data class LrcLibRecord(
        val id: Long = 0L,
        val trackName: String? = null,
        val artistName: String? = null,
        val plainLyrics: String? = null,
        val syncedLyrics: String? = null,
    )

    private companion object {
        const val BASE = "https://lrclib.net"
    }
}
