package com.nigutu.app.data.repository

import androidx.room.withTransaction
import com.nigutu.app.data.local.NigutuDatabase
import com.nigutu.app.data.local.PlaylistDao
import com.nigutu.app.data.local.PlaylistEntity
import com.nigutu.app.data.local.PlaylistEntryEntity
import com.nigutu.app.data.local.SongDao
import com.nigutu.app.data.local.toDomain
import com.nigutu.app.domain.model.Playlist
import com.nigutu.app.domain.model.Song
import com.nigutu.app.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** Room-backed playlist store with JSON export/import. */
@Singleton
class PlaylistRepositoryImpl @Inject constructor(
    private val database: NigutuDatabase,
    private val playlistDao: PlaylistDao,
    private val songDao: SongDao,
    private val json: Json,
) : PlaylistRepository {

    override fun observePlaylists(): Flow<List<Playlist>> =
        playlistDao.observePlaylists().map { rows -> rows.map { it.toDomain() } }

    override fun observePlaylistSongs(playlistId: Long): Flow<List<Song>> =
        playlistDao.observePlaylistSongs(playlistId).map { rows -> rows.map { it.toDomain() } }

    override suspend fun createPlaylist(name: String, description: String): Long {
        val now = System.currentTimeMillis()
        return playlistDao.insertPlaylist(
            PlaylistEntity(name = name, description = description, createdAt = now, updatedAt = now),
        )
    }

    override suspend fun deletePlaylist(playlistId: Long) = playlistDao.deletePlaylist(playlistId)

    override suspend fun addSong(playlistId: Long, songId: String): Result<Unit> = runCatching {
        val song = songDao.getById(songId)
            ?: throw IllegalArgumentException("Song $songId is not in the library")
        database.withTransaction {
            val now = System.currentTimeMillis()
            val position = playlistDao.maxPosition(playlistId) + 1
            playlistDao.insertEntry(PlaylistEntryEntity(playlistId, song.id, position, now))
            playlistDao.touch(playlistId, now)
        }
    }

    override suspend fun removeSong(playlistId: Long, songId: String) {
        playlistDao.deleteEntry(playlistId, songId)
        playlistDao.touch(playlistId, System.currentTimeMillis())
    }

    override suspend fun exportPlaylist(playlistId: Long): Result<String> = runCatching {
        val playlist = playlistDao.getPlaylist(playlistId)
            ?: throw IllegalArgumentException("Playlist not found")
        val ids = playlistDao.entryIds(playlistId)
        val songs = songDao.getByIds(ids).map { it.toDomain() }
        this.json.encodeToString(
            PlaylistExport.serializer(),
            PlaylistExport(
                name = playlist.name,
                description = playlist.description,
                songs = songs.map { ExportedSong(it.id, it.title, it.artist, it.album, it.durationMs) },
            ),
        )
    }

    override suspend fun importPlaylist(json: String): Result<Long> = runCatching {
        val payload = this.json.decodeFromString<PlaylistExport>(json)
        val now = System.currentTimeMillis()
        val id = playlistDao.insertPlaylist(
            PlaylistEntity(
                name = payload.name.ifBlank { "Imported playlist" },
                description = payload.description,
                createdAt = now,
                updatedAt = now,
            ),
        )
        database.withTransaction {
            payload.songs.forEach { song ->
                if (songDao.getById(song.id) != null) {
                    val position = playlistDao.maxPosition(id) + 1
                    playlistDao.insertEntry(PlaylistEntryEntity(id, song.id, position, now))
                }
            }
            playlistDao.touch(id, now)
        }
        id
    }

    @Serializable
    private data class PlaylistExport(
        val name: String,
        val description: String = "",
        val songs: List<ExportedSong> = emptyList(),
    )

    @Serializable
    private data class ExportedSong(
        val id: String,
        val title: String,
        val artist: String,
        val album: String,
        val durationMs: Long,
    )
}
