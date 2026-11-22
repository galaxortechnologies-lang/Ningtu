package com.nigutu.app.domain.usecase

import com.nigutu.app.domain.model.Song
import com.nigutu.app.domain.repository.MusicRepository
import com.nigutu.app.domain.repository.PlaylistRepository
import javax.inject.Inject

/**
 * Toggles the favourite state of a song.
 *
 * Keeping the write behind a use case makes it easy to add analytics or
 * offline synchronisation later without touching any ViewModel.
 */
class ToggleFavoriteUseCase @Inject constructor(
    private val musicRepository: MusicRepository,
) {
    suspend operator fun invoke(song: Song) = musicRepository.toggleFavorite(song)
}
