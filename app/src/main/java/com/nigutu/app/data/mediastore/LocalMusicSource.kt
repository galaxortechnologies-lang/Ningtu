package com.nigutu.app.data.mediastore

import com.nigutu.app.domain.model.Song

/** Abstraction over the device's local audio source, so repositories stay testable. */
interface LocalMusicSource {
    /** Returns every playable audio track currently on the device. */
    suspend fun scan(): List<Song>
}
