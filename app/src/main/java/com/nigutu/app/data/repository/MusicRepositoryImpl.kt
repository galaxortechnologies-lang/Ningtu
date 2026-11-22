package com.nigutu.app.data.repository

import com.nigutu.app.data.local.FavoriteDao
import com.nigutu.app.data.local.FavoriteEntity
import com.nigutu.app.data.local.HistoryDao
import com.nigutu.app.data.local.HistoryEntity
import com.nigutu.app.data.local.SearchHistoryDao
import com.nigutu.app.data.local.SearchHistoryEntity
import com.nigutu.app.data.local.SongDao
import com.nigutu.app.data.local.toDomain
import com.nigutu.app.data.local.toEntity
import com.nigutu.app.data.mediastore.LocalMusicSource
import com.nigutu.app.domain.model.Album
import com.nigutu.app.domain.model.Artist
import com.nigutu.app.domain.model.Song
import com.nigutu.app.domain.repository.MusicRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for the local library.
 *
 * MediaStore is scanned on demand; Room is the observable cache, so the UI
 * reacts instantly to scans, favourites and listening history.
 */
@Singleton
class MusicRepositoryImpl @Inject constructor(
    private val songDao: SongDao,
    private val favoriteDao: FavoriteDao,
    private val historyDao: HistoryDao,
    private val searchHistoryDao: SearchHistoryDao,
    private val scanner: LocalMusicSource,
) : MusicRepository {

    override fun observeSongs(): Flow<List<Song>> =
        songDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeFavorites(): Flow<List<Song>> =
        favoriteDao.observeFavoriteSongs().map { list -> list.map { it.toDomain() } }

    override fun observeHistory(limit: Int): Flow<List<Song>> =
        historyDao.observeRecent(limit).map { list -> list.map { it.toDomain() } }

    override fun observeAlbums(): Flow<List<Album>> = observeSongs().map { songs ->
        songs.groupBy { it.album to it.artist }
            .map { (key, tracks) ->
                Album(
                    name = key.first,
                    artist = key.second,
                    artworkUri = tracks.firstNotNullOfOrNull { it.artworkUri },
                    songCount = tracks.size,
                )
            }
            .sortedWith(compareByDescending<Album> { it.songCount }.thenBy { it.name.lowercase() })
    }

    override fun observeArtists(): Flow<List<Artist>> = observeSongs().map { songs ->
        songs.groupBy { it.artist }
            .map { (artist, tracks) ->
                Artist(
                    name = artist,
                    songCount = tracks.size,
                    artworkUri = tracks.firstNotNullOfOrNull { it.artworkUri },
                )
            }
            .sortedWith(compareByDescending<Artist> { it.songCount }.thenBy { it.name.lowercase() })
    }

    override suspend fun scanDeviceLibrary(): Result<Int> = runCatching {
        val scanned = scanner.scan()
        if (scanned.isEmpty()) {
            songDao.deleteAllLocal()
            0
        } else {
            songDao.upsertAll(scanned.map { it.toEntity() })
            songDao.deleteStaleLocalSongs(scanned.map { it.id })
            songDao.count()
        }
    }

    override suspend fun toggleFavorite(song: Song) {
        if (favoriteDao.isFavorite(song.id)) {
            favoriteDao.delete(song.id)
        } else {
            // Cache the song so favourites can join against the songs table,
            // including online tracks that were never scanned from the device.
            songDao.upsertAll(listOf(song.toEntity()))
            favoriteDao.insert(FavoriteEntity(song.id, System.currentTimeMillis()))
        }
    }

    override suspend fun isFavorite(songId: String): Boolean = favoriteDao.isFavorite(songId)

    override suspend fun recordPlay(song: Song) {
        val now = System.currentTimeMillis()
        songDao.upsertAll(listOf(song.toEntity(now)))
        val existing = historyDao.get(song.id)
        historyDao.insert(
            HistoryEntity(
                songId = song.id,
                playedAt = now,
                playCount = (existing?.playCount ?: 0) + 1,
            ),
        )
    }

    override fun observeSearchHistory(limit: Int): Flow<List<String>> =
        searchHistoryDao.observeRecent(limit)

    override suspend fun saveSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        searchHistoryDao.insert(SearchHistoryEntity(trimmed, System.currentTimeMillis()))
    }

    override suspend fun clearSearchHistory() = searchHistoryDao.clear()
}
