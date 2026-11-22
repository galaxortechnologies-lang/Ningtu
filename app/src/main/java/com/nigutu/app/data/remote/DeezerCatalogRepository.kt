package com.nigutu.app.data.remote

import com.nigutu.app.domain.model.Genre
import com.nigutu.app.domain.model.Song
import com.nigutu.app.domain.repository.CatalogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Online catalog backed by the public Deezer API.
 *
 * Deezer exposes a keyless REST catalogue with 30-second preview streams for
 * each track. Nigutu plays those previews directly and never bypasses DRM or
 * reproduces full-length copyrighted audio.
 */
@Singleton
class DeezerCatalogRepository @Inject constructor(
    private val http: JsonHttpClient,
) : CatalogRepository {

    override suspend fun trending(limit: Int): Result<List<Song>> = withContext(Dispatchers.IO) {
        http.get<DeezerEnvelope>(http.url("$BASE/chart/0/tracks", mapOf("limit" to "$limit")))
            .mapCatching { envelope -> envelope.requireData().mapNotNull { it.toSongOrNull() } }
    }

    override suspend fun search(query: String, limit: Int): Result<List<Song>> =
        withContext(Dispatchers.IO) {
            if (query.isBlank()) return@withContext Result.success(emptyList())
            http.get<DeezerEnvelope>(
                http.url("$BASE/search", mapOf("q" to query.trim(), "limit" to "$limit")),
            ).mapCatching { envelope -> envelope.requireData().mapNotNull { it.toSongOrNull() } }
        }

    override suspend fun genres(): Result<List<Genre>> = withContext(Dispatchers.IO) {
        http.get<DeezerGenreEnvelope>(http.url("$BASE/genre"))
            .mapCatching { envelope ->
                envelope.data.orEmpty().map { Genre(it.id, it.name) }
            }
    }

    override suspend fun genreTracks(genreId: Long, limit: Int): Result<List<Song>> =
        withContext(Dispatchers.IO) {
            http.get<DeezerEnvelope>(
                http.url("$BASE/chart/$genreId/tracks", mapOf("limit" to "$limit")),
            ).mapCatching { envelope -> envelope.requireData().mapNotNull { it.toSongOrNull() } }
        }

    @Serializable
    private data class DeezerEnvelope(
        val data: List<DeezerTrack>? = null,
        val error: DeezerError? = null,
    ) {
        fun requireData(): List<DeezerTrack> {
            error?.let { throw IOException("Deezer error ${it.code}: ${it.message.orEmpty()}") }
            return data.orEmpty()
        }
    }

    @Serializable
    private data class DeezerGenreEnvelope(
        val data: List<DeezerGenreDto>? = null,
    )

    @Serializable
    private data class DeezerGenreDto(val id: Long, val name: String)

    @Serializable
    private data class DeezerTrack(
        val id: Long,
        val title: String = "",
        val duration: Long = 0L,
        val preview: String? = null,
        val artist: DeezerArtist? = null,
        val album: DeezerAlbum? = null,
    )

    @Serializable
    private data class DeezerArtist(val name: String = "")

    @Serializable
    private data class DeezerAlbum(
        val title: String = "",
        @SerialName("cover_big") val coverBig: String? = null,
        @SerialName("cover_xl") val coverXl: String? = null,
    )

    @Serializable
    private data class DeezerError(
        val type: String? = null,
        val message: String? = null,
        val code: Int = 0,
    )

    private fun DeezerTrack.toSongOrNull(): Song? {
        val stream = preview?.takeIf { it.isNotBlank() } ?: return null
        if (title.isBlank()) return null
        return Song(
            id = "remote:deezer:$id",
            title = title,
            artist = artist?.name?.ifBlank { "Unknown artist" } ?: "Unknown artist",
            album = album?.title.orEmpty(),
            durationMs = duration * 1000L,
            uri = stream,
            artworkUri = album?.coverXl ?: album?.coverBig,
            isLocal = false,
        )
    }

    private companion object {
        const val BASE = "https://api.deezer.com"
    }
}
