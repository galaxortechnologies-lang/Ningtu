package com.nigutu.app.data.local

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Cached song metadata. */
@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY title COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): SongEntity?

    @Query("SELECT * FROM songs WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<SongEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(songs: List<SongEntity>)

    @Query("DELETE FROM songs WHERE isLocal = 1 AND id NOT IN (:keepIds)")
    suspend fun deleteStaleLocalSongs(keepIds: List<String>)

    @Query("DELETE FROM songs WHERE isLocal = 1")
    suspend fun deleteAllLocal()

    @Query("SELECT COUNT(*) FROM songs")
    suspend fun count(): Int
}

/** Favourites, stored by song id so they survive rescans. */
@Dao
interface FavoriteDao {
    @Query("SELECT songId FROM favorites ORDER BY addedAt DESC")
    fun observeIds(): Flow<List<String>>

    @Query(
        """
        SELECT s.* FROM songs s
        INNER JOIN favorites f ON f.songId = s.id
        ORDER BY f.addedAt DESC
        """,
    )
    fun observeFavoriteSongs(): Flow<List<SongEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE songId = :songId)")
    suspend fun isFavorite(songId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE songId = :songId")
    suspend fun delete(songId: String)
}

/** Playlist rows plus a computed song count. */
data class PlaylistWithCount(
    @Embedded val playlist: PlaylistEntity,
    val songCount: Int,
)

/** Playlists and their ordered entries. */
@Dao
interface PlaylistDao {
    @Query(
        """
        SELECT p.*, (SELECT COUNT(*) FROM playlist_entries e WHERE e.playlistId = p.id) AS songCount
        FROM playlists p
        ORDER BY p.updatedAt DESC
        """,
    )
    fun observePlaylists(): Flow<List<PlaylistWithCount>>

    @Query("SELECT * FROM playlists WHERE id = :id LIMIT 1")
    suspend fun getPlaylist(id: Long): PlaylistEntity?

    @Insert
    suspend fun insertPlaylist(entity: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :name, description = :description, updatedAt = :now WHERE id = :id")
    suspend fun updatePlaylist(id: Long, name: String, description: String, now: Long)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: Long)

    @Query("UPDATE playlists SET updatedAt = :now WHERE id = :playlistId")
    suspend fun touch(playlistId: Long, now: Long)

    @Query(
        """
        SELECT s.* FROM songs s
        INNER JOIN playlist_entries e ON e.songId = s.id
        WHERE e.playlistId = :playlistId
        ORDER BY e.position ASC
        """,
    )
    fun observePlaylistSongs(playlistId: Long): Flow<List<SongEntity>>

    @Query("SELECT COALESCE(MAX(position), -1) FROM playlist_entries WHERE playlistId = :playlistId")
    suspend fun maxPosition(playlistId: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEntry(entity: PlaylistEntryEntity)

    @Query("DELETE FROM playlist_entries WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun deleteEntry(playlistId: Long, songId: String)

    @Query("SELECT songId FROM playlist_entries WHERE playlistId = :playlistId ORDER BY position ASC")
    suspend fun entryIds(playlistId: Long): List<String>
}

/** Listening history. */
@Dao
interface HistoryDao {
    @Query(
        """
        SELECT s.* FROM songs s
        INNER JOIN history h ON h.songId = s.id
        ORDER BY h.playedAt DESC
        LIMIT :limit
        """,
    )
    fun observeRecent(limit: Int): Flow<List<SongEntity>>

    @Query("SELECT * FROM history WHERE songId = :songId LIMIT 1")
    suspend fun get(songId: String): HistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: HistoryEntity)

    @Query("DELETE FROM history")
    suspend fun clear()
}

/** Search history. */
@Dao
interface SearchHistoryDao {
    @Query("SELECT query FROM search_history ORDER BY searchedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE query = :query")
    suspend fun delete(query: String)

    @Query("DELETE FROM search_history")
    suspend fun clear()
}
