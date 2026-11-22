package com.nigutu.app.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.nigutu.app.domain.model.RepeatMode
import com.nigutu.app.domain.model.Song
import android.net.Uri

/** Converts a domain song into a Media3 item, carrying artwork and metadata. */
fun Song.toMediaItem(): MediaItem = MediaItem.Builder()
    .setMediaId(id)
    .setUri(uri)
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .apply { artworkUri?.let { setArtworkUri(Uri.parse(it)) } }
            .build(),
    )
    .build()

/** Rebuilds a domain song from a Media3 item, used for UI and history. */
fun MediaItem.toSong(): Song {
    val metadata = mediaMetadata
    return Song(
        id = mediaId,
        title = metadata.title?.toString().orEmpty().ifBlank { "Unknown title" },
        artist = metadata.artist?.toString().orEmpty().ifBlank { "Unknown artist" },
        album = metadata.albumTitle?.toString().orEmpty(),
        durationMs = metadata.durationMs ?: 0L,
        uri = localConfiguration?.uri?.toString().orEmpty(),
        artworkUri = metadata.artworkUri?.toString(),
        isLocal = mediaId.startsWith("local:"),
    )
}

/** Maps Media3's integer repeat mode to the domain enum. */
fun Int.toRepeatMode(): RepeatMode = when (this) {
    androidx.media3.common.Player.REPEAT_MODE_ONE -> RepeatMode.ONE
    androidx.media3.common.Player.REPEAT_MODE_ALL -> RepeatMode.ALL
    else -> RepeatMode.OFF
}

/** Maps the domain repeat mode to Media3's expected integer. */
fun RepeatMode.toPlayerRepeatMode(): Int = when (this) {
    RepeatMode.OFF -> androidx.media3.common.Player.REPEAT_MODE_OFF
    RepeatMode.ONE -> androidx.media3.common.Player.REPEAT_MODE_ONE
    RepeatMode.ALL -> androidx.media3.common.Player.REPEAT_MODE_ALL
}
