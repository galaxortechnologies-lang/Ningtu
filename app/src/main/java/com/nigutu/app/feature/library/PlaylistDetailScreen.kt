package com.nigutu.app.feature.library

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nigutu.app.designsystem.EmptyState
import com.nigutu.app.designsystem.GradientBackground
import com.nigutu.app.designsystem.NigutuCyan
import com.nigutu.app.designsystem.TrackRow
import androidx.compose.material.icons.rounded.QueueMusic

/** Detail screen for one playlist. */
@Composable
fun PlaylistDetailScreen(
    onBack: () -> Unit,
    onOpenPlayer: () -> Unit,
    viewModel: PlaylistDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var menuExpanded by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(state.name.ifBlank { "Playlist" }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.play(0, onOpenPlayer)
                    }) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = "Play playlist", tint = NigutuCyan)
                    }
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Export playlist") },
                            onClick = {
                                menuExpanded = false
                                viewModel.export()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Delete playlist") },
                            onClick = {
                                menuExpanded = false
                                confirmDelete = true
                            },
                            leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        GradientBackground(Modifier.padding(padding)) {
            if (state.songs.isEmpty()) {
                EmptyState(
                    icon = Icons.Rounded.QueueMusic,
                    title = "This playlist is empty",
                    message = "Add songs from your library to build it up.",
                    modifier = Modifier.padding(top = 48.dp),
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    item {
                        Text(
                            "${state.songs.size} songs",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        )
                    }
                    itemsIndexed(state.songs, key = { _, song -> song.id }) { index, song ->
                        TrackRow(
                            song = song,
                            isFavorite = song.id in state.favoriteIds,
                            isPlaying = false,
                            onClick = { viewModel.play(index, onOpenPlayer) },
                            onFavoriteClick = { viewModel.toggleFavorite(song) },
                            trailing = {
                                IconButton(onClick = { viewModel.removeSong(song) }) {
                                    Icon(
                                        Icons.Rounded.RemoveCircleOutline,
                                        contentDescription = "Remove from playlist",
                                    )
                                }
                            },
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                    }
                }
            }
        }
    }

    state.exportJson?.let { json ->
        AlertDialog(
            onDismissRequest = viewModel::dismissExport,
            title = { Text("Playlist exported") },
            text = {
                Column {
                    Text(
                        "Share this JSON to back up the playlist or move it to another device.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        json,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 6,
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, json)
                        putExtra(Intent.EXTRA_SUBJECT, "${state.name} playlist")
                    }
                    context.startActivity(Intent.createChooser(intent, "Share playlist"))
                    viewModel.dismissExport()
                }) { Text("Share") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = viewModel::dismissExport) { Text("Close") }
                }
            },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete playlist?") },
            text = { Text("This removes the playlist but keeps your music files.") },
            confirmButton = {
                Button(onClick = {
                    confirmDelete = false
                    viewModel.delete(onBack)
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            },
        )
    }
}
