package com.nigutu.app.playback

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.util.Log
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** One equaliser band, exposed to the UI in plain values. */
data class EqBand(
    val index: Short,
    val centerFrequencyHz: Int,
    val minLevelMb: Short,
    val maxLevelMb: Short,
    val levelMb: Short,
)

/** Snapshot of the audio effects state. */
data class AudioEffectsState(
    val supported: Boolean = false,
    val enabled: Boolean = false,
    val presetName: String = "",
    val presets: List<String> = emptyList(),
    val bands: List<EqBand> = emptyList(),
    val bassBoostStrength: Int = 0,
)

/**
 * Owns the platform equaliser and bass boost for the playback session.
 *
 * Effects are created inside the playback service process against the
 * ExoPlayer audio session, so they affect every track without any UI coupling.
 */
@Singleton
class AudioEffectsController @Inject constructor() {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null

    private val _state = MutableStateFlow(AudioEffectsState())
    val state: StateFlow<AudioEffectsState> = _state.asStateFlow()

    /** Attaches effects to [player]'s audio session; safe to call repeatedly. */
    fun attach(player: ExoPlayer) {
        release()
        try {
            val eq = Equalizer(0, player.audioSessionId).apply { enabled = false }
            val presets = (0 until eq.numberOfPresets.toInt()).map { index ->
                runCatching { eq.getPresetName(index.toShort()).trim() }.getOrDefault("Preset $index")
            }
            val bands = (0 until eq.numberOfBands.toInt()).map { band ->
                EqBand(
                    index = band.toShort(),
                    centerFrequencyHz = eq.getCenterFreq(band.toShort()) / 1000,
                    minLevelMb = eq.bandLevelRange[0],
                    maxLevelMb = eq.bandLevelRange[1],
                    levelMb = eq.getBandLevel(band.toShort()),
                )
            }
            val presetCount = eq.numberOfPresets.toInt()
            val currentPreset = runCatching {
                val index = eq.currentPreset.toInt()
                if (index in 0 until presetCount) eq.getPresetName(eq.currentPreset).trim() else ""
            }.getOrDefault("")

            equalizer = eq
            bassBoost = BassBoost(0, player.audioSessionId).apply { enabled = false }
            _state.value = AudioEffectsState(
                supported = true,
                enabled = false,
                presetName = currentPreset,
                presets = presets,
                bands = bands,
            )
        } catch (e: RuntimeException) {
            // Some devices and audio HALs do not expose an equaliser.
            Log.w(TAG, "Audio effects unavailable", e)
            _state.value = AudioEffectsState(supported = false)
        }
    }

    fun setEnabled(enabled: Boolean) {
        val eq = equalizer ?: return
        runCatching { eq.enabled = enabled }
        runCatching { bassBoost?.enabled = enabled && _state.value.bassBoostStrength > 0 }
        _state.value = _state.value.copy(enabled = enabled)
    }

    fun setBandLevel(bandIndex: Short, levelMb: Short) {
        val eq = equalizer ?: return
        runCatching { eq.setBandLevel(bandIndex, levelMb) }
        _state.value = _state.value.copy(
            bands = _state.value.bands.map { if (it.index == bandIndex) it.copy(levelMb = levelMb) else it },
        )
    }

    fun applyPreset(index: Short) {
        val eq = equalizer ?: return
        runCatching {
            eq.usePreset(index)
            _state.value = _state.value.copy(
                presetName = eq.getPresetName(index.toShort()).trim(),
                bands = _state.value.bands.map { band ->
                    band.copy(levelMb = eq.getBandLevel(band.index))
                },
            )
        }
    }

    fun setBassBoostStrength(strength: Int) {
        val boost = bassBoost ?: return
        val clamped = strength.coerceIn(0, 1000).toShort()
        runCatching {
            boost.setStrength(clamped)
            boost.enabled = _state.value.enabled && clamped > 0
        }
        _state.value = _state.value.copy(bassBoostStrength = strength.coerceIn(0, 1000))
    }

    /** Releases native effect resources. */
    fun release() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        equalizer = null
        bassBoost = null
    }

    private companion object {
        const val TAG = "AudioEffects"
    }
}
