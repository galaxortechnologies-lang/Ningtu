package com.nigutu.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Cached track metadata scanned from the device media store. */
@Entity(tableName = "songs", indices = [Index("album"), Index("artist")])
data class SongEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uri: String,
    val artworkUri: String?,
    val isLocal: Boolean,
    val trackNumber: Int,
    val year: Int,
    val updatedAt: Long,
)

/** A song the user marked as favourite. */
@Entity(tableName = "favorites", indices = [Index("addedAt")])
data class FavoriteEntity(
    @PrimaryKey val songId: String,
    val addedAt: Long,
)

/** A user-created playlist. */
@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val description: String,
    val createdAt: Long,
    val updatedAt: Long,
)

/** Ordered membership of a song in a playlist. */
@Entity(
    tableName = "playlist_entries",
    primaryKeys = ["playlistId", "songId"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("playlistId"), Index("songId"), Index("position")],
)
data class PlaylistEntryEntity(
    val playlistId: Long,
    val songId: String,
    val position: Int,
    val addedAt: Long,
)

/** Play counts and recency, used for recently played and recommendations. */
@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey val songId: String,
    val playedAt: Long,
    val playCount: Int,
)

/** Recent search queries, most recent first. */
@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey val query: String,
    val searchedAt: Long,
)
