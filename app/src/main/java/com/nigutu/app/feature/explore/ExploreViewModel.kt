package com.nigutu.app.feature.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nigutu.app.domain.model.Genre
import com.nigutu.app.domain.model.Song
import com.nigutu.app.domain.repository.CatalogRepository
import com.nigutu.app.domain.repository.MusicRepository
import com.nigutu.app.domain.usecase.ToggleFavoriteUseCase
import com.nigutu.app.playback.PlaybackConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Explore feed state. */
data class ExploreUiState(
    val genres: List<Genre> = emptyList(),
    val chart: List<Song> = emptyList(),
    val selectedGenre: Genre? = null,
    val genreTracks: List<Song> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val favoriteIds: Set<String> = emptySet(),
)

/** Browse online genres and charts. */
@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val catalogRepository: CatalogRepository,
    musicRepository: MusicRepository,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val playback: PlaybackConnection,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            musicRepository.observeFavorites()
                .map { favorites -> favorites.map { it.id }.toSet() }
                .collect { ids -> _uiState.update { it.copy(favoriteIds = ids) } }
        }
        load()
    }

    /** Loads genres and the global chart, with graceful degradation. */
    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val genresResult = catalogRepository.genres()
            val chartResult = catalogRepository.trending(30)
            _uiState.update { state ->
                state.copy(
                    genres = genresResult.getOrDefault(state.genres),
                    chart = chartResult.getOrDefault(state.chart),
                    isLoading = false,
                    error = chartResult.exceptionOrNull()?.let { "Could not reach the music catalog. Check your connection." },
                )
            }
        }
    }

    /** Loads the chart for a genre, or clears it when the same genre is tapped twice. */
    fun selectGenre(genre: Genre) {
        if (_uiState.value.selectedGenre?.id == genre.id) {
            _uiState.update { it.copy(selectedGenre = null, genreTracks = emptyList()) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(selectedGenre = genre, isLoading = true, error = null, genreTracks = emptyList()) }
            catalogRepository.genreTracks(genre.id, 40)
                .onSuccess { tracks -> _uiState.update { it.copy(genreTracks = tracks, isLoading = false) } }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, error = error.message ?: "Could not load this genre")
                    }
                }
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
}
