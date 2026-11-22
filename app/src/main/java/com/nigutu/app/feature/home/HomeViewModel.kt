package com.nigutu.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nigutu.app.domain.model.Album
import com.nigutu.app.domain.model.Artist
import com.nigutu.app.domain.model.Song
import com.nigutu.app.domain.repository.CatalogRepository
import com.nigutu.app.domain.repository.MusicRepository
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

/** Everything the Home screen renders. */
data class HomeUiState(
    val songs: List<Song> = emptyList(),
    val recent: List<Song> = emptyList(),
    val favorites: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val trending: List<Song> = emptyList(),
    val favoriteIds: Set<String> = emptySet(),
    val isScanning: Boolean = false,
    val scanError: String? = null,
    val trendingLoading: Boolean = true,
    val trendingError: String? = null,
) {
    val isEmptyLibrary: Boolean get() = songs.isEmpty() && !isScanning && scanError == null
}

/** Home feed: local library highlights plus the online trending chart. */
@HiltViewModel
class HomeViewModel @Inject constructor(
    musicRepository: MusicRepository,
    private val catalogRepository: CatalogRepository,
    private val refreshLibrary: RefreshLibraryUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase,
    private val playback: PlaybackConnection,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                musicRepository.observeSongs(),
                musicRepository.observeHistory(20),
                musicRepository.observeAlbums(),
                musicRepository.observeArtists(),
                musicRepository.observeFavorites(),
            ) { songs, history, albums, artists, favorites ->
                HomeUiState(
                    songs = songs,
                    recent = history,
                    albums = albums,
                    artists = artists,
                    favorites = favorites,
                    favoriteIds = favorites.map { it.id }.toSet(),
                    isScanning = _uiState.value.isScanning,
                    scanError = _uiState.value.scanError,
                    trending = _uiState.value.trending,
                    trendingLoading = _uiState.value.trendingLoading,
                    trendingError = _uiState.value.trendingError,
                )
            }.collect { state -> _uiState.value = state }
        }
        refreshTrending()
    }

    /** Re-scans device audio; safe to call when permission was just granted. */
    fun scanLibrary() {
        if (_uiState.value.isScanning) return
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, scanError = null) }
            refreshLibrary()
                .onFailure { error -> _uiState.update { it.copy(scanError = error.message ?: "Scan failed") } }
            _uiState.update { it.copy(isScanning = false) }
        }
    }

    /** Loads (or retries) the online trending chart. */
    fun refreshTrending() {
        viewModelScope.launch {
            _uiState.update { it.copy(trendingLoading = true, trendingError = null) }
            catalogRepository.trending()
                .onSuccess { songs ->
                    _uiState.update { it.copy(trending = songs, trendingLoading = false) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(trendingLoading = false, trendingError = error.message ?: "Could not load trending music")
                    }
                }
        }
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch { toggleFavorite(song) }
    }

    /** Plays [songs] starting at [index], then navigates to the player. */
    fun play(songs: List<Song>, index: Int, onStarted: () -> Unit) {
        if (songs.isEmpty()) return
        viewModelScope.launch {
            if (playback.playQueue(songs, index)) onStarted()
        }
    }

    /** Shuffles the whole local library. */
    fun shuffleLibrary(onStarted: () -> Unit) {
        val shuffled = _uiState.value.songs.shuffled()
        play(shuffled, 0, onStarted)
    }
}
