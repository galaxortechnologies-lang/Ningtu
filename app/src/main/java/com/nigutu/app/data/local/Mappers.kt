package com.nigutu.app.data.local

import com.nigutu.app.domain.model.Playlist
import com.nigutu.app.domain.model.Song

/** Converts a cached row into a domain song. */
fun SongEntity.toDomain(): Song = Song(
    id = id,
    title = title,
    artist = artist,
    album = album,
    durationMs = durationMs,
    uri = uri,
    artworkUri = artworkUri,
    isLocal = isLocal,
    trackNumber = trackNumber,
    year = year,
)

/** Converts a domain song into a cache row. */
fun Song.toEntity(now: Long = System.currentTimeMillis()): SongEntity = SongEntity(
    id = id,
    title = title,
    artist = artist,
    album = album,
    durationMs = durationMs,
    uri = uri,
    artworkUri = artworkUri,
    isLocal = isLocal,
    trackNumber = trackNumber,
    year = year,
    updatedAt = now,
)

/** Converts a playlist row with its count into a domain playlist. */
fun PlaylistWithCount.toDomain(): Playlist = Playlist(
    id = playlist.id,
    name = playlist.name,
    description = playlist.description,
    createdAt = playlist.createdAt,
    songCount = songCount,
)
