package com.nigutu.app.feature.library

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nigutu.app.domain.model.Song
import com.nigutu.app.domain.repository.MusicRepository
import com.nigutu.app.domain.repository.PlaylistRepository
import com.nigutu.app.domain.usecase.ToggleFavoriteUseCase
import com.nigutu.app.navigation.Destination
import com.nigutu.app.playback.PlaybackConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Playlist detail state. */
data class PlaylistDetailUiState(
    val name: String = "",
    val songs: List<Song> = emptyList(),
    val favoriteIds: Set<String> = emptySet(),
    val exportJson: String? = null,
    val message: String? = null,
)

/** Playlist detail: playback, removal, export and deletion. */
@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val playlistRepository: PlaylistRepository,
    musicRepository: MusicRepository,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val playback: PlaybackConnection,
) : ViewModel() {

    private val playlistId: Long = savedStateHandle[Destination.Playlist.ARG_PLAYLIST_ID] ?: 0L

    private val _uiState = MutableStateFlow(PlaylistDetailUiState())
    val uiState: StateFlow<PlaylistDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            playlistRepository.observePlaylists().collect { playlists ->
                val current = playlists.firstOrNull { it.id == playlistId }
                if (current != null) _uiState.update { it.copy(name = current.name) }
            }
        }
        viewModelScope.launch {
            playlistRepository.observePlaylistSongs(playlistId).collect { songs ->
                _uiState.update { it.copy(songs = songs) }
            }
        }
        viewModelScope.launch {
            musicRepository.observeFavorites().collect { favorites ->
                _uiState.update { it.copy(favoriteIds = favorites.map { it.id }.toSet()) }
            }
        }
    }

    fun play(index: Int, onStarted: () -> Unit) {
        val songs = _uiState.value.songs
        if (songs.isEmpty()) return
        viewModelScope.launch {
            if (playback.playQueue(songs, index)) onStarted()
        }
    }

    fun removeSong(song: Song) {
        viewModelScope.launch {
            playlistRepository.removeSong(playlistId, song.id)
            _uiState.update { it.copy(message = "Removed ${song.title}") }
        }
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch { toggleFavoriteUseCase(song) }
    }

    fun export() {
        viewModelScope.launch {
            playlistRepository.exportPlaylist(playlistId)
                .onSuccess { json -> _uiState.update { it.copy(exportJson = json) } }
                .onFailure { error -> _uiState.update { it.copy(message = error.message ?: "Export failed") } }
        }
    }

    fun dismissExport() {
        _uiState.update { it.copy(exportJson = null) }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            playlistRepository.deletePlaylist(playlistId)
            onDeleted()
        }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
