package com.nigutu.app.feature.home

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nigutu.app.core.util.formatDuration
import com.nigutu.app.designsystem.ArtworkCard
import com.nigutu.app.designsystem.ArtworkImage
import com.nigutu.app.designsystem.EmptyState
import com.nigutu.app.designsystem.ErrorState
import com.nigutu.app.designsystem.GradientBackground
import com.nigutu.app.designsystem.LoadingSection
import com.nigutu.app.designsystem.NigutuCyan
import com.nigutu.app.designsystem.NigutuViolet
import com.nigutu.app.designsystem.QuickActionChip
import com.nigutu.app.designsystem.SectionHeader
import com.nigutu.app.designsystem.TrackRow

/** Home tab: greeting, quick actions, recent plays, trending and collections. */
@Composable
fun HomeScreen(
    hasAudioAccess: Boolean,
    onRequestAudioAccess: () -> Unit,
    onOpenPlayer: () -> Unit,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    onOpenLibrary: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(hasAudioAccess) {
        if (hasAudioAccess) viewModel.scanLibrary()
    }

    GradientBackground {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item {
                HomeHeader(
                    isScanning = state.isScanning,
                    onRefresh = { viewModel.scanLibrary() },
                )
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        QuickActionChip(
                            label = "Shuffle library",
                            icon = Icons.Rounded.Shuffle,
                            onClick = { viewModel.shuffleLibrary(onOpenPlayer) },
                        )
                    }
                    item {
                        QuickActionChip(
                            label = "Favourites",
                            icon = Icons.Rounded.Favorite,
                            onClick = { viewModel.play(state.favorites, 0, onOpenPlayer) },
                        )
                    }
                    item {
                        QuickActionChip(
                            label = "Local music",
                            icon = Icons.Rounded.LibraryMusic,
                            onClick = { viewModel.play(state.songs, 0, onOpenPlayer) },
                        )
                    }
                }
            }

            if (!hasAudioAccess) {
                item {
                    PermissionCard(onGrant = onRequestAudioAccess)
                }
            }

            if (state.recent.isNotEmpty()) {
                item { SectionHeader(title = "Recently played", actionLabel = "Library", onAction = onOpenLibrary) }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        itemsIndexed(state.recent, key = { _, song -> song.id }) { index, song ->
                            ArtworkCard(
                                title = song.title,
                                subtitle = song.artist,
                                artworkUri = song.artworkUri,
                                onClick = { viewModel.play(state.recent, index, onOpenPlayer) },
                            )
                        }
                    }
                }
            }

            item { SectionHeader(title = "Trending now", actionLabel = "Refresh", onAction = { viewModel.refreshTrending() }) }
            when {
                state.trendingLoading -> item { LoadingSection("Loading the charts…") }
                state.trendingError != null -> item {
                    ErrorState(
                        message = "Check your connection and try again.",
                        onRetry = { viewModel.refreshTrending() },
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
                state.trending.isEmpty() -> item {
                    EmptyState(
                        icon = Icons.Rounded.WifiOff,
                        title = "No chart data",
                        message = "Trending tracks could not be loaded right now.",
                    )
                }
                else -> itemsIndexed(state.trending, key = { _, song -> song.id }) { index, song ->
                    TrackRow(
                        song = song,
                        isFavorite = song.id in state.favoriteIds,
                        isPlaying = false,
                        onClick = { viewModel.play(state.trending, index, onOpenPlayer) },
                        onFavoriteClick = { viewModel.toggleFavorite(song) },
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                }
            }

            if (state.albums.isNotEmpty()) {
                item { SectionHeader(title = "Your albums") }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(state.albums, key = { it.name + it.artist }) { album ->
                            ArtworkCard(
                                title = album.name,
                                subtitle = album.artist,
                                artworkUri = album.artworkUri,
                                onClick = { onAlbumClick(album.name) },
                            )
                        }
                    }
                }
            }

            if (state.artists.isNotEmpty()) {
                item { SectionHeader(title = "Popular artists") }
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

            if (state.isEmptyLibrary && hasAudioAccess) {
                item {
                    EmptyState(
                        icon = Icons.Rounded.Album,
                        title = "Your library is empty",
                        message = "Scan your device to find the music already stored on it.",
                        actionLabel = "Scan device",
                        onAction = { viewModel.scanLibrary() },
                    )
                }
            }

            state.scanError?.let { error ->
                item {
                    ErrorState(
                        message = error,
                        onRetry = { viewModel.scanLibrary() },
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(isScanning: Boolean, onRefresh: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp, top = 20.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "Nigutu",
                style = MaterialTheme.typography.labelLarge,
                color = NigutuCyan,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Listen without limits",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        if (isScanning) {
            Text(
                "Scanning…",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onRefresh) {
            Icon(Icons.Rounded.Refresh, contentDescription = "Rescan library")
        }
    }
}

@Composable
private fun PermissionCard(onGrant: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .fillMaxWidth()
            .background(
                Brush.linearGradient(listOf(NigutuViolet.copy(alpha = 0.28f), NigutuCyan.copy(alpha = 0.18f))),
                RoundedCornerShape(22.dp),
            )
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ArtworkImage(artworkUri = null, size = 40.dp, cornerRadius = 12.dp)
            Spacer(Modifier.size(12.dp))
            Text("Unlock your library", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Nigutu only needs access to audio files on this device to build your library. Nothing is uploaded.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Button(onClick = onGrant) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
            Spacer(Modifier.size(6.dp))
            Text("Allow access")
        }
    }
}
