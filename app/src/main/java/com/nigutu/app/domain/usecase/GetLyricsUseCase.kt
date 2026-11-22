package com.nigutu.app.domain.usecase

import com.nigutu.app.domain.model.Lyrics
import com.nigutu.app.domain.model.Song
import com.nigutu.app.domain.repository.LyricsRepository
import javax.inject.Inject

/** Loads lyrics for a song, preferring time-synchronised ones. */
class GetLyricsUseCase @Inject constructor(
    private val lyricsRepository: LyricsRepository,
) {
    suspend operator fun invoke(song: Song): Result<Lyrics> = lyricsRepository.lyricsFor(song)
}
