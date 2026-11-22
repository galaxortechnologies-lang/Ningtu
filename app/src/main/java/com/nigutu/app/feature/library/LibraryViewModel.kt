package com.nigutu.app.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nigutu.app.domain.model.Album
import com.nigutu.app.domain.model.Artist
import com.nigutu.app.domain.model.Playlist
import com.nigutu.app.domain.model.Song
import com.nigutu.app.domain.repository.MusicRepository
import com.nigutu.app.domain.repository.PlaylistRepository
import com.nigutu.app.domain.usecase.CreatePlaylistUseCase
import com.nigutu.app.domain.usecase.RefreshLibraryUseCase
import com.nigutu.app.domain.usecase.ToggleFavoriteUseCase
import com.nigutu.app.playback.PlaybackConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The sections available inside the Library tab. */
enum class LibraryTab(val label: String) {
    FAVORITES("Favourites"),
    PLAYLISTS("Playlists"),
    RECENT("Recent"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    LOCAL("Local"),
}

/** Library screen state. */
data class LibraryUiState(
    val selectedTab: LibraryTab = LibraryTab.FAVORITES,
    val songs: List<Song> = emptyList(),
    val favorites: List<Song> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val recent: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val favoriteIds: Set<String> = emptySet(),
    val isScanning: Boolean = false,
    val message: String? = null,
)

/** Library tab backed by Room, with playlist management and rescanning. */
@HiltViewModel
class LibraryViewModel @Inject constructor(
    musicRepository: MusicRepository,
    private val playlistRepository: PlaylistRepository,
    private val createPlaylistUseCase: CreatePlaylistUseCase,
    private val refreshLibrary: RefreshLibraryUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val playback: PlaybackConnection,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                musicRepository.observeSongs(),
                musicRepository.observeFavorites(),
                playlistRepository.observePlaylists(),
                musicRepository.observeHistory(50),
                musicRepository.observeAlbums(),
            ) { songs, favorites, playlists, recent, albums ->
                _uiState.value.copy(
                    songs = songs,
                    favorites = favorites,
                    playlists = playlists,
                    recent = recent,
                    albums = albums,
                    favoriteIds = favorites.map { it.id }.toSet(),
                )
            }.collect { state -> _uiState.value = state }
        }

        // Artists are derived from the same song table but collected separately:
        // the combine above never completes, so it cannot also drain this flow.
        viewModelScope.launch {
            musicRepository.observeArtists().collect { artists ->
                _uiState.update { it.copy(artists = artists) }
            }
        }
    }

    fun selectTab(tab: LibraryTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun scanLibrary() {
        if (_uiState.value.isScanning) return
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true) }
            refreshLibrary()
                .onSuccess { count ->
                    _uiState.update { it.copy(message = if (count == 0) "No audio files found on this device." else null) }
                }
                .onFailure { error -> _uiState.update { it.copy(message = error.message ?: "Scan failed") } }
            _uiState.update { it.copy(isScanning = false) }
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            createPlaylistUseCase(name)
                .onSuccess { playlist ->
                    _uiState.update { it.copy(message = "\"${playlist.name}\" created") }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(message = error.message ?: "Could not create playlist") }
                }
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            playlistRepository.deletePlaylist(playlistId)
            _uiState.update { it.copy(message = "Playlist deleted") }
        }
    }

    fun importPlaylist(json: String) {
        viewModelScope.launch {
            playlistRepository.importPlaylist(json)
                .onSuccess { _uiState.update { it.copy(message = "Playlist imported") } }
                .onFailure { error -> _uiState.update { it.copy(message = error.message ?: "Import failed") } }
        }
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch { toggleFavoriteUseCase(song) }
    }

    fun play(songs: List<Song>, index: Int, onStarted: () -> Unit) {
        if (songs.isEmpty()) return
        viewModelScope.launch {
            if (playback.playQueue(songs, index)) onStarted()
        }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
