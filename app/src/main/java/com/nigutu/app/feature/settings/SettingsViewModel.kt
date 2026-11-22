package com.nigutu.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nigutu.app.domain.model.AppSettings
import com.nigutu.app.domain.model.ThemeMode
import com.nigutu.app.domain.repository.SettingsRepository
import com.nigutu.app.playback.AudioEffectsController
import com.nigutu.app.playback.AudioEffectsState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Settings, equaliser and about-screen actions. */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val audioEffects: AudioEffectsController,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppSettings(),
    )

    val audioEffectsState: StateFlow<AudioEffectsState> = audioEffects.state

    init {
        // Keep the native effects in sync with the stored preferences.
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                if (audioEffects.state.value.supported) {
                    audioEffects.setEnabled(settings.equalizerEnabled)
                    audioEffects.setBassBoostStrength(settings.bassBoostStrength)
                }
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch {
        settingsRepository.setThemeMode(mode)
    }

    fun setWifiOnlyStreaming(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setWifiOnlyStreaming(enabled)
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        audioEffects.setEnabled(enabled)
        viewModelScope.launch { settingsRepository.setEqualizerEnabled(enabled) }
    }

    fun setBassBoost(strength: Int) {
        audioEffects.setBassBoostStrength(strength)
        viewModelScope.launch { settingsRepository.setBassBoostStrength(strength) }
    }

    fun setBandLevel(bandIndex: Short, levelMb: Short) {
        audioEffects.setBandLevel(bandIndex, levelMb)
    }

    fun applyPreset(index: Short) {
        audioEffects.applyPreset(index)
    }
}
