package com.nigutu.app.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nigutu.app.designsystem.ArtworkCard
import com.nigutu.app.designsystem.EmptyState
import com.nigutu.app.designsystem.GradientBackground
import com.nigutu.app.designsystem.PlaylistCard
import com.nigutu.app.designsystem.SectionHeader
import com.nigutu.app.designsystem.TrackRow

/** Library tab: favourites, playlists, recents, albums, artists and local files. */
@Composable
fun LibraryScreen(
    onOpenPlayer: () -> Unit,
    onPlaylistClick: (Long) -> Unit,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        state.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.consumeMessage()
        }
    }

    GradientBackground {
        Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 8.dp, top = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Library", style = MaterialTheme.typography.headlineLarge)
                        Text(
                            "${state.songs.size} local tracks · ${state.favorites.size} favourites",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = viewModel::scanLibrary) {
                        Icon(Icons.Rounded.LibraryMusic, contentDescription = "Rescan library")
                    }
                }
            }

            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(LibraryTab.entries.toList(), key = { it.name }) { tab ->
                        FilterChip(
                            selected = state.selectedTab == tab,
                            onClick = { viewModel.selectTab(tab) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }

            when (state.selectedTab) {
                LibraryTab.FAVORITES -> {
                    if (state.favorites.isEmpty()) {
                        item {
                            EmptyState(
                                icon = Icons.Rounded.Favorite,
                                title = "No favourites yet",
                                message = "Tap the heart on any song to keep it close.",
                            )
                        }
                    } else {
                        itemsIndexed(state.favorites, key = { _, song -> song.id }) { index, song ->
                            TrackRow(
                                song = song,
                                isFavorite = true,
                                isPlaying = false,
                                onClick = { viewModel.play(state.favorites, index, onOpenPlayer) },
                                onFavoriteClick = { viewModel.toggleFavorite(song) },
                                modifier = Modifier.padding(horizontal = 8.dp),
                            )
                        }
                    }
                }

                LibraryTab.PLAYLISTS -> {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedButton(onClick = { showCreateDialog = true }) {
                                Icon(Icons.Rounded.Add, contentDescription = null)
                                Spacer(Modifier.height(0.dp))
                                Text("New playlist")
                            }
                            OutlinedButton(onClick = { showImportDialog = true }) {
                                Icon(Icons.Rounded.PlaylistAdd, contentDescription = null)
                                Text("Import")
                            }
                        }
                    }
                    if (state.playlists.isEmpty()) {
                        item {
                            EmptyState(
                                icon = Icons.Rounded.PlaylistAdd,
                                title = "No playlists",
                                message = "Create your first playlist and start collecting tracks.",
                                actionLabel = "New playlist",
                                onAction = { showCreateDialog = true },
                            )
                        }
                    } else {
                        items(state.playlists, key = { it.id }) { playlist ->
                            PlaylistCard(
                                name = playlist.name,
                                songCount = playlist.songCount,
                                onClick = { onPlaylistClick(playlist.id) },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            )
                        }
                    }
                }

                LibraryTab.RECENT -> {
                    if (state.recent.isEmpty()) {
                        item {
                            EmptyState(
                                icon = Icons.Rounded.LibraryMusic,
                                title = "Nothing played yet",
                                message = "Your listening history will appear here.",
                            )
                        }
                    } else {
                        itemsIndexed(state.recent, key = { _, song -> song.id }) { index, song ->
                            TrackRow(
                                song = song,
                                isFavorite = song.id in state.favoriteIds,
                                isPlaying = false,
                                onClick = { viewModel.play(state.recent, index, onOpenPlayer) },
                                onFavoriteClick = { viewModel.toggleFavorite(song) },
                                modifier = Modifier.padding(horizontal = 8.dp),
                            )
                        }
                    }
                }

                LibraryTab.ALBUMS -> {
                    items(state.albums, key = { it.name + it.artist }) { album ->
                        TrackRow(
                            song = com.nigutu.app.domain.model.Song(
                                id = "album:${album.name}",
                                title = album.name,
                                artist = album.artist,
                                album = album.name,
                                durationMs = 0L,
                                uri = "",
                                artworkUri = album.artworkUri,
                            ),
                            isFavorite = false,
                            isPlaying = false,
                            onClick = { onAlbumClick(album.name) },
                            onFavoriteClick = {},
                            trailing = {
                                Text(
                                    "${album.songCount}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                    }
                }

                LibraryTab.ARTISTS -> {
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            items(state.artists, key = { it.name }) { artist ->
                                ArtworkCard(
                                    title = artist.name,
                                    subtitle = "${artist.songCount} songs",
                                    artworkUri = artist.artworkUri,
                                    onClick = { onArtistClick(artist.name) },
                                    round = true,
                                    cardSize = 124.dp,
                                )
                            }
                        }
                    }
                }

                LibraryTab.LOCAL -> {
                    item { SectionHeader(title = "All local music") }
                    if (state.songs.isEmpty()) {
                        item {
                            EmptyState(
                                icon = Icons.Rounded.LibraryMusic,
                                title = "No local music found",
                                message = "Grant audio access and rescan to load music stored on this device.",
                                actionLabel = "Rescan",
                                onAction = viewModel::scanLibrary,
                            )
                        }
                    } else {
                        itemsIndexed(state.songs, key = { _, song -> song.id }) { index, song ->
                            TrackRow(
                                song = song,
                                isFavorite = song.id in state.favoriteIds,
                                isPlaying = false,
                                onClick = { viewModel.play(state.songs, index, onOpenPlayer) },
                                onFavoriteClick = { viewModel.toggleFavorite(song) },
                                modifier = Modifier.padding(horizontal = 8.dp),
                            )
                        }
                    }
                }
            }
        }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }

    if (showCreateDialog) {
        PlaylistNameDialog(
            title = "New playlist",
            confirmLabel = "Create",
            onDismiss = { showCreateDialog = false },
            onConfirm = { name ->
                viewModel.createPlaylist(name)
                showCreateDialog = false
            },
        )
    }

    if (showImportDialog) {
        ImportPlaylistDialog(
            onDismiss = { showImportDialog = false },
            onConfirm = { json ->
                viewModel.importPlaylist(json)
                showImportDialog = false
            },
        )
    }
}

@Composable
private fun PlaylistNameDialog(
    title: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Playlist name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank(),
            ) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ImportPlaylistDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var json by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import playlist") },
        text = {
            Column {
                Text(
                    "Paste an exported Nigutu playlist (JSON).",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = json,
                    onValueChange = { json = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    label = { Text("Playlist JSON") },
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(json) }, enabled = json.isNotBlank()) { Text("Import") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
