package com.example.musicon.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.musicon.R
import com.example.musicon.data.LibraryViewMode
import com.example.musicon.data.local.TrackEntity
import com.example.musicon.ui.components.StellarBackground
import com.example.musicon.ui.components.TrackOptionsBottomSheet
import com.example.musicon.ui.theme.LavenderTitle
import com.example.musicon.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    viewModel: MainViewModel,
    onOpenSettings: () -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenCutter: (TrackEntity) -> Unit,
    onOpenPlayer: () -> Unit
) {
    val tracks by viewModel.filteredTracks.collectAsState()
    val allTracks by viewModel.allTracks.collectAsState()
    val playlists by viewModel.allPlaylists.collectAsState()
    val customFolders by viewModel.customFolders.collectAsState()
    val searchQuery by viewModel.searchQuery
    val sortOrder by viewModel.songSortOrder.collectAsState()
    val viewMode by viewModel.libraryViewMode.collectAsState()
    val sleepTimerRemaining by viewModel.sleepTimerRemaining.collectAsState()
    
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    var isSearchActive by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var selectedTrackOptions by remember { mutableStateOf<TrackEntity?>(null) }
    var trackToEdit by remember { mutableStateOf<TrackEntity?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var currentPlaylistDetail by remember { mutableStateOf<com.example.musicon.data.local.Playlist?>(null) }

    // Multi-selection state
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    val isSelectionMode = selectedIds.isNotEmpty()
    var showBulkPlaylistDialog by remember { mutableStateOf(false) }

    val baseTabs = listOf("All Songs", "Playlists", "Albums", "Artists", "Genres", "Recent", "Popular")
    val tabs = baseTabs + customFolders.map { 
        it.substringAfterLast("/").substringAfterLast("%2F").ifBlank { "Folder" }
    }
    val pagerState = rememberPagerState { tabs.size }

    BackHandler(isSelectionMode || searchQuery.isNotEmpty() || currentPlaylistDetail != null || isSearchActive) {
        if (isSelectionMode) selectedIds = emptySet()
        else if (isSearchActive) {
            isSearchActive = false
            viewModel.updateSearchQuery("")
        }
        else if (currentPlaylistDetail != null) currentPlaylistDetail = null
    }

    if (currentPlaylistDetail != null) {
        PlaylistDetailScreen(
            playlist = currentPlaylistDetail!!,
            viewModel = viewModel,
            onBack = { currentPlaylistDetail = null },
            viewMode = viewMode,
            isLandscape = isLandscape
        )
    } else {
        StellarBackground {
            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                topBar = {
                    if (isSelectionMode) {
                        SelectionTopBar(count = selectedIds.size, onClose = { selectedIds = emptySet() })
                    } else {
                        LibraryTopBar(
                            searchQuery = searchQuery,
                            isSearchActive = isSearchActive,
                            onSearchToggle = { isSearchActive = !isSearchActive },
                            onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                            onOpenDrawer = onOpenDrawer,
                            onOpenSettings = onOpenSettings,
                            onSortClick = { showSortMenu = true },
                            onViewModeToggle = {
                                val next = if (viewMode == LibraryViewMode.LIST) LibraryViewMode.GRID else LibraryViewMode.LIST
                                viewModel.updateLibraryViewMode(next)
                            },
                            viewMode = viewMode,
                            timerRemaining = sleepTimerRemaining,
                            isLandscape = isLandscape
                        )
                    }
                },
                bottomBar = {
                    AnimatedVisibility(visible = isSelectionMode, enter = expandVertically(), exit = shrinkVertically()) {
                        SelectionBottomBar(
                            onPlay = { viewModel.playSelected(allTracks.filter { it.id in selectedIds }); selectedIds = emptySet() },
                            onNext = { viewModel.addToQueueNext(allTracks.filter { it.id in selectedIds }); selectedIds = emptySet() },
                            onPlaylist = { showBulkPlaylistDialog = true },
                            onDelete = { viewModel.bulkDelete(allTracks.filter { it.id in selectedIds }); selectedIds = emptySet() },
                            onUpload = { viewModel.bulkUpload(allTracks.filter { it.id in selectedIds }); selectedIds = emptySet() },
                            onRemove = { viewModel.removeFromLibrary(allTracks.filter { it.id in selectedIds }); selectedIds = emptySet() }
                        )
                    }
                }
            ) { innerPadding ->
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = {
                        scope.launch {
                            isRefreshing = true
                            viewModel.scanLocalStorage()
                            delay(1500)
                            isRefreshing = false
                        }
                    },
                    modifier = Modifier.padding(innerPadding).fillMaxSize()
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (!isSelectionMode) {
                            ScrollableTabRow(
                                selectedTabIndex = pagerState.currentPage,
                                containerColor = Color.Transparent,
                                edgePadding = 16.dp,
                                divider = {},
                                indicator = { tabPositions ->
                                    if (pagerState.currentPage < tabPositions.size) {
                                        TabRowDefaults.SecondaryIndicator(
                                            modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            ) {
                                tabs.forEachIndexed { index, title ->
                                    Tab(
                                        selected = pagerState.currentPage == index,
                                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                                        text = { Text(title, color = if (pagerState.currentPage == index) Color.White else Color.Gray, fontSize = if (isLandscape) 14.sp else 17.sp) }
                                    )
                                }
                            }
                        }

                        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.Top) { page ->
                            when (page) {
                                0 -> SongsTab(
                                    tracks = tracks,
                                    selectedIds = selectedIds,
                                    viewMode = viewMode,
                                    onTrackClick = { track ->
                                        if (isSelectionMode) selectedIds = if (track.id in selectedIds) selectedIds - track.id else selectedIds + track.id
                                        else {
                                            viewModel.playTrackList(tracks, track)
                                        }
                                    },
                                    onTrackLongClick = { track -> selectedIds = selectedIds + track.id },
                                    onOptions = { selectedTrackOptions = it },
                                    onShuffleAll = { viewModel.playSelected(tracks.shuffled()) },
                                    onPlayAll = { viewModel.playSelected(tracks) },
                                    onToggleFavorite = { viewModel.toggleFavorite(it) }
                                )
                                1 -> PlaylistsTab(playlists, viewMode, { currentPlaylistDetail = it }, { showCreatePlaylistDialog = true })
                                2 -> GroupedTab(allTracks, "Album", viewMode) { viewModel.playSelected(it) }
                                3 -> GroupedTab(allTracks, "Artist", viewMode) { viewModel.playSelected(it) }
                                4 -> GroupedTab(allTracks, "Genre", viewMode) { viewModel.playSelected(it) }
                                5 -> {
                                    val recent by viewModel.recentlyPlayed.collectAsState()
                                    SongsTab(recent, selectedIds, viewMode, { viewModel.playTrackList(recent, it) }, { selectedIds = selectedIds + it.id }, { selectedTrackOptions = it }, { viewModel.playSelected(recent.shuffled()) }, { viewModel.playSelected(recent) }, { viewModel.toggleFavorite(it) })
                                }
                                6 -> {
                                    val popular by viewModel.mostPlayed.collectAsState()
                                    SongsTab(popular, selectedIds, viewMode, { viewModel.playTrackList(popular, it) }, { selectedIds = selectedIds + it.id }, { selectedTrackOptions = it }, { viewModel.playSelected(popular.shuffled()) }, { viewModel.playSelected(popular) }, { viewModel.toggleFavorite(it) })
                                }
                                else -> {
                                    val folderPath = customFolders[page - 7]
                                    val folderTracks = allTracks.filter { it.localPath?.startsWith(folderPath) == true }
                                    SongsTab(folderTracks, selectedIds, viewMode, { viewModel.playTrackList(folderTracks, it) }, { selectedIds = selectedIds + it.id }, { selectedTrackOptions = it }, { viewModel.playSelected(folderTracks.shuffled()) }, { viewModel.playSelected(folderTracks) }, { viewModel.toggleFavorite(it) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreatePlaylistDialog) {
        com.example.musicon.ui.components.CreatePlaylistDialog(onDismiss = { showCreatePlaylistDialog = false }, onConfirm = { viewModel.createPlaylist(it, selectedIds.toList()); showCreatePlaylistDialog = false; selectedIds = emptySet() })
    }

    if (showSortMenu) {
        SortMenu(
            currentOrder = sortOrder,
            onDismiss = { showSortMenu = false },
            onSortSelected = { viewModel.updateSongSortOrder(it); showSortMenu = false }
        )
    }

    if (showBulkPlaylistDialog) {
        com.example.musicon.ui.components.AddToPlaylistDialog(
            playlists = playlists,
            onDismiss = { showBulkPlaylistDialog = false },
            onPlaylistSelected = { viewModel.bulkAddTracksToPlaylist(it, selectedIds.toList()); showBulkPlaylistDialog = false; selectedIds = emptySet() },
            onCreateNew = { showBulkPlaylistDialog = false; showCreatePlaylistDialog = true }
        )
    }

    if (selectedTrackOptions != null) {
        TrackOptionsBottomSheet(
            track = selectedTrackOptions!!,
            onDismiss = { selectedTrackOptions = null },
            onAction = { action ->
                when (action) {
                    "favorite" -> viewModel.toggleFavorite(selectedTrackOptions!!)
                    "play" -> { viewModel.playTrack(selectedTrackOptions!!) }
                    "play_next" -> viewModel.addToQueueNext(listOf(selectedTrackOptions!!))
                    "download" -> viewModel.downloadTrack(selectedTrackOptions!!)
                    "add_to_playlist" -> { showBulkPlaylistDialog = false; selectedIds = setOf(selectedTrackOptions!!.id); showBulkPlaylistDialog = true }
                    "cut" -> onOpenCutter(selectedTrackOptions!!)
                    "edit" -> trackToEdit = selectedTrackOptions
                    "remove" -> viewModel.removeFromLibrary(listOf(selectedTrackOptions!!))
                }
                selectedTrackOptions = null
            }
        )
    }

    if (trackToEdit != null) {
        com.example.musicon.ui.components.EditTrackDialog(
            track = trackToEdit!!,
            onDismiss = { trackToEdit = null },
            onConfirm = { t, ar, al, c, l ->
                viewModel.updateTrackMetadata(trackToEdit!!.id, t, ar, al, c, l)
                trackToEdit = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlist: com.example.musicon.data.local.Playlist,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    viewMode: LibraryViewMode,
    isLandscape: Boolean
) {
    val playlistTracks by viewModel.getTracksForPlaylist(playlist.id).collectAsState(emptyList())

    StellarBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(playlist.name, color = Color.White, fontFamily = FontFamily.Cursive, fontSize = if (isLandscape) 18.sp else 24.sp) },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) } },
                    actions = {
                        IconButton(onClick = { }) { Icon(Icons.Default.Add, null, tint = Color.White) }
                        IconButton(onClick = { }) { Icon(Icons.Default.MoreVert, null, tint = Color.White) }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
                    windowInsets = WindowInsets(0, 0, 0, 0)
                )
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                if (viewMode == LibraryViewMode.GRID) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 100.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(8.dp)
                    ) {
                        items(playlistTracks) { track ->
                            StellarGridItem(track, false, { viewModel.playTrackList(playlistTracks, it) }, {}, { viewModel.toggleFavorite(track) })
                        }
                    }
                } else {
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(playlistTracks) { track ->
                            StellarTrackItem(track, false, { viewModel.playTrackList(playlistTracks, track) }, {}, { 
                            }, { viewModel.toggleFavorite(track) })
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryTopBar(
    searchQuery: String, 
    isSearchActive: Boolean,
    onSearchToggle: () -> Unit,
    onSearchQueryChange: (String) -> Unit, 
    onOpenDrawer: () -> Unit, 
    onOpenSettings: () -> Unit,
    onSortClick: () -> Unit,
    onViewModeToggle: () -> Unit,
    viewMode: LibraryViewMode,
    timerRemaining: Long?,
    isLandscape: Boolean
) {
    TopAppBar(
        title = {
            if (isSearchActive) {
                TextField(
                    value = searchQuery, onValueChange = onSearchQueryChange, placeholder = { Text("Search songs...", color = Color.Gray, fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    singleLine = true,
                    trailingIcon = { IconButton(onClick = onSearchToggle) { Icon(Icons.Default.Close, null, tint = Color.Gray) } }
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("MusicOn", color = Color.White, fontFamily = FontFamily.Cursive, fontWeight = FontWeight.Bold, fontSize = if (isLandscape) 18.sp else 22.sp)
                    if (timerRemaining != null) {
                        Spacer(Modifier.width(12.dp))
                        Surface(color = MaterialTheme.colorScheme.primary.copy(0.2f), shape = RoundedCornerShape(8.dp)) {
                            Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Timer, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("${timerRemaining / 60000}m", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        },
        navigationIcon = { IconButton(onClick = onOpenDrawer) { Icon(Icons.Default.Menu, null, tint = Color.White, modifier = if (isLandscape) Modifier.size(20.dp) else Modifier) } },
        actions = {
            if (!isSearchActive) {
                IconButton(onClick = onSearchToggle) { Icon(Icons.Default.Search, null, tint = Color.White, modifier = if (isLandscape) Modifier.size(20.dp) else Modifier) }
                IconButton(onClick = onViewModeToggle) { 
                    Icon(if (viewMode == LibraryViewMode.LIST) Icons.Default.GridView else Icons.AutoMirrored.Filled.List, null, tint = Color.White, modifier = if (isLandscape) Modifier.size(20.dp) else Modifier) 
                }
                IconButton(onClick = onSortClick) { Icon(Icons.AutoMirrored.Filled.Sort, null, tint = Color.White, modifier = if (isLandscape) Modifier.size(20.dp) else Modifier) }
                IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, null, tint = Color.White, modifier = if (isLandscape) Modifier.size(20.dp) else Modifier) }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
        modifier = Modifier.statusBarsPadding()
    )
}

@Composable
fun SortMenu(
    currentOrder: String,
    onDismiss: () -> Unit,
    onSortSelected: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sort Songs By", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) { // Compact
                SortCategory("Name", "NAME", currentOrder, onSortSelected)
                SortCategory("Artist", "ARTIST", currentOrder, onSortSelected)
                SortCategory("Recent", "RECENT", currentOrder, onSortSelected)
                SortCategory("Duration", "DURATION", currentOrder, onSortSelected)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
fun SortCategory(label: String, baseValue: String, current: String, onSelect: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            SortMiniButton(if (baseValue == "RECENT") "Newest" else if (baseValue == "DURATION") "Longest" else "A-Z", "${baseValue}_ASC", current, onSelect, Modifier.weight(1f))
            SortMiniButton(if (baseValue == "RECENT") "Oldest" else if (baseValue == "DURATION") "Shortest" else "Z-A", "${baseValue}_DESC", current, onSelect, Modifier.weight(1f))
        }
    }
}

@Composable
fun SortMiniButton(label: String, value: String, current: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    val isSelected = value == current
    Button(
        onClick = { onSelect(value) },
        modifier = modifier.height(36.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(0.05f),
            contentColor = if (isSelected) Color.Black else Color.White
        ),
        contentPadding = PaddingValues(horizontal = 8.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(label, fontSize = 11.sp, maxLines = 1)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionTopBar(count: Int, onClose: () -> Unit) {
    TopAppBar(title = { Text("$count selected", color = Color.White) }, navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Default.Close, null, tint = Color.White) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White.copy(alpha = 0.1f)))
}

@Composable
fun SelectionBottomBar(onPlay: () -> Unit, onNext: () -> Unit, onPlaylist: () -> Unit, onUpload: () -> Unit, onRemove: () -> Unit, onDelete: () -> Unit) {
    Surface(color = Color(0xFF1E1B36).copy(alpha = 0.95f), modifier = Modifier.fillMaxWidth().height(70.dp)) {
        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            SelectionActionItem(Icons.Default.PlayArrow, "Play", onPlay)
            SelectionActionItem(Icons.AutoMirrored.Filled.PlaylistPlay, "Next", onNext)
            SelectionActionItem(Icons.AutoMirrored.Filled.PlaylistAdd, "Playlist", onPlaylist)
            SelectionActionItem(Icons.Default.CloudUpload, "GDrive", onUpload)
            SelectionActionItem(Icons.Default.RemoveCircleOutline, "Remove", onRemove)
            SelectionActionItem(Icons.Default.Delete, "Delete", onDelete)
        }
    }
}

@Composable
fun SelectionActionItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp)); Text(label, fontSize = 9.sp, color = Color.White) } }
}

@Composable
fun SongsTab(
    tracks: List<TrackEntity>, 
    selectedIds: Set<String>, 
    viewMode: LibraryViewMode,
    onTrackClick: (TrackEntity) -> Unit, 
    onTrackLongClick: (TrackEntity) -> Unit, 
    onOptions: (TrackEntity) -> Unit, 
    onShuffleAll: () -> Unit, 
    onPlayAll: () -> Unit, 
    onToggleFavorite: (TrackEntity) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        if (selectedIds.isEmpty()) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StellarActionButton(label = "Shuffle", icon = Icons.Default.Shuffle, modifier = Modifier.weight(1f), onClick = onShuffleAll)
                StellarActionButton(label = "Play", icon = Icons.Default.PlayArrow, modifier = Modifier.weight(1f), onClick = onPlayAll)
            }
        }
        
        if (viewMode == LibraryViewMode.GRID) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 100.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(tracks) { track ->
                    StellarGridItem(track, track.id in selectedIds, onTrackClick, onTrackLongClick, { onToggleFavorite(track) })
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(tracks) { track ->
                    StellarTrackItem(track, track.id in selectedIds, { onTrackClick(track) }, { onTrackLongClick(track) }, { onOptions(track) }, { onToggleFavorite(track) })
                }
            }
        }
    }
}

@Composable
fun PlaylistsTab(
    playlists: List<com.example.musicon.data.local.Playlist>, 
    viewMode: LibraryViewMode,
    onPlaylistClick: (com.example.musicon.data.local.Playlist) -> Unit,
    onCreatePlaylist: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        StellarActionButton(
            label = "Create New Playlist", 
            icon = Icons.Default.Add, 
            modifier = Modifier.fillMaxWidth().padding(16.dp), 
            onCreatePlaylist
        )
        
        if (playlists.isEmpty()) Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("No playlists", color = Color.Gray) }
        else {
            if (viewMode == LibraryViewMode.GRID) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 100.dp),
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    items(playlists) { playlist ->
                        PlaylistGridItem(playlist, onPlaylistClick)
                    }
                }
            } else {
                LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                    items(playlists) { playlist ->
                        ListItem(
                            headlineContent = { Text(playlist.name, color = Color.White) },
                            leadingContent = { 
                                val icon = if (playlist.name == "Favorite") Icons.Default.Favorite else Icons.AutoMirrored.Filled.PlaylistAdd
                                Icon(icon, null, tint = if (playlist.name == "Favorite") Color.Red else Color.White)
                            },
                            modifier = Modifier.clickable { onPlaylistClick(playlist) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GroupedTab(tracks: List<TrackEntity>, groupType: String, viewMode: LibraryViewMode, onPlayGroup: (List<TrackEntity>) -> Unit) {
    val grouped = remember(tracks) {
        when (groupType) {
            "Album" -> tracks.groupBy { it.displayAlbum }
            "Artist" -> tracks.groupBy { it.displayArtist }
            "Genre" -> tracks.groupBy { it.genre ?: "Unknown" }
            else -> emptyMap()
        }
    }
    if (grouped.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No $groupType found", color = Color.Gray) }
    else {
        if (viewMode == LibraryViewMode.GRID) {
            LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 100.dp), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(8.dp)) {
                grouped.forEach { (name, groupTracks) ->
                    item {
                        GroupGridItem(name, groupTracks, onPlayGroup)
                    }
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                grouped.forEach { (name, groupTracks) ->
                    item {
                        ListItem(
                            headlineContent = { Text(name, color = Color.White, fontWeight = FontWeight.Bold) },
                            supportingContent = { Text("${groupTracks.size} songs", color = Color.Gray) },
                            leadingContent = { Box(Modifier.size(48.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.05f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Album, null, tint = Color.Gray) } },
                            modifier = Modifier.clickable { onPlayGroup(groupTracks) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StellarTrackItem(track: TrackEntity, isSelected: Boolean, onPlay: () -> Unit, onLongClick: () -> Unit, onOptions: () -> Unit, onToggleFavorite: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().background(if (isSelected) Color.White.copy(alpha = 0.1f) else Color.Transparent).combinedClickable(onClick = onPlay, onLongClick = onLongClick, onDoubleClick = onToggleFavorite).padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box {
            val imageModel = remember(track.customCoverPath, track.localPath) {
                val file = track.customCoverPath?.let { File(it) }
                if (file != null && file.exists()) {
                    file
                } else {
                    track.localPath ?: R.drawable.ic_launcher_foreground
                }
            }
            AsyncImage(model = imageModel, contentDescription = null, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
            if (isSelected) Box(modifier = Modifier.size(48.dp).background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Check, null, tint = Color.White) }
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = track.displayName, style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Cursive, color = LavenderTitle, fontSize = 18.sp), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                // Red heart icon next to name
                IconButton(onClick = onToggleFavorite, modifier = Modifier.size(24.dp)) { 
                    Icon(if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, modifier = Modifier.size(16.dp), tint = if (track.isFavorite) Color.Red else Color.Gray) 
                }
            }
            Text(text = "${track.displayArtist} | ${formatDuration(track.duration)}", style = MaterialTheme.typography.labelSmall, color = Color.LightGray, maxLines = 1)
        }
        IconButton(onClick = onOptions) { Icon(Icons.Default.MoreVert, null, tint = Color.Gray, modifier = Modifier.size(20.dp)) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StellarGridItem(track: TrackEntity, isSelected: Boolean, onPlay: (TrackEntity) -> Unit, onLongClick: (TrackEntity) -> Unit, onToggleFavorite: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(4.dp)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(onClick = { onPlay(track) }, onLongClick = { onLongClick(track) }, onDoubleClick = onToggleFavorite)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            val imageModel = remember(track.customCoverPath, track.localPath) {
                val file = track.customCoverPath?.let { File(it) }
                if (file != null && file.exists()) file else track.localPath ?: R.drawable.ic_launcher_foreground
            }
            AsyncImage(
                model = imageModel, contentDescription = null,
                modifier = Modifier.aspectRatio(1f).fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            if (isSelected) Box(modifier = Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Check, null, tint = Color.White) }
            
            if (track.isFavorite) {
                Icon(Icons.Default.Favorite, null, tint = Color.Red, modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(16.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(text = track.displayName, color = LavenderTitle, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(text = track.displayArtist, color = Color.Gray, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

@Composable
fun PlaylistGridItem(playlist: com.example.musicon.data.local.Playlist, onClick: (com.example.musicon.data.local.Playlist) -> Unit) {
    Column(
        modifier = Modifier.padding(6.dp).clickable { onClick(playlist) },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(70.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(0.05f)), contentAlignment = Alignment.Center) {
            Icon(
                if (playlist.name == "Favorite") Icons.Default.Favorite else Icons.AutoMirrored.Filled.PlaylistAdd,
                null, tint = if (playlist.name == "Favorite") Color.Red else Color.Gray,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(playlist.name, color = Color.White, fontSize = 11.sp, maxLines = 1, textAlign = TextAlign.Center)
    }
}

@Composable
fun GroupGridItem(name: String, tracks: List<TrackEntity>, onClick: (List<TrackEntity>) -> Unit) {
    Column(
        modifier = Modifier.padding(6.dp).clickable { onClick(tracks) },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(70.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(0.05f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Album, null, tint = Color.Gray, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(name, color = Color.White, fontSize = 11.sp, maxLines = 1, textAlign = TextAlign.Center)
        Text("${tracks.size} songs", color = Color.Gray, fontSize = 9.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun StellarActionButton(label: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick, 
        modifier = modifier.height(38.dp), 
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
            contentColor = Color.Black
        ), 
        shape = RoundedCornerShape(22.dp)
    ) {
        Icon(icon, null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

fun formatDuration(millis: Long): String {
    val seconds = (millis / 1000) % 60
    val minutes = (millis / (1000 * 60)) % 60
    return String.format("%d:%02d", minutes, seconds)
}
