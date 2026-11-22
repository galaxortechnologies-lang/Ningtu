package com.nigutu.app.domain.usecase

import com.nigutu.app.domain.model.Playlist
import com.nigutu.app.domain.repository.PlaylistRepository
import javax.inject.Inject

/**
 * Creates a playlist with a validated, trimmed name.
 *
 * Validation lives here so that both the UI and future import paths share it.
 */
class CreatePlaylistUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository,
) {
    suspend operator fun invoke(name: String, description: String = ""): Result<Playlist> {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Playlist name cannot be empty"))
        }
        if (trimmed.length > 80) {
            return Result.failure(IllegalArgumentException("Playlist name is too long"))
        }
        val id = playlistRepository.createPlaylist(trimmed, description.trim())
        return Result.success(
            Playlist(
                id = id,
                name = trimmed,
                description = description.trim(),
                createdAt = System.currentTimeMillis(),
                songCount = 0,
            ),
        )
    }
}
