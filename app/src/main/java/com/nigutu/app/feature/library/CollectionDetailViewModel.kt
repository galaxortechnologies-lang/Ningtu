package com.nigutu.app.feature.library

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nigutu.app.domain.model.Song
import com.nigutu.app.domain.repository.MusicRepository
import com.nigutu.app.domain.usecase.ToggleFavoriteUseCase
import com.nigutu.app.navigation.Destination
import com.nigutu.app.playback.PlaybackConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.net.URLDecoder
import javax.inject.Inject

/** Loads the tracks of one album or artist, chosen by the navigation argument. */
@HiltViewModel
class CollectionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    musicRepository: MusicRepository,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val playback: PlaybackConnection,
) : ViewModel() {

    private val albumArgument: String? = savedStateHandle[Destination.Album.ARG_ALBUM_NAME]
    private val artistArgument: String? = savedStateHandle[Destination.Artist.ARG_ARTIST_NAME]

    /** Navigation may or may not have decoded the argument, so accept both forms. */
    private fun candidates(raw: String?): Set<String> {
        if (raw == null) return emptySet()
        val decoded = runCatching { URLDecoder.decode(raw, "UTF-8") }.getOrDefault(raw)
        return setOf(raw, decoded)
    }

    private val albumNames = candidates(albumArgument)
    private val artistNames = candidates(artistArgument)

    val title: String = (albumNames + artistNames).firstOrNull().orEmpty()

    val songs: StateFlow<List<Song>> = musicRepository.observeSongs()
        .map { songs ->
            when {
                albumNames.isNotEmpty() -> songs.filter { it.album in albumNames }
                artistNames.isNotEmpty() -> songs.filter { it.artist in artistNames }
                else -> emptyList()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val favoriteIds: StateFlow<Set<String>> = musicRepository.observeFavorites()
        .map { favorites -> favorites.map { it.id }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun toggleFavorite(song: Song) {
        viewModelScope.launch { toggleFavoriteUseCase(song) }
    }

    fun play(index: Int, onStarted: () -> Unit) {
        val current = songs.value
        if (current.isEmpty()) return
        viewModelScope.launch {
            if (playback.playQueue(current, index)) onStarted()
        }
    }
}
