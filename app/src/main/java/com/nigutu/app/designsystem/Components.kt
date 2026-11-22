package com.nigutu.app.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nigutu.app.core.util.formatDuration
import com.nigutu.app.domain.model.Song

/** Full-screen vertical brand gradient behind content. */
@Composable
fun GradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        colors.background,
                        colors.surface,
                        colors.background,
                    ),
                ),
            ),
    ) {
        content()
    }
}

/** Album artwork with a graceful gradient placeholder. */
@Composable
fun ArtworkImage(
    artworkUri: String?,
    modifier: Modifier = Modifier,
    size: Dp? = null,
    cornerRadius: Dp = 16.dp,
    contentDescription: String? = null,
) {
    val shape = RoundedCornerShape(cornerRadius)
    val base = if (size != null) modifier.size(size) else modifier
    Box(
        modifier = base
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        NigutuViolet.copy(alpha = 0.65f),
                        Color(0xFF1F2937),
                        NigutuCyan.copy(alpha = 0.45f),
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (artworkUri.isNullOrBlank()) {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = contentDescription,
                tint = Color.White.copy(alpha = 0.75f),
                modifier = Modifier.size(if (size != null) size / 3 else 32.dp),
            )
        } else {
            AsyncImage(
                model = artworkUri,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** Section title with an optional trailing action. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

/** A compact song row used across Home, Search and Library lists. */
@Composable
fun TrackRow(
    song: Song,
    isFavorite: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier,
    showArtwork: Boolean = true,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showArtwork) {
            ArtworkImage(
                artworkUri = song.artworkUri,
                size = 52.dp,
                cornerRadius = 12.dp,
                contentDescription = song.title,
            )
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = buildString {
                    append(song.artist)
                    if (song.isLocal.not()) append(" · Preview")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (song.durationMs > 0L) {
            Text(
                text = formatDuration(song.durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (trailing != null) {
            trailing()
        } else {
            IconButton(onClick = onFavoriteClick) {
                Icon(
                    imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = if (isFavorite) "Remove from favourites" else "Add to favourites",
                    tint = if (isFavorite) NigutuPink else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Vertical card for albums and artists on horizontal carousels. */
@Composable
fun ArtworkCard(
    title: String,
    subtitle: String,
    artworkUri: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cardSize: Dp = 148.dp,
    round: Boolean = false,
) {
    Column(
        modifier = modifier
            .width(cardSize)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
    ) {
        ArtworkImage(
            artworkUri = artworkUri,
            modifier = if (round) Modifier.width(cardSize).aspectRatio(1f) else Modifier.fillMaxWidth().aspectRatio(1f),
            cornerRadius = if (round) cardSize / 2 else 18.dp,
            contentDescription = title,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Playlist card with a stacked gradient tile. */
@Composable
fun PlaylistCard(
    name: String,
    songCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.linearGradient(listOf(NigutuViolet, NigutuCyan)),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Album, contentDescription = null, tint = Color.White)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = if (songCount == 1) "1 song" else "$songCount songs",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Centred empty state with an optional call to action. */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

/** Inline error with a retry action. */
@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
            .padding(16.dp),
    ) {
        Text("Something went wrong", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onRetry) { Text("Try again") }
    }
}

/** Favourite toggle with an animated tint. */
@Composable
fun FavoriteButton(
    isFavorite: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint by animateColorAsState(
        targetValue = if (isFavorite) NigutuPink else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "favoriteTint",
    )
    IconButton(onClick = onToggle, modifier = modifier) {
        Icon(
            imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            contentDescription = if (isFavorite) "Remove from favourites" else "Add to favourites",
            tint = tint,
        )
    }
}

/** Compact now-playing bar shown above the bottom navigation. */
@Composable
fun MiniPlayer(
    song: Song,
    isPlaying: Boolean,
    progress: Float,
    onOpen: () -> Unit,
    onTogglePlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onOpen),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 6.dp,
    ) {
        Column {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ArtworkImage(
                    artworkUri = song.artworkUri,
                    size = 44.dp,
                    cornerRadius = 10.dp,
                    contentDescription = song.title,
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        song.title,
                        style = MaterialTheme.typography.titleMedium,
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
                IconButton(onClick = onTogglePlay) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .height(2.dp)
                        .background(Brush.horizontalGradient(listOf(NigutuViolet, NigutuCyan))),
                )
            }
        }
    }
}

/** Small brand chip for quick actions. */
@Composable
fun QuickActionChip(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .clip(CircleShape)
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        shape = CircleShape,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = NigutuCyan, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Loading placeholder used while a section fetches data. */
@Composable
fun LoadingSection(label: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.GraphicEq,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
