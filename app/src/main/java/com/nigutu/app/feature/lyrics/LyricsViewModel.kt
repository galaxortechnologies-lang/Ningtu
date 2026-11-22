package com.nigutu.app.feature.lyrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nigutu.app.core.util.LrcParser
import com.nigutu.app.domain.model.Lyrics
import com.nigutu.app.domain.model.Song
import com.nigutu.app.domain.usecase.GetLyricsUseCase
import com.nigutu.app.playback.PlaybackConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Lyrics screen state. */
data class LyricsUiState(
    val song: Song? = null,
    val lyrics: Lyrics? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
) {
    /** Index of the lyric line matching the current playback position. */
    fun activeLineIndex(positionMs: Long): Int =
        lyrics?.let { LrcParser.activeIndex(it.synced, positionMs) } ?: -1
}

/** Loads synced lyrics for the track that is currently playing. */
@HiltViewModel
class LyricsViewModel @Inject constructor(
    private val playback: PlaybackConnection,
    private val getLyrics: GetLyricsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LyricsUiState())
    val uiState: StateFlow<LyricsUiState> = _uiState.asStateFlow()

    val playbackState = playback.state
    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            playback.state
                .map { it.currentSong }
                .distinctUntilChanged()
                .collect { song ->
                    _uiState.update { it.copy(song = song) }
                    if (song != null) load(song)
                }
        }
    }

    /** Retries the lyrics lookup for the current song. */
    fun retry() {
        _uiState.value.song?.let { load(it) }
    }

    private fun load(song: Song) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, lyrics = null) }
            getLyrics(song)
                .onSuccess { lyrics -> _uiState.update { it.copy(lyrics = lyrics, isLoading = false) } }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "No lyrics available for this track.",
                        )
                    }
                }
        }
    }
}
