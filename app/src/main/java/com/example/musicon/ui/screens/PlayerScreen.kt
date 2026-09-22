package com.example.musicon.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import com.example.musicon.logic.formatSleepTime
import com.example.musicon.data.local.TrackEntity
import com.example.musicon.ui.components.AddToPlaylistDialog
import com.example.musicon.ui.components.BackgroundGridDialog
import com.example.musicon.ui.components.CreatePlaylistDialog
import com.example.musicon.ui.components.EditTrackDialog
import com.example.musicon.ui.components.LocalIsBackgroundBright
import com.example.musicon.ui.components.RenameDialog
import com.example.musicon.ui.components.StellarBackground
import com.example.musicon.ui.components.TechnicalInfoPopup
import com.example.musicon.ui.components.SleepTimerDialog
import java.io.File
import kotlinx.coroutines.delay

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

    val currentTrack by viewModel.currentPlayingTrack.collectAsState()
    val queue by viewModel.playbackQueue.collectAsState()
    val imageMode by viewModel.playerImageMode.collectAsState()
    
    var showInfoPopup by remember { mutableStateOf(false) }
    val themeMode by viewModel.themeMode.collectAsState()
    val backgroundMode by viewModel.backgroundMode.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val primaryColor = MaterialTheme.colorScheme.primary
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    var isPlaying by remember { mutableStateOf(player.isPlaying) }
    var position by remember { mutableLongStateOf(player.currentPosition) }
    var isDragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableLongStateOf(0L) }
    
    var shuffleMode by remember { mutableStateOf(player.shuffleModeEnabled) }
    var repeatMode by remember { mutableIntStateOf(player.repeatMode) }
    val duration = player.duration.coerceAtLeast(1L)
    
    var selectedTab by remember { mutableIntStateOf(0) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showBackgroundDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    val sleepTimerRemaining by viewModel.sleepTimerRemaining.collectAsState()

    val listener = object : Player.Listener {
        override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
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
            delay(500)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (imageMode == PlayerImageMode.FULL_SCREEN && currentTrack != null) {
            val trackForBg = currentTrack!!
            val artworkUri = remember(trackForBg.id, trackForBg.customCoverPath) {
                val path = trackForBg.customCoverPath ?: trackForBg.localPath
                if (path != null) {
                    if (path.startsWith("content://") || path.startsWith("http")) Uri.parse(path) else File(path)
                } else null
            }
            
            if (artworkUri != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(artworkUri).crossfade(true).build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)))
            }
        }

        StellarBackground(
            showInternalBackground = imageMode != PlayerImageMode.FULL_SCREEN,
            themeMode = themeMode,
            backgroundMode = backgroundMode
        ) {
            Column(
                modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val isBright = LocalIsBackgroundBright.current
                val contentColor = if (isBright) Color.Black else Color.White
                val secondaryColor = if (isBright) Color.DarkGray else Color.Gray

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(64.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) { 
                        Icon(Icons.Default.KeyboardArrowDown, null, tint = contentColor, modifier = Modifier.size(32.dp)) 
                    }

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
                        modifier = Modifier.width(140.dp)
                    ) {
                        Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Player", color = if (selectedTab == 0) contentColor else secondaryColor, fontSize = 12.sp) } )
                        Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Lyrics", color = if (selectedTab == 1) contentColor else secondaryColor, fontSize = 12.sp) } )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { showBackgroundDialog = true }) { Icon(Icons.Default.Wallpaper, null, tint = contentColor) }

                        IconButton(onClick = {
                            val nextMode = when (imageMode) {
                                PlayerImageMode.SQUARE -> PlayerImageMode.FULL_SCREEN
                                PlayerImageMode.FULL_SCREEN -> PlayerImageMode.ROTATION
                                PlayerImageMode.ROTATION -> PlayerImageMode.SQUARE
                            }
                            viewModel.updatePlayerImageMode(nextMode)
                        }) {
                            Icon(
                                when(imageMode) {
                                    PlayerImageMode.SQUARE -> Icons.Default.CropSquare
                                    PlayerImageMode.FULL_SCREEN -> Icons.Default.Fullscreen
                                    PlayerImageMode.ROTATION -> Icons.Default.Sync
                                },
                                contentDescription = "Change View Mode",
                                tint = contentColor
                            )
                        }

                        var showMoreMenu by remember { mutableStateOf(false) }
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(Icons.Default.MoreVert, null, tint = contentColor)
                            DropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("Share Song", fontFamily = FontFamily.Cursive) }, 
                                    leadingIcon = { Icon(Icons.Default.Share, null) }, 
                                    onClick = { currentTrack?.let { viewModel.shareTrack(it) }; showMoreMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Rename Song", fontFamily = FontFamily.Cursive) }, 
                                    leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, null) }, 
                                    onClick = { showRenameDialog = true; showMoreMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Sleep Timer", fontFamily = FontFamily.Cursive) }, 
                                    leadingIcon = { Icon(Icons.Default.Timer, null) }, 
                                    onClick = { showSleepTimerDialog = true; showMoreMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Add to Playlist", fontFamily = FontFamily.Cursive) }, 
                                    leadingIcon = { Icon(Icons.Default.PlaylistAdd, null) }, 
                                    onClick = { showAddToPlaylistDialog = true; showMoreMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Upload to Drive", fontFamily = FontFamily.Cursive) }, 
                                    leadingIcon = { Icon(Icons.Default.CloudUpload, null) }, 
                                    onClick = { currentTrack?.let { viewModel.uploadTrack(it) }; showMoreMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete from Device", color = Color.Red, fontFamily = FontFamily.Cursive) }, 
                                    leadingIcon = { Icon(Icons.Default.DeleteForever, null, tint = Color.Red) }, 
                                    onClick = { currentTrack?.let { viewModel.bulkDelete(listOf(it)) }; showMoreMenu = false }
                                )
                            }
                        }
                    }
                }

                // 85% Content / 15% Queue
                Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (selectedTab == 0) {
                        Box(modifier = Modifier.weight(0.85f).fillMaxWidth()) {
                            if (isLandscape) {
                                PlayerLayoutLandscape(
                                    player = player,
                                    viewModel = viewModel,
                                    currentTrack = currentTrack,
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
                                    repeatMode = repeatMode,
                                    onInfoClick = { showInfoPopup = true },
                                    sleepTimerRemaining = sleepTimerRemaining
                                )
                            } else {
                                PlayerLayoutPortrait(
                                    player = player,
                                    viewModel = viewModel,
                                    currentTrack = currentTrack,
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
                                    repeatMode = repeatMode,
                                    onInfoClick = { showInfoPopup = true },
                                    sleepTimerRemaining = sleepTimerRemaining
                                )
                            }
                        }
                        
                        // 15% Fixed Queue - Decreased Icon Size
                        Column(modifier = Modifier.weight(0.15f).fillMaxWidth().background(Color.Transparent).padding(bottom = 12.dp)) {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                contentPadding = PaddingValues(horizontal = 24.dp)
                            ) {
                                itemsIndexed(queue) { _, track ->
                                    val isCurrent = currentTrack?.id == track.id
                                    val artworkUri = remember(track.id) {
                                        val path = track.customCoverPath ?: track.localPath
                                        if (path != null) {
                                            if (path.startsWith("content://") || path.startsWith("http")) Uri.parse(path) else File(path)
                                        } else null
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .size(if (isLandscape) 40.dp else 46.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .border(2.dp, if (isCurrent) primaryColor else Color.Transparent, RoundedCornerShape(10.dp))
                                            .background(if (isCurrent) primaryColor.copy(0.2f) else Color.White.copy(0.05f))
                                            .clickable { viewModel.playTrack(track) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (artworkUri != null) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(LocalContext.current).data(artworkUri).crossfade(true).build(),
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Icon(Icons.Default.MusicNote, null, tint = Color.Gray, modifier = Modifier.fillMaxSize(0.5f))
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        var showEditTrackDialog by remember { mutableStateOf(false) }
                        LyricsView(
                            modifier = Modifier.fillMaxSize(),
                            currentPosition = if (isDragging) dragPosition else position,
                            lyrics = currentTrack?.lyrics,
                            primaryColor = primaryColor,
                            onEditLyrics = { showEditTrackDialog = true }
                        )
                        if (showEditTrackDialog && currentTrack != null) {
                            EditTrackDialog(
                                track = currentTrack!!,
                                onDismiss = { showEditTrackDialog = false },
                                onConfirm = { t, ar, al, c, l -> 
                                    viewModel.updateTrackMetadata(currentTrack!!.id, t, ar, al, c, l)
                                    showEditTrackDialog = false 
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showRenameDialog && currentTrack != null) {
        RenameDialog(initialName = currentTrack!!.displayName, onDismiss = { showRenameDialog = false }, onConfirm = { viewModel.updateTrackMetadata(currentTrack!!.id, it, null, null, null, null); showRenameDialog = false })
    }

    if (showAddToPlaylistDialog) {
        val playlists by viewModel.allPlaylists.collectAsState()
        AddToPlaylistDialog(
            playlists = playlists,
            onDismiss = { showAddToPlaylistDialog = false },
            onPlaylistSelected = { pid -> currentTrack?.let { viewModel.bulkAddTracksToPlaylist(pid, listOf(it.id)) }; showAddToPlaylistDialog = false },
            onCreateNew = { showAddToPlaylistDialog = false; showCreatePlaylistDialog = true }
        )
    }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(onDismiss = { showCreatePlaylistDialog = false }, onConfirm = { viewModel.createPlaylist(it); showCreatePlaylistDialog = false })
    }

    if (showInfoPopup && currentTrack != null) {
        TechnicalInfoPopup(track = currentTrack!!, onDismiss = { showInfoPopup = false })
    }

    if (showSleepTimerDialog) {
        SleepTimerDialog(onDismiss = { showSleepTimerDialog = false }, onSet = { h, m, s -> viewModel.setSleepTimer(h, m, s); showSleepTimerDialog = false })
    }

    if (showBackgroundDialog) {
        BackgroundGridDialog(currentMode = backgroundMode, onDismiss = { showBackgroundDialog = false }, onModeSelected = { viewModel.updateBackgroundMode(it) })
    }
}

@Composable
fun PlayerLayoutPortrait(
    player: Player,
    viewModel: MainViewModel,
    currentTrack: TrackEntity?,
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
    repeatMode: Int,
    onInfoClick: () -> Unit,
    sleepTimerRemaining: Long?
) {
    var rotationAngle by remember { mutableStateOf(0f) }
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            val startTime = System.currentTimeMillis()
            val startAngle = rotationAngle
            while (true) {
                val elapsed = System.currentTimeMillis() - startTime
                rotationAngle = (startAngle + (elapsed / 30f)) % 360f
                delay(16)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom // Push controls to bottom
    ) {
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth()
                .pointerInput(Unit) {
                    var dragOffset = 0f
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (dragOffset > 80) {
                                player.seekToPrevious()
                                player.play()
                            } else if (dragOffset < -80) {
                                player.seekToNext()
                                player.play()
                            }
                            dragOffset = 0f
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            dragOffset += dragAmount
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (imageMode != PlayerImageMode.FULL_SCREEN && currentTrack != null) {
                val artworkUri = remember(currentTrack.id) {
                    val path = currentTrack.customCoverPath ?: currentTrack.localPath
                    if (path != null) {
                        if (path.startsWith("content://") || path.startsWith("http")) Uri.parse(path) else File(path)
                    } else null
                }

                Box(contentAlignment = Alignment.Center) {
                    val isBright = LocalIsBackgroundBright.current
                    val fallbackColor = if (isBright) Color.Black else Color.White
                    
                    if (artworkUri != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current).data(artworkUri).crossfade(true).build(),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize(0.95f) 
                                .aspectRatio(1f)
                                .clip(if (imageMode == PlayerImageMode.ROTATION) CircleShape else RoundedCornerShape(32.dp))
                                .rotate(if (imageMode == PlayerImageMode.ROTATION) rotationAngle else 0f),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = fallbackColor.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxSize(0.7f).rotate(rotationAngle)
                        )
                    }

                    // Display Sleep Timer at the center
                    if (sleepTimerRemaining != null) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = formatSleepTime(sleepTimerRemaining),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
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
            isLandscape = false,
            onInfoClick = onInfoClick
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun PlayerLayoutLandscape(
    player: Player,
    viewModel: MainViewModel,
    currentTrack: TrackEntity?,
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
    repeatMode: Int,
    onInfoClick: () -> Unit,
    sleepTimerRemaining: Long?
) {
    var rotationAngle by remember { mutableStateOf(0f) }
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            val startTime = System.currentTimeMillis()
            val startAngle = rotationAngle
            while (true) {
                val elapsed = System.currentTimeMillis() - startTime
                rotationAngle = (startAngle + (elapsed / 30f)) % 360f
                delay(16)
            }
        }
    }
    
    Row(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.weight(1f).fillMaxHeight()
            .pointerInput(Unit) {
                var dragOffset = 0f
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (dragOffset > 100) {
                            player.seekToPrevious()
                            player.play()
                        } else if (dragOffset < -100) {
                            player.seekToNext()
                            player.play()
                        }
                        dragOffset = 0f
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        dragOffset += dragAmount
                    }
                )
            }, contentAlignment = Alignment.Center) {
            if (imageMode != PlayerImageMode.FULL_SCREEN && currentTrack != null) {
                val artworkUri = remember(currentTrack.id) {
                    val path = currentTrack.customCoverPath ?: currentTrack.localPath
                    if (path != null) {
                        if (path.startsWith("content://")) Uri.parse(path) else File(path)
                    } else null
                }

                Box(contentAlignment = Alignment.Center) {
                    val isBright = LocalIsBackgroundBright.current
                    val fallbackColor = if (isBright) Color.Black else Color.White
                    
                    if (artworkUri != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current).data(artworkUri).crossfade(true).build(),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxHeight(0.95f)
                                .aspectRatio(1f)
                                .clip(if (imageMode == PlayerImageMode.ROTATION) CircleShape else RoundedCornerShape(32.dp))
                                .rotate(if (imageMode == PlayerImageMode.ROTATION) rotationAngle else 0f),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = fallbackColor.copy(alpha = 0.5f),
                            modifier = Modifier.size(220.dp).rotate(rotationAngle)
                        )
                    }

                    // Display Sleep Timer at the center
                    if (sleepTimerRemaining != null) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = formatSleepTime(sleepTimerRemaining),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.width(32.dp))

        Box(modifier = Modifier.weight(1.5f)) { 
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
                isLandscape = true,
                onInfoClick = onInfoClick
            )
        }
    }
}

@Composable
fun PlayerControls(
    currentTrack: TrackEntity?,
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
    isLandscape: Boolean = false,
    onInfoClick: () -> Unit = {}
) {
    val isBright = LocalIsBackgroundBright.current
    val contentColor = if (isBright) Color.Black else Color.White
    val secondaryColor = if (isBright) Color.DarkGray else Color.White.copy(alpha = 0.7f)

    Column(
        modifier = Modifier.fillMaxWidth().wrapContentHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "${formatTime(if (isDragging) dragPosition else position)} / ${formatTime(duration)}",
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = if (isLandscape) 22.sp else 28.sp, fontWeight = FontWeight.Bold),
            color = contentColor
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Spacer(modifier = Modifier.width(48.dp)) // Balanced space for the favorite icon on the right
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(currentTrack?.displayName ?: "Unknown", style = MaterialTheme.typography.titleLarge, color = contentColor, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                Text(currentTrack?.displayArtist ?: "Unknown Artist", style = MaterialTheme.typography.bodyMedium, color = secondaryColor, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            }
            IconButton(onClick = { currentTrack?.let { viewModel.toggleFavorite(it) } }) {
                Icon(if (currentTrack?.isFavorite == true) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, tint = if (currentTrack?.isFavorite == true) Color.Red else contentColor)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Slider(
            value = (if (isDragging) dragPosition else position).toFloat(),
            onValueChange = { onDraggingChange(true); onDragPositionChange(it.toLong()) },
            onValueChangeFinished = { player.seekTo(dragPosition); onPositionUpdate(dragPosition); onDraggingChange(false) },
            valueRange = 0f..duration.toFloat(),
            colors = SliderDefaults.colors(thumbColor = contentColor, activeTrackColor = primaryColor, inactiveTrackColor = contentColor.copy(alpha = 0.2f))
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { player.shuffleModeEnabled = !player.shuffleModeEnabled }) { Icon(Icons.Default.Shuffle, null, tint = if (shuffleMode) primaryColor else contentColor.copy(alpha = 0.6f)) }
            IconButton(onClick = { player.seekToPrevious(); player.play() }) { Icon(Icons.Default.SkipPrevious, null, tint = contentColor, modifier = Modifier.size(44.dp)) }
            Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(primaryColor.copy(alpha = 0.9f)).clickable { if (isPlaying) player.pause() else player.play() }, contentAlignment = Alignment.Center) {
                Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(36.dp))
            }
            IconButton(onClick = { player.seekToNext(); player.play() }) { Icon(Icons.Default.SkipNext, null, tint = contentColor, modifier = Modifier.size(44.dp)) }
            IconButton(onClick = { player.repeatMode = if (repeatMode == Player.REPEAT_MODE_OFF) Player.REPEAT_MODE_ALL else if (repeatMode == Player.REPEAT_MODE_ALL) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF }) {
                Icon(if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat, null, tint = if (repeatMode != Player.REPEAT_MODE_OFF) primaryColor else contentColor.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
fun LyricsView(modifier: Modifier = Modifier, currentPosition: Long, lyrics: String?, primaryColor: Color, onEditLyrics: () -> Unit) {
    val lyricsLines = remember(lyrics) { if (!lyrics.isNullOrBlank()) LrcParser.parse(lyrics) else emptyList() }
    val listState = rememberLazyListState()
    val currentLineIndex = remember(lyricsLines, currentPosition) {
        val index = lyricsLines.indexOfLast { it.timeMs <= currentPosition }
        if (index == -1) 0 else index
    }

    LaunchedEffect(currentLineIndex) { if (lyricsLines.isNotEmpty()) listState.animateScrollToItem(currentLineIndex, scrollOffset = -200) }

    Box(
        modifier = modifier.fillMaxSize().graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen).drawWithContent {
            drawContent()
            drawRect(brush = Brush.verticalGradient(0f to Color.Transparent, 0.15f to Color.Black, 0.85f to Color.Black, 1f to Color.Transparent), blendMode = BlendMode.DstIn)
        }.padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        if (lyricsLines.isEmpty()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.MusicNote, null, modifier = Modifier.size(48.dp), tint = primaryColor.copy(alpha = 0.3f))
                Spacer(Modifier.height(16.dp))
                Text(if (lyrics.isNullOrBlank()) "No lyrics found" else "Plain text lyrics", style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                Spacer(Modifier.height(24.dp))
                Button(onClick = onEditLyrics, colors = ButtonDefaults.buttonColors(containerColor = primaryColor)) {
                    Icon(if (lyrics.isNullOrBlank()) Icons.Default.Add else Icons.Default.Edit, null, tint = Color.Black)
                    Spacer(Modifier.width(8.dp))
                    Text(if (lyrics.isNullOrBlank()) "Add Lyrics" else "Edit Lyrics", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Box(Modifier.fillMaxSize()) {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(32.dp), contentPadding = PaddingValues(vertical = 300.dp)) {
                    itemsIndexed(lyricsLines) { index, line ->
                        val isCurrent = index == currentLineIndex
                        val color by animateColorAsState(targetValue = if (isCurrent) Color.White else Color.White.copy(alpha = 0.25f), animationSpec = tween(400))
                        val scale by animateFloatAsState(targetValue = if (isCurrent) 1.35f else 1.0f, animationSpec = tween(400, easing = EaseOutBack))
                        Text(text = line.text, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal, fontSize = 22.sp * scale, lineHeight = 36.sp), color = color, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().graphicsLayer { alpha = if (isCurrent) 1f else 0.4f })
                    }
                }
                
                IconButton(
                    onClick = onEditLyrics,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 80.dp).background(primaryColor.copy(alpha = 0.8f), CircleShape)
                ) {
                    Icon(Icons.Default.Edit, null, tint = Color.Black)
                }
            }
        }
    }
}

fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
