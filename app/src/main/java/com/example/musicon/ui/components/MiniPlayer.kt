package com.example.musicon.ui.components

import androidx.compose.animation.core.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.example.musicon.logic.formatSleepTime

@Composable
fun MiniPlayer(
    onNavigateToPlayer: () -> Unit,
    player: Player?,
    viewModel: com.example.musicon.ui.viewmodel.MainViewModel,
    modifier: Modifier = Modifier,
    isLeftMenuOpen: Boolean = false,
    isRightSidebarOpen: Boolean = false
) {
    if (player == null) return

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    
    // Exact Widths from MainActivity
    val leftDrawerWidth = 300.dp
    val rightSidebarWidth = 320.dp
    
    val targetWidth = when {
        isLeftMenuOpen -> screenWidth - leftDrawerWidth
        isRightSidebarOpen -> screenWidth - rightSidebarWidth
        else -> screenWidth
    }

    val animatedWidth by animateDpAsState(targetValue = targetWidth, label = "width")

    val currentPlayingTrack by viewModel.currentPlayingTrack.collectAsState()
    val sleepTimerRemaining by viewModel.sleepTimerRemaining.collectAsState()
    var isPlaying by remember { mutableStateOf(player.isPlaying) }
    var currentMediaItem by remember { mutableStateOf(player.currentMediaItem) }
    var position by remember { mutableLongStateOf(player.currentPosition) }

    val listener = object : Player.Listener {
        override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
        override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) { currentMediaItem = mediaItem }
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

    val isBright = LocalIsBackgroundBright.current
    val contentColor = if (isBright) Color.Black else Color.White
    val secondaryColor = if (isBright) Color.DarkGray else Color.LightGray

    Box(modifier = Modifier.fillMaxWidth()) {
        val playerShape = when {
            isLeftMenuOpen -> RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 12.dp, bottomEnd = 12.dp)
            isRightSidebarOpen -> RoundedCornerShape(topEnd = 0.dp, bottomEnd = 0.dp, topStart = 12.dp, bottomStart = 12.dp)
            else -> RoundedCornerShape(12.dp)
        }
        
        Box(
            modifier = Modifier
                .width(animatedWidth)
                .align(if (isLeftMenuOpen) Alignment.BottomEnd else if (isRightSidebarOpen) Alignment.BottomStart else Alignment.BottomCenter)
                .padding(vertical = if (isLeftMenuOpen || isRightSidebarOpen) 0.dp else 4.dp)
                .padding(horizontal = if (isLeftMenuOpen || isRightSidebarOpen) 0.dp else 8.dp)
                .height(64.dp)
                .clip(playerShape)
                .background(if (isBright) Color.White.copy(0.95f) else Color(0xFF1E1B36))
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
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = contentColor,
                        maxLines = 1
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = currentMediaItem?.mediaMetadata?.artist?.toString() ?: "Unknown Artist",
                            style = MaterialTheme.typography.bodySmall,
                            color = secondaryColor,
                            maxLines = 1,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "${formatTime(position)} / ${formatTime(player.duration)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = contentColor.copy(alpha = 0.6f)
                        )
                        sleepTimerRemaining?.let { remaining ->
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = formatSleepTime(remaining),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
                IconButton(onClick = { currentPlayingTrack?.let { viewModel.toggleFavorite(it) } }) {
                    Icon(
                        if (currentPlayingTrack?.isFavorite == true) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = if (currentPlayingTrack?.isFavorite == true) Color.Red else (if (isBright) Color.DarkGray else Color(0xFFC3B1E1))
                    )
                }
                IconButton(onClick = { if (isPlaying) player.pause() else player.play() }) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, 
                        contentDescription = null,
                        tint = contentColor
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
}

private fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
