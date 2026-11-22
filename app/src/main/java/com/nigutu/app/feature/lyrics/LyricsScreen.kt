package com.nigutu.app.feature.lyrics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Subject
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nigutu.app.designsystem.EmptyState
import com.nigutu.app.designsystem.ErrorState
import com.nigutu.app.designsystem.GradientBackground
import com.nigutu.app.designsystem.NigutuCyan
import com.nigutu.app.designsystem.NigutuViolet

/**
 * Lyrics view backed by LRCLIB.
 *
 * Time-synchronised lyrics scroll automatically with playback; plain lyrics
 * are shown as a static text column.
 */
@Composable
fun LyricsScreen(
    onBack: () -> Unit,
    viewModel: LyricsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playback by viewModel.playbackState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val lyrics = uiState.lyrics
    val activeIndex = uiState.activeLineIndex(playback.positionMs)

    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0 && lyrics?.synced?.isNotEmpty() == true) {
            runCatching { listState.animateScrollToItem(activeIndex) }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Lyrics", style = MaterialTheme.typography.titleLarge)
                        uiState.song?.let { song ->
                            Text(
                                "${song.title} · ${song.artist}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
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
        GradientBackground(Modifier.padding(padding)) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) {
                    when {
                        uiState.song == null -> EmptyState(
                            icon = Icons.Rounded.Subject,
                            title = "Nothing playing",
                            message = "Play a song to see its lyrics.",
                            modifier = Modifier.padding(top = 48.dp),
                        )

                        uiState.isLoading -> Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) { CircularProgressIndicator() }

                        uiState.error != null -> Column(Modifier.padding(20.dp)) {
                            ErrorState(
                                message = uiState.error.orEmpty(),
                                onRetry = viewModel::retry,
                            )
                        }

                        lyrics == null -> EmptyState(
                            icon = Icons.Rounded.Subject,
                            title = "No lyrics found",
                            message = "LRCLIB does not have lyrics for this track yet.",
                            actionLabel = "Try again",
                            onAction = viewModel::retry,
                            modifier = Modifier.padding(top = 48.dp),
                        )

                        lyrics.isSynced -> LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 32.dp),
                            verticalArrangement = Arrangement.spacedBy(18.dp),
                        ) {
                            itemsIndexed(lyrics.synced) { index, line ->
                                Text(
                                    text = line.text,
                                    style = if (index == activeIndex) {
                                        MaterialTheme.typography.headlineMedium
                                    } else {
                                        MaterialTheme.typography.titleLarge
                                    },
                                    fontWeight = if (index == activeIndex) FontWeight.Bold else FontWeight.Normal,
                                    color = if (index == activeIndex) {
                                        NigutuViolet
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }

                        else -> LazyColumn(
                            contentPadding = PaddingValues(24.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(lyrics.plain.split("\n")) { line ->
                                Text(
                                    text = line,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }

                if (lyrics != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (lyrics.isSynced) "Synced lyrics" else "Plain lyrics",
                            style = MaterialTheme.typography.labelSmall,
                            color = NigutuCyan,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "Source: ${lyrics.source}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
