package com.example.musicon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import coil.compose.AsyncImage

@Composable
fun MiniPlayer(
    onNavigateToPlayer: () -> Unit,
    player: Player?,
    viewModel: com.example.musicon.ui.viewmodel.MainViewModel,
    modifier: Modifier = Modifier
) {
    if (player == null) return

    val currentPlayingTrack by viewModel.currentPlayingTrack.collectAsState()
    var isPlaying by remember { mutableStateOf(player.isPlaying) }
    var currentMediaItem by remember { mutableStateOf(player.currentMediaItem) }
    var position by remember { mutableLongStateOf(player.currentPosition) }

    val listener = object : Player.Listener {
        override fun onIsPlayingChanged(playing: Boolean) {
            isPlaying = playing
        }
        override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
            currentMediaItem = mediaItem
        }
    }

    DisposableEffect(player) {
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            position = player.currentPosition
            kotlinx.coroutines.delay(1000)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .height(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E1B36).copy(alpha = 0.95f))
            .clickable { onNavigateToPlayer() }
            .padding(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize()
        ) {
            AsyncImage(
                model = currentMediaItem?.mediaMetadata?.artworkUri,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Gray),
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = currentMediaItem?.mediaMetadata?.title?.toString() ?: "No Song",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    maxLines = 1
                )
                Text(
                    text = currentMediaItem?.mediaMetadata?.artist?.toString() ?: "Unknown Artist",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray,
                    maxLines = 1
                )
            }
            IconButton(onClick = { currentPlayingTrack?.let { viewModel.toggleFavorite(it) } }) {
                Icon(
                    if (currentPlayingTrack?.isFavorite == true) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = if (currentPlayingTrack?.isFavorite == true) Color.Red else Color(0xFFC3B1E1)
                )
            }
            IconButton(onClick = { if (isPlaying) player.pause() else player.play() }) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, 
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }
        
        // Progress Line
        val progress = if (player.duration > 0) position.toFloat() / player.duration else 0f
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .height(2.dp),
            color = Color.Red,
            trackColor = Color.Transparent
        )
    }
}
