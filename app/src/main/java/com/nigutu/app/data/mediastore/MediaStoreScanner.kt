package com.nigutu.app.data.mediastore

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import android.util.Log
import com.nigutu.app.domain.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads the user's audio library through MediaStore.
 *
 * Scoped storage friendly: only metadata is queried, and audio is played via
 * its content URI, so no file paths or broad storage permissions are needed.
 */
@Singleton
class MediaStoreScanner @Inject constructor(
    @ApplicationContext private val context: Context,
) : LocalMusicSource {
    override suspend fun scan(): List<Song> = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
        )

        val songs = mutableListOf<Song>()
        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            "${MediaStore.Audio.Media.IS_MUSIC} != 0",
            null,
            "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC",
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val trackColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val yearColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)

            while (cursor.moveToNext()) {
                val mediaId = cursor.getLong(idColumn)
                val albumId = cursor.getLong(albumIdColumn)
                val duration = cursor.getLong(durationColumn)
                if (duration <= 0L) continue

                songs += Song(
                    id = "local:$mediaId",
                    title = cursor.getString(titleColumn).orEmpty().ifBlank { "Unknown title" },
                    artist = cursor.getString(artistColumn)
                        .orEmpty()
                        .ifBlank { UNKNOWN_ARTIST },
                    album = cursor.getString(albumColumn)
                        .orEmpty()
                        .ifBlank { "Unknown album" },
                    durationMs = duration,
                    uri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        mediaId,
                    ).toString(),
                    artworkUri = if (albumId > 0L) {
                        ContentUris.withAppendedId(ALBUM_ART_URI, albumId).toString()
                    } else {
                        null
                    },
                    isLocal = true,
                    trackNumber = cursor.getInt(trackColumn),
                    year = cursor.getInt(yearColumn),
                )
            }
        }
        Log.d(TAG, "Scanned ${songs.size} local tracks")
        songs
    }

    private companion object {
        const val TAG = "MediaStoreScanner"
        const val UNKNOWN_ARTIST = "Unknown artist"
        val ALBUM_ART_URI = android.net.Uri.parse("content://media/external/audio/albumart")
    }
}
