package com.nigutu.app.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nigutu.app.domain.model.Song
import com.nigutu.app.domain.repository.CatalogRepository
import com.nigutu.app.domain.repository.MusicRepository
import com.nigutu.app.domain.usecase.ToggleFavoriteUseCase
import com.nigutu.app.playback.PlaybackConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Search screen state. */
data class SearchUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val localResults: List<Song> = emptyList(),
    val onlineResults: List<Song> = emptyList(),
    val history: List<String> = emptyList(),
    val favoriteIds: Set<String> = emptySet(),
    val error: String? = null,
) {
    val hasResults: Boolean get() = localResults.isNotEmpty() || onlineResults.isNotEmpty()
    val showSuggestions: Boolean get() = query.isBlank()
}

/** Debounced search across the device library and the online catalog. */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val catalogRepository: CatalogRepository,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val playback: PlaybackConnection,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val songs = musicRepository.observeSongs()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            musicRepository.observeSearchHistory()
                .collect { history -> _uiState.update { it.copy(history = history) } }
        }
        viewModelScope.launch {
            musicRepository.observeFavorites()
                .map { favorites -> favorites.map { it.id }.toSet() }
                .collect { ids -> _uiState.update { it.copy(favoriteIds = ids) } }
        }
    }

    /** Updates the query and schedules a debounced search. */
    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query, error = null) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { it.copy(localResults = emptyList(), onlineResults = emptyList(), isSearching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(DEBOUNCE_MS)
            runSearch(query)
        }
    }

    /** Runs the search immediately, e.g. from the keyboard's search action. */
    fun onSearchSubmitted() {
        val query = _uiState.value.query.trim()
        if (query.isEmpty()) return
        searchJob?.cancel()
        viewModelScope.launch {
            runSearch(query)
            musicRepository.saveSearch(query)
        }
    }

    /** Repeats a query from the search history. */
    fun onHistorySelected(query: String) {
        _uiState.update { it.copy(query = query) }
        searchJob?.cancel()
        viewModelScope.launch { runSearch(query) }
    }

    private suspend fun runSearch(query: String) {
        _uiState.update { it.copy(isSearching = true, error = null) }
        val needle = query.trim().lowercase()
        val local = songs.value.filter { song ->
            song.title.lowercase().contains(needle) ||
                song.artist.lowercase().contains(needle) ||
                song.album.lowercase().contains(needle)
        }
        val online = catalogRepository.search(query)
        _uiState.update {
            it.copy(
                isSearching = false,
                localResults = local,
                onlineResults = online.getOrDefault(it.onlineResults),
                error = online.exceptionOrNull()?.let { "Online search is unavailable right now." },
            )
        }
    }

    fun clearHistory() {
        viewModelScope.launch { musicRepository.clearSearchHistory() }
    }

    fun clearQuery() {
        searchJob?.cancel()
        _uiState.update {
            it.copy(query = "", localResults = emptyList(), onlineResults = emptyList(), error = null, isSearching = false)
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

    private companion object {
        const val DEBOUNCE_MS = 320L
    }
}
