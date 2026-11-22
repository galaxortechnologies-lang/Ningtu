package com.nigutu.app.domain.repository

import com.nigutu.app.domain.model.Album
import com.nigutu.app.domain.model.AppSettings
import com.nigutu.app.domain.model.Artist
import com.nigutu.app.domain.model.Genre
import com.nigutu.app.domain.model.Lyrics
import com.nigutu.app.domain.model.Playlist
import com.nigutu.app.domain.model.Song
import kotlinx.coroutines.flow.Flow

/** Read/write access to the device music library and user listening data. */
interface MusicRepository {
    fun observeSongs(): Flow<List<Song>>
    fun observeFavorites(): Flow<List<Song>>
    fun observeHistory(limit: Int = 30): Flow<List<Song>>
    fun observeAlbums(): Flow<List<Album>>
    fun observeArtists(): Flow<List<Artist>>
    suspend fun scanDeviceLibrary(): Result<Int>
    suspend fun toggleFavorite(song: Song)
    suspend fun isFavorite(songId: String): Boolean
    suspend fun recordPlay(song: Song)
    fun observeSearchHistory(limit: Int = 10): Flow<List<String>>
    suspend fun saveSearch(query: String)
    suspend fun clearSearchHistory()
}

/** Playlist CRUD plus import/export. */
interface PlaylistRepository {
    fun observePlaylists(): Flow<List<Playlist>>
    fun observePlaylistSongs(playlistId: Long): Flow<List<Song>>
    suspend fun createPlaylist(name: String, description: String = ""): Long
    suspend fun deletePlaylist(playlistId: Long)
    suspend fun addSong(playlistId: Long, songId: String): Result<Unit>
    suspend fun removeSong(playlistId: Long, songId: String)
    suspend fun exportPlaylist(playlistId: Long): Result<String>
    suspend fun importPlaylist(json: String): Result<Long>
}

/** Online catalog provider abstraction (search, charts, genres). */
interface CatalogRepository {
    suspend fun trending(limit: Int = 25): Result<List<Song>>
    suspend fun search(query: String, limit: Int = 25): Result<List<Song>>
    suspend fun genres(): Result<List<Genre>>
    suspend fun genreTracks(genreId: Long, limit: Int = 25): Result<List<Song>>
}

/** Lyrics lookup abstraction, backed by LRCLIB. */
interface LyricsRepository {
    suspend fun lyricsFor(song: Song): Result<Lyrics>
}

/** Persisted user preferences. */
interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun setThemeMode(mode: com.nigutu.app.domain.model.ThemeMode)
    suspend fun setWifiOnlyStreaming(enabled: Boolean)
    suspend fun setEqualizerEnabled(enabled: Boolean)
    suspend fun setBassBoostStrength(strength: Int)
    suspend fun setPlaybackSpeed(speed: Float)
}
