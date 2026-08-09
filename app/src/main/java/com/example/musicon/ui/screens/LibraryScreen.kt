package com.example.musicon.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.musicon.R
import com.example.musicon.data.local.TrackEntity
import com.example.musicon.ui.components.StellarBackground
import com.example.musicon.ui.components.TrackOptionsBottomSheet
import com.example.musicon.ui.theme.LavenderTitle
import com.example.musicon.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: MainViewModel,
    onOpenSettings: () -> Unit,
    onOpenDrawer: () -> Unit
) {
    val tracks by viewModel.allTracks.collectAsState()
    val playlists by viewModel.allPlaylists.collectAsState()
    var selectedTrackOptions by remember { mutableStateOf<TrackEntity?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Multi-selection state
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    val isSelectionMode = selectedIds.isNotEmpty()
    var showBulkPlaylistDialog by remember { mutableStateOf(false) }

    val tabs = listOf("Songs", "Playlists", "Albums", "Artists", "Genres")
    var selectedTab by remember { mutableIntStateOf(0) }

    val filteredTracks = remember(tracks, searchQuery) {
        if (searchQuery.isEmpty()) tracks else {
            tracks.filter { 
                it.displayName.contains(searchQuery, ignoreCase = true) ||
                it.displayArtist.contains(searchQuery, ignoreCase = true) ||
                it.displayAlbum.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    BackHandler(isSelectionMode || isSearchActive) {
        if (isSelectionMode) selectedIds = emptySet()
        else if (isSearchActive) isSearchActive = false
    }

    StellarBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                if (isSelectionMode) {
                    SelectionTopBar(
                        count = selectedIds.size,
                        onClose = { selectedIds = emptySet() }
                    )
                } else {
                    LibraryTopBar(
                        isSearchActive = isSearchActive,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        onOpenDrawer = onOpenDrawer,
                        onToggleSearch = { isSearchActive = !isSearchActive },
                        onOpenSettings = onOpenSettings,
                        onCreatePlaylist = { showCreatePlaylistDialog = true }
                    )
                }
            },
            bottomBar = {
                AnimatedVisibility(
                    visible = isSelectionMode,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    SelectionBottomBar(
                        onPlay = {
                            val selectedTracks = tracks.filter { it.id in selectedIds }
                            viewModel.playSelected(selectedTracks)
                            selectedIds = emptySet()
                        },
                        onNext = {
                            val selectedTracks = tracks.filter { it.id in selectedIds }
                            viewModel.addToQueueNext(selectedTracks)
                            selectedIds = emptySet()
                        },
                        onPlaylist = {
                            showBulkPlaylistDialog = true
                        },
                        onDelete = {
                            val selectedTracks = tracks.filter { it.id in selectedIds }
                            viewModel.bulkDelete(selectedTracks)
                            selectedIds = emptySet()
                        },
                        onUpload = {
                            val selectedTracks = tracks.filter { it.id in selectedIds }
                            viewModel.bulkUpload(selectedTracks)
                            selectedIds = emptySet()
                        },
                        onRemove = {
                            val selectedTracks = tracks.filter { it.id in selectedIds }
                            viewModel.removeFromLibrary(selectedTracks)
                            selectedIds = emptySet()
                        }
                    )
                }
            }
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                if (!isSelectionMode && !isSearchActive) {
                    ScrollableTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        edgePadding = 16.dp,
                        divider = {},
                        indicator = { tabPositions ->
                            if (selectedTab < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                    color = Color.White
                                )
                            }
                        }
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = { Text(title, color = if (selectedTab == index) Color.White else Color.Gray, fontSize = 17.sp) }
                            )
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    when (selectedTab) {
                        0 -> SongsTab(
                            tracks = filteredTracks,
                            selectedIds = selectedIds,
                            onTrackClick = { track ->
                                if (isSelectionMode) {
                                    selectedIds = if (track.id in selectedIds) selectedIds - track.id else selectedIds + track.id
                                } else {
                                    viewModel.playTrack(track)
                                }
                            },
                            onTrackLongClick = { track ->
                                selectedIds = selectedIds + track.id
                            },
                            onOptions = { selectedTrackOptions = it },
                            onShuffleAll = { viewModel.playSelected(filteredTracks.shuffled()) },
                            onPlayAll = { viewModel.playSelected(filteredTracks) }
                        )
                        1 -> PlaylistsTab(playlists)
                        else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No items found", color = Color.Gray)
                        }
                    }
                }
            }
        }
    }

    if (showCreatePlaylistDialog) {
        com.example.musicon.ui.components.CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onConfirm = { name ->
                viewModel.createPlaylist(name)
                showCreatePlaylistDialog = false
            }
        )
    }

    if (showBulkPlaylistDialog) {
        com.example.musicon.ui.components.AddToPlaylistDialog(
            playlists = playlists,
            onDismiss = { showBulkPlaylistDialog = false },
            onPlaylistSelected = { playlistId ->
                viewModel.bulkAddTracksToPlaylist(playlistId, selectedIds.toList())
                showBulkPlaylistDialog = false
                selectedIds = emptySet()
            },
            onCreateNew = {
                showBulkPlaylistDialog = false
                showCreatePlaylistDialog = true
            }
        )
    }

    if (selectedTrackOptions != null) {
        TrackOptionsBottomSheet(
            track = selectedTrackOptions!!,
            onDismiss = { selectedTrackOptions = null },
            onAction = { action ->
                when (action) {
                    "favorite" -> viewModel.toggleFavorite(selectedTrackOptions!!)
                    "play" -> viewModel.playTrack(selectedTrackOptions!!)
                    "download" -> viewModel.downloadTrack(selectedTrackOptions!!)
                    "add_to_playlist" -> { showBulkPlaylistDialog = false; selectedIds = setOf(selectedTrackOptions!!.id); showBulkPlaylistDialog = true }
                }
                selectedTrackOptions = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryTopBar(
    isSearchActive: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onOpenDrawer: () -> Unit,
    onToggleSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onCreatePlaylist: () -> Unit
) {
    TopAppBar(
        title = {
            if (isSearchActive) {
                TextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search songs, artists, albums", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )
            }
        },
        navigationIcon = {
            if (isSearchActive) {
                IconButton(onClick = onToggleSearch) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }
            } else {
                IconButton(onClick = onOpenDrawer) { Icon(Icons.Default.Menu, null, tint = Color.White) }
            }
        },
        actions = {
            if (!isSearchActive) {
                IconButton(onClick = onCreatePlaylist) { Icon(Icons.Default.Add, "Add Playlist", tint = Color.White) }
                IconButton(onClick = onToggleSearch) { Icon(Icons.Default.Search, null, tint = Color.White) }
                IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, null, tint = Color.White) }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionTopBar(count: Int, onClose: () -> Unit) {
    TopAppBar(
        title = { Text("$count selected", color = Color.White) },
        navigationIcon = {
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, null, tint = Color.White) }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White.copy(alpha = 0.1f))
    )
}

@Composable
fun SelectionBottomBar(
    onPlay: () -> Unit,
    onNext: () -> Unit,
    onPlaylist: () -> Unit,
    onUpload: () -> Unit,
    onRemove: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        color = Color(0xFF1E1B36).copy(alpha = 0.95f),
        modifier = Modifier.fillMaxWidth().height(70.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPlay) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.PlayArrow, null, tint = Color.White); Text("Play", fontSize = 10.sp, color = Color.White) } }
            IconButton(onClick = onNext) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.AutoMirrored.Filled.PlaylistPlay, null, tint = Color.White); Text("Next", fontSize = 10.sp, color = Color.White) } }
            IconButton(onClick = onPlaylist) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null, tint = Color.White); Text("Playlist", fontSize = 10.sp, color = Color.White) } }
            IconButton(onClick = onUpload) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.CloudUpload, null, tint = Color.White); Text("GDrive", fontSize = 10.sp, color = Color.White) } }
            IconButton(onClick = onRemove) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.RemoveCircleOutline, null, tint = Color.White); Text("Remove", fontSize = 10.sp, color = Color.White) } }
            IconButton(onClick = onDelete) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Delete, null, tint = Color.White); Text("Delete", fontSize = 10.sp, color = Color.White) } }
        }
    }
}

