package com.nigutu.app.domain.usecase

import com.nigutu.app.domain.model.Playlist
import com.nigutu.app.domain.model.Song
import com.nigutu.app.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatePlaylistUseCaseTest {

    private class FakePlaylistRepository : PlaylistRepository {
        val created = mutableListOf<Pair<String, String>>()

        override fun observePlaylists(): Flow<List<Playlist>> = flowOf(emptyList())
        override fun observePlaylistSongs(playlistId: Long): Flow<List<Song>> = flowOf(emptyList())
        override suspend fun createPlaylist(name: String, description: String): Long {
            created += name to description
            return 42L
        }

        override suspend fun deletePlaylist(playlistId: Long) = Unit
        override suspend fun addSong(playlistId: Long, songId: String): Result<Unit> = Result.success(Unit)
        override suspend fun removeSong(playlistId: Long, songId: String) = Unit
        override suspend fun exportPlaylist(playlistId: Long): Result<String> = Result.success("{}")
        override suspend fun importPlaylist(json: String): Result<Long> = Result.success(1L)
    }

    @Test
    fun `trims and stores a valid name`() = runTest {
        val repository = FakePlaylistRepository()
        val result = CreatePlaylistUseCase(repository)("  Late night  ", " chill ")

        assertTrue(result.isSuccess)
        assertEquals(42L, result.getOrNull()?.id)
        assertEquals(listOf("Late night" to "chill"), repository.created)
    }

    @Test
    fun `rejects blank names`() = runTest {
        val repository = FakePlaylistRepository()
        val result = CreatePlaylistUseCase(repository)("   ")

        assertTrue(result.isFailure)
        assertTrue(repository.created.isEmpty())
    }

    @Test
    fun `rejects overly long names`() = runTest {
        val repository = FakePlaylistRepository()
        val result = CreatePlaylistUseCase(repository)("x".repeat(81))

        assertTrue(result.isFailure)
        assertTrue(repository.created.isEmpty())
    }
}
