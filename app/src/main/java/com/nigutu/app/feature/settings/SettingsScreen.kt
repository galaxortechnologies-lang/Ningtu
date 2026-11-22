package com.nigutu.app.feature.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nigutu.app.BuildConfig
import com.nigutu.app.designsystem.GradientBackground
import com.nigutu.app.designsystem.NigutuCyan
import com.nigutu.app.designsystem.NigutuViolet
import com.nigutu.app.domain.model.ThemeMode

/** Settings and profile tab, including the developer credit. */
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val effects by viewModel.audioEffectsState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    GradientBackground {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column {
                    Text("Profile", style = MaterialTheme.typography.headlineLarge)
                    Text(
                        "Make Nigutu yours",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item { DeveloperCard(onInstagram = { context.openUrl(INSTAGRAM_URL) }) }

            item {
                SettingsCard(title = "Appearance", icon = Icons.Rounded.Palette) {
                    Text(
                        "Theme",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = settings.themeMode == mode,
                                onClick = { viewModel.setThemeMode(mode) },
                                label = {
                                    Text(
                                        when (mode) {
                                            ThemeMode.SYSTEM -> "System"
                                            ThemeMode.LIGHT -> "Light"
                                            ThemeMode.DARK -> "Dark"
                                        },
                                    )
                                },
                            )
                        }
                    }
                }
            }

            item {
                SettingsCard(title = "Playback", icon = Icons.Rounded.Wifi) {
                    ToggleRow(
                        title = "Stream over Wi-Fi only",
                        subtitle = "Online tracks are skipped on mobile data.",
                        checked = settings.wifiOnlyStreaming,
                        onCheckedChange = viewModel::setWifiOnlyStreaming,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                context.startActivity(intent)
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.Notifications,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = NigutuCyan,
                        )
                        Spacer(Modifier.size(10.dp))
                        Text(
                            "Notification settings",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(Icons.Rounded.ChevronRight, contentDescription = null)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Online tracks stream at the catalog's preview quality; per-quality selection is not offered by the current provider.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                SettingsCard(title = "Equaliser", icon = Icons.Rounded.GraphicEq) {
                    if (!effects.supported) {
                        Text(
                            "The equaliser is available while a track is playing, and only on devices that expose one.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        ToggleRow(
                            title = "Enable equaliser",
                            subtitle = "Applies to every track in this session.",
                            checked = effects.enabled,
                            onCheckedChange = viewModel::setEqualizerEnabled,
                        )
                        if (effects.presets.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Preset: ${effects.presetName.ifBlank { "Custom" }}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                effects.presets.take(4).forEachIndexed { index, preset ->
                                    FilterChip(
                                        selected = effects.presetName == preset,
                                        onClick = { viewModel.applyPreset(index.toShort()) },
                                        label = { Text(preset, style = MaterialTheme.typography.labelSmall) },
                                    )
                                }
                            }
                        }
                        effects.bands.forEach { band ->
                            var level by remember(band.index, band.levelMb) {
                                mutableFloatStateOf(band.levelMb.toFloat())
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = band.centerFrequencyHz.formatFrequency(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(width = 52.dp, height = 20.dp),
                                )
                                Slider(
                                    value = level,
                                    onValueChange = { level = it },
                                    onValueChangeFinished = {
                                        viewModel.setBandLevel(band.index, level.toInt().toShort())
                                    },
                                    valueRange = band.minLevelMb.toFloat()..band.maxLevelMb.toFloat(),
                                    enabled = effects.enabled,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        ToggleRow(
                            title = "Bass boost",
                            subtitle = "Strength ${effects.bassBoostStrength / 10}%",
                            checked = effects.bassBoostStrength > 0,
                            onCheckedChange = { enabled ->
                                viewModel.setBassBoost(if (enabled) 600 else 0)
                            },
                        )
                    }
                }
            }

            item {
                SettingsCard(title = "About Nigutu", icon = Icons.Rounded.Info) {
                    Text(
                        "Nigutu ${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "A local-first music player with online discovery, built with Kotlin, Jetpack Compose, Media3 and Room.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Code,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = NigutuCyan,
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            "Released under the GNU GPL v3",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeveloperCard(onInstagram: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    listOf(NigutuViolet.copy(alpha = 0.32f), NigutuCyan.copy(alpha = 0.16f)),
                ),
                RoundedCornerShape(24.dp),
            )
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.Person,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(44.dp),
        )
        Spacer(Modifier.size(14.dp))
        Column(Modifier.weight(1f)) {
            Text("Anup Thakur", style = MaterialTheme.typography.titleMedium)
            Text(
                "@anup_thakurrrr",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            "Instagram",
            style = MaterialTheme.typography.labelLarge,
            color = NigutuCyan,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .background(
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.35f),
                    RoundedCornerShape(12.dp),
                )
                .clickable(onClick = onInstagram)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(22.dp))
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = NigutuViolet, modifier = Modifier.size(20.dp))
            Spacer(Modifier.size(10.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(14.dp))
        content()
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun Int.formatFrequency(): String =
    if (this >= 1000) "${this / 1000}.${(this % 1000) / 100} kHz" else "$this Hz"

private fun android.content.Context.openUrl(url: String) {
    runCatching {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}

private const val INSTAGRAM_URL = "https://www.instagram.com/anup_thakurrrr/"
