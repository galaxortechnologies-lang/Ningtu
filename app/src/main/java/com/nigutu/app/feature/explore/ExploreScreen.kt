package com.nigutu.app.feature.explore

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nigutu.app.designsystem.EmptyState
import com.nigutu.app.designsystem.ErrorState
import com.nigutu.app.designsystem.GradientBackground
import com.nigutu.app.designsystem.LoadingSection
import com.nigutu.app.designsystem.SectionHeader
import com.nigutu.app.designsystem.TrackRow

/** Explore tab: genres and their charts. */
@Composable
fun ExploreScreen(
    onOpenPlayer: () -> Unit,
    viewModel: ExploreViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val visibleTracks = if (state.selectedGenre != null) state.genreTracks else state.chart

    GradientBackground {
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
                        Text("Explore", style = MaterialTheme.typography.headlineLarge)
                        Text(
                            "Charts and genres from the online catalog",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = viewModel::load) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Refresh")
                    }
                }
            }

            if (state.genres.isNotEmpty()) {
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.genres, key = { it.id }) { genre ->
                            FilterChip(
                                selected = state.selectedGenre?.id == genre.id,
                                onClick = { viewModel.selectGenre(genre) },
                                label = { Text(genre.name) },
                            )
                        }
                    }
                }
            }

            item {
                SectionHeader(
                    title = state.selectedGenre?.name?.let { "$it chart" } ?: "Global top tracks",
                )
            }

            when {
                state.isLoading -> item { LoadingSection("Tuning the catalog…") }
                state.error != null -> item {
                    ErrorState(
                        message = state.error.orEmpty(),
                        onRetry = viewModel::load,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
                visibleTracks.isEmpty() -> item {
                    EmptyState(
                        icon = Icons.Rounded.Explore,
                        title = "Nothing here yet",
                        message = "Pick another genre or refresh the chart.",
                        modifier = Modifier.padding(top = 24.dp),
                    )
                }
                else -> itemsIndexed(visibleTracks, key = { _, song -> song.id }) { index, song ->
                    TrackRow(
                        song = song,
                        isFavorite = song.id in state.favoriteIds,
                        isPlaying = false,
                        onClick = { viewModel.play(visibleTracks, index, onOpenPlayer) },
                        onFavoriteClick = { viewModel.toggleFavorite(song) },
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Streaming tracks are 30-second previews supplied by the Deezer public API.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        }
    }
}
