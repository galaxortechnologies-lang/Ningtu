package com.nigutu.app.feature.search

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nigutu.app.designsystem.EmptyState
import com.nigutu.app.designsystem.ErrorState
import com.nigutu.app.designsystem.GradientBackground
import com.nigutu.app.designsystem.SectionHeader
import com.nigutu.app.designsystem.TrackRow

/** Search tab: songs, artists, albums and playlists across both libraries. */
@Composable
fun SearchScreen(
    onOpenPlayer: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val keyboard = LocalSoftwareKeyboardController.current

    GradientBackground {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Text("Search", style = MaterialTheme.typography.headlineLarge)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = viewModel::onQueryChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Songs, artists, albums") },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (state.isSearching) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                    )
                                }
                                if (state.query.isNotEmpty()) {
                                    IconButton(onClick = viewModel::clearQuery) {
                                        Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                                    }
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                keyboard?.hide()
                                viewModel.onSearchSubmitted()
                            },
                        ),
                    )
                }
            }

            if (state.showSuggestions) {
                if (state.history.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Recent searches",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = viewModel::clearHistory) { Text("Clear") }
                        }
                    }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(state.history, key = { it }) { query ->
                                SuggestionChip(
                                    onClick = { viewModel.onHistorySelected(query) },
                                    label = { Text(query) },
                                    icon = {
                                        Icon(
                                            Icons.Rounded.History,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
                item {
                    EmptyState(
                        icon = Icons.Rounded.Search,
                        title = "Find your next favourite",
                        message = "Search your device library and the online catalog at once.",
                        modifier = Modifier.padding(top = 32.dp),
                    )
                }
            } else {
                if (state.localResults.isNotEmpty()) {
                    item { SectionHeader(title = "In your library") }
                    itemsIndexed(state.localResults, key = { _, song -> "local-${song.id}" }) { index, song ->
                        TrackRow(
                            song = song,
                            isFavorite = song.id in state.favoriteIds,
                            isPlaying = false,
                            onClick = { viewModel.play(state.localResults, index, onOpenPlayer) },
                            onFavoriteClick = { viewModel.toggleFavorite(song) },
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                    }
                }

                state.error?.let { message ->
                    item {
                        ErrorState(
                            message = message,
                            onRetry = viewModel::onSearchSubmitted,
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                    }
                }

                if (state.onlineResults.isNotEmpty()) {
                    item { SectionHeader(title = "From the catalog") }
                    itemsIndexed(state.onlineResults, key = { _, song -> "remote-${song.id}" }) { index, song ->
                        TrackRow(
                            song = song,
                            isFavorite = song.id in state.favoriteIds,
                            isPlaying = false,
                            onClick = { viewModel.play(state.onlineResults, index, onOpenPlayer) },
                            onFavoriteClick = { viewModel.toggleFavorite(song) },
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                    }
                }

                if (!state.isSearching && !state.hasResults) {
                    item {
                        EmptyState(
                            icon = Icons.Rounded.Search,
                            title = "No matches",
                            message = "Nothing matched \"${state.query}\". Try a different spelling or a broader term.",
                        )
                    }
                }
            }
        }
    }
}
