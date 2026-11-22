package com.nigutu.app.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.nigutu.app.core.util.formatClock
import com.nigutu.app.core.util.formatDuration
import com.nigutu.app.designsystem.EmptyState
import com.nigutu.app.designsystem.FavoriteButton
import com.nigutu.app.designsystem.NigutuCyan
import com.nigutu.app.designsystem.NigutuViolet
import com.nigutu.app.domain.model.RepeatMode
import androidx.compose.material.icons.rounded.MusicNote

/** Full-screen now-playing experience. */
@Composable
fun PlayerScreen(
    onBack: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenLyrics: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.playbackState.collectAsStateWithLifecycle()
    val isFavorite by viewModel.isCurrentFavorite.collectAsStateWithLifecycle()
    val sleepRemaining by viewModel.sleepTimerRemainingMs.collectAsStateWithLifecycle()
    var showSleepTimer by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableFloatStateOf(-1f) }

    val song = state.currentSong

    Box(modifier = Modifier.fillMaxSize()) {
        song?.artworkUri?.let { artwork ->
            AsyncImage(
                model = artwork,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = 0.35f,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.55f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.92f),
                            MaterialTheme.colorScheme.background,
                        ),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Close player")
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "NOW PLAYING",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (sleepRemaining != null) {
                        Text(
                            "Sleep in ${formatClock(sleepRemaining!!)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = NigutuCyan,
                        )
                    }
                }
                IconButton(onClick = { showSleepTimer = true }) {
                    Icon(Icons.Rounded.MoreHoriz, contentDescription = "Sleep timer")
                }
            }

            if (song == null) {
                EmptyState(
                    icon = Icons.Rounded.MusicNote,
                    title = "Nothing playing",
                    message = "Pick a song from Home, Search or your Library to start listening.",
                    modifier = Modifier.weight(1f),
                    actionLabel = "Back",
                    onAction = onBack,
                )
            } else {
                Spacer(Modifier.height(12.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    shape = RoundedCornerShape(28.dp),
                    color = Color.Transparent,
                    shadowElevation = 12.dp,
                ) {
                    AsyncImage(
                        model = song.artworkUri,
                        contentDescription = song.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(NigutuViolet.copy(alpha = 0.7f), NigutuCyan.copy(alpha = 0.5f)),
                                ),
                            ),
                    )
                }

                Spacer(Modifier.height(28.dp))
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = buildString {
                        append(song.artist)
                        if (song.album.isNotBlank()) append(" · ${song.album}")
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(Modifier.height(16.dp))

                val sliderValue = if (dragPosition >= 0f) dragPosition else state.progress
                Slider(
                    value = sliderValue.coerceIn(0f, 1f),
                    onValueChange = { dragPosition = it },
                    onValueChangeFinished = {
                        viewModel.seekTo(dragPosition.coerceIn(0f, 1f))
                        dragPosition = -1f
                    },
                    enabled = state.durationMs > 0L,
                )
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        formatDuration(if (dragPosition >= 0f) (state.durationMs * dragPosition).toLong() else state.positionMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        formatDuration(state.durationMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = viewModel::toggleShuffle) {
                        Icon(
                            Icons.Rounded.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (state.shuffleEnabled) NigutuViolet else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = viewModel::previous, modifier = Modifier.size(52.dp)) {
                        Icon(
                            Icons.Rounded.SkipPrevious,
                            contentDescription = "Previous",
                            modifier = Modifier.size(38.dp),
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(72.dp),
                    ) {
                        IconButton(onClick = viewModel::togglePlayPause, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (state.isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(38.dp),
                            )
                        }
                    }
                    IconButton(onClick = viewModel::next, modifier = Modifier.size(52.dp)) {
                        Icon(
                            Icons.Rounded.SkipNext,
                            contentDescription = "Next",
                            modifier = Modifier.size(38.dp),
                        )
                    }
                    IconButton(onClick = viewModel::cycleRepeat) {
                        Icon(
                            imageVector = if (state.repeatMode == RepeatMode.ONE) {
                                Icons.Rounded.RepeatOne
                            } else {
                                Icons.Rounded.Repeat
                            },
                            contentDescription = "Repeat",
                            tint = if (state.repeatMode == RepeatMode.OFF) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                NigutuViolet
                            },
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FavoriteButton(isFavorite = isFavorite, onToggle = viewModel::toggleFavorite)
                    PlayerActionButton(
                        icon = Icons.Rounded.MusicNote,
                        label = "Lyrics",
                        onClick = onOpenLyrics,
                    )
                    PlayerActionButton(
                        icon = Icons.Rounded.QueueMusic,
                        label = "Queue",
                        onClick = onOpenQueue,
                    )
                }
            }
        }
    }

    if (showSleepTimer) {
        SleepTimerDialog(
            remainingMs = sleepRemaining,
            onSelect = { minutes ->
                viewModel.startSleepTimer(minutes)
                showSleepTimer = false
            },
            onCancelTimer = {
                viewModel.cancelSleepTimer()
                showSleepTimer = false
            },
            onDismiss = { showSleepTimer = false },
        )
    }
}

@Composable
private fun PlayerActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SleepTimerDialog(
    remainingMs: Long?,
    onSelect: (Int) -> Unit,
    onCancelTimer: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sleep timer") },
        text = {
            Column {
                Text(
                    remainingMs?.let { "Playback will pause in ${formatClock(it)}." }
                        ?: "Pause playback automatically after a set time.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 15, 30, 60).forEach { minutes ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.width(56.dp),
                        ) {
                            TextButton(onClick = { onSelect(minutes) }) {
                                Text("${minutes}m", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (remainingMs != null) {
                TextButton(onClick = onCancelTimer) { Text("Cancel timer") }
            } else {
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        },
    )
}
