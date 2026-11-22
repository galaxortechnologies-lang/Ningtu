package com.nigutu.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nigutu.app.domain.model.AppSettings
import com.nigutu.app.domain.repository.SettingsRepository
import com.nigutu.app.playback.PlaybackConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Holds app-shell state: playback, theme and one-off messages. */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val playback: PlaybackConnection,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val playbackState = playback.state
    val sleepTimerRemainingMs: StateFlow<Long?> = playback.sleepTimerRemainingMs
    val messages: SharedFlow<String> = playback.messages

    val settings: StateFlow<AppSettings> = settingsRepository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppSettings(),
    )

    fun togglePlayPause() {
        viewModelScope.launch { playback.togglePlayPause() }
    }

    fun next() {
        viewModelScope.launch { playback.next() }
    }

    fun previous() {
        viewModelScope.launch { playback.previous() }
    }
}
