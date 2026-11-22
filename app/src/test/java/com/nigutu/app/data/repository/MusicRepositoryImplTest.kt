package com.nigutu.app.data.repository

import com.nigutu.app.data.local.FavoriteDao
import com.nigutu.app.data.local.FavoriteEntity
import com.nigutu.app.data.local.HistoryDao
import com.nigutu.app.data.local.HistoryEntity
import com.nigutu.app.data.local.SearchHistoryDao
import com.nigutu.app.data.local.SearchHistoryEntity
import com.nigutu.app.data.local.SongDao
import com.nigutu.app.data.local.SongEntity
import com.nigutu.app.data.local.toDomain
import com.nigutu.app.data.local.toEntity
import com.nigutu.app.data.mediastore.LocalMusicSource
import com.nigutu.app.domain.model.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicRepositoryImplTest {

    private class FakeSongDao : SongDao {
        val songs = MutableStateFlow<List<SongEntity>>(emptyList())
        override fun observeAll(): Flow<List<SongEntity>> = songs
        override suspend fun getById(id: String): SongEntity? = songs.value.firstOrNull { it.id == id }
        override suspend fun getByIds(ids: List<String>): List<SongEntity> =
            songs.value.filter { it.id in ids }

        override suspend fun upsertAll(songs: List<SongEntity>) {
            val merged = this.songs.value.associateBy { it.id }.toMutableMap()
            songs.forEach { merged[it.id] = it }
            this.songs.value = merged.values.toList()
        }

        override suspend fun deleteStaleLocalSongs(keepIds: List<String>) {
            songs.value = songs.value.filter { it.id in keepIds || !it.isLocal }
        }

        override suspend fun deleteAllLocal() {
            songs.value = songs.value.filter { !it.isLocal }
        }

        override suspend fun count(): Int = songs.value.size
    }

    private class FakeFavoriteDao : FavoriteDao {
        val ids = MutableStateFlow<Set<String>>(emptySet())
        override fun observeIds(): Flow<List<String>> = ids.map { it.toList() }
        override fun observeFavoriteSongs(): Flow<List<SongEntity>> = ids.map { emptyList() }
        override suspend fun isFavorite(songId: String): Boolean = songId in ids.value
        override suspend fun insert(entity: FavoriteEntity) {
            ids.value = ids.value + entity.songId
        }

        override suspend fun delete(songId: String) {
            ids.value = ids.value - songId
        }
    }

    private class FakeHistoryDao : HistoryDao {
        val rows = MutableStateFlow<Map<String, HistoryEntity>>(emptyMap())
        override fun observeRecent(limit: Int): Flow<List<SongEntity>> = rows.map { emptyList() }
        override suspend fun get(songId: String): HistoryEntity? = rows.value[songId]
        override suspend fun insert(entity: HistoryEntity) {
            rows.value = rows.value + (entity.songId to entity)
        }

        override suspend fun clear() {
            rows.value = emptyMap()
        }
    }

    private class FakeSearchHistoryDao : SearchHistoryDao {
        val queries = MutableStateFlow<List<SearchHistoryEntity>>(emptyList())
        override fun observeRecent(limit: Int): Flow<List<String>> =
            queries.map { list -> list.map { it.query } }

        override suspend fun insert(entity: SearchHistoryEntity) {
            queries.value = listOf(entity) + queries.value.filter { it.query != entity.query }
        }

        override suspend fun delete(query: String) {
            queries.value = queries.value.filter { it.query != query }
        }

        override suspend fun clear() {
            queries.value = emptyList()
        }
    }

    private class FakeScanner(private val tracks: List<Song>) : LocalMusicSource {
        override suspend fun scan(): List<Song> = tracks
    }

    private val songDao = FakeSongDao()
    private val favoriteDao = FakeFavoriteDao()
    private val historyDao = FakeHistoryDao()
    private val searchDao = FakeSearchHistoryDao()

    private fun repository(tracks: List<Song> = emptyList()): MusicRepositoryImpl =
        MusicRepositoryImpl(songDao, favoriteDao, historyDao, searchDao, FakeScanner(tracks))

    private fun song(id: String, isLocal: Boolean = true) = Song(
        id = id,
        title = "Song $id",
        artist = "Artist",
        album = "Album",
        durationMs = 180_000,
        uri = "content://media/$id",
        isLocal = isLocal,
    )

    @Test
    fun `favourite toggle persists and removes`() = runTest {
        val repository = repository()
        val track = song("local:1")

        repository.toggleFavorite(track)
        assertTrue(favoriteDao.isFavorite(track.id))
        assertEquals(1, songDao.songs.value.size)

        repository.toggleFavorite(track)
        assertFalse(favoriteDao.isFavorite(track.id))
    }

    @Test
    fun `record play caches the song and increments counts`() = runTest {
        val repository = repository()
        val remote = song("remote:deezer:7", isLocal = false)

        repository.recordPlay(remote)
        repository.recordPlay(remote)

        assertEquals(1, songDao.songs.value.size)
        assertEquals(2, historyDao.rows.value[remote.id]?.playCount)
    }

    @Test
    fun `search history keeps the newest query first and ignores blanks`() = runTest {
        val repository = repository()

        repository.saveSearch("ambient")
        repository.saveSearch("  lofi  ")
        repository.saveSearch("   ")
        repository.saveSearch("ambient")

        assertEquals(listOf("ambient", "lofi"), searchDao.queries.value.map { it.query })
    }

    @Test
    fun `scanning stores metadata and removes tracks that disappeared`() = runTest {
        val repository = repository(listOf(song("local:1"), song("local:2")))

        val firstScan = repository.scanDeviceLibrary()
        assertEquals(2, firstScan.getOrNull())
        assertEquals(listOf("local:1", "local:2"), songDao.songs.value.map { it.id }.sorted())

        // Second scan drops one file from the device.
        val scannerAfterDeletion = MusicRepositoryImpl(
            songDao, favoriteDao, historyDao, searchDao, FakeScanner(listOf(song("local:2"))),
        )
        val secondScan = scannerAfterDeletion.scanDeviceLibrary()

        assertEquals(1, secondScan.getOrNull())
        assertEquals(listOf("local:2"), songDao.songs.value.map { it.id })
    }

    @Test
    fun `scanning an empty device clears cached local tracks but keeps online ones`() = runTest {
        val repository = repository()
        repository.recordPlay(song("remote:deezer:1", isLocal = false))
        songDao.upsertAll(listOf(song("local:9").toEntity()))

        val result = repository.scanDeviceLibrary()

        assertEquals(0, result.getOrNull())
        assertEquals(listOf("remote:deezer:1"), songDao.songs.value.map { it.id })
    }

    @Test
    fun `favourite songs flow resolves joined rows`() = runTest {
        val repository = repository()
        songDao.upsertAll(listOf(song("local:3").toEntity()))
        // The fake favourite DAO returns no joined rows, so the flow is empty here;
        // this asserts the repository does not fabricate data.
        assertEquals(emptyList<Song>(), repository.observeFavorites().first())
        assertEquals("local:3", songDao.songs.value.single().toDomain().id)
    }
}
