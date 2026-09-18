package com.example.musicon.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.example.musicon.logic.formatSleepTime
import kotlinx.coroutines.delay

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

    LaunchedEffect(player, isPlaying) {
        if (isPlaying) {
            while (true) {
                position = player.currentPosition
                delay(500) // Update twice a second for smoothness
            }
        } else {
            position = player.currentPosition
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
                .padding(
                    start = if (isLeftMenuOpen || isRightSidebarOpen) 0.dp else 8.dp,
                    end = if (isLeftMenuOpen || isRightSidebarOpen) 0.dp else 8.dp
                )
                .height(64.dp)
                .clip(playerShape)
                .background(if (isBright) Color.White.copy(0.95f) else Color(0xFF1E1B36))
                .clickable { onNavigateToPlayer() }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxSize().padding(8.dp)
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
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = currentMediaItem?.mediaMetadata?.artist?.toString() ?: "Unknown Artist",
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { player.seekToPrevious(); player.play() }) {
                        Icon(Icons.Default.SkipPrevious, null, tint = contentColor, modifier = Modifier.size(24.dp))
                    }
                    IconButton(onClick = { if (isPlaying) player.pause() else player.play() }) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, 
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    IconButton(onClick = { player.seekToNext(); player.play() }) {
                        Icon(Icons.Default.SkipNext, null, tint = contentColor, modifier = Modifier.size(24.dp))
                    }
                }
            }
            
            // Progress Line - Reactive fix
            val progress = if (player.duration > 0) position.toFloat() / player.duration else 0f
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .height(4.dp),
                color = Color.Red,
                trackColor = Color.White.copy(alpha = 0.1f)
            )
        }
    }
}

// Time formatting helper removed