@Composable
fun SongsTab(
    tracks: List<TrackEntity>,
    selectedIds: Set<String>,
    onTrackClick: (TrackEntity) -> Unit,
    onTrackLongClick: (TrackEntity) -> Unit,
    onOptions: (TrackEntity) -> Unit,
    onShuffleAll: () -> Unit,
    onPlayAll: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        if (selectedIds.isEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StellarActionButton(
                    label = "Shuffle",
                    icon = Icons.Default.Shuffle,
                    modifier = Modifier.weight(1f),
                    onClick = onShuffleAll
                )
                StellarActionButton(
                    label = "Play",
                    icon = Icons.Default.PlayArrow,
                    modifier = Modifier.weight(1f),
                    onClick = onPlayAll
                )
            }
        }

        LazyColumn(Modifier.fillMaxSize()) {
            items(tracks) { track ->
                StellarTrackItem(
                    track = track,
                    isSelected = track.id in selectedIds,
                    onPlay = { onTrackClick(track) },
                    onLongClick = { onTrackLongClick(track) },
                    onOptions = { onOptions(track) }
                )
            }
        }
    }
}

@Composable
fun PlaylistsTab(playlists: List<com.example.musicon.data.local.Playlist>) {
    if (playlists.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No playlists created yet", color = Color.Gray)
        }
    } else {
        LazyColumn(Modifier.fillMaxSize()) {
            items(playlists) { playlist ->
                ListItem(
                    headlineContent = { Text(playlist.name, color = Color.White) },
                    leadingContent = { Icon(Icons.AutoMirrored.Filled.PlaylistPlay, null, tint = Color.White) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StellarTrackItem(
    track: TrackEntity,
    isSelected: Boolean,
    onPlay: () -> Unit,
    onLongClick: () -> Unit,
    onOptions: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) Color.White.copy(alpha = 0.1f) else Color.Transparent)
            .combinedClickable(
                onClick = onPlay,
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            AsyncImage(
                model = track.customCoverPath ?: track.localPath ?: R.drawable.ic_launcher_foreground,
                contentDescription = null,
                modifier = Modifier.size(52.dp).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            if (isSelected) {
                Box(
                    modifier = Modifier.size(52.dp).background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, null, tint = Color.White)
                }
            }
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
            Text(
                text = track.displayName,
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Cursive, color = LavenderTitle, fontSize = 19.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${track.displayArtist} | ${formatDuration(track.duration)} | ${track.bitrate ?: "320k"}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.LightGray,
                maxLines = 1
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (track.isFavorite) {
                Icon(Icons.Default.Favorite, null, modifier = Modifier.size(16.dp).padding(end = 8.dp), tint = Color.Red)
            }
            if (track.gDriveId != null) {
                Icon(Icons.Default.Cloud, null, modifier = Modifier.size(16.dp), tint = Color.White.copy(alpha = 0.4f))
            }
            IconButton(onClick = onOptions) {
                Icon(Icons.Default.MoreVert, null, tint = Color.Gray)
            }
        }
    }
}

@Composable
fun StellarActionButton(label: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(22.dp)
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = Color.White, fontSize = 14.sp)
    }
}

fun formatDuration(millis: Long): String {
    val seconds = (millis / 1000) % 60
    val minutes = (millis / (1000 * 60)) % 60
    return String.format("%d:%02d", minutes, seconds)
}
