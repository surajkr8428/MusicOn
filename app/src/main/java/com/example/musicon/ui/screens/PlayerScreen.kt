package com.example.musicon.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import com.example.musicon.R
import com.example.musicon.logic.LyricLine
import com.example.musicon.logic.LrcParser
import com.example.musicon.ui.theme.LavenderTitle
import com.example.musicon.ui.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PlayerScreen(
    viewModel: MainViewModel,
    player: Player?,
    onBack: () -> Unit
) {
    if (player == null) return

    val context = LocalContext.current
    val tracks by viewModel.allTracks.collectAsState()
    var isPlaying by remember { mutableStateOf(player.isPlaying) }
    var currentMediaItem by remember { mutableStateOf(player.currentMediaItem) }
    var position by remember { mutableLongStateOf(player.currentPosition) }
    val duration = player.duration.coerceAtLeast(1L)
    
    var backgroundColor by remember { mutableStateOf(Color(0xFF1E1B36)) }
    var isLyricsVisible by remember { mutableStateOf(false) }

    // Dummy Lyrics
    val lyrics = remember(currentMediaItem) {
        LrcParser.parse("""
            [00:00.00]Welcome to MusicOn
            [00:05.00]Experience the stellar sound
            [00:10.00]Cursive titles and galaxy glows
            [00:15.00]Your music, redefined
            [00:20.00]Enjoy the rotation
            [00:25.00]Syncing with the universe
            [00:30.00]Feel every beat
            [00:35.00]Stellar UI, high fidelity
            [00:40.00]Your library, anywhere
            [00:45.00]Google Drive & Local Sync
            [00:50.00]MusicOn - Play the stars
        """.trimIndent())
    }

    val infiniteTransition = rememberInfiniteTransition(label = "ArtRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ThumbnailRotation"
    )

    BackHandler(onBack = onBack)

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
            kotlinx.coroutines.delay(500)
        }
    }

    LaunchedEffect(currentMediaItem) {
        val artUri = currentMediaItem?.mediaMetadata?.artworkUri
        if (artUri != null) {
            withContext(Dispatchers.IO) {
                try {
                    val inputStream = context.contentResolver.openInputStream(artUri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    if (bitmap != null) {
                        val palette = Palette.from(bitmap).generate()
                        backgroundColor = Color(palette.getDarkVibrantColor(0xFF1E1B36.toInt()))
                    }
                } catch (e: Exception) { }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(backgroundColor.copy(alpha = 0.8f), Color.Black)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.White)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Song", 
                        color = if (!isLyricsVisible) Color.White else Color.White.copy(alpha = 0.5f), 
                        fontWeight = if (!isLyricsVisible) FontWeight.Bold else FontWeight.Normal, 
                        fontSize = 14.sp,
                        modifier = Modifier.clickable { isLyricsVisible = false }
                    )
                    Text("|", color = Color.White.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 8.dp))
                    Text(
                        text = "Lyrics", 
                        color = if (isLyricsVisible) Color.White else Color.White.copy(alpha = 0.5f), 
                        fontWeight = if (isLyricsVisible) FontWeight.Bold else FontWeight.Normal, 
                        fontSize = 14.sp,
                        modifier = Modifier.clickable { isLyricsVisible = true }
                    )
                }
                Row {
                    IconButton(onClick = {}) { Icon(Icons.Default.Checkroom, null, tint = Color.White) }
                    IconButton(onClick = {}) { Icon(Icons.Default.MoreVert, null, tint = Color.White) }
                }
            }

            Spacer(modifier = Modifier.weight(0.2f))

            Crossfade(targetState = isLyricsVisible, label = "ContentSwitch") { showLyrics ->
                if (showLyrics) {
                    LyricsView(lyrics, position)
                } else {
                    SongMainView(position, duration, isPlaying, rotation, currentMediaItem)
                }
            }

            Spacer(modifier = Modifier.weight(0.2f))

            // Info
            Text(
                text = currentMediaItem?.mediaMetadata?.title?.toString() ?: "No Song",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = FontFamily.Cursive,
                    color = LavenderTitle,
                    fontSize = 34.sp
                ),
                maxLines = 1
            )
            Text(
                text = currentMediaItem?.mediaMetadata?.artist?.toString() ?: "Unknown Artist",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.LightGray
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { currentMediaItem?.let { item -> tracks.find { it.id == item.mediaId }?.let { viewModel.toggleFavorite(it) } } }) {
                    val isFav = currentMediaItem?.let { item -> tracks.find { it.id == item.mediaId }?.isFavorite } ?: false
                    Icon(if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, tint = if (isFav) Color.Red else Color.White)
                }
                IconButton(onClick = {}) { Icon(Icons.Default.Timer, null, tint = Color.White) }
                IconButton(onClick = {}) { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null, tint = Color.White) }
                IconButton(onClick = {}) { Icon(Icons.AutoMirrored.Filled.QueueMusic, null, tint = Color.White) }
                IconButton(onClick = {}) { Icon(Icons.Default.Tune, null, tint = Color.White) }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Playback Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {}) { Icon(Icons.Default.Shuffle, null, tint = Color.White.copy(alpha = 0.7f)) }
                IconButton(onClick = { player.seekToPrevious() }) {
                    Icon(Icons.Default.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(48.dp))
                }
                
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                        .clickable { if (isPlaying) player.pause() else player.play() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }

                IconButton(onClick = { player.seekToNext() }) {
                    Icon(Icons.Default.SkipNext, null, tint = Color.White, modifier = Modifier.size(48.dp))
                }
                IconButton(onClick = {}) { Icon(Icons.Default.Block, null, tint = Color.White.copy(alpha = 0.7f)) }
            }

            Spacer(modifier = Modifier.weight(0.1f))

            // Queue Strip
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 24.dp)
            ) {
                items(10) {
                    AsyncImage(
                        model = R.drawable.ic_launcher_foreground,
                        contentDescription = null,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.05f)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

@Composable
fun SongMainView(
    position: Long,
    duration: Long,
    isPlaying: Boolean,
    rotation: Float,
    currentMediaItem: androidx.media3.common.MediaItem?
) {
    Box(
        modifier = Modifier.size(320.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(310.dp)) {
            val sweepAngle = (position.toFloat() / duration) * 360f
            drawCircle(
                color = Color.White.copy(alpha = 0.1f),
                style = Stroke(width = 4.dp.toPx())
            )
            drawArc(
                color = Color.White,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )
            
            val angleRad = Math.toRadians((sweepAngle - 90).toDouble())
            val dotRadius = 155.dp.toPx()
            val dotX = (center.x + dotRadius * Math.cos(angleRad)).toFloat()
            val dotY = (center.y + dotRadius * Math.sin(angleRad)).toFloat()
            drawCircle(Color.White, radius = 6.dp.toPx(), center = androidx.compose.ui.geometry.Offset(dotX, dotY))
        }
        
        AsyncImage(
            model = currentMediaItem?.mediaMetadata?.artworkUri ?: R.drawable.ic_launcher_foreground,
            contentDescription = null,
            modifier = Modifier
                .size(260.dp)
                .graphicsLayer(rotationZ = if (isPlaying) rotation else 0f)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        
        Text(
            text = formatTime(position),
            color = Color.White,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun LyricsView(lyrics: List<LyricLine>, currentPosition: Long) {
    val listState = rememberLazyListState()
    val activeIndex = lyrics.indexOfLast { it.timeMs <= currentPosition }

    LaunchedEffect(activeIndex) {
        if (activeIndex != -1) {
            listState.animateScrollToItem(activeIndex)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        contentPadding = PaddingValues(vertical = 120.dp)
    ) {
        itemsIndexed(lyrics) { index, line ->
            val isActive = index == activeIndex
            val color by animateColorAsState(if (isActive) LavenderTitle else Color.White.copy(alpha = 0.3f), label = "LyricColor")
            val scale by animateFloatAsState(if (isActive) 1.2f else 1f, label = "LyricScale")

            Text(
                text = line.text,
                color = color,
                fontSize = 18.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .graphicsLayer(scaleX = scale, scaleY = scale)
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
