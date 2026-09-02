package com.example.musicon.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.musicon.R
import com.example.musicon.data.PlayerImageMode
import com.example.musicon.ui.theme.LavenderTitle
import com.example.musicon.ui.viewmodel.MainViewModel
import com.example.musicon.logic.LrcParser
import com.example.musicon.logic.LyricLine
import java.io.File

@Composable
fun PlayerScreen(
    viewModel: MainViewModel,
    player: Player?,
    onBack: () -> Unit
) {
    if (player == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White)
        }
        return
    }

    val queue by viewModel.playbackQueue.collectAsState()
    val imageMode by viewModel.playerImageMode.collectAsState()
    val primaryColor = MaterialTheme.colorScheme.primary
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    var isPlaying by remember { mutableStateOf(player.isPlaying) }
    var currentMediaItem by remember { mutableStateOf(player.currentMediaItem) }
    var position by remember { mutableLongStateOf(player.currentPosition) }
    var isDragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableLongStateOf(0L) }
    
    var shuffleMode by remember { mutableStateOf(player.shuffleModeEnabled) }
    var repeatMode by remember { mutableIntStateOf(player.repeatMode) }
    val duration = player.duration.coerceAtLeast(1L)
    
    var selectedTab by remember { mutableIntStateOf(0) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    val currentTrackState by viewModel.currentPlayingTrack.collectAsState()
    val currentTrack = currentTrackState
    val sleepTimerRemaining by viewModel.sleepTimerRemaining.collectAsState()

    val listener = object : Player.Listener {
        override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
        override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) { currentMediaItem = mediaItem }
        override fun onShuffleModeEnabledChanged(enabled: Boolean) { shuffleMode = enabled }
        override fun onRepeatModeChanged(mode: Int) { repeatMode = mode }
    }

    DisposableEffect(player) {
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    LaunchedEffect(isPlaying, isDragging) {
        while (isPlaying && !isDragging) {
            position = player.currentPosition
            viewModel.savePlaybackState(position)
            kotlinx.coroutines.delay(500)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Full Screen Background (Back Layer)
        if (imageMode == PlayerImageMode.FULL_SCREEN && currentTrack != null) {
            val trackForBg = currentTrack 
            val artworkUri = remember(trackForBg.id) {
                val path = trackForBg.customCoverPath ?: trackForBg.localPath
                if (path != null) {
                    if (path.startsWith("content://")) Uri.parse(path) else File(path)
                } else {
                    R.drawable.ic_launcher_foreground
                }
            }
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(artworkUri)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)))
        }

        // Mid Layer: Stellar Background
        com.example.musicon.ui.components.StellarBackground(
            showInternalBackground = imageMode != PlayerImageMode.FULL_SCREEN
        ) {
            // Front Layer: Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(), // Seat flush but avoid clock
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header (Fixed Height 64dp, smaller in landscape)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(if (isLandscape) 48.dp else 64.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }
                    
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        divider = {},
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = primaryColor
                            )
                        },
                        modifier = Modifier.width(if (isLandscape) 240.dp else 160.dp)
                    ) {
                        Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Player", color = if (selectedTab == 0) Color.White else Color.Gray, fontSize = 14.sp) } )
                        Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Lyrics", color = if (selectedTab == 1) Color.White else Color.Gray, fontSize = 14.sp) } )
                    }

                    Row {
                        var showMoreMenu by remember { mutableStateOf(false) }
                        var showAddToPlaylistDialog by remember { mutableStateOf(false) }
                        val playlists by viewModel.allPlaylists.collectAsState()

                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(Icons.Default.MoreVert, null, tint = Color.White)
                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Add to Playlist") },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null) },
                                    onClick = { showAddToPlaylistDialog = true; showMoreMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Sleep Timer") },
                                    leadingIcon = { Icon(Icons.Default.Timer, null) },
                                    onClick = { showSleepTimerDialog = true; showMoreMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Add to Cloud") },
                                    leadingIcon = { Icon(Icons.Default.CloudUpload, null) },
                                    onClick = { currentTrack?.let { viewModel.uploadTrack(it) }; showMoreMenu = false }
                                )
                                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                                DropdownMenuItem(
                                    text = { Text("Remove from List") },
                                    leadingIcon = { Icon(Icons.Default.RemoveCircleOutline, null) },
                                    onClick = { currentTrack?.let { viewModel.removeFromLibrary(listOf(it)) }; showMoreMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete from Device", color = Color.Red) },
                                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color.Red) },
                                    onClick = { currentTrack?.let { viewModel.bulkDelete(listOf(it)) }; showMoreMenu = false }
                                )
                            }
                        }

                        if (showAddToPlaylistDialog && currentTrack != null) {
                            com.example.musicon.ui.components.AddToPlaylistDialog(
                                playlists = playlists,
                                onDismiss = { showAddToPlaylistDialog = false },
                                onPlaylistSelected = { viewModel.addTrackToPlaylist(it, currentTrack.id); showAddToPlaylistDialog = false },
                                onCreateNew = { showAddToPlaylistDialog = false; viewModel.createPlaylist("New Playlist", listOf(currentTrack.id)) }
                            )
                        }
                    }
                }

                // Sleep Timer Message
                AnimatedVisibility(
                    visible = sleepTimerRemaining != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    sleepTimerRemaining?.let { remaining ->
                        Surface(
                            color = Color.White.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Text(
                                text = "Music stops in ${remaining / 60000}m",
                                color = Color.White, // Match Song Name Color
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.headlineSmall.copy(fontSize = if (isLandscape) 18.sp else 14.sp),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Weighted Content Area
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (selectedTab == 0) {
                        if (isLandscape) {
                            PlayerLayoutLandscape(
                                player = player,
                                viewModel = viewModel,
                                currentTrack = currentTrack,
                                queue = queue,
                                imageMode = imageMode,
                                primaryColor = primaryColor,
                                isPlaying = isPlaying,
                                position = position,
                                duration = duration,
                                isDragging = isDragging,
                                dragPosition = dragPosition,
                                onDragPositionChange = { dragPosition = it },
                                onDraggingChange = { isDragging = it },
                                onPositionUpdate = { position = it },
                                shuffleMode = shuffleMode,
                                repeatMode = repeatMode
                            )
                        } else {
                            PlayerLayoutPortrait(
                                player = player,
                                viewModel = viewModel,
                                currentTrack = currentTrack,
                                queue = queue,
                                imageMode = imageMode,
                                primaryColor = primaryColor,
                                isPlaying = isPlaying,
                                position = position,
                                duration = duration,
                                isDragging = isDragging,
                                dragPosition = dragPosition,
                                onDragPositionChange = { dragPosition = it },
                                onDraggingChange = { isDragging = it },
                                onPositionUpdate = { position = it },
                                shuffleMode = shuffleMode,
                                repeatMode = repeatMode
                            )
                        }
                    } else {
                        var showEditDialog by remember { mutableStateOf(false) }
                        
                        Box(modifier = Modifier.fillMaxSize()) {
                            LyricsView(
                                modifier = Modifier.fillMaxSize(),
                                currentPosition = if (isDragging) dragPosition else position,
                                lyrics = currentTrack?.lyrics,
                                primaryColor = primaryColor
                            )
                            
                            // Edit/Add Button
                            FloatingActionButton(
                                onClick = { showEditDialog = true },
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(24.dp),
                                containerColor = primaryColor,
                                contentColor = Color.Black
                            ) {
                                Icon(if (currentTrack?.lyrics.isNullOrBlank()) Icons.Default.Add else Icons.Default.Edit, null)
                            }
                        }

                        if (showEditDialog && currentTrack != null) {
                            com.example.musicon.ui.components.EditTrackDialog(
                                track = currentTrack,
                                onDismiss = { showEditDialog = false },
                                onConfirm = { t, ar, al, c, l ->
                                    viewModel.updateTrackMetadata(currentTrack.id, t, ar, al, c, l)
                                    showEditDialog = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSleepTimerDialog) {
        com.example.musicon.ui.screens.SleepTimerDialog(
            onDismiss = { showSleepTimerDialog = false },
            onSet = { viewModel.setSleepTimer(it); showSleepTimerDialog = false }
        )
    }
}

@Composable
fun PlayerLayoutPortrait(
    player: Player,
    viewModel: MainViewModel,
    currentTrack: com.example.musicon.data.local.TrackEntity?,
    queue: List<com.example.musicon.data.local.TrackEntity>,
    imageMode: PlayerImageMode,
    primaryColor: Color,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    isDragging: Boolean,
    dragPosition: Long,
    onDragPositionChange: (Long) -> Unit,
    onDraggingChange: (Boolean) -> Unit,
    onPositionUpdate: (Long) -> Unit,
    shuffleMode: Boolean,
    repeatMode: Int
) {
    val rotationTransition = rememberInfiniteTransition(label = "rotation")
    val rotation by rotationTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(10000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "rotation"
    )

    var hasSkippedInSession by remember { mutableStateOf(false) }
    val coverPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null && currentTrack != null) {
            viewModel.updateTrackMetadata(
                currentTrack.id, null, null, null, uri.toString(), null
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Flexible Image Area (Aligned to BOTTOM to touch duration)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(currentTrack?.id) {
                    detectTapGestures(
                        onDoubleTap = { currentTrack?.let { viewModel.toggleFavorite(it) } }
                    )
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { hasSkippedInSession = false },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            if (!hasSkippedInSession) {
                                if (dragAmount > 50) { player.seekToPrevious(); hasSkippedInSession = true }
                                else if (dragAmount < -50) { player.seekToNext(); hasSkippedInSession = true }
                            }
                        }
                    )
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            if (imageMode != PlayerImageMode.FULL_SCREEN && currentTrack != null) {
                val trackForImg = currentTrack
                val artworkUri = remember(trackForImg.id) {
                    val path = trackForImg.customCoverPath ?: trackForImg.localPath
                    if (path != null) {
                        if (path.startsWith("content://")) Uri.parse(path) else File(path)
                    } else null
                }

                Box(contentAlignment = Alignment.Center) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(artworkUri ?: R.drawable.ic_launcher_foreground)
                            .crossfade(true)
                            .build(), 
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize(0.95f) // Take full available height to touch duration
                            .aspectRatio(1f)
                            .clip(if (imageMode == PlayerImageMode.ROTATION) CircleShape else RoundedCornerShape(24.dp))
                            .rotate(if (imageMode == PlayerImageMode.ROTATION && isPlaying) rotation else 0f),
                        contentScale = ContentScale.Crop
                    )
                    
                    if (currentTrack.customCoverPath == null) {
                        IconButton(
                            onClick = { coverPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), CircleShape)
                        ) {
                            Icon(Icons.Default.Add, "Add Image", tint = Color.White)
                        }
                    }
                }
            } else if (imageMode == PlayerImageMode.FULL_SCREEN) {
                Spacer(modifier = Modifier.fillMaxHeight(0.3f))
            }
        }

        PlayerControls(
            currentTrack = currentTrack,
            player = player,
            viewModel = viewModel,
            primaryColor = primaryColor,
            isPlaying = isPlaying,
            position = position,
            duration = duration,
            isDragging = isDragging,
            dragPosition = dragPosition,
            onDragPositionChange = onDragPositionChange,
            onDraggingChange = onDraggingChange,
            onPositionUpdate = onPositionUpdate,
            shuffleMode = shuffleMode,
            repeatMode = repeatMode,
            queue = queue
        )
    }
}

@Composable
fun PlayerLayoutLandscape(
    player: Player,
    viewModel: MainViewModel,
    currentTrack: com.example.musicon.data.local.TrackEntity?,
    queue: List<com.example.musicon.data.local.TrackEntity>,
    imageMode: PlayerImageMode,
    primaryColor: Color,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    isDragging: Boolean,
    dragPosition: Long,
    onDragPositionChange: (Long) -> Unit,
    onDraggingChange: (Boolean) -> Unit,
    onPositionUpdate: (Long) -> Unit,
    shuffleMode: Boolean,
    repeatMode: Int
) {
    val rotationTransition = rememberInfiniteTransition(label = "rotation")
    val rotation by rotationTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(10000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "rotation"
    )
    
    var hasSkippedInSession by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .pointerInput(currentTrack?.id) {
                    detectTapGestures(onDoubleTap = { currentTrack?.let { viewModel.toggleFavorite(it) } })
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { hasSkippedInSession = false },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            if (!hasSkippedInSession) {
                                if (dragAmount > 50) { player.seekToPrevious(); hasSkippedInSession = true }
                                else if (dragAmount < -50) { player.seekToNext(); hasSkippedInSession = true }
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (imageMode != PlayerImageMode.FULL_SCREEN && currentTrack != null) {
                val artworkUri = remember(currentTrack.id) {
                    val path = currentTrack.customCoverPath ?: currentTrack.localPath
                    if (path != null) {
                        if (path.startsWith("content://")) Uri.parse(path) else File(path)
                    } else null
                }

                Box(contentAlignment = Alignment.Center) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(artworkUri ?: R.drawable.ic_launcher_foreground)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxHeight(1f) // Maximize in landscape
                            .aspectRatio(1f)
                            .clip(if (imageMode == PlayerImageMode.ROTATION) CircleShape else RoundedCornerShape(24.dp))
                            .rotate(if (imageMode == PlayerImageMode.ROTATION && isPlaying) rotation else 0f),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        Spacer(Modifier.width(32.dp))

        Box(modifier = Modifier.weight(1.5f)) { // Adjusted weight for better landscape visibility
            PlayerControls(
                currentTrack = currentTrack,
                player = player,
                viewModel = viewModel,
                primaryColor = primaryColor,
                isPlaying = isPlaying,
                position = position,
                duration = duration,
                isDragging = isDragging,
                dragPosition = dragPosition,
                onDragPositionChange = onDragPositionChange,
                onDraggingChange = onDraggingChange,
                onPositionUpdate = onPositionUpdate,
                shuffleMode = shuffleMode,
                repeatMode = repeatMode,
                queue = queue,
                isLandscape = true
            )
        }
    }
}

@Composable
fun PlayerControls(
    currentTrack: com.example.musicon.data.local.TrackEntity?,
    player: Player,
    viewModel: MainViewModel,
    primaryColor: Color,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    isDragging: Boolean,
    dragPosition: Long,
    onDragPositionChange: (Long) -> Unit,
    onDraggingChange: (Boolean) -> Unit,
    onPositionUpdate: (Long) -> Unit,
    shuffleMode: Boolean,
    repeatMode: Int,
    queue: List<com.example.musicon.data.local.TrackEntity>,
    isLandscape: Boolean = false
) {
    Column(
        modifier = Modifier.fillMaxWidth().wrapContentHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Duration Text (Touching artwork)
        Text(
            text = "${formatTime(if (isDragging) dragPosition else position)} / ${formatTime(duration)}",
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 32.sp, fontWeight = FontWeight.Bold),
            color = Color.White,
            modifier = Modifier.padding(top = 0.dp)
        )

        Spacer(modifier = Modifier.height(if (isLandscape) 8.dp else 16.dp))

        // Track Info (Centered)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.size(48.dp))
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(currentTrack?.displayName ?: "Unknown", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                Text(currentTrack?.displayArtist ?: "Unknown Artist", style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            }
            IconButton(onClick = { currentTrack?.let { viewModel.toggleFavorite(it) } }) {
                Icon(if (currentTrack?.isFavorite == true) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, tint = if (currentTrack?.isFavorite == true) Color.Red else Color.White)
            }
        }

        Spacer(modifier = Modifier.height(if (isLandscape) 4.dp else 8.dp))

        Slider(
            value = (if (isDragging) dragPosition else position).toFloat(),
            onValueChange = { onDraggingChange(true); onDragPositionChange(it.toLong()) },
            onValueChangeFinished = { player.seekTo(dragPosition); onPositionUpdate(dragPosition); onDraggingChange(false) },
            valueRange = 0f..duration.toFloat(),
            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = primaryColor, inactiveTrackColor = Color.White.copy(alpha = 0.2f))
        )

        Spacer(modifier = Modifier.height(if (isLandscape) 4.dp else 12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { player.shuffleModeEnabled = !player.shuffleModeEnabled }) { 
                Icon(Icons.Default.Shuffle, null, tint = if (shuffleMode) primaryColor else Color.White.copy(alpha = 0.6f)) 
            }
            IconButton(onClick = { player.seekToPrevious() }) { Icon(Icons.Default.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(44.dp)) }
            Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(primaryColor.copy(alpha = 0.9f)).clickable { if (isPlaying) player.pause() else player.play() }, contentAlignment = Alignment.Center) {
                Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(36.dp))
            }
            IconButton(onClick = { player.seekToNext() }) { Icon(Icons.Default.SkipNext, null, tint = Color.White, modifier = Modifier.size(44.dp)) }
            IconButton(onClick = { 
                player.repeatMode = when(repeatMode) {
                    Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                    Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                    else -> Player.REPEAT_MODE_OFF
                }
            }) { 
                val icon = if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat
                Icon(icon, null, tint = if (repeatMode != Player.REPEAT_MODE_OFF) primaryColor else Color.White.copy(alpha = 0.6f)) 
            }
        }

        if (!isLandscape) {
            Spacer(modifier = Modifier.height(24.dp))
            LazyRow(modifier = Modifier.fillMaxWidth().height(56.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                itemsIndexed(queue) { _, track ->
                    val isCurrent = currentTrack?.id == track.id
                    val imageModel = remember(track.id) {
                        val path = track.customCoverPath ?: track.localPath
                        if (path != null) {
                            if (path.startsWith("content://")) Uri.parse(path) else File(path)
                        } else {
                            R.drawable.ic_launcher_foreground
                        }
                    }
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(imageModel).crossfade(true).build(),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).border(2.dp, if (isCurrent) primaryColor else Color.Transparent, RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.05f)).clickable { viewModel.playTrack(track) },
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

@Composable
fun LyricsView(
    modifier: Modifier = Modifier,
    currentPosition: Long,
    lyrics: String?,
    primaryColor: Color
) {
    val lyricsLines = remember(lyrics) { if (lyrics != null) LrcParser.parse(lyrics) else emptyList() }
    val listState = rememberLazyListState()
    val currentLineIndex = remember(lyricsLines, currentPosition) {
        val index = lyricsLines.indexOfLast { it.timeMs <= currentPosition }
        if (index == -1) 0 else index
    }

    LaunchedEffect(currentLineIndex) { if (lyricsLines.isNotEmpty()) listState.animateScrollToItem(currentLineIndex, scrollOffset = -200) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .drawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.15f to Color.Black,
                        0.85f to Color.Black,
                        1f to Color.Transparent
                    ),
                    blendMode = BlendMode.DstIn
                )
            }
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        if (lyricsLines.isEmpty()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.MusicNote, null, modifier = Modifier.size(48.dp), tint = primaryColor.copy(alpha = 0.3f))
                Spacer(Modifier.height(16.dp))
                Text(if (lyrics.isNullOrBlank()) "No lyrics found" else "Plain text lyrics", style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                if (!lyrics.isNullOrBlank()) {
                    Text(lyrics, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.6f), textAlign = TextAlign.Center, modifier = Modifier.verticalScroll(rememberScrollState()))
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(32.dp),
                contentPadding = PaddingValues(vertical = 300.dp)
            ) {
                itemsIndexed(lyricsLines) { index, line ->
                    val isCurrent = index == currentLineIndex
                    val color by animateColorAsState(
                        targetValue = if (isCurrent) Color.White else Color.White.copy(alpha = 0.25f),
                        animationSpec = tween(400)
                    )
                    val scale by animateFloatAsState(
                        targetValue = if (isCurrent) 1.35f else 1.0f,
                        animationSpec = tween(400, easing = EaseOutBack)
                    )

                    Text(
                        text = line.text, 
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal, 
                            fontSize = 24.sp * scale,
                            lineHeight = 36.sp
                        ), 
                        color = color, 
                        textAlign = TextAlign.Center, 
                        modifier = Modifier.fillMaxWidth().graphicsLayer {
                            alpha = if (isCurrent) 1f else 0.4f
                        }
                    )
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
