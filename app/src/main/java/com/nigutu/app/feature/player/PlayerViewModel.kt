package com.nigutu.app.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nigutu.app.domain.model.Song
import com.nigutu.app.domain.repository.MusicRepository
import com.nigutu.app.domain.usecase.ToggleFavoriteUseCase
import com.nigutu.app.playback.PlaybackConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Exposes playback controls to the full-screen player and queue. */
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playback: PlaybackConnection,
    musicRepository: MusicRepository,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
) : ViewModel() {

    val playbackState = playback.state
    val sleepTimerRemainingMs: StateFlow<Long?> = playback.sleepTimerRemainingMs

    private val favoriteIds = musicRepository.observeFavorites()
        .map { favorites -> favorites.map { it.id }.toSet() }

    /** Whether the current track is in the user's favourites. */
    val isCurrentFavorite: StateFlow<Boolean> =
        combine(playback.state, favoriteIds) { state, ids ->
            state.currentSong?.id in ids
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun togglePlayPause() = viewModelScope.launch { playback.togglePlayPause() }
    fun next() = viewModelScope.launch { playback.next() }
    fun previous() = viewModelScope.launch { playback.previous() }
    fun toggleShuffle() = viewModelScope.launch { playback.toggleShuffle() }
    fun cycleRepeat() = viewModelScope.launch { playback.cycleRepeatMode() }

    /** Seeks within the current track from a 0..1 slider fraction. */
    fun seekTo(fraction: Float) = viewModelScope.launch { playback.seekToFraction(fraction) }

    fun toggleFavorite() {
        val song = playback.state.value.currentSong ?: return
        viewModelScope.launch { toggleFavoriteUseCase(song) }
    }

    fun addToQueue(song: Song) = viewModelScope.launch { playback.addToQueue(song) }

    fun removeFromQueue(index: Int) = viewModelScope.launch { playback.removeFromQueue(index) }

    fun playQueueIndex(index: Int) = viewModelScope.launch { playback.playQueueIndex(index) }

    fun startSleepTimer(minutes: Int) = playback.startSleepTimer(minutes)
    fun cancelSleepTimer() = playback.cancelSleepTimer()
}
