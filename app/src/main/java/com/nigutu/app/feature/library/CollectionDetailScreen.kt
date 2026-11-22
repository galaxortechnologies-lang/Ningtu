package com.nigutu.app.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nigutu.app.designsystem.EmptyState
import com.nigutu.app.designsystem.TrackRow

/** Track list for one album or artist. */
@Composable
fun CollectionDetailScreen(
    onBack: () -> Unit,
    onOpenPlayer: () -> Unit,
    viewModel: CollectionDetailViewModel = hiltViewModel(),
) {
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(viewModel.title.ifBlank { "Collection" }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        if (songs.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.Album,
                title = "No tracks",
                message = "This collection has no tracks in your local library.",
                modifier = Modifier.padding(padding),
            )
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                    TrackRow(
                        song = song,
                        isFavorite = song.id in favoriteIds,
                        isPlaying = false,
                        onClick = { viewModel.play(index, onOpenPlayer) },
                        onFavoriteClick = { viewModel.toggleFavorite(song) },
                    )
                }
            }
        }
    }
}
