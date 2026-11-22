package com.nigutu.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Nigutu's local database.
 *
 * Only metadata lives here: audio files themselves are streamed or read
 * through the media store, never copied.
 */
@Database(
    entities = [
        SongEntity::class,
        FavoriteEntity::class,
        PlaylistEntity::class,
        PlaylistEntryEntity::class,
        HistoryEntity::class,
        SearchHistoryEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class NigutuDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun historyDao(): HistoryDao
    abstract fun searchHistoryDao(): SearchHistoryDao

    companion object {
        const val NAME = "nigutu.db"
    }
}
