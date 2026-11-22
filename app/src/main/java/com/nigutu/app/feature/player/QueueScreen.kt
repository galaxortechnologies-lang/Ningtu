package com.nigutu.app.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nigutu.app.designsystem.ArtworkImage
import com.nigutu.app.designsystem.EmptyState
import com.nigutu.app.designsystem.NigutuViolet

/** Shows the playback queue with jump-to and remove actions. */
@Composable
fun QueueScreen(
    onBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.playbackState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Queue") },
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
        if (state.queue.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.QueueMusic,
                title = "The queue is empty",
                message = "Start a song to build up a queue.",
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            itemsIndexed(state.queue, key = { index, song -> "$index-${song.id}" }) { index, song ->
                val isCurrent = song.id == state.currentSong?.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isCurrent) NigutuViolet.copy(alpha = 0.14f) else MaterialTheme.colorScheme.background,
                            RoundedCornerShape(16.dp),
                        )
                        .clickable { viewModel.playQueueIndex(index) }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ArtworkImage(
                        artworkUri = song.artworkUri,
                        size = 46.dp,
                        cornerRadius = 10.dp,
                        contentDescription = song.title,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            song.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            song.artist,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (isCurrent) {
                        Icon(
                            Icons.Rounded.GraphicEq,
                            contentDescription = "Now playing",
                            tint = NigutuViolet,
                            modifier = Modifier.size(20.dp),
                        )
                    } else {
                        IconButton(onClick = { viewModel.removeFromQueue(index) }) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Remove from queue",
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
