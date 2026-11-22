package com.nigutu.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nigutu.app.domain.model.AppSettings
import com.nigutu.app.domain.model.ThemeMode
import com.nigutu.app.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore("nigutu_settings")

/** DataStore-backed user preferences. */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : SettingsRepository {

    override val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            themeMode = runCatching {
                ThemeMode.valueOf(prefs[THEME] ?: ThemeMode.DARK.name)
            }.getOrDefault(ThemeMode.DARK),
            wifiOnlyStreaming = prefs[WIFI_ONLY] ?: true,
            equalizerEnabled = prefs[EQ_ENABLED] ?: false,
            bassBoostStrength = prefs[BASS] ?: 0,
            playbackSpeed = prefs[SPEED] ?: 1f,
        )
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[THEME] = mode.name }
    }

    override suspend fun setWifiOnlyStreaming(enabled: Boolean) {
        context.settingsDataStore.edit { it[WIFI_ONLY] = enabled }
    }

    override suspend fun setEqualizerEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[EQ_ENABLED] = enabled }
    }

    override suspend fun setBassBoostStrength(strength: Int) {
        context.settingsDataStore.edit { it[BASS] = strength.coerceIn(0, 1000) }
    }

    override suspend fun setPlaybackSpeed(speed: Float) {
        context.settingsDataStore.edit { it[SPEED] = speed.coerceIn(0.5f, 2f) }
    }

    private companion object {
        val THEME = stringPreferencesKey("theme_mode")
        val WIFI_ONLY = booleanPreferencesKey("wifi_only_streaming")
        val EQ_ENABLED = booleanPreferencesKey("equalizer_enabled")
        val BASS = intPreferencesKey("bass_boost")
        val SPEED = floatPreferencesKey("playback_speed")
    }
}
